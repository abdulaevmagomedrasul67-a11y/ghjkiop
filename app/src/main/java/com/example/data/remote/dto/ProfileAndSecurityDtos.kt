package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "university") val university: String? = null,
    @Json(name = "faculty") val faculty: String? = null,
    @Json(name = "coin_balance") val coinBalance: Int = 0
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "university") val university: String? = null
)

@JsonClass(generateAdapter = true)
data class BaseApiResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
) {
    val success: Boolean get() = status == "success" || error == null
}

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class ChangePasswordResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)
