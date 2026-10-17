package com.example.data.repository

import com.example.domain.model.WorkDocument
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.WorkRepository
import kotlinx.coroutines.flow.Flow

class HistoryRepositoryImpl(
    private val workRepository: WorkRepository
) : HistoryRepository {

    override fun getHistory(): Flow<List<WorkDocument>> = workRepository.getAllWorks()

    override suspend fun addToHistory(document: WorkDocument) {
        workRepository.saveWork(document)
    }

    override suspend fun clearHistory() {
        // no-op for safety
    }
}
