package me.shirobyte42.glosso.presentation.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.domain.model.LetterFeedbackModel
import me.shirobyte42.glosso.presentation.theme.GlossoFeedbackClose
import me.shirobyte42.glosso.presentation.theme.levelColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioTopBar(
    category: Int,
    targetLanguage: String,
    currentStreak: Int,
    onBack: () -> Unit,
    onShowTutorial: () -> Unit,
    onSettings: () -> Unit
) {
    val levelNames = when (targetLanguage) {
        "fr" -> listOf(
            stringResource(R.string.studio_level_fr_beginner),
            stringResource(R.string.studio_level_fr_elementary),
            stringResource(R.string.studio_level_fr_intermediate),
            stringResource(R.string.studio_level_fr_upper_int),
            stringResource(R.string.studio_level_fr_advanced),
            stringResource(R.string.studio_level_fr_mastery)
        )
        else -> listOf(
            stringResource(R.string.studio_level_beginner),
            stringResource(R.string.studio_level_elementary),
            stringResource(R.string.studio_level_intermediate),
            stringResource(R.string.studio_level_upper_int),
            stringResource(R.string.studio_level_advanced),
            stringResource(R.string.studio_level_mastery)
        )
    }
    val fallback = stringResource(R.string.studio_level_fallback, category + 1)

    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(levelColor(category), CircleShape)
                )
                Text(
                    levelNames.getOrElse(category) { fallback },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.studio_cd_back))
                }
                if (currentStreak > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "$currentStreak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onShowTutorial) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(R.string.studio_cd_how_feedback_works),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.studio_cd_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun ModelLoadErrorCard(message: String?, onGoToSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    stringResource(R.string.studio_model_load_failed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Button(
                    onClick = onGoToSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.studio_btn_go_to_settings), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun SentenceCard(
    sentenceText: String,
    translation: String?,
    ipa: String,
    letterFeedback: List<LetterFeedbackModel>?,
    isMastered: Boolean,
    isReview: Boolean,
    showIpa: Boolean,
    textStyle: TextStyle,
    onWordClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 190.dp)
                .padding(vertical = 22.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isMastered || isReview) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isReview) {
                        Surface(color = MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.studio_badge_review), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    if (isMastered) {
                        Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.studio_badge_mastered), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            ClickableSentenceText(
                text = sentenceText,
                feedback = letterFeedback,
                onWordClick = onWordClick,
                style = textStyle
            )

            translation?.let { tr ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            if (showIpa) {
                Spacer(modifier = Modifier.height(8.dp))
                WordAlignedIpa(sentenceText = sentenceText, ipa = ipa, feedback = letterFeedback)
            }
        }
    }
}

@Composable
fun PairHintsColumn(pairHints: List<me.shirobyte42.glosso.domain.model.PairHint>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        pairHints.forEach { hint ->
            Surface(
                color = GlossoFeedbackClose.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GlossoFeedbackClose.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "/${hint.expected}/ → /${hint.actual}/",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlossoFeedbackClose
                    )
                    Text(
                        hint.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreDisplay(
    feedbackScore: Int?,
    animatedScore: Int,
    animatedScoreProgress: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = feedbackScore != null,
        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = tween(400, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(300))
    ) {
        feedbackScore?.let {
            val scoreColor = when {
                it >= 85 -> MaterialTheme.colorScheme.secondary
                it >= 50 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = animatedScoreProgress,
                    modifier = Modifier.size(112.dp),
                    color = scoreColor,
                    strokeWidth = 8.dp,
                    trackColor = scoreColor.copy(alpha = 0.1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.studio_score_percent, animatedScore),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        stringResource(R.string.studio_accuracy_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Keeps the practice loop understandable without relying on icon-only controls.
 */
@Composable
fun PracticeActionCard(
    isRecording: Boolean,
    isAnalyzing: Boolean,
    hasRecordedVoice: Boolean,
    onRecord: () -> Unit,
    onPlayRecording: () -> Unit,
    onNext: () -> Unit
) {
    val actionLabel = when {
        isAnalyzing -> stringResource(R.string.studio_action_analyzing)
        isRecording -> stringResource(R.string.studio_action_stop_recording)
        else -> stringResource(R.string.studio_action_record_voice)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            Button(
                onClick = onRecord,
                enabled = !isAnalyzing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(actionLabel, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPlayRecording,
                    enabled = hasRecordedVoice && !isRecording && !isAnalyzing,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.studio_action_listen_back), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onNext,
                    enabled = !isRecording && !isAnalyzing,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.studio_action_next), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
    }
}

@Composable
fun FeedbackSummary(score: Int?, isMastered: Boolean) {
    score ?: return
    val (title, body, color) = when {
        isMastered -> Triple(
            stringResource(R.string.studio_feedback_mastered_title),
            stringResource(R.string.studio_feedback_mastered_body),
            MaterialTheme.colorScheme.secondary
        )
        score >= 70 -> Triple(
            stringResource(R.string.studio_feedback_close_title),
            stringResource(R.string.studio_feedback_close_body),
            MaterialTheme.colorScheme.tertiary
        )
        else -> Triple(
            stringResource(R.string.studio_feedback_retry_title),
            stringResource(R.string.studio_feedback_retry_body),
            MaterialTheme.colorScheme.error
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PlayRecordingButton(enabled: Boolean, onPlay: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onPlay),
        color = if (enabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent,
        border = if (enabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.studio_cd_play_recording),
                modifier = Modifier.size(26.dp),
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun NextSentenceButton(onNext: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onNext),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = stringResource(R.string.studio_cd_next_sentence),
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaybackSpeedToggle(isSlow: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (isSlow)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.SlowMotionVideo,
                contentDescription = stringResource(R.string.studio_cd_playback_speed),
                modifier = Modifier.size(16.dp),
                tint = if (isSlow)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isSlow) stringResource(R.string.studio_speed_slow_short) else stringResource(R.string.studio_speed_normal_short),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSlow)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BatchCompleteCard(
    batchTotalSize: Int,
    suggestedDrillPhoneme: String?,
    onPracticePhoneme: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 40.dp, horizontal = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                stringResource(R.string.studio_batch_complete_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                stringResource(R.string.studio_batch_complete_body, batchTotalSize),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            suggestedDrillPhoneme?.let { phoneme ->
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Text(
                    stringResource(R.string.studio_needs_practice),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = { onPracticePhoneme(phoneme) },
                    shape = RoundedCornerShape(16.dp),
                    color = GlossoFeedbackClose.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, GlossoFeedbackClose.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.studio_practice_phoneme, phoneme), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GlossoFeedbackClose)
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = GlossoFeedbackClose)
                    }
                }
            }
        }
    }
}
