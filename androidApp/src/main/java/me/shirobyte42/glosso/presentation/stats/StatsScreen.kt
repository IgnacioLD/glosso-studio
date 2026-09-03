package me.shirobyte42.glosso.presentation.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.presentation.components.GlossoCard
import me.shirobyte42.glosso.presentation.components.GlossoSectionLabel
import me.shirobyte42.glosso.presentation.components.GlossoStatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.stats_title),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.stats_back))
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlossoStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_summary_mastered),
                    value = state.totalMastered.toString(),
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.secondary
                )
                GlossoStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_summary_active_days),
                    value = state.totalActivityDays.toString(),
                    icon = Icons.Default.EventAvailable,
                    color = MaterialTheme.colorScheme.tertiary
                )
                GlossoStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_summary_best_combo),
                    value = state.bestCombo.toString(),
                    icon = Icons.Default.LocalFireDepartment,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            StatsSectionCard(
                icon = { Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                title = stringResource(R.string.stats_section_timeline),
                subtitle = stringResource(R.string.stats_timeline_subtitle)
            ) {
                if (state.hasAnyTimelineActivity) {
                    MasteryTimelineChart(points = state.timeline)
                } else {
                    StatsEmptyHint(text = stringResource(R.string.stats_empty_timeline))
                }
            }

            StatsSectionCard(
                icon = { Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                title = stringResource(R.string.stats_section_activity),
                subtitle = stringResource(R.string.stats_activity_subtitle)
            ) {
                if (state.hasAnyActivity) {
                    ActivityHeatmap(activityDates = state.activityDates)
                } else {
                    StatsEmptyHint(text = stringResource(R.string.stats_empty_timeline))
                }
            }

            StatsSectionCard(
                icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = stringResource(R.string.stats_section_sounds),
                subtitle = stringResource(R.string.stats_sounds_subtitle)
            ) {
                if (state.weakPhonemes.isEmpty()) {
                    StatsEmptyHint(text = stringResource(R.string.stats_empty_sounds))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        state.weakPhonemes.forEach { stat ->
                            PhonemeStatRow(stat = stat)
                        }
                    }
                }
            }

            StatsSectionCard(
                icon = { Icon(Icons.Default.Replay, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = stringResource(R.string.stats_section_reviews),
                subtitle = stringResource(R.string.stats_reviews_subtitle)
            ) {
                if (state.reviewScheduled == 0) {
                    StatsEmptyHint(text = stringResource(R.string.stats_reviews_empty))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReviewCountPill(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.stats_reviews_due, state.reviewDue),
                            highlight = state.reviewDue > 0
                        )
                        ReviewCountPill(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.stats_reviews_scheduled, state.reviewScheduled),
                            highlight = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatsSectionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    GlossoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                icon()
                GlossoSectionLabel(text = title)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatsEmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    )
}

@Composable
private fun ReviewCountPill(
    label: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Box(modifier = Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
