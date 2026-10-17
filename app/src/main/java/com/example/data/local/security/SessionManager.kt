package com.example.data.local.security

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.LlmSettings

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("studtool_prefs", Context.MODE_PRIVATE)

    fun saveUserId(id: Long) {
        prefs.edit().putLong(KEY_USER_ID, id).apply()
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, 1L)
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, "demo_token_studtool")
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "student@university.ru") ?: "student@university.ru"
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Али Абдулаев") ?: "Али Абдулаев"
    }

    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserUniversity(): String {
        return prefs.getString(KEY_USER_UNIVERSITY, "МГТУ им. Н.Э. Баумана") ?: "МГТУ им. Н.Э. Баумана"
    }

    fun saveUserUniversity(university: String) {
        prefs.edit().putString(KEY_USER_UNIVERSITY, university).apply()
    }

    fun getCoinBalance(): Int {
        return prefs.getInt(KEY_COIN_BALANCE, 1500)
    }

    fun saveCoinBalance(coins: Int) {
        prefs.edit().putInt(KEY_COIN_BALANCE, coins).apply()
    }

    fun deductCoins(amount: Int): Boolean {
        val current = getCoinBalance()
        if (current >= amount) {
            saveCoinBalance(current - amount)
            return true
        }
        return false
    }

    fun addCoins(amount: Int) {
        saveCoinBalance(getCoinBalance() + amount)
    }

    fun isLoggedIn(): Boolean {
        return getUserId() > 0
    }

    fun logout() {
        val seenTour = hasSeenTour()
        val theme = getThemeMode()
        val lang = getAppLanguage()
        prefs.edit().clear()
            .putBoolean(KEY_HAS_SEEN_TOUR, seenTour)
            .putString(KEY_THEME_MODE, theme)
            .putString(KEY_APP_LANGUAGE, lang)
            .putLong(KEY_USER_ID, 0L)
            .apply()
    }

    fun saveLlmSettings(settings: LlmSettings) {
        prefs.edit()
            .putString(KEY_ENDPOINT, settings.endpoint)
            .putString(KEY_API_KEY, settings.apiKey)
            .putString(KEY_MODEL, settings.model)
            .putLong(KEY_TIMEOUT, settings.timeout)
            .putFloat(KEY_TEMPERATURE, settings.temperature.toFloat())
            .putFloat(KEY_TOP_P, settings.topP.toFloat())
            .apply()
    }

    fun getLlmSettings(): LlmSettings {
        return LlmSettings(
            endpoint = prefs.getString(KEY_ENDPOINT, "https://api.vibecode.online/v1/chat/completions")
                ?: "https://api.vibecode.online/v1/chat/completions",
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, "vibe-code-pro-flash") ?: "vibe-code-pro-flash",
            timeout = prefs.getLong(KEY_TIMEOUT, 90L),
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.65f).toDouble(),
            topP = prefs.getFloat(KEY_TOP_P, 0.95f).toDouble()
        )
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getAppLanguage(): String {
        return prefs.getString(KEY_APP_LANGUAGE, "ru") ?: "ru"
    }

    fun saveAppLanguage(language: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language).apply()
    }

    fun hasSeenTour(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_TOUR, false)
    }

    fun setHasSeenTour(seen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_TOUR, seen).apply()
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_UNIVERSITY = "user_university"
        private const val KEY_COIN_BALANCE = "coin_balance"
        private const val KEY_ENDPOINT = "llm_endpoint"
        private const val KEY_API_KEY = "llm_api_key"
        private const val KEY_MODEL = "llm_model"
        private const val KEY_TIMEOUT = "llm_timeout"
        private const val KEY_TEMPERATURE = "llm_temperature"
        private const val KEY_TOP_P = "llm_top_p"
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_HAS_SEEN_TOUR = "has_seen_tour"
    }
}
