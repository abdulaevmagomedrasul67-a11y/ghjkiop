package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.core.di.AppContainer
import com.example.core.ui.navigation.AppNavHost
import com.example.ui.theme.StudToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as? StudToolApp)?.container ?: AppContainer(applicationContext)

        setContent {
            val themeMode by appContainer.settingsRepository.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            StudToolTheme(darkTheme = useDarkTheme) {
                AppNavHost(
                    appContainer = appContainer,
                    showSplashInitially = true
                )
            }
        }
    }
}
