package com.example.domain.repository

import com.example.domain.model.LlmSettings

interface LlmRepository {
    suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        settings: LlmSettings
    ): Result<String>
}
