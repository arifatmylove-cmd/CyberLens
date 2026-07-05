package com.cyberlens.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberBlue,
    onPrimary = CyberBg,
    primaryContainer = CyberSurfaceVariant,
    onPrimaryContainer = CyberBlue,
    secondary = CyberGreen,
    onSecondary = CyberBg,
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = CyberGreen,
    tertiary = CyberPurple,
    onTertiary = CyberBg,
    background = CyberBg,
    onBackground = CyberWhite,
    surface = CyberSurface,
    onSurface = CyberWhite,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberLightGray,
    error = CyberRed,
    onError = CyberBg,
    outline = CyberGray,
    outlineVariant = CyberSurfaceVariant,
)

@Composable
fun CyberLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = CyberTypography,
        content = content
    )
}
