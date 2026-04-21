package me.shirobyte42.glosso.presentation.theme

import androidx.compose.ui.graphics.Color

// Glosso Professional Palette — Light
val GlossoPrimary = Color(0xFF6366F1) // Modern Indigo
val GlossoPrimaryLight = Color(0xFF818CF8)
val GlossoSecondary = Color(0xFF10B981) // Emerald Mastery
val GlossoTertiary = Color(0xFFF59E0B) // Amber Streak
val GlossoBackground = Color(0xFFFFFFFF)
val GlossoSurface = Color(0xFFF8FAFC)
val GlossoOnSurface = Color(0xFF1E293B) // Deep Slate/Charcoal
val GlossoOutline = Color(0xFFE2E8F0)

// Dark palette
val GlossoDarkBackground = Color(0xFF0F0F14)
val GlossoDarkSurface = Color(0xFF1A1A24)
val GlossoDarkSurfaceVariant = Color(0xFF242433)
val GlossoDarkOutline = Color(0xFF2E2E42)
val GlossoDarkOnSurface = Color(0xFFE2E8F0)
val GlossoDarkPrimaryContainer = Color(0xFF1E1E3A)
val GlossoDarkSecondaryContainer = Color(0xFF0D2E24)

// Feedback colors (slightly softer than pure red/orange)
val GlossoFeedbackClose = Color(0xFFFB8C00)   // Softer orange
val GlossoFeedbackMissed = Color(0xFFE53935)  // Softer red

// CEFR Level colors (A1 → C2 difficulty gradient: green → pink)
val LevelColorA1 = Color(0xFF10B981) // Emerald — Beginner
val LevelColorA2 = Color(0xFF3B82F6) // Blue — Elementary
val LevelColorB1 = Color(0xFF8B5CF6) // Violet — Intermediate
val LevelColorB2 = Color(0xFFF59E0B) // Amber — Upper-Int
val LevelColorC1 = Color(0xFFEF4444) // Red — Advanced
val LevelColorC2 = Color(0xFFEC4899) // Pink — Mastery

fun levelColor(index: Int): Color = when (index) {
    0 -> LevelColorA1
    1 -> LevelColorA2
    2 -> LevelColorB1
    3 -> LevelColorB2
    4 -> LevelColorC1
    5 -> LevelColorC2
    else -> LevelColorA1
}
