package com.example.data.local.security

import android.app.Activity
import android.content.Context

class AppBiometricManager(private val context: Context) {

    fun canAuthenticate(): Boolean = false

    fun promptBiometric(
        activity: Activity,
        title: String = "Вход в StudTool",
        subtitle: String = "Подтвердите личность",
        negativeButtonText: String = "Использовать PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        onError("Биометрическая аутентификация недоступна")
    }
}
