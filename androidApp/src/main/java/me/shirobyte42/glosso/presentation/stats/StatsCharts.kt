package me.shirobyte42.glosso.presentation.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.shirobyte42.glosso.domain.model.MasteryTimelinePoint
import me.shirobyte42.glosso.domain.model.PhonemeStat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class HeatmapCell(val date: LocalDate, val active: Boolean)

/**
 * GitHub-style activity calendar: one column per week (oldest left), one row per weekday.
 */
@Composable
fun ActivityHeatmap(
    activityDates: Set<String>,
    modifier: Modifier = Modifier,
    weeks: Int = 16
) {
    val cells = remember(activityDates, weeks) { buildHeatmapCells(activityDates, weeks) }
    val activeColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Canvas(modifier = modifier.fillMaxWidth().height(112.dp)) {
        val weekCount = cells.size
        if (weekCount == 0) return@Canvas
        val gap = 3.dp.toPx()
        val cellSize = ((size.width - gap * (weekCount - 1)) / weekCount)
            .coerceAtMost((size.height - gap * 6) / 7f)
        val gridHeight = cellSize * 7 + gap * 6
        val topOffset = (size.height - gridHeight) / 2f

        cells.forEachIndexed { weekIndex, weekCells ->
            weekCells.forEachIndexed { dayIndex, cell ->
                if (cell == null) return@forEachIndexed
                drawRoundRect(
                    color = if (cell.active) activeColor else inactiveColor,
                    topLeft = Offset(
                        x = weekIndex * (cellSize + gap),
                        y = topOffset + dayIndex * (cellSize + gap)
                    ),
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(cellSize * 0.28f)
                )
            }
        }
    }
}

private fun buildHeatmapCells(activityDates: Set<String>, weeks: Int): List<List<HeatmapCell?>> {
    val today = LocalDate.now()
    val windowStart = today.minusDays((weeks * 7 - 1).toLong())
    val gridStart = windowStart.with(DayOfWeek.MONDAY)
    val totalWeeks = java.time.temporal.ChronoUnit.WEEKS.between(gridStart, today) + 1

    return (0 until totalWeeks).map { week ->
        (0 until 7).map { day ->
            val date = gridStart.plusWeeks(week.toLong()).plusDays(day.toLong())
            if (date.isAfter(today) || date.isBefore(windowStart)) {
                null
            } else {
                HeatmapCell(date = date, active = activityDates.contains(date.toString()))
            }
        }
    }
}

/**
 * Bar chart of phrases mastered per day. Bars animate in on first composition.
 */
@Composable
fun MasteryTimelineChart(
    points: List<MasteryTimelinePoint>,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val maxCount = points.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.secondary
    val todayColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            if (points.isEmpty()) return@Canvas
            val slot = size.width / points.size
            val barWidth = slot * 0.62f
            val corner = CornerRadius(barWidth / 3f)

            points.forEachIndexed { index, point ->
                val isToday = index == points.lastIndex
                val color = when {
                    point.count == 0 -> emptyColor
                    isToday -> todayColor
                    else -> barColor
                }
                val fullHeight = (point.count.toFloat() / maxCount) * (size.height - 4.dp.toPx())
                val height = if (point.count == 0) 2.dp.toPx() else (fullHeight * progress.value).coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x = index * slot + (slot - barWidth) / 2f, y = size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = corner
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TimelineLabels(points)
    }
}

@Composable
private fun TimelineLabels(points: List<MasteryTimelinePoint>) {
    if (points.size < 3) return
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val labels = listOf(points.first(), points[points.size / 2], points.last())
    val alignments = listOf(
        androidx.compose.ui.text.style.TextAlign.Start,
        androidx.compose.ui.text.style.TextAlign.Center,
        androidx.compose.ui.text.style.TextAlign.End
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, point ->
            Text(
                text = runCatching { LocalDate.parse(point.date).format(formatter) }.getOrDefault(""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (index == labels.lastIndex) FontWeight.Bold else FontWeight.Normal,
                textAlign = alignments[index],
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * One row per weak phoneme: IPA symbol chip plus a bar of the miss ratio.
 */
@Composable
fun PhonemeStatRow(
    stat: PhonemeStat,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.error
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accentColor.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stat.phoneme,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${stat.missed}/${stat.total}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = stat.missRatio.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.12f)
            )
        }
    }
}
