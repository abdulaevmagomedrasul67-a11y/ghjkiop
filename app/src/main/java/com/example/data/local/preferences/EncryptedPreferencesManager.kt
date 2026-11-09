package com.example.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.utils.AppConstants
import com.example.domain.model.LlmSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Менеджер хранения конфигурации BYOM LLM (URL, ключ, модель, таймаут).
 */
class EncryptedPreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _endpointFlow = MutableStateFlow(getEndpoint())
    val endpointFlow: StateFlow<String> = _endpointFlow.asStateFlow()

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _modelFlow = MutableStateFlow(getModel())
    val modelFlow: StateFlow<String> = _modelFlow.asStateFlow()

    private val _timeoutFlow = MutableStateFlow(getTimeout())
    val timeoutFlow: StateFlow<Int> = _timeoutFlow.asStateFlow()

    fun getEndpoint(): String {
        return sharedPreferences.getString(
            AppConstants.KEY_ENDPOINT,
            DEFAULT_ENDPOINT
        ) ?: DEFAULT_ENDPOINT
    }

    fun getApiKey(): String {
        return sharedPreferences.getString(AppConstants.KEY_API_KEY, "") ?: ""
    }

    fun getModel(): String {
        return sharedPreferences.getString(
            AppConstants.KEY_MODEL,
            DEFAULT_MODEL
        ) ?: DEFAULT_MODEL
    }

    fun getTimeout(): Int {
        return sharedPreferences.getInt(AppConstants.KEY_TIMEOUT, 120)
    }

    fun saveSettings(endpoint: String, apiKey: String, model: String, timeout: Int = 120) {
        val cleanEndpoint = endpoint.trim().removeSuffix("/")
        val cleanKey = apiKey.trim()
        val cleanModel = model.trim().ifEmpty { DEFAULT_MODEL }

        Log.d("SETTINGS", "Saving timeout: $timeout")

        sharedPreferences.edit()
            .putString(AppConstants.KEY_ENDPOINT, cleanEndpoint)
            .putString(AppConstants.KEY_API_KEY, cleanKey)
            .putString(AppConstants.KEY_MODEL, cleanModel)
            .putInt(AppConstants.KEY_TIMEOUT, timeout)
            .apply()

        _endpointFlow.value = cleanEndpoint
        _apiKeyFlow.value = cleanKey
        _modelFlow.value = cleanModel
        _timeoutFlow.value = timeout
    }

    fun saveLlmSettings(settings: LlmSettings) {
        saveSettings(
            endpoint = settings.endpoint,
            apiKey = settings.apiKey,
            model = settings.model,
            timeout = settings.timeout.toInt()
        )
    }

    fun loadLlmSettings(): LlmSettings {
        return LlmSettings(
            endpoint = getEndpoint(),
            apiKey = getApiKey(),
            model = getModel(),
            timeout = getTimeout().toLong()
        )
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "gpt-4o"
    }
}
