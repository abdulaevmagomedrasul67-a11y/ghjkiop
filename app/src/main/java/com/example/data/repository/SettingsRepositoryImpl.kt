package com.example.data.repository

import com.example.data.local.security.SessionManager
import com.example.data.remote.llm.VibeCodeApiClient
import com.example.domain.model.LlmSettings
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepositoryImpl(
    private val sessionManager: SessionManager
) : SettingsRepository {

    private val settingsState = MutableStateFlow(sessionManager.getLlmSettings())

    private val _endpointFlow = MutableStateFlow(settingsState.value.endpoint)
    override val endpointFlow: StateFlow<String> = _endpointFlow.asStateFlow()

    private val _apiKeyFlow = MutableStateFlow(settingsState.value.apiKey)
    override val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _modelFlow = MutableStateFlow(settingsState.value.model)
    override val modelFlow: StateFlow<String> = _modelFlow.asStateFlow()

    private val _timeoutFlow = MutableStateFlow(settingsState.value.timeout.toInt())
    override val timeoutFlow: StateFlow<Int> = _timeoutFlow.asStateFlow()

    override fun getLlmSettings(): Flow<LlmSettings> = settingsState.asStateFlow()

    override suspend fun saveLlmSettings(settings: LlmSettings) {
        val normalizedEndpoint = VibeCodeApiClient.normalizeEndpoint(settings.endpoint)
        val clampedTimeout = settings.timeout.coerceIn(10L, 300L)
        val normalized = settings.copy(
            endpoint = normalizedEndpoint,
            timeout = clampedTimeout
        )
        sessionManager.saveLlmSettings(normalized)
        settingsState.value = normalized
        _endpointFlow.value = normalized.endpoint
        _apiKeyFlow.value = normalized.apiKey
        _modelFlow.value = normalized.model
        _timeoutFlow.value = normalized.timeout.toInt()
    }

    override fun getEndpoint(): String = VibeCodeApiClient.normalizeEndpoint(settingsState.value.endpoint)

    override fun getApiKey(): String = settingsState.value.apiKey

    override fun getModel(): String = settingsState.value.model

    override fun getTimeout(): Int = settingsState.value.timeout.toInt().coerceIn(10, 300)

    override fun saveSettings(endpoint: String, apiKey: String, model: String, timeout: Int) {
        val normalizedEndpoint = VibeCodeApiClient.normalizeEndpoint(endpoint)
        val clampedTimeout = timeout.coerceIn(10, 300).toLong()
        val updated = settingsState.value.copy(
            endpoint = normalizedEndpoint,
            apiKey = apiKey.trim(),
            model = model.trim().ifEmpty { "vibe-code-pro-flash" },
            timeout = clampedTimeout
        )
        sessionManager.saveLlmSettings(updated)
        settingsState.value = updated
        _endpointFlow.value = updated.endpoint
        _apiKeyFlow.value = updated.apiKey
        _modelFlow.value = updated.model
        _timeoutFlow.value = updated.timeout.toInt()
    }
}
