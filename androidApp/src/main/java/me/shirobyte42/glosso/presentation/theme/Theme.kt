package me.shirobyte42.glosso.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GlossoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAECFF),
    onPrimaryContainer = Color(0xFF201E52),
    secondary = GlossoSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F6EC),
    onSecondaryContainer = Color(0xFF06301F),
    tertiary = GlossoTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEAD8),
    onTertiaryContainer = Color(0xFF3D1A03),
    background = GlossoBackground,
    onBackground = GlossoOnSurface,
    surface = GlossoSurface,
    onSurface = GlossoOnSurface,
    surfaceVariant = GlossoSurfaceMuted,
    onSurfaceVariant = GlossoOnSurfaceVariant,
    outline = GlossoOutline,
    outlineVariant = Color(0xFFE8EBF1),
    scrim = Color(0xFF0B0C11),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF7A1414)
)

private val DarkColorScheme = darkColorScheme(
    primary = GlossoPrimaryLight,
    onPrimary = Color(0xFF20265C),
    primaryContainer = GlossoDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF4ADE9E),
    onSecondary = Color(0xFF05291C),
    secondaryContainer = GlossoDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFDBA74),
    onTertiary = Color(0xFF3D1A03),
    tertiaryContainer = Color(0xFF3A2109),
    onTertiaryContainer = Color(0xFFFED7AA),
    background = GlossoDarkBackground,
    onBackground = GlossoDarkOnSurface,
    surface = GlossoDarkSurface,
    onSurface = GlossoDarkOnSurface,
    surfaceVariant = GlossoDarkSurfaceVariant,
    onSurfaceVariant = GlossoDarkOnSurfaceVariant,
    outline = GlossoDarkOutline,
    outlineVariant = Color(0xFF232734),
    scrim = Color(0xFF000000),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF5A0F0F),
    errorContainer = Color(0xFF3A1515),
    onErrorContainer = Color(0xFFFECACA)
)

private val GlossoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
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

    // Keep system bar icons legible even when the app theme is forced against
    // the system setting.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GlossoTypography,
        shapes = GlossoShapes,
        content = content
    )
}
