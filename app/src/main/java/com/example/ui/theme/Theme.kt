package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = NaturalPrimary,
    onPrimary = NaturalOnPrimary,
    secondary = NaturalSecondary,
    onSecondary = NaturalOnSecondary,
    tertiary = NaturalTertiary,
    onTertiary = NaturalOnTertiary,
    background = NaturalBackground,
    onBackground = NaturalOnBackground,
    surface = NaturalSurface,
    onSurface = NaturalOnSurface,
    surfaceVariant = NaturalSurfaceVariant,
    onSurfaceVariant = NaturalOnSurfaceVariant,
    outline = NaturalOutline,
    outlineVariant = NaturalOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = NaturalPrimary, // The design specifies beautiful organic charcoal and clay
    onPrimary = NaturalOnPrimary,
    secondary = NaturalSecondary,
    onSecondary = NaturalOnSecondary,
    tertiary = NaturalTertiary,
    onTertiary = NaturalOnTertiary,
    background = NaturalPrimary, // Rich dark charcoal background for dark mode
    onBackground = NaturalOnPrimary,
    surface = NaturalPrimary,
    onSurface = NaturalOnPrimary,
    surfaceVariant = NaturalSurfaceVariant,
    onSurfaceVariant = NaturalOnSurfaceVariant,
    outline = NaturalOutline,
    outlineVariant = NaturalOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We default dynamicColor to false to fully honor the requested Natural Tones design theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
