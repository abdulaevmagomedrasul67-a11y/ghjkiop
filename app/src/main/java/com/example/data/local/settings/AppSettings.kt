package com.example.data.local.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("studtool_app_settings", Context.MODE_PRIVATE)

    private val _languageFlow = MutableStateFlow(getLanguage())
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "ru") ?: "ru"
    }

    fun setLanguage(langCode: String) {
        val clean = if (langCode == "en") "en" else "ru"
        prefs.edit().putString(KEY_LANGUAGE, clean).apply()
        _languageFlow.value = clean
    }

    fun isTutorialCompleted(): Boolean {
        return prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)
    }

    fun setTutorialCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, completed).apply()
    }

    fun getLastProfileId(): Long? {
        val id = prefs.getLong(KEY_LAST_PROFILE_ID, -1L)
        return if (id > 0) id else null
    }

    fun setLastProfileId(profileId: Long) {
        prefs.edit().putLong(KEY_LAST_PROFILE_ID, profileId).apply()
    }

    fun getLastWorkType(): String? {
        return prefs.getString(KEY_LAST_WORK_TYPE, null)
    }

    fun setLastWorkType(typeId: String) {
        prefs.edit().putString(KEY_LAST_WORK_TYPE, typeId).apply()
    }

    fun getLastTargetPages(): Int? {
        val pages = prefs.getInt(KEY_LAST_TARGET_PAGES, -1)
        return if (pages > 0) pages else null
    }

    fun setLastTargetPages(pages: Int) {
        prefs.edit().putInt(KEY_LAST_TARGET_PAGES, pages).apply()
    }

    companion object {
        private const val KEY_LANGUAGE = "key_app_language"
        private const val KEY_TUTORIAL_COMPLETED = "key_tutorial_completed"
        private const val KEY_LAST_PROFILE_ID = "key_last_profile_id"
        private const val KEY_LAST_WORK_TYPE = "key_last_work_type"
        private const val KEY_LAST_TARGET_PAGES = "key_last_target_pages"
    }
}
