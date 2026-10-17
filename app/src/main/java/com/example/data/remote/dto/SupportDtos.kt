package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateSupportTicketRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "category") val category: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "message") val message: String,
    @Json(name = "work_id") val workId: Long? = null,
    @Json(name = "contact_email") val contactEmail: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateSupportTicketResponse(
    @Json(name = "status") val status: String,
    @Json(name = "ticket_id") val ticketId: Long? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class MySupportTicketsRequest(
    @Json(name = "user_id") val userId: Long
)

@JsonClass(generateAdapter = true)
data class SupportTicketDto(
    @Json(name = "id") val id: Long,
    @Json(name = "user_id") val userId: Long,
    @Json(name = "category") val category: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "message") val message: String,
    @Json(name = "status") val status: String, // open, in_progress, resolved, rejected
    @Json(name = "admin_reply") val adminReply: String? = null,
    @Json(name = "refunded_amount") val refundedAmount: Int? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "work_id") val workId: Long? = null,
    @Json(name = "processed_at") val processedAt: String? = null
) {
    val refundEskoins: Int get() = refundedAmount ?: 0
}

@JsonClass(generateAdapter = true)
data class MySupportTicketsResponse(
    @Json(name = "status") val status: String,
    @Json(name = "tickets") val tickets: List<SupportTicketDto> = emptyList(),
    @Json(name = "error") val error: String? = null
)
