package com.example.domain.repository

import com.example.domain.model.LlmSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    fun getLlmSettings(): Flow<LlmSettings>
    suspend fun saveLlmSettings(settings: LlmSettings)

    val themeMode: StateFlow<String>
    fun setThemeMode(mode: String)

    val appLanguage: StateFlow<String>
    fun setAppLanguage(lang: String)

    val hasSeenTour: StateFlow<Boolean>
    fun setHasSeenTour(seen: Boolean)
}
