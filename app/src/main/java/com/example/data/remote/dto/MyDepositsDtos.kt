package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MyDepositsRequest(
    @Json(name = "user_id")
    val userId: Long,
    @Json(name = "action")
    val action: String = "my_deposits"
)

@JsonClass(generateAdapter = true)
data class DepositRequestItemDto(
    @Json(name = "id")
    val id: Long,
    @Json(name = "amount_rub")
    val amountRub: String,
    @Json(name = "eskoins_amount")
    val eskoinsAmount: Int = 0,
    @Json(name = "discount_percent")
    val discountPercent: Int = 0,
    @Json(name = "final_amount_rub")
    val finalAmountRub: String? = null,
    @Json(name = "status")
    val status: String, // pending, approved, rejected
    @Json(name = "admin_comment")
    val adminComment: String? = null,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "processed_at")
    val processedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MyDepositsResponse(
    @Json(name = "requests")
    val requests: List<DepositRequestItemDto> = emptyList(),
    @Json(name = "success")
    val success: Boolean? = true,
    @Json(name = "error")
    val error: String? = null
)
