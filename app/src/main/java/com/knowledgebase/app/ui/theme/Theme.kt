package com.knowledgebase.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors - inspired by the original web app's CSS variables
val LightBg = Color(0xFFFAFAF8)
val LightBgAlt = Color(0xFFF2F1ED)
val LightSurface = Color(0xFFFFFFFF)
val LightFg = Color(0xFF1C1B18)
val LightFg2 = Color(0xFF6B6860)
val LightAccent = Color(0xFF2563A8)
val LightAccentBg = Color(0xFFE8F0FB)
val LightGreen = Color(0xFF1A6640)
val LightRed = Color(0xFF8B2020)
val LightAmber = Color(0xFF7A4A00)

// Dark theme colors
val DarkBg = Color(0xFF16161A)
val DarkBgAlt = Color(0xFF1E1E24)
val DarkSurface = Color(0xFF1E1E24)
val DarkFg = Color(0xFFE8E6E0)
val DarkFg2 = Color(0xFF9A9890)
val DarkAccent = Color(0xFF5B9DE0)
val DarkAccentBg = Color(0xFF1A2A40)
val DarkGreen = Color(0xFF4CAF73)
val DarkRed = Color(0xFFE05252)
val DarkAmber = Color(0xFFE0A052)

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = LightAccentBg,
    onPrimaryContainer = LightAccent,
    secondary = LightGreen,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = LightFg,
    surface = LightSurface,
    onSurface = LightFg,
    surfaceVariant = LightBgAlt,
    onSurfaceVariant = LightFg2,
    outline = Color(0xFFDDDBD4),
    error = LightRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color(0xFF16161A),
    primaryContainer = DarkAccentBg,
    onPrimaryContainer = DarkAccent,
    secondary = DarkGreen,
    background = DarkBg,
    onBackground = DarkFg,
    surface = DarkSurface,
    onSurface = DarkFg,
    surfaceVariant = DarkBgAlt,
    onSurfaceVariant = DarkFg2,
    outline = Color(0xFF2E2E38),
    error = DarkRed,
    onError = Color.White
)

@Composable
fun KnowledgeBaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
