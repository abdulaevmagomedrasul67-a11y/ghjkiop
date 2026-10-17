package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckDeviceRequest(
    @Json(name = "android_id")
    val androidId: String
)

@JsonClass(generateAdapter = true)
data class CheckDeviceResponse(
    @Json(name = "registered")
    val registered: Boolean = false,
    @Json(name = "user_id")
    val userId: Long? = null,
    @Json(name = "error")
    val error: String? = null
)
