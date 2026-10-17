package com.example.domain.usecase

import com.example.data.remote.dto.SupportTicketDto
import com.example.data.repository.SupportRepository
import com.example.domain.model.SupportCategory

class CreateSupportTicketUseCase(private val repository: SupportRepository) {
    suspend operator fun invoke(
        category: SupportCategory,
        subject: String,
        message: String,
        workId: Long? = null,
        contactEmail: String? = null
    ): Result<Long> {
        return repository.createTicket(
            category = category,
            subject = subject,
            message = message,
            workId = workId,
            contactEmail = contactEmail
        )
    }

    suspend operator fun invoke(
        category: String,
        subject: String,
        message: String,
        contact: String? = null,
        workId: Long? = null
    ): Result<Long> {
        val catEnum = SupportCategory.fromCode(category)
        return repository.createTicket(
            category = catEnum,
            subject = subject,
            message = message,
            workId = workId,
            contactEmail = contact
        )
    }
}

class GetMySupportTicketsUseCase(private val repository: SupportRepository) {
    suspend operator fun invoke(): Result<List<SupportTicketDto>> {
        return repository.getMyTickets()
    }
}
