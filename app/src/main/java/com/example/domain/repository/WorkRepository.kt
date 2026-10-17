package com.example.domain.repository

import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkType
import kotlinx.coroutines.flow.Flow

interface WorkRepository {
    fun getAllWorks(): Flow<List<WorkDocument>>
    fun getWorksByType(type: WorkType): Flow<List<WorkDocument>>
    suspend fun getWorkById(id: Long): WorkDocument?
    suspend fun saveWork(document: WorkDocument): Long
    suspend fun updateDocxPath(workId: Long, path: String)
    suspend fun deleteWork(id: Long)
}
