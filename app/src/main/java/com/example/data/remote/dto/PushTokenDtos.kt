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
data class RegisterPushTokenResponse(
    @Json(name = "success") val success: Boolean? = true,
    @Json(name = "status") val status: String? = "ok",
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class UnregisterPushTokenRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "push_token") val pushToken: String? = null
)

@JsonClass(generateAdapter = true)
data class UnregisterPushTokenResponse(
    @Json(name = "success") val success: Boolean? = true,
    @Json(name = "status") val status: String? = "ok",
    @Json(name = "error") val error: String? = null
)
