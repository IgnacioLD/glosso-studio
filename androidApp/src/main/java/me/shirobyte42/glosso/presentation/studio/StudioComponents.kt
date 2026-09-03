package me.shirobyte42.glosso.presentation.studio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.domain.model.LetterFeedbackModel
import me.shirobyte42.glosso.domain.model.MatchStatusModel
import me.shirobyte42.glosso.presentation.theme.GlossoFeedbackClose
import me.shirobyte42.glosso.presentation.theme.GlossoFeedbackMissed
import me.shirobyte42.glosso.presentation.theme.levelColor

@Composable
fun ColoredPronunciationText(
    text: String,
    feedback: List<LetterFeedbackModel>? = null,
    style: TextStyle,
    textAlign: TextAlign = TextAlign.Center
) {
    if (feedback == null || feedback.isEmpty()) {
        Text(
            text = text,
            style = style,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        val annotatedString = buildAnnotatedString {
            feedback.forEach { model ->
                val color = when (model.status) {
                    MatchStatusModel.PERFECT -> Color.Unspecified
                    MatchStatusModel.CLOSE -> GlossoFeedbackClose
                    MatchStatusModel.MISSED -> GlossoFeedbackMissed
                }
                withStyle(style = SpanStyle(color = color)) {
                    append(model.char)
                }
            }
        }
        Text(
            text = annotatedString,
            style = style,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ClickableWord(
    displayText: String,
    color: Color,
    style: TextStyle,
    enabled: Boolean,
    onClick: () -> Unit,
    annotatedText: androidx.compose.ui.text.AnnotatedString? = null
) {
    var isHighlighted by remember { mutableStateOf(false) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            kotlinx.coroutines.delay(800)
            isHighlighted = false
        }
    }
    val textDecoration = if (isHighlighted) TextDecoration.Underline else TextDecoration.None
    val mod = if (enabled) Modifier.clickable {
        isHighlighted = true
        onClick()
    } else Modifier
    if (annotatedText != null) {
        Text(
            text = annotatedText,
            style = style.copy(textDecoration = textDecoration),
            modifier = mod
        )
    } else {
        Text(
            text = displayText,
            style = style.copy(
                color = color,
                textDecoration = textDecoration
            ),
            modifier = mod
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordAlignedIpa(
    sentenceText: String,
    ipa: String,
    feedback: List<LetterFeedbackModel>?
) {
    val words = sentenceText.trim().split(Regex("\\s+"))
    val ipaWords = ipa.trim().split(Regex("\\s+"))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        var charOffset = 0
        words.forEachIndexed { i, word ->
            val start = charOffset
            charOffset += word.length + 1 // +1 for space
            val ipaColor = if (feedback != null) {
                val end = minOf(start + word.length, feedback.size)
                val statuses = (start until end).mapNotNull { idx ->
                    feedback.getOrNull(idx)?.status
                }
                // Word-level color by majority, not "any": a single mis-aligned
                // phoneme shouldn't paint the whole word red. Red only when the
                // word is genuinely wrong (>50% missed); yellow when there's any
                // issue but the word is still mostly right.
                val total = statuses.size
                val missed = statuses.count { it == MatchStatusModel.MISSED }
                val close = statuses.count { it == MatchStatusModel.CLOSE }
                when {
                    total == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    missed * 2 > total -> GlossoFeedbackMissed
                    missed > 0 || close > 0 -> GlossoFeedbackClose
                    else -> MaterialTheme.colorScheme.secondary
                }
            } else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 3.dp)
            ) {
                Text(
                    text = ipaWords.getOrElse(i) { "" },
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = ipaColor
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClickableSentenceText(
    text: String,
    feedback: List<LetterFeedbackModel>?,
    onWordClick: (String) -> Unit,
    style: TextStyle,
    textAlign: TextAlign = TextAlign.Center
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (textAlign == TextAlign.Center) Arrangement.Center else Arrangement.Start
    ) {
        var charOffset = 0
        Regex("\\S+|\\s+").findAll(text).forEach { match ->
            val token = match.value
            val start = charOffset
            charOffset += token.length
            val isWord = token.isNotBlank()

            val annotated = if (feedback != null && isWord) {
                buildAnnotatedString {
                    token.forEachIndexed { charIdx, char ->
                        val globalIdx = start + charIdx
                        val status = feedback.getOrNull(globalIdx)?.status
                        val charColor = when (status) {
                            MatchStatusModel.MISSED -> GlossoFeedbackMissed
                            MatchStatusModel.CLOSE -> GlossoFeedbackClose
                            else -> Color.Unspecified
                        }
                        if (charColor != Color.Unspecified) {
                            pushStyle(SpanStyle(color = charColor))
                            append(char)
                            pop()
                        } else {
                            append(char)
                        }
                    }
                }
            } else null

            val cleanForTts = token.filter { it.isLetter() }
            ClickableWord(
                displayText = token,
                color = Color.Unspecified,
                style = style,
                enabled = isWord && cleanForTts.isNotEmpty(),
                onClick = { onWordClick(cleanForTts) },
                annotatedText = annotated
            )
        }
    }
}

@Composable
fun StudioTutorialOverlay(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.tutorial_how_to_read_feedback),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.tutorial_intro),
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeedbackGuideItem(
                        color = MaterialTheme.colorScheme.onSurface,
                        label = stringResource(R.string.feedback_correct_label),
                        description = stringResource(R.string.feedback_correct_desc)
                    )
                    FeedbackGuideItem(
                        color = GlossoFeedbackClose,
                        label = stringResource(R.string.feedback_close_label),
                        description = stringResource(R.string.feedback_close_desc)
                    )
                    FeedbackGuideItem(
                        color = GlossoFeedbackMissed,
                        label = stringResource(R.string.feedback_missed_label),
                        description = stringResource(R.string.feedback_missed_desc)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.tutorial_mastery_note),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.tutorial_btn_got_it), fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun FeedbackGuideItem(color: Color, label: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MilestoneCelebrationDialog(milestone: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "\uD83C\uDF1F",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.milestone_caps_title),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "$milestone",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.milestone_sentences_mastered),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.milestone_btn_keep_going), fontWeight = FontWeight.Black)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun BatchProgressBar(mastered: Int, total: Int, levelIndex: Int = 0) {
    if (total == 0) return
    val color = levelColor(levelIndex)
    val animatedProgress by animateFloatAsState(
        targetValue = if (total > 0) mastered.toFloat() / total else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "batchProgress"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.studio_batch_progress_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                stringResource(R.string.studio_batch_progress_count, mastered, total),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = if (mastered == total) MaterialTheme.colorScheme.secondary else color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (mastered == total) MaterialTheme.colorScheme.secondary else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
