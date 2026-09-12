package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NovaBlue,
    onPrimary = Color.White,
    primaryContainer = NovaPurple,
    onPrimaryContainer = Color.White,
    secondary = NovaCyan,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = NovaIndigo,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NovaBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EEFF),
    onPrimaryContainer = NovaBlue,
    secondary = NovaPurple,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = NovaCyan,
    onTertiary = Color.Black,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun NovaAiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
