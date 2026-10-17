package com.example.data.repository

import com.example.data.remote.llm.VibeCodeApiClient
import com.example.domain.model.LlmSettings
import com.example.domain.repository.LlmRepository

class LlmRepositoryImpl(
    private val apiClient: VibeCodeApiClient = VibeCodeApiClient()
) : LlmRepository {

    override suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        settings: LlmSettings
    ): Result<String> {
        return apiClient.generateText(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            settings = settings
        )
    }
}
