package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CngCyanElectric,
    onPrimary = CngGraphiteDarkest,
    primaryContainer = CngGraphiteSurfaceVariant,
    onPrimaryContainer = CngCyanElectric,
    secondary = CngBlueAccent,
    onSecondary = CngTextPrimary,
    tertiary = CngAmberWarning,
    onTertiary = CngGraphiteDarkest,
    background = CngGraphiteDarkest,
    onBackground = CngTextPrimary,
    surface = CngGraphiteDark,
    onSurface = CngTextPrimary,
    surfaceVariant = CngGraphiteSurface,
    onSurfaceVariant = CngTextSecondary,
    outline = CngGraphiteBorder,
    error = CngRedAlert,
    onError = CngTextPrimary
)

@Composable
fun ClutchAndGearTheme(
    content: @Composable () -> Unit
) {
    // We enforce our purposeful automotive dark aesthetic across all devices
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
