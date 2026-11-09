package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.security.SessionManager
import com.example.data.remote.api.StudToolApiService
import com.example.data.remote.dto.RegisterPushTokenRequest
import com.example.data.remote.dto.UnregisterPushTokenRequest

class PushTokenRepository(
    private val api: StudToolApiService,
    private val sessionManager: SessionManager,
    private val context: Context? = null
) {

    suspend fun registerToken(token: String? = null) {
        val userId = sessionManager.getUserId()
        if (userId <= 0L) return
        val actualToken = token ?: return
        if (actualToken.isEmpty()) return

        try {
            val response = api.registerPushToken(
                RegisterPushTokenRequest(
                    userId = userId,
                    pushToken = actualToken
                )
            )
            if (response.success == true) {
                sessionManager.savePushToken(actualToken)
            }
        } catch (e: Exception) {
            Log.e("PUSH", "Failed to register token", e)
        }
    }

    suspend fun unregisterToken() {
        val userId = sessionManager.getUserId()
        if (userId <= 0L) return
        try {
            api.unregisterPushToken(UnregisterPushTokenRequest(userId = userId))
            sessionManager.savePushToken("")
        } catch (e: Exception) {
            Log.e("PUSH", "Failed to unregister token", e)
        }
    }

    fun getSavedToken(): String? {
        return sessionManager.getPushToken()
    }
}
