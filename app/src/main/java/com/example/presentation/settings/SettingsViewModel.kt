package com.example.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.security.SessionManager
import com.example.data.local.settings.AppSettings
import com.example.data.remote.LlmConnectionChecker
import com.example.data.remote.llm.VibeCodeApiClient
import com.example.domain.model.LlmSettings
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val endpoint: String = "",
    val apiKey: String = "",
    val model: String = "",
    val timeout: Int = 120, // -1 = без лимита (до 300с)
    val language: String = "Русский",
    val isTesting: Boolean = false,
    val testMessage: String? = null,
    val isTestSuccess: Boolean? = null,
    val isSavedSuccess: Boolean = false
)

enum class LlmPreset(
    val title: String,
    val endpoint: String,
    val defaultModel: String,
    val hint: String
) {
    OPEN_ROUTER(
        title = "OpenRouter",
        endpoint = "https://openrouter.ai/api/v1",
        defaultModel = "openai/gpt-4o",
        hint = "Доступ к 200+ моделям по одному ключу"
    ),
    OPEN_AI(
        title = "OpenAI",
        endpoint = "https://api.openai.com/v1",
        defaultModel = "gpt-4o",
        hint = "Официальный OpenAI API"
    ),
    DEEPSEEK(
        title = "DeepSeek",
        endpoint = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        hint = "Быстрая и доступная модель"
    ),
    GROQ(
        title = "Groq",
        endpoint = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        hint = "Сверхбыстрая генерация Llama"
    ),
    LOCAL_OLLAMA(
        title = "Локальный (Ollama)",
        endpoint = "http://10.0.2.2:11434/v1",
        defaultModel = "llama3",
        hint = "Локальная модель без ключа"
    )
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager? = null,
    private val appSettings: AppSettings? = null,
    private val connectionChecker: LlmConnectionChecker = LlmConnectionChecker()
) : ViewModel() {

    private val initialTimeout = sessionManager?.loadLlmSettings()?.timeout?.toInt() ?: 120
    private val initialLanguage = if (appSettings?.getLanguage() == "en") "English" else "Русский"

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            timeout = initialTimeout,
            language = initialLanguage
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val endpoint = settingsRepository.getEndpoint()
            val apiKey = settingsRepository.getApiKey()
            val model = settingsRepository.getModel()
            val savedSession = sessionManager?.loadLlmSettings()
            val timeout = savedSession?.timeout?.toInt() ?: settingsRepository.getTimeout()
            val langCode = appSettings?.getLanguage() ?: "ru"
            val langName = if (langCode == "en") "English" else "Русский"

            _uiState.update { it.copy(
                endpoint = endpoint,
                apiKey = apiKey,
                model = model,
                timeout = timeout,
                language = langName
            ) }
        }
    }

    fun onEndpointChanged(newEndpoint: String) {
        _uiState.update { it.copy(
            endpoint = newEndpoint,
            isSavedSuccess = false,
            testMessage = null
        ) }
    }

    fun onApiKeyChanged(newKey: String) {
        _uiState.update { it.copy(
            apiKey = newKey,
            isSavedSuccess = false,
            testMessage = null
        ) }
    }

    fun onModelChanged(newModel: String) {
        _uiState.update { it.copy(
            model = newModel,
            isSavedSuccess = false,
            testMessage = null
        ) }
    }

    fun updateTimeout(seconds: Int) {
        _uiState.update { it.copy(
            timeout = seconds,
            isSavedSuccess = false
        ) }
    }

    fun onLanguageChanged(newLanguage: String) {
        _uiState.update { it.copy(
            language = newLanguage,
            isSavedSuccess = false
        ) }
        val code = if (newLanguage == "English") "en" else "ru"
        appSettings?.setLanguage(code)
    }

    fun applyPreset(preset: LlmPreset) {
        _uiState.update { it.copy(
            endpoint = preset.endpoint,
            model = preset.defaultModel,
            isSavedSuccess = false,
            testMessage = null
        ) }
    }

    fun testConnection() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(
                isTesting = true,
                testMessage = null,
                isTestSuccess = null
            ) }

            val result = connectionChecker.testConnection(
                endpoint = state.endpoint,
                apiKey = state.apiKey,
                model = state.model
            )

            when (result) {
                is LlmConnectionChecker.CheckResult.Success -> {
                    _uiState.update { it.copy(
                        isTesting = false,
                        isTestSuccess = true,
                        testMessage = result.message
                    ) }
                }
                is LlmConnectionChecker.CheckResult.Error -> {
                    _uiState.update { it.copy(
                        isTesting = false,
                        isTestSuccess = false,
                        testMessage = result.message
                    ) }
                }
            }
        }
    }

    suspend fun saveSettingsSync() {
        val current = _uiState.value
        val normalizedEndpoint = VibeCodeApiClient.normalizeEndpoint(current.endpoint)
        val resolvedTimeout = if (current.timeout <= 0) 300 else current.timeout.coerceIn(30, 300)

        sessionManager?.saveLlmSettings(
            LlmSettings(
                endpoint = normalizedEndpoint,
                apiKey = current.apiKey,
                model = current.model,
                timeout = resolvedTimeout.toLong()
            )
        )
        val langCode = if (current.language == "English") "en" else "ru"
        appSettings?.setLanguage(langCode)

        settingsRepository.saveSettings(
            endpoint = normalizedEndpoint,
            apiKey = current.apiKey,
            model = current.model,
            timeout = resolvedTimeout
        )
        _uiState.update { it.copy(endpoint = normalizedEndpoint, isSavedSuccess = true) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            saveSettingsSync()
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val sessionManager: SessionManager? = null,
        private val appSettings: AppSettings? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, sessionManager, appSettings) as T
        }
    }
}
