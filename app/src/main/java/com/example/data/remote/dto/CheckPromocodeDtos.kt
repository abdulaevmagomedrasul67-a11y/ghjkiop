package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckPromocodeRequest(
    @Json(name = "code")
    val code: String,
    @Json(name = "user_id")
    val userId: Long,
    @Json(name = "action")
    val action: String = "check_promocode"
)

@JsonClass(generateAdapter = true)
data class CheckPromocodeResponse(
    @Json(name = "valid")
    val valid: Boolean = false,
    @Json(name = "discount_percent")
    val discountPercent: Int = 0,
    @Json(name = "promocode_id")
    val promocodeId: Long? = null,
    @Json(name = "error")
    val error: String? = null
)
