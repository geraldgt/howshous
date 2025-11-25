package io.github.howshous.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = NearWhite,
    secondary = LilacAccent,
    background = NearWhite,
    surface = SurfaceLight,
    onSurface = OnSurfaceVariant
)

private val DarkColors = darkColorScheme(
    primary = PrimaryTealLight,
    onPrimary = NearWhite,
    secondary = LilacAccent,
    background = MutedDark,
    surface = MutedDark,
    onSurface = NearWhite
)

@Composable
fun HowsHousTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,   // ← from Type.kt
        content = content
    )
}