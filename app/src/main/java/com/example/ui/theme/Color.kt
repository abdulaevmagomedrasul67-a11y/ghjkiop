package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0A0A0A)
val InkSoft = Color(0xFF2E2D2A)
val InkFaint = Color(0xFF6E6B64)
val Paper = Color(0xFFF0EDE6)
val PaperWarm = Color(0xFFE6E1D6)
val Line = Color(0xFFD4CFC4)
val LineStrong = Color(0xFF0A0A0A)
val Accent = Color(0xFFC8451F)
val Good = Color(0xFF1E6B47)

// Legacy M3 template colors kept for compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFFC8451F)
val PurpleGrey40 = Color(0xFF2E2D2A)
val Pink40 = Color(0xFF1E6B47)

@Immutable
data class StudToolColors(
    val ink: Color = Ink,
    val inkSoft: Color = InkSoft,
    val inkFaint: Color = InkFaint,
    val paper: Color = Paper,
    val paperWarm: Color = PaperWarm,
    val line: Color = Line,
    val lineStrong: Color = LineStrong,
    val accent: Color = Accent,
    val good: Color = Good
)
