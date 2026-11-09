package com.example.domain.usecase

import com.example.data.repository.SupportRepository
import com.example.domain.model.SupportCategory

class CreateSupportTicketUseCase(
    private val repository: SupportRepository
) {
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
        contact: String?,
        workId: Long?
    ): Result<Long> {
        return repository.createTicket(
            category = SupportCategory.fromCode(category),
            subject = subject,
            message = message,
            workId = workId,
            contactEmail = contact
        )
    }
}
