package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    secondary = CyberSecondary,
    tertiary = CyberAccent,
    background = CyberBackground,
    surface = CyberSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = CyberTextPrimary,
    onSurface = CyberTextPrimary,
    error = CyberError,
    onError = Color.White,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary
)

private val CyberLightColorScheme = lightColorScheme(
    primary = CyberPrimary,
    secondary = CyberSecondary,
    tertiary = CyberAccent,
    background = CyberBackground,
    surface = CyberSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = CyberTextPrimary,
    onSurface = CyberTextPrimary,
    error = CyberError,
    onError = Color.White,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Use the light "Sleek Interface" theme by default
    dynamicColor: Boolean = false, // Preserve our beautiful custom design theme colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CyberDarkColorScheme else CyberLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
