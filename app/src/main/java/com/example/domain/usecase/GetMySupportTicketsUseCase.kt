package com.example.domain.usecase

import com.example.data.remote.dto.SupportTicketDto
import com.example.data.repository.SupportRepository

class GetMySupportTicketsUseCase(
    private val repository: SupportRepository
) {
    suspend operator fun invoke(): Result<List<SupportTicketDto>> {
        return repository.getMyTickets()
    }
}
