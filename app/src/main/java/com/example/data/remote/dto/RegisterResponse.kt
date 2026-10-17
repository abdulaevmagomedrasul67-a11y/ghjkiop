package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @Json(name = "success")
    val success: Boolean? = null,
    @Json(name = "user_id")
    val userId: Long? = null,
    @Json(name = "eskoins")
    val eskoins: Int? = null,
    @Json(name = "error")
    val error: String? = null
)
