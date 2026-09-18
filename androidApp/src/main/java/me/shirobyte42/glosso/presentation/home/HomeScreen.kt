package me.shirobyte42.glosso.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.data.local.DownloadErrorKind
import me.shirobyte42.glosso.domain.model.SUPPORTED_LANGUAGES
import me.shirobyte42.glosso.presentation.components.GlossoCard
import me.shirobyte42.glosso.presentation.components.GlossoSectionLabel
import me.shirobyte42.glosso.presentation.components.readableWidth

private val LEVEL_CODES = listOf("A1", "A2", "B1", "B2", "C1", "C2")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStudio: (Int) -> Unit,
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
            title = { Text(stringResource(R.string.home_initial_setup), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(stringResource(R.string.home_setup_description))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.home_wifi_warning_initial),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.startInitialSetup() }) {
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
            title = { Text(stringResource(R.string.home_setup_required), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(stringResource(R.string.home_download_description, levelName))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.home_wifi_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.startDownload() }) {
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
            title = { Text(titleText, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (levelName != null)
                            stringResource(R.string.home_preparing_curriculum)
                        else
                            stringResource(R.string.home_setting_up_recognition),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    LinearProgressIndicator(
                        progress = state.downloadProgress,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.home_progress_percent, (state.downloadProgress * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = { }
        )
    }

    if (state.downloadError != null) {
        val errorText = when (state.downloadErrorKind) {
            DownloadErrorKind.NETWORK -> stringResource(R.string.download_error_network)
            DownloadErrorKind.SERVER -> stringResource(R.string.download_error_server)
            DownloadErrorKind.VERIFICATION -> stringResource(R.string.download_error_verification)
            else -> stringResource(R.string.download_error_generic)
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissDownloadError() },
            title = { Text(stringResource(R.string.home_download_failed), fontWeight = FontWeight.SemiBold) },
            text = { Text(errorText) },
            confirmButton = {
                Button(onClick = { viewModel.retryDownload() }) {
                    Text(stringResource(R.string.home_btn_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDownloadError() }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (state.showLanguageSelector) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLanguageSelector() },
            title = { Text(stringResource(R.string.home_select_language), fontWeight = FontWeight.SemiBold) },
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.home_title_glosso),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    val currentLang = SUPPORTED_LANGUAGES.firstOrNull { it.code == state.targetLanguage }
                    Surface(
                        onClick = { viewModel.showLanguageSelector() },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentLang?.flag ?: "\uD83C\uDF10",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentLang?.displayName ?: stringResource(R.string.home_cd_language),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val downloaded = state.levelStats.withIndex().filter { it.value.isDownloaded }
        val current = downloaded.firstOrNull { it.value.progress < 1f } ?: downloaded.firstOrNull()

        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ContinueCard(
                    levelName = levelNames.getOrElse(current?.index ?: 0) { "" },
                    code = LEVEL_CODES[current?.index ?: 0],
                    stat = current?.value,
                    onClick = { viewModel.onLevelClick(current?.index ?: 0, onNavigateToStudio) }
                )
            }

            item {
                GlossoCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatBlock(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 18.dp, top = 16.dp, bottom = 16.dp),
                            value = state.streak.toString(),
                            label = stringResource(R.string.home_streak_label)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        StatBlock(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 18.dp, top = 16.dp, bottom = 16.dp),
                            value = state.masteryScore.toString(),
                            label = stringResource(R.string.home_mastered_label)
                        )
                    }
                }
            }

            if (!state.isScoringEnabled) {
                item {
                    ScoringOffCard(onEnable = { viewModel.requestScoringSetup() })
                }
            }

            item {
                GlossoSectionLabel(text = stringResource(R.string.home_all_levels))
            }

            item {
                GlossoCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.levelStats.forEachIndexed { index, stat ->
                            LevelRow(
                                code = LEVEL_CODES[index],
                                title = levelNames.getOrElse(index) { "" },
                                stat = stat,
                                onClick = { viewModel.onLevelClick(index, onNavigateToStudio) }
                            )
                            if (index != state.levelStats.lastIndex) {
                                Divider(
                                    modifier = Modifier.padding(start = 68.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContinueCard(
    levelName: String,
    code: String,
    stat: LevelStat?,
    onClick: () -> Unit
) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    GlossoCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = container,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (stat != null) stringResource(R.string.home_continue_label)
                else stringResource(R.string.home_get_started_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = onContainer.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (stat != null) levelName else code,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = onContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (stat != null)
                    stringResource(R.string.home_level_count, stat.mastered, stat.total)
                else
                    stringResource(R.string.home_get_started_body),
                style = MaterialTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.8f)
            )

            if (stat != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = stat.progress,
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = onContainer,
                    trackColor = onContainer.copy(alpha = 0.18f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_continue_cta),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LevelRow(
    code: String,
    title: String,
    stat: LevelStat,
    onClick: () -> Unit
) {
    val complete = stat.isDownloaded && stat.progress >= 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = if (stat.isDownloaded)
                    stringResource(R.string.home_level_count, stat.mastered, stat.total)
                else
                    stringResource(R.string.home_level_not_downloaded),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        when {
            complete -> Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            stat.isDownloaded -> Text(
                text = stringResource(R.string.home_progress_percent, (stat.progress * 100).toInt()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> Text(
                text = stringResource(R.string.home_level_setup),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ScoringOffCard(onEnable: () -> Unit) {
    GlossoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_scoring_off_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_scoring_off_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onEnable) {
                Text(
                    text = stringResource(R.string.home_scoring_off_cta),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OnboardingDialog(onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

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
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp)) },
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
            Button(onClick = { if (step < steps.lastIndex) step++ else onDismiss() }) {
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
