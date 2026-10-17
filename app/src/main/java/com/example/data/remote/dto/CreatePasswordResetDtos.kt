package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreatePasswordResetRequest(
    @Json(name = "email")
    val email: String,
    @Json(name = "whatsapp")
    val whatsapp: String,
    @Json(name = "problem")
    val problem: String,
    @Json(name = "action")
    val action: String = "create_password_reset"
)

@JsonClass(generateAdapter = true)
data class CreatePasswordResetResponse(
    @Json(name = "success")
    val success: Boolean = false,
    @Json(name = "message")
    val message: String? = null,
    @Json(name = "error")
    val error: String? = null
)
