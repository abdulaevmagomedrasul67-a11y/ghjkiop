package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalStudToolColors = staticCompositionLocalOf { StudToolColors() }
val LocalStudToolTypography = staticCompositionLocalOf { StudToolTypography() }

private val SwissColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = PaperWarm,
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = Paper,
    secondaryContainer = PaperWarm,
    onSecondaryContainer = Ink,
    tertiary = Good,
    onTertiary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperWarm,
    onSurfaceVariant = InkSoft,
    outline = Line,
    outlineVariant = Line
)

object StudToolTheme {
    val colors: StudToolColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStudToolColors.current

    val typography: StudToolTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalStudToolTypography.current

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = StudToolShapes
}

@Composable
fun StudToolTheme(
    content: @Composable () -> Unit
) {
    val colors = StudToolColors()
    val typography = StudToolTypography()

    CompositionLocalProvider(
        LocalStudToolColors provides colors,
        LocalStudToolTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = SwissColorScheme,
            typography = Typography,
            shapes = StudToolShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    StudToolTheme(content = content)
}
