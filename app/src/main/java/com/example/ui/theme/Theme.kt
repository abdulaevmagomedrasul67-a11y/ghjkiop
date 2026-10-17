package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class StudToolColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val onPrimary: Color,
    val accentGold: Color,
    val accentGoldSoft: Color,
    val accentGoldDeep: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val success: Color,
    val error: Color,
    val isDark: Boolean
)

private val LightStudToolColors = StudToolColors(
    background = StudPaperLight,
    surface = StudSurfaceLight,
    surfaceElevated = StudSurfaceVariantLight,
    primary = StudPrimaryBlue,
    onPrimary = Color.White,
    accentGold = StudAmberGold,
    accentGoldSoft = StudAmberSoftBg,
    accentGoldDeep = StudAmberDeep,
    textPrimary = StudTextPrimaryLight,
    textSecondary = StudTextSecondaryLight,
    textMuted = StudTextMutedLight,
    border = StudBorderLight,
    success = StudEmerald,
    error = StudCrimson,
    isDark = false
)

private val DarkStudToolColors = StudToolColors(
    background = StudNavyDark,
    surface = StudNavySurface,
    surfaceElevated = StudNavyElevated,
    primary = StudPrimaryBlueLight,
    onPrimary = Color.White,
    accentGold = StudAmberGold,
    accentGoldSoft = Color(0xFF451A03).copy(alpha = 0.65f),
    accentGoldDeep = Color(0xFFFDE68A),
    textPrimary = StudTextPrimaryDark,
    textSecondary = StudTextSecondaryDark,
    textMuted = StudTextMutedDark,
    border = StudBorderDark,
    success = StudEmerald,
    error = StudCrimson,
    isDark = true
)

val LocalStudToolColors = staticCompositionLocalOf { LightStudToolColors }

object StudToolTheme {
    val colors: StudToolColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStudToolColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = StudPrimaryBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = StudAmberGold,
    onSecondary = StudNavyDark,
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = StudEmerald,
    background = StudNavyDark,
    onBackground = StudTextPrimaryDark,
    surface = StudNavySurface,
    onSurface = StudTextPrimaryDark,
    surfaceVariant = StudNavyElevated,
    onSurfaceVariant = StudTextSecondaryDark,
    outline = StudBorderDark,
    outlineVariant = StudBorderDark.copy(alpha = 0.7f),
    error = StudCrimson,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

private val LightColorScheme = lightColorScheme(
    primary = StudPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = StudPrimaryContainerLight,
    onPrimaryContainer = StudOnPrimaryContainerLight,
    secondary = StudAmberDark,
    onSecondary = Color.White,
    secondaryContainer = StudAmberSoftBg,
    onSecondaryContainer = StudAmberDeep,
    tertiary = StudEmeraldDark,
    background = StudPaperLight,
    onBackground = StudTextPrimaryLight,
    surface = StudSurfaceLight,
    onSurface = StudTextPrimaryLight,
    surfaceVariant = StudSurfaceVariantLight,
    onSurfaceVariant = StudTextSecondaryLight,
    outline = StudBorderLight,
    outlineVariant = StudBorderLight,
    error = StudCrimson,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun StudToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val studColors = if (darkTheme) DarkStudToolColors else LightStudToolColors

    CompositionLocalProvider(LocalStudToolColors provides studColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = StudToolShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    StudToolTheme(darkTheme = darkTheme, content = content)
}
