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

    fun getEmail(): String = getUserEmail()

    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Иван Иванов") ?: "Иван Иванов"
    }

    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun savePushToken(token: String) {
        prefs.edit().putString(KEY_PUSH_TOKEN, token).apply()
    }

    fun getPushToken(): String? {
        return prefs.getString(KEY_PUSH_TOKEN, null)
    }

    // Legacy coin helpers (hidden from UI, kept only for internal repo compatibility)
    fun getCoinBalance(): Int {
        return prefs.getInt(KEY_COIN_BALANCE, 0)
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

    // Subscription state (Stage 2)
    fun isSubscriptionActive(): Boolean {
        return prefs.getBoolean(KEY_SUB_ACTIVE, true)
    }

    fun setSubscriptionActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SUB_ACTIVE, active).apply()
    }

    fun hasHadSubscription(): Boolean {
        return prefs.getBoolean(KEY_HAS_HAD_SUB, false)
    }

    fun setHasHadSubscription(hasHad: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_HAD_SUB, hasHad).apply()
    }

    fun getSubscriptionPlanType(): String? {
        return prefs.getString(KEY_SUB_PLAN_TYPE, "month")
    }

    fun getSubscriptionExpiresAt(): String {
        return prefs.getString(KEY_SUB_EXPIRES_AT, "31.12.2026") ?: "31.12.2026"
    }

    fun getSubscriptionDaysLeft(): Int {
        return prefs.getInt(KEY_SUB_DAYS_LEFT, 45)
    }

    fun isSubscriptionAutoRenew(): Boolean {
        return prefs.getBoolean(KEY_SUB_AUTO_RENEW, true)
    }

    fun saveSubscriptionState(
        active: Boolean,
        hasHadSubscription: Boolean,
        planType: String?,
        expiresAt: String,
        daysLeft: Int,
        autoRenew: Boolean = true
    ) {
        prefs.edit()
            .putBoolean(KEY_SUB_ACTIVE, active)
            .putBoolean(KEY_HAS_HAD_SUB, hasHadSubscription)
            .putString(KEY_SUB_PLAN_TYPE, planType ?: "month")
            .putString(KEY_SUB_EXPIRES_AT, expiresAt)
            .putInt(KEY_SUB_DAYS_LEFT, daysLeft)
            .putBoolean(KEY_SUB_AUTO_RENEW, autoRenew)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return getUserId() > 0
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun saveLlmSettings(settings: LlmSettings) {
        prefs.edit()
            .putString(KEY_ENDPOINT, settings.endpoint)
            .putString(KEY_API_KEY, settings.apiKey)
            .putString(KEY_MODEL, settings.model)
            .putLong(KEY_TIMEOUT, settings.timeout.coerceIn(10L, 300L))
            .apply()
    }

    fun loadLlmSettings(): LlmSettings = getLlmSettings()

    fun getLlmSettings(): LlmSettings {
        val model = prefs.getString(KEY_MODEL, "vibe-code-pro-flash") ?: "vibe-code-pro-flash"
        val defaultTimeout = if (listOf("flash", "mini", "fast", "haiku", "nano").any { model.lowercase().contains(it) }) 90L else 180L
        return LlmSettings(
            endpoint = prefs.getString(KEY_ENDPOINT, "https://api.vibecode.online/v1") ?: "https://api.vibecode.online/v1",
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = model,
            timeout = prefs.getLong(KEY_TIMEOUT, defaultTimeout).coerceIn(10L, 300L)
        )
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_PUSH_TOKEN = "push_token"
        private const val KEY_COIN_BALANCE = "coin_balance"
        private const val KEY_SUB_ACTIVE = "subscription_active"
        private const val KEY_HAS_HAD_SUB = "has_had_subscription"
        private const val KEY_SUB_PLAN_TYPE = "subscription_plan_type"
        private const val KEY_SUB_EXPIRES_AT = "subscription_expires_at"
        private const val KEY_SUB_DAYS_LEFT = "subscription_days_left"
        private const val KEY_SUB_AUTO_RENEW = "subscription_auto_renew"
        private const val KEY_ENDPOINT = "llm_endpoint"
        private const val KEY_API_KEY = "llm_api_key"
        private const val KEY_MODEL = "llm_model"
        private const val KEY_TIMEOUT = "llm_timeout"
    }
}
