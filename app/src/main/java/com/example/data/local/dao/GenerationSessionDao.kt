package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.GenerationSession
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationSessionDao {

    @Query("SELECT * FROM generation_sessions ORDER BY rowid DESC LIMIT 1")
    suspend fun getLatestSession(): GenerationSession?

    @Query("SELECT * FROM generation_sessions ORDER BY rowid DESC LIMIT 1")
    fun observeLatestSession(): Flow<GenerationSession?>

    @Query("SELECT * FROM generation_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): GenerationSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: GenerationSession)

    @Query("DELETE FROM generation_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM generation_sessions")
    suspend fun clearAllSessions()
}
