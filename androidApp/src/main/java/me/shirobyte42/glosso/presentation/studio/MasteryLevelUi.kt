package me.shirobyte42.glosso.presentation.studio

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.domain.model.MasteryLevel
import me.shirobyte42.glosso.presentation.theme.GlossoFeedbackClose
import me.shirobyte42.glosso.presentation.theme.GlossoFeedbackMissed

/** The learner-facing name of a mastery level, in the current UI language. */
@Composable
fun masteryLevelLabel(level: MasteryLevel): String = stringResource(
    when (level) {
        MasteryLevel.PERFECT -> R.string.mastery_level_perfect
        MasteryLevel.MASTERED -> R.string.mastery_level_mastered
        MasteryLevel.ALMOST -> R.string.mastery_level_almost
        MasteryLevel.ROUGH -> R.string.mastery_level_rough
        MasteryLevel.NOT_YET -> R.string.mastery_level_not_yet
    }
)

/**
 * The colour that carries a level everywhere: the level badge, the feedback
 * card and the per-word underline. Ordered from "nothing to fix" to "needs work".
 */
@Composable
fun masteryLevelColor(level: MasteryLevel): Color = when (level) {
    MasteryLevel.PERFECT -> MaterialTheme.colorScheme.primary
    MasteryLevel.MASTERED -> MaterialTheme.colorScheme.secondary
    MasteryLevel.ALMOST -> MaterialTheme.colorScheme.tertiary
    MasteryLevel.ROUGH -> GlossoFeedbackClose
    MasteryLevel.NOT_YET -> GlossoFeedbackMissed
}

/** A short, recognisable icon for each level. */
fun masteryLevelIcon(level: MasteryLevel): ImageVector = when (level) {
    MasteryLevel.PERFECT -> Icons.Default.AutoAwesome
    MasteryLevel.MASTERED -> Icons.Default.Star
    MasteryLevel.ALMOST -> Icons.Default.TrendingUp
    MasteryLevel.ROUGH -> Icons.Default.TrendingFlat
    MasteryLevel.NOT_YET -> Icons.Default.Refresh
}
