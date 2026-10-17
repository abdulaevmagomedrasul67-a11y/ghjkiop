package com.example.data.repository

import com.example.data.local.security.SessionManager
import com.example.domain.model.LlmSettings
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepositoryImpl(
    private val sessionManager: SessionManager
) : SettingsRepository {

    private val settingsFlow = MutableStateFlow(sessionManager.getLlmSettings())
    private val _themeMode = MutableStateFlow(sessionManager.getThemeMode())
    private val _appLanguage = MutableStateFlow(sessionManager.getAppLanguage())
    private val _hasSeenTour = MutableStateFlow(sessionManager.hasSeenTour())

    override val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    override val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()
    override val hasSeenTour: StateFlow<Boolean> = _hasSeenTour.asStateFlow()

    override fun getLlmSettings(): Flow<LlmSettings> = settingsFlow.asStateFlow()

    override suspend fun saveLlmSettings(settings: LlmSettings) {
        sessionManager.saveLlmSettings(settings)
        settingsFlow.value = settings
    }

    override fun setThemeMode(mode: String) {
        sessionManager.saveThemeMode(mode)
        _themeMode.value = mode
    }

    override fun setAppLanguage(lang: String) {
        sessionManager.saveAppLanguage(lang)
        _appLanguage.value = lang
    }

    override fun setHasSeenTour(seen: Boolean) {
        sessionManager.setHasSeenTour(seen)
        _hasSeenTour.value = seen
    }
}
