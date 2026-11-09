package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 4.5. Room-сущность промежуточного сохранения прогресса генерации длинной работы.
 * При обрыве или ошибке позволяет продолжить генерацию с этапа currentStage.
 */
@Entity(tableName = "generation_sessions")
data class GenerationSession(
    @PrimaryKey
    val id: String,
    val workId: Int,
    val currentStage: Int,
    val totalStages: Int,
    val partialData: String
)
