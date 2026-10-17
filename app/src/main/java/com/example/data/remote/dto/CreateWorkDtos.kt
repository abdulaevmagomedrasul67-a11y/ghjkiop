package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateWorkRequest(
    @Json(name = "user_id")
    val userId: Long,
    @Json(name = "work_type")
    val workType: String,
    @Json(name = "topic")
    val topic: String,
    @Json(name = "subject")
    val subject: String? = null,
    @Json(name = "pages_target")
    val pagesTarget: Int = 15,
    @Json(name = "language")
    val language: String = "ru"
)

@JsonClass(generateAdapter = true)
data class CreateWorkResponse(
    @Json(name = "success")
    val success: Boolean = false,
    @Json(name = "work_id")
    val workId: Long? = null,
    @Json(name = "charged")
    val charged: Int? = null,
    @Json(name = "new_balance")
    val newBalance: Int? = null,
    @Json(name = "error")
    val error: String? = null,
    @Json(name = "required")
    val required: Int? = null,
    @Json(name = "balance")
    val balance: Int? = null
)
