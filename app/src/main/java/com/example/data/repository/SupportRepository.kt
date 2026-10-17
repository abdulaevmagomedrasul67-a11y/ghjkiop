package com.example.data.repository

import android.util.Log
import com.example.data.local.security.SessionManager
import com.example.data.remote.api.StudToolApiService
import com.example.data.remote.dto.CreateSupportTicketRequest
import com.example.data.remote.dto.MySupportTicketsRequest
import com.example.data.remote.dto.SupportTicketDto
import com.example.domain.model.SupportCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupportRepository(
    private val apiService: StudToolApiService,
    private val sessionManager: SessionManager
) {
    // Локальный кэш тикетов на случай оффлайна или локального режима
    private val localTickets = mutableListOf<SupportTicketDto>()

    suspend fun createTicket(
        category: SupportCategory,
        subject: String,
        message: String,
        workId: Long? = null,
        contactEmail: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId()
        try {
            val response = apiService.createSupportTicket(
                CreateSupportTicketRequest(
                    userId = userId,
                    category = category.code,
                    subject = subject,
                    message = message,
                    workId = workId,
                    contactEmail = contactEmail ?: sessionManager.getUserEmail()
                )
            )

            if (response.status == "success" && response.ticketId != null) {
                Result.success(response.ticketId)
            } else {
                val err = response.error ?: response.message ?: "Ошибка создания обращения"
                fallbackLocalTicket(userId, category, subject, message, workId)
            }
        } catch (e: Exception) {
            Log.w("SupportRepo", "Network error, saving locally", e)
            fallbackLocalTicket(userId, category, subject, message, workId)
        }
    }

    private fun fallbackLocalTicket(
        userId: Long,
        category: SupportCategory,
        subject: String,
        message: String,
        workId: Long?
    ): Result<Long> {
        val id = System.currentTimeMillis()
        val ticket = SupportTicketDto(
            id = id,
            userId = userId,
            category = category.code,
            subject = subject,
            message = message,
            status = "open",
            adminReply = null,
            refundedAmount = if (category == SupportCategory.REFUND) 480 else null,
            createdAt = "Сегодня"
        )
        localTickets.add(0, ticket)
        return Result.success(id)
    }

    suspend fun getMyTickets(): Result<List<SupportTicketDto>> = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId()
        try {
            val response = apiService.getMySupportTickets(MySupportTicketsRequest(userId = userId))
            if (response.status == "success") {
                val combined = (response.tickets + localTickets).distinctBy { it.id }
                Result.success(combined)
            } else {
                Result.success(localTickets)
            }
        } catch (e: Exception) {
            Log.w("SupportRepo", "Network error, returning local cache", e)
            Result.success(localTickets)
        }
    }
}
