package me.shirobyte42.glosso.presentation.components

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.shirobyte42.glosso.presentation.LocalWindowWidthClass

/**
 * Caps a single-column screen's width and centres it on wider-than-phone windows,
 * so cards do not stretch edge to edge on tablets. A no-op on compact phone
 * windows. Screens that want their own multi-pane treatment (the Studio) read
 * [LocalWindowWidthClass] directly instead of using this.
 */
@Composable
fun Modifier.readableWidth(maxWidth: Dp = 760.dp): Modifier {
    if (LocalWindowWidthClass.current == WindowWidthSizeClass.Compact) return this
    return this.layout { measurable, constraints ->
        val cap = maxWidth.roundToPx().coerceAtMost(constraints.maxWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = cap, maxWidth = cap))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place((constraints.maxWidth - placeable.width) / 2, 0)
        }
    }
}
