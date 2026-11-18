package com.example.data.remote

data class SifaloCheckoutRequest(
    val account: String,
    val gateway: String,
    val amount: Double,
    val currency: String = "USD",
    val order_id: String,
    val return_url: String
)

data class SifaloCheckoutResponse(
    val code: String? = null,
    val status: String? = null,
    val sid: String? = null,
    val order_id: String? = null,
    val payment_url: String? = null,
    val response: String? = null,
    val message: String? = null
)

data class SifaloVerifyRequest(
    val sid: String,
    val order_id: String
)

data class SifaloVerifyResponse(
    val code: String? = null,
    val status: String? = null,
    val sid: String? = null,
    val order_id: String? = null,
    val response: String? = null,
    val message: String? = null
)
