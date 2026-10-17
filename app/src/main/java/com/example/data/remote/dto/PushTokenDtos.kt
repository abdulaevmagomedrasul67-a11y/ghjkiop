package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterPushTokenRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "push_token") val pushToken: String,
    @Json(name = "device_type") val deviceType: String = "android"
)

@JsonClass(generateAdapter = true)
data class UnregisterPushTokenRequest(
    @Json(name = "user_id") val userId: Long
)

@JsonClass(generateAdapter = true)
data class PushTokenResponse(
    @Json(name = "success") val success: Boolean? = true,
    @Json(name = "error") val error: String? = null
)
