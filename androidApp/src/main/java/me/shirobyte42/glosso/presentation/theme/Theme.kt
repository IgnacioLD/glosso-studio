package me.shirobyte42.glosso.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GlossoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E9FF),
    onPrimaryContainer = Color(0xFF1A1A5C),
    secondary = GlossoSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF052E1A),
    tertiary = GlossoTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF3D2000),
    background = GlossoBackground,
    onBackground = GlossoOnSurface,
    surface = GlossoSurface,
    onSurface = GlossoOnSurface,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = GlossoOutline,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = GlossoPrimaryLight,
    onPrimary = Color(0xFF1A1A4E),
    primaryContainer = GlossoDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFC7C8FF),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF002618),
    secondaryContainer = GlossoDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3D2000),
    tertiaryContainer = Color(0xFF2D1A00),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = GlossoDarkBackground,
    onBackground = GlossoDarkOnSurface,
    surface = GlossoDarkSurface,
    onSurface = GlossoDarkOnSurface,
    surfaceVariant = GlossoDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = GlossoDarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun GlossoTheme(
    themeMode: Int = 0, // 0=system, 1=light, 2=dark
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GlossoTypography,
        content = content
    )
}
