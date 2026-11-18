package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SifaloPayApi {
    @POST(".")
    suspend fun initiatePayment(
        @Header("Authorization") basicAuth: String,
        @Body payload: SifaloCheckoutRequest
    ): Response<SifaloCheckoutResponse>

    @POST("verify/")
    suspend fun verifyTransaction(
        @Header("Authorization") basicAuth: String,
        @Body payload: SifaloVerifyRequest
    ): Response<SifaloVerifyResponse>
}
