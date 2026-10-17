package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateDepositRequest(
    @Json(name = "user_id")
    val userId: Long,
    @Json(name = "amount_rub")
    val amountRub: Int,
    @Json(name = "promocode_id")
    val promocodeId: Long? = null
)

@JsonClass(generateAdapter = true)
data class CreateDepositResponse(
    @Json(name = "success")
    val success: Boolean? = null,
    @Json(name = "request_id")
    val requestId: Long? = null,
    @Json(name = "eskoins_amount")
    val eskoinsAmount: Int? = null,
    @Json(name = "final_amount_rub")
    val finalAmountRub: Int? = null,
    @Json(name = "error")
    val error: String? = null
)
