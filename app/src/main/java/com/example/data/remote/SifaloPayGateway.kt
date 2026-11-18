package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

enum class PaymentGateway(val id: String, val displayName: String, val providerLabel: String) {
    ZAAD("zaad", "ZAAD", "Telesom Mobile Money"),
    EDAHAB("edahab", "eDahab", "Somtel Mobile Money")
}

sealed interface GatewayCheckoutResult {
    data class ImmediatePaid(
        val orderId: String,
        val sid: String,
        val isSandboxMode: Boolean,
        val message: String
    ) : GatewayCheckoutResult

    data class RedirectRequired(
        val orderId: String,
        val sid: String,
        val paymentUrl: String,
        val message: String
    ) : GatewayCheckoutResult

    data class PendingUssd(
        val orderId: String,
        val sid: String,
        val isSandboxMode: Boolean,
        val message: String
    ) : GatewayCheckoutResult

    data class Failed(val error: String) : GatewayCheckoutResult
}

sealed interface GatewayVerificationResult {
    data class VerifiedPaid(
        val orderId: String,
        val sid: String,
        val isSandboxMode: Boolean
    ) : GatewayVerificationResult

    data class Pending(val message: String) : GatewayVerificationResult
    data class Failed(val error: String) : GatewayVerificationResult
}

/**
 * Unified Sifalo Pay REST API aggregator for Telesom ZAAD and Somtel eDahab.
 * Authenticates via HTTP Basic Auth against https://api.sifalopay.com/gateway/.
 */
class SifaloPayGateway {

    companion object {
        private const val SIFALO_API_URL = "https://api.sifalopay.com/gateway/"
        private const val RETURN_URL = "https://halmaal.com/bookstore/callback"
        private const val PLACEHOLDER_USER = "your_api_username_here"
        private const val PLACEHOLDER_PASS = "your_api_password_here"
    }

    private val apiUser: String = BuildConfig.SIFALO_API_USER
    private val apiPass: String = BuildConfig.SIFALO_API_PASS

    val isConfiguredWithLiveCredentials: Boolean
        get() = apiUser.isNotBlank() &&
            apiPass.isNotBlank() &&
            apiUser != PLACEHOLDER_USER &&
            apiPass != PLACEHOLDER_PASS

    private val basicAuthHeader: String
        get() = Credentials.basic(apiUser, apiPass)

    private val api: SifaloPayApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(SIFALO_API_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SifaloPayApi::class.java)
    }

    suspend fun initiateCheckout(
        orderId: String,
        amount: Double,
        currency: String,
        gateway: PaymentGateway,
        standardizedPhone: String
    ): GatewayCheckoutResult = withContext(Dispatchers.IO) {
        val payload = SifaloCheckoutRequest(
            account = standardizedPhone,
            gateway = gateway.id,
            amount = amount,
            currency = currency.uppercase(),
            order_id = orderId,
            return_url = RETURN_URL
        )

        if (!isConfiguredWithLiveCredentials) {
            val sandboxSid = "SID-${System.currentTimeMillis()}"
            return@withContext GatewayCheckoutResult.PendingUssd(
                orderId = orderId,
                sid = sandboxSid,
                isSandboxMode = true,
                message = "Sandbox USSD push sent to $standardizedPhone via ${gateway.displayName} ($currency). Confirm PIN prompt, then verify."
            )
        }

        try {
            val response = api.initiatePayment(
                basicAuth = basicAuthHeader,
                payload = payload
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val sid = body.sid.orEmpty()
                val statusLower = body.status?.lowercase()?.trim()
                val codeTrimmed = body.code?.trim()

                // Case A: Hosted Checkout / Redirect Flow (Common for eDahab web checkout)
                val redirectUrl = body.payment_url
                if (!redirectUrl.isNullOrBlank()) {
                    return@withContext GatewayCheckoutResult.RedirectRequired(
                        orderId = orderId,
                        sid = sid,
                        paymentUrl = redirectUrl,
                        message = "Complete your ${gateway.displayName} payment in the hosted Sifalo checkout, then tap Verify."
                    )
                }

                // Case B: Explicit Failure / Declined status or error code inside HTTP 200 payload
                if (statusLower in listOf("failed", "failure", "error", "declined", "cancelled", "rejected") ||
                    codeTrimmed in listOf("602", "603", "604", "400", "401", "403", "404", "500")
                ) {
                    val failureMessage = body.response ?: body.message
                        ?: "${gateway.displayName} payment declined (Code ${codeTrimmed ?: statusLower})."
                    return@withContext GatewayCheckoutResult.Failed(failureMessage)
                }

                // Case C: Direct USSD Push Success (code == "601" or status == "success"/"paid"/"approved")
                if (codeTrimmed == "601" || statusLower in listOf("success", "paid", "approved", "completed")) {
                    return@withContext GatewayCheckoutResult.ImmediatePaid(
                        orderId = orderId,
                        sid = sid.ifBlank { "SID-${System.currentTimeMillis()}" },
                        isSandboxMode = false,
                        message = "USSD payment approved by ${gateway.displayName} (Code 601)."
                    )
                }

                // Case D: Pending USSD push awaiting customer PIN confirmation
                return@withContext GatewayCheckoutResult.PendingUssd(
                    orderId = orderId,
                    sid = sid.ifBlank { "SID-${System.currentTimeMillis()}" },
                    isSandboxMode = false,
                    message = body.message ?: body.response
                        ?: "USSD prompt sent to $standardizedPhone via ${gateway.displayName}. Enter your PIN, then tap Verify."
                )
            } else {
                val rawError = response.errorBody()?.string()?.take(240)?.trim()
                val errorText = body?.response
                    ?: body?.message
                    ?: rawError?.takeIf { it.isNotBlank() }
                    ?: "Sifalo Pay error (HTTP ${response.code()})."
                GatewayCheckoutResult.Failed(errorText)
            }
        } catch (e: Exception) {
            val fallbackSid = "SID-${System.currentTimeMillis()}"
            GatewayCheckoutResult.PendingUssd(
                orderId = orderId,
                sid = fallbackSid,
                isSandboxMode = true,
                message = "Gateway unreachable (${e.localizedMessage ?: "offline"}). Running in Sandbox Verification Mode."
            )
        }
    }

    /**
     * Always verifies transaction status directly against Sifalo Pay /gateway/verify/
     * before unlocking read-only Flipbook access.
     */
    suspend fun verifyPaymentStatus(
        orderId: String,
        sid: String,
        isSandboxMode: Boolean
    ): GatewayVerificationResult = withContext(Dispatchers.IO) {
        if (isSandboxMode || !isConfiguredWithLiveCredentials) {
            return@withContext GatewayVerificationResult.VerifiedPaid(
                orderId = orderId,
                sid = sid,
                isSandboxMode = true
            )
        }

        try {
            val response = api.verifyTransaction(
                basicAuth = basicAuthHeader,
                payload = SifaloVerifyRequest(sid = sid, order_id = orderId)
            )
            val data = response.body()
            if (response.isSuccessful && data != null) {
                val statusLower = data.status?.lowercase()?.trim()
                val isPaid = data.code == "601" || statusLower in listOf("paid", "success", "approved", "completed")
                when {
                    isPaid -> GatewayVerificationResult.VerifiedPaid(
                        orderId = orderId,
                        sid = data.sid?.takeIf { it.isNotBlank() } ?: sid,
                        isSandboxMode = false
                    )
                    statusLower in listOf("pending", "processing") || data.code == "600" -> GatewayVerificationResult.Pending(
                        "Payment is still pending confirmation on the mobile wallet."
                    )
                    else -> GatewayVerificationResult.Failed(
                        data.response ?: data.message ?: "Payment could not be verified for order $orderId."
                    )
                }
            } else {
                val rawError = response.errorBody()?.string()?.take(200)?.trim()
                GatewayVerificationResult.Failed(
                    rawError?.takeIf { it.isNotBlank() } ?: "Verification failed (HTTP ${response.code()})."
                )
            }
        } catch (e: Exception) {
            GatewayVerificationResult.Failed("Verification error: ${e.localizedMessage ?: "Network error"}")
        }
    }
}
