package com.example.data.repository

import com.example.data.remote.llm.VibeCodeApiClient
import com.example.domain.model.GostProfile
import com.example.domain.model.LlmSettings
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkRequest
import com.example.domain.repository.LlmChatOutput
import com.example.domain.repository.LlmRepository

class LlmRepositoryImpl(
    private val apiClient: VibeCodeApiClient = VibeCodeApiClient()
) : LlmRepository {

    private val delegate = com.example.data.remote.LlmRepositoryImpl()

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

    override suspend fun executeRawChat(
        systemPrompt: String,
        userPrompt: String,
        endpoint: String,
        apiKey: String,
        model: String,
        maxTokens: Int?,
        temperature: Double
    ): Result<LlmChatOutput> {
        return apiClient.executeChatWithLengthRetry(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            endpoint = endpoint,
            apiKey = apiKey,
            model = model,
            targetMaxTokens = maxTokens ?: 4000,
            temperature = temperature
        )
    }

    override suspend fun generateAcademicDocument(
        request: WorkRequest,
        profile: GostProfile,
        endpoint: String,
        apiKey: String,
        model: String
    ): Result<WorkDocument> {
        return delegate.generateAcademicDocument(request, profile, endpoint, apiKey, model)
    }
}
