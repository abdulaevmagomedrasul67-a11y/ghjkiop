package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetUserRequest(
    @Json(name = "user_id")
    val userId: Long
)

@JsonClass(generateAdapter = true)
data class GetUserResponse(
    @Json(name = "user_id")
    val userId: Long? = null,
    @Json(name = "full_name")
    val fullName: String? = null,
    @Json(name = "email")
    val email: String? = null,
    @Json(name = "eskoins")
    val eskoins: Int? = null,
    @Json(name = "is_banned")
    val isBanned: Boolean? = null,
    @Json(name = "error")
    val error: String? = null
)
