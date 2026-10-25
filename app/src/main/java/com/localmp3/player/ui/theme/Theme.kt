package com.localmp3.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = JadeDeep,
    onPrimary = Color.White,
    primaryContainer = JadeSoft,
    onPrimaryContainer = JadeDeep,
    secondary = Copper,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E4D8),
    onSecondaryContainer = Color(0xFF5C2E12),
    tertiary = JadeMid,
    background = Mist,
    onBackground = Ink,
    surface = SurfaceLift,
    onSurface = Ink,
    surfaceVariant = MistWarm,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFB7C4BF),
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun LocalMp3Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = LocalMp3Typography,
        content = content,
    )
}
