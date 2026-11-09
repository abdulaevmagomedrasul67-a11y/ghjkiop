package com.example.domain.repository

import com.example.domain.model.LlmSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val endpointFlow: StateFlow<String>
    val apiKeyFlow: StateFlow<String>
    val modelFlow: StateFlow<String>
    val timeoutFlow: StateFlow<Int>

    fun getLlmSettings(): Flow<LlmSettings>
    suspend fun saveLlmSettings(settings: LlmSettings)

    fun getEndpoint(): String
    fun getApiKey(): String
    fun getModel(): String
    fun getTimeout(): Int
    fun saveSettings(endpoint: String, apiKey: String, model: String, timeout: Int = 180)
}
