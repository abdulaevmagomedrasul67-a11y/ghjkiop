package com.example.data.local.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PinManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("studtool_pin_prefs", Context.MODE_PRIVATE)

    @Volatile
    private var unlockedThisSession: Boolean = false

    fun hasPin(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        unlockedThisSession = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val matches = storedHash == hashPin(pin)
        if (matches) {
            unlockedThisSession = true
        }
        return matches
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
        unlockedThisSession = true
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isUnlockedThisSession(): Boolean = unlockedThisSession

    fun setUnlockedThisSession(unlocked: Boolean) {
        unlockedThisSession = unlocked
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    }
}
