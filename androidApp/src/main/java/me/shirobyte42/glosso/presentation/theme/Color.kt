package me.shirobyte42.glosso.presentation.theme

import androidx.compose.ui.graphics.Color

// A calm, single-accent system. Colour is reserved for meaning (progress,
// success, caution) rather than decoration, which keeps dense practice screens
// readable and stops six difficulty levels from competing for attention.
val GlossoPrimary = Color(0xFF4F46E5)          // Indigo - primary action
val GlossoPrimaryLight = Color(0xFFA9B4FF)
val GlossoSecondary = Color(0xFF0E9F6E)        // Success / mastered
val GlossoTertiary = Color(0xFFC2410C)         // Streak / warm accent

val GlossoBackground = Color(0xFFF1F2F7)
val GlossoSurface = Color(0xFFFFFFFF)
val GlossoSurfaceMuted = Color(0xFFEDEFF5)
val GlossoOnSurface = Color(0xFF14161D)
val GlossoOnSurfaceVariant = Color(0xFF5D6576)
val GlossoOutline = Color(0xFFDCE0E9)

// Dark palette
val GlossoDarkBackground = Color(0xFF0B0C11)
val GlossoDarkSurface = Color(0xFF15171E)
val GlossoDarkSurfaceVariant = Color(0xFF1E212B)
val GlossoDarkOutline = Color(0xFF2B2F3A)
val GlossoDarkOnSurface = Color(0xFFE9EAF0)
val GlossoDarkOnSurfaceVariant = Color(0xFF9AA1B0)
val GlossoDarkPrimaryContainer = Color(0xFF2A3170)
val GlossoDarkSecondaryContainer = Color(0xFF10382A)

// Feedback colors - softened so they read as guidance, not alarm.
val GlossoFeedbackClose = Color(0xFFD97706)   // Amber
val GlossoFeedbackMissed = Color(0xFFDC2626)  // Red

// CEFR level accents. Muted, related hues so difficulty reads as a gradual
// progression instead of a rainbow.
val LevelColorA1 = Color(0xFF0E9F6E)
val LevelColorA2 = Color(0xFF0E8FA8)
val LevelColorB1 = Color(0xFF4F46E5)
val LevelColorB2 = Color(0xFF7C5CD6)
val LevelColorC1 = Color(0xFFB45309)
val LevelColorC2 = Color(0xFFBE3455)

fun levelColor(index: Int): Color = when (index) {
    0 -> LevelColorA1
    1 -> LevelColorA2
    2 -> LevelColorB1
    3 -> LevelColorB2
    4 -> LevelColorC1
    5 -> LevelColorC2
    else -> LevelColorA1
}
