package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MouseDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = OnCyanPrimary,
    primaryContainer = CyanPrimaryContainer,
    onPrimaryContainer = OnCyanPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = OnAmberSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = OnAmberSecondaryContainer,
    tertiary = VioletTertiary,
    onTertiary = Color.White,
    tertiaryContainer = VioletTertiaryContainer,
    onTertiaryContainer = Color(0xFFE9D5FF),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceContainerDark,
    outline = BorderSubtle,
    outlineVariant = SurfaceHighlight,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun MouseTheme(
    darkTheme: Boolean = true, // MOUSE defaults to sleek AI OS dark aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MouseDarkColorScheme,
        typography = Typography,
        content = content
    )
}
