package me.shirobyte42.glosso.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.domain.model.SUPPORTED_LANGUAGES
import me.shirobyte42.glosso.presentation.LocalWindowWidthClass
import me.shirobyte42.glosso.presentation.components.GlossoStatCard
import me.shirobyte42.glosso.presentation.theme.levelColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStudio: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val levelNames = listOf(
        stringResource(R.string.level_beginner),
        stringResource(R.string.level_elementary),
        stringResource(R.string.level_intermediate),
        stringResource(R.string.level_upper_intermediate),
        stringResource(R.string.level_advanced),
        stringResource(R.string.level_mastery)
    )

    if (state.showOnboarding) {
        OnboardingDialog(onDismiss = { viewModel.dismissOnboarding() })
    }

    if (state.isInitialSetupRequired) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelSetup() },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            title = { Text(stringResource(R.string.home_initial_setup), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.home_setup_description))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.home_wifi_warning_initial),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.startInitialSetup() },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.home_btn_setup))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelSetup() }) {
                    Text(stringResource(R.string.home_btn_cancel))
                }
            }
        )
    }

    if (state.isDownloadRequired) {
        val fallbackLevelName = stringResource(R.string.home_this_level)
        val levelName = state.pendingLevelIndex?.let { levelNames.getOrNull(it) } ?: fallbackLevelName
        AlertDialog(
            onDismissRequest = { viewModel.cancelDownload() },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            title = { Text(stringResource(R.string.home_setup_required), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.home_download_description, levelName))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.home_wifi_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.startDownload() },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.home_btn_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDownload() }) {
                    Text(stringResource(R.string.home_btn_cancel))
                }
            }
        )
    }

    if (state.isDownloading) {
        val levelName = state.pendingLevelIndex?.let { levelNames.getOrNull(it) }
        val titleText = if (levelName != null)
            stringResource(R.string.home_downloading_level, levelName)
        else
            stringResource(R.string.home_setting_up_model)

        AlertDialog(
            onDismissRequest = { },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(titleText, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (levelName != null)
                            stringResource(R.string.home_preparing_curriculum)
                        else
                            stringResource(R.string.home_setting_up_recognition),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = state.downloadProgress,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.home_progress_percent, (state.downloadProgress * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = { }
        )
    }

    if (state.downloadError != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.home_download_failed)) },
            text = { Text(state.downloadError ?: stringResource(R.string.home_unknown_setup_error)) },
            confirmButton = {
                Button(onClick = { viewModel.refreshStats() }) {
                    Text(stringResource(R.string.home_btn_retry))
                }
            }
        )
    }

    if (state.showLanguageSelector) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLanguageSelector() },
            title = { Text(stringResource(R.string.home_select_language), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SUPPORTED_LANGUAGES.forEach { lang ->
                        val experimentalSuffix = if (lang.experimental) stringResource(R.string.home_language_experimental) else ""
                        val label = stringResource(R.string.home_language_label, lang.flag, lang.displayName) + experimentalSuffix
                        TextButton(
                            onClick = { viewModel.setLanguage(lang.code) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (lang.code == state.targetLanguage)
                                    stringResource(R.string.home_language_label_selected, label)
                                else label,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissLanguageSelector() }) {
                    Text(stringResource(R.string.home_btn_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.home_title_glosso),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.showLanguageSelector() }) {
                        Icon(Icons.Default.Language, contentDescription = stringResource(R.string.home_cd_language), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_cd_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlossoStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.home_stat_consistency),
                    value = stringResource(R.string.home_stat_days, state.streak),
                    icon = Icons.Default.Favorite,
                    color = MaterialTheme.colorScheme.tertiary
                )
                GlossoStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.home_stat_mastery),
                    value = stringResource(R.string.home_stat_phrases, state.masteryScore),
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.home_curriculum_progress),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            val subtitles = levelNames
            val codes = listOf("A1", "A2", "B1", "B2", "C1", "C2")

            val widthClass = LocalWindowWidthClass.current
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (widthClass == WindowWidthSizeClass.Expanded) 200.dp else 150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(subtitles) { index, subtitle ->
                    val stat = state.levelStats.getOrElse(index) { LevelStat(0, 10, 0f) }
                    LevelCard(
                        title = subtitle,
                        code = codes[index],
                        levelIndex = index,
                        progress = stat.progress,
                        masteredCount = stat.mastered,
                        totalCount = stat.total,
                        isDownloaded = stat.isDownloaded,
                        onClick = { viewModel.onLevelClick(index, onNavigateToStudio) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelCard(
    title: String,
    code: String,
    levelIndex: Int,
    progress: Float,
    masteredCount: Int,
    totalCount: Int,
    isDownloaded: Boolean,
    onClick: () -> Unit
) {
    val color = levelColor(levelIndex)
    val isComplete = isDownloaded && progress >= 1f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val progressPct = (progress * 100).toInt()

    val gradientColors = if (isComplete)
        listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.10f))
    else
        listOf(color.copy(alpha = 0.14f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f))

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.height(168.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isComplete) 1.5.dp else 1.dp,
            color = if (isComplete) color.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: badge row + title
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = color.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = code,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = color
                            )
                        }
                        if (isComplete) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Bottom: progress or setup
                if (isDownloaded) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.home_level_phrases, masteredCount, totalCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isComplete)
                                    stringResource(R.string.home_level_complete)
                                else
                                    stringResource(R.string.home_progress_percent, progressPct),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = color
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = animatedProgress,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = color,
                            trackColor = color.copy(alpha = 0.12f)
                        )
                    }
                } else {
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.home_tap_to_setup),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingDialog(onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    val steps = listOf(
        Triple(
            stringResource(R.string.home_onboarding_welcome_title),
            stringResource(R.string.home_onboarding_welcome_body),
            Icons.Default.RecordVoiceOver
        ),
        Triple(
            stringResource(R.string.home_onboarding_how_title),
            stringResource(R.string.home_onboarding_how_body),
            Icons.Default.Mic
        ),
        Triple(
            stringResource(R.string.home_onboarding_mastery_title),
            stringResource(R.string.home_onboarding_mastery_body),
            Icons.Default.Star
        )
    )

    val (title, body, icon) = steps[step]

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp)) },
        title = { Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == step) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == step) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (step < steps.lastIndex) step++ else onDismiss() },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (step < steps.lastIndex)
                        stringResource(R.string.btn_next)
                    else
                        stringResource(R.string.btn_lets_go)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_skip)) }
        }
    )
}
