package com.example.domain.repository

import com.example.domain.model.WorkDocument
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistory(): Flow<List<WorkDocument>>
    suspend fun addToHistory(document: WorkDocument)
    suspend fun clearHistory()
}
