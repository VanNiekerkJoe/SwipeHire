package com.swipehire.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Violet40,
    onPrimary = Color.White,
    primaryContainer = Violet80,
    onPrimaryContainer = Violet20,
    secondary = Mint40,
    onSecondary = Color.White,
    secondaryContainer = Mint80,
    onSecondaryContainer = Mint20,
    tertiary = Amber,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF0ECFB),
    onSurfaceVariant = TextSecondaryLight,
    outlineVariant = CardLightHairline,
    error = Coral,
)

private val DarkColors = darkColorScheme(
    primary = Violet80,
    onPrimary = VioletDeep,
    primaryContainer = VioletDeep,
    onPrimaryContainer = Violet80,
    secondary = Mint40,
    onSecondary = SurfaceDark,
    secondaryContainer = Mint20,
    onSecondaryContainer = Mint80,
    tertiary = Amber,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = TextSecondaryDark,
    outlineVariant = CardDarkHairline,
    error = Coral,
)

/** App-wide theme mode, driven by the persisted setting. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun SwipeHireTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (useDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = SwipeHireTypography,
        content = content
    )
}
