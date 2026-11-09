package com.example.domain.repository

import com.example.domain.model.GostProfile
import com.example.domain.model.LlmSettings
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkRequest

data class LlmChatOutput(
    val content: String,
    val finishReason: String? = null
)

interface LlmRepository {
    suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        settings: LlmSettings
    ): Result<String>

    suspend fun executeRawChat(
        systemPrompt: String,
        userPrompt: String,
        endpoint: String,
        apiKey: String,
        model: String,
        maxTokens: Int? = 4000,
        temperature: Double = 0.7
    ): Result<LlmChatOutput> {
        return generateCompletion(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            settings = LlmSettings(endpoint = endpoint, apiKey = apiKey, model = model)
        ).map { LlmChatOutput(content = it, finishReason = "stop") }
    }

    suspend fun generateAcademicDocument(
        request: WorkRequest,
        profile: GostProfile,
        endpoint: String,
        apiKey: String,
        model: String
    ): Result<WorkDocument> {
        return Result.failure(UnsupportedOperationException("Not implemented in test double"))
    }
}
