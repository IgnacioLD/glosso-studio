package me.shirobyte42.glosso.presentation.studio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.core.content.ContextCompat
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.presentation.LocalWindowWidthClass
import me.shirobyte42.glosso.domain.model.LetterFeedbackModel
import me.shirobyte42.glosso.domain.model.MatchStatusModel
import me.shirobyte42.glosso.presentation.components.MarkdownText
import me.shirobyte42.glosso.presentation.theme.GlossoFeedbackClose
import me.shirobyte42.glosso.presentation.theme.GlossoFeedbackMissed
import me.shirobyte42.glosso.presentation.theme.levelColor
import me.shirobyte42.glosso.presentation.util.TopicEmojiMap

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
                when {
                    statuses.any { it == MatchStatusModel.MISSED } -> GlossoFeedbackMissed
                    statuses.any { it == MatchStatusModel.CLOSE } -> GlossoFeedbackClose
                    statuses.isNotEmpty() -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
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
        Box(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    category: Int,
    topics: List<String>? = null,
    resume: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: StudioViewModel = koinViewModel { org.koin.core.parameter.parametersOf(category) }
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val levelColor = levelColor(category)
    val isExpandedWidth = LocalWindowWidthClass.current == WindowWidthSizeClass.Expanded

    // In-app review prompt
    if (state.shouldPromptReview) {
        viewModel.consumeReviewPrompt()
        LaunchedEffect(Unit) {
            try {
                val reviewManager = com.google.android.play.core.review.ReviewManagerFactory.create(context)
                reviewManager.requestReviewFlow().addOnCompleteListener { request ->
                    if (request.isSuccessful) {
                        val activity = context as? androidx.activity.ComponentActivity
                        activity?.let { reviewManager.launchReviewFlow(it, request.result) }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    if (state.isTutorialVisible) {
        StudioTutorialOverlay(onDismiss = { viewModel.dismissTutorial() })
    }

    state.pendingMilestone?.let { milestone ->
        MilestoneCelebrationDialog(
            milestone = milestone,
            onDismiss = { viewModel.acknowledgeMilestone() }
        )
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Count-up animation for score
    val animatedScore by animateIntAsState(
        targetValue = state.feedback?.score ?: 0,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "scoreCountUp"
    )
    val animatedScoreProgress by animateFloatAsState(
        targetValue = (state.feedback?.score ?: 0) / 100f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "scoreProgress"
    )

    // Haptic feedback on score reveal
    LaunchedEffect(state.feedback) {
        state.feedback?.let { fb ->
            if (fb.score >= 85) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            else haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(category, topics, resume) {
        viewModel.loadTopics(category)
        if (resume) {
            viewModel.resumeBatch(category)
        } else {
            viewModel.setTopics(category, topics ?: emptyList())
        }
    }

    // Auto-play reference voice when a new sentence loads
    val currentSentenceText = state.currentSentence?.text
    LaunchedEffect(currentSentenceText) {
        if (currentSentenceText != null && !state.isLoading) {
            kotlinx.coroutines.delay(300)
            viewModel.playReference()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.studio_app_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Level color accent dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(levelColor, CircleShape)
                            )
                            val levelNames = when (state.targetLanguage) {
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
                            Text(
                                levelNames.getOrElse(category) { fallback }.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                        if (state.currentStreak > 0) {
                            Surface(
                                color = Color(0xFFFF5722).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5722),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "${state.currentStreak}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFF5722)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showTutorial() }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.studio_cd_how_feedback_works),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.studio_cd_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Model load failure — full-screen error overlay
        if (state.modelError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                            state.modelError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = onNavigateToSettings,
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
            return@Scaffold
        }

        if (isExpandedWidth && !state.isBatchComplete) {
            // Expanded (tablet/landscape) — two-column layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left column: sentence card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (state.batchTotalSize > 0) {
                        BatchProgressBar(mastered = state.batchMasteredCount, total = state.batchTotalSize, levelIndex = category)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    state.currentSentence?.let { sentence ->
                        val isReview = sentence.text in state.reviewSentenceTexts
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (state.isMastered || isReview) {
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
                                        if (state.isMastered) {
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
                                ClickableSentenceText(text = sentence.text, feedback = state.feedback?.letterFeedback, onWordClick = { viewModel.speakWord(it) }, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 40.sp))
                                sentence.translationFor(state.uiLanguage)?.takeIf { state.isTranslationVisible && it.isNotBlank() }?.let { tr ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    )
                                }
                                if (state.isIpaVisible && sentence.ipa.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    WordAlignedIpa(sentenceText = sentence.text, ipa = sentence.ipa, feedback = state.feedback?.letterFeedback)
                                }
                            }
                        }
                        val pairHints = state.feedback?.pairHints
                        if (!pairHints.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                pairHints.forEach { hint ->
                                    Surface(color = GlossoFeedbackClose.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, GlossoFeedbackClose.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("/${hint.expected}/ → /${hint.actual}/", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GlossoFeedbackClose)
                                            Text(hint.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Right column: controls and score
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Score
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(visible = state.feedback != null, enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = tween(400, easing = FastOutSlowInEasing)), exit = fadeOut(tween(300))) {
                            state.feedback?.let { feedback ->
                                val scoreColor = when { feedback.score >= 85 -> MaterialTheme.colorScheme.secondary; feedback.score >= 50 -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.error }
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(progress = animatedScoreProgress, modifier = Modifier.size(130.dp), color = scoreColor, strokeWidth = 9.dp, trackColor = scoreColor.copy(alpha = 0.1f))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(stringResource(R.string.studio_score_percent, animatedScore), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                                        Text(stringResource(R.string.studio_accuracy_label), style = MaterialTheme.typography.labelSmall, color = scoreColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Record button
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isRecording) { Box(modifier = Modifier.size(140.dp).scale(pulseScale).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))) }
                        if (state.isAnalyzing) { CircularProgressIndicator(modifier = Modifier.size(100.dp), strokeWidth = 4.dp, color = MaterialTheme.colorScheme.primary) }
                        FloatingActionButton(onClick = { if (hasPermission) viewModel.toggleRecording() else launcher.launch(Manifest.permission.RECORD_AUDIO) }, containerColor = if (state.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(84.dp)) {
                            Icon(if (state.isRecording) Icons.Default.Stop else Icons.Default.Mic, null, modifier = Modifier.size(38.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).clickable(enabled = state.hasRecordedVoice) { viewModel.playRecordedVoice() }, color = if (state.hasRecordedVoice) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, stringResource(R.string.studio_cd_play_recording), modifier = Modifier.size(26.dp), tint = if (state.hasRecordedVoice) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) }
                        }
                        Surface(modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).clickable { viewModel.advanceInBatch() }, color = Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowForward, stringResource(R.string.studio_cd_next_sentence), modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        } else Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Batch progress bar
                if (state.batchTotalSize > 0) {
                    BatchProgressBar(
                        mastered = state.batchMasteredCount,
                        total = state.batchTotalSize,
                        levelIndex = category
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Batch complete card OR sentence card
                if (state.isBatchComplete) {
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
                                stringResource(R.string.studio_batch_complete_body, state.batchTotalSize),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            state.suggestedDrillPhoneme?.let { phoneme ->
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
                                    onClick = { viewModel.startDrillBatch(category, phoneme) },
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
                } else {
                    // Sentence card — premium feel with clear border
                    state.currentSentence?.let { sentence ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val isReview = sentence.text in state.reviewSentenceTexts
                                if (state.isMastered || isReview) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isReview) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiary,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Refresh,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        stringResource(R.string.studio_badge_review),
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                        if (state.isMastered) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        stringResource(R.string.studio_badge_mastered),
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }

                                ClickableSentenceText(
                                    text = sentence.text,
                                    feedback = state.feedback?.letterFeedback,
                                    onWordClick = { viewModel.speakWord(it) },
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 36.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                // Translation (meaning in the user's interface language)
                                sentence.translationFor(state.uiLanguage)?.takeIf { state.isTranslationVisible && it.isNotBlank() }?.let { tr ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    )
                                }

                                // IPA display — unified word-aligned layout across every language
                                if (state.isIpaVisible && sentence.ipa.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    WordAlignedIpa(
                                        sentenceText = sentence.text,
                                        ipa = sentence.ipa,
                                        feedback = state.feedback?.letterFeedback
                                    )
                                }
                            }
                        }

                        // Pair hints — show after feedback when there are CLOSE phoneme confusions
                        val pairHints = state.feedback?.pairHints
                        if (!pairHints.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
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
                    } ?: run {
                        if (state.isLoading) {
                            Box(modifier = Modifier.height(150.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Single Play button (no voice variants in v2.2)
                if (!state.isBatchComplete) {
                    val hasAudio = state.currentSentence?.audio1 != null
                    if (hasAudio) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setVoiceIndex(0, autoPlay = true) },
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.studio_btn_play),
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.studio_btn_play).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Playback speed toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { viewModel.togglePlaybackSpeed() },
                            shape = RoundedCornerShape(12.dp),
                            color = if (state.playbackSpeed < 1.0f)
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
                                    tint = if (state.playbackSpeed < 1.0f)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (state.playbackSpeed < 1.0f) stringResource(R.string.studio_speed_slow_short) else stringResource(R.string.studio_speed_normal_short),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.playbackSpeed < 1.0f)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Score display with count-up animation
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.animation.AnimatedVisibility(
                            visible = state.feedback != null,
                            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                                initialScale = 0.85f,
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            state.feedback?.let { feedback ->
                                val scoreColor = when {
                                    feedback.score >= 85 -> MaterialTheme.colorScheme.secondary
                                    feedback.score >= 50 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }

                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = animatedScoreProgress,
                                        modifier = Modifier.size(130.dp),
                                        color = scoreColor,
                                        strokeWidth = 9.dp,
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

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Control bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isRecording) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                    )
                }

                if (state.isBatchComplete) {
                    Button(
                        onClick = { viewModel.loadBatch(category) },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.studio_btn_next_batch), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play recording button
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable(enabled = state.hasRecordedVoice) { viewModel.playRecordedVoice() },
                            color = if (state.hasRecordedVoice) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent,
                            border = if (state.hasRecordedVoice) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.studio_cd_play_recording),
                                    modifier = Modifier.size(26.dp),
                                    tint = if (state.hasRecordedVoice) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Record button
                        Box(contentAlignment = Alignment.Center) {
                            if (state.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(100.dp),
                                    strokeWidth = 4.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            FloatingActionButton(
                                onClick = {
                                    if (hasPermission) viewModel.toggleRecording()
                                    else launcher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                containerColor = if (state.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(84.dp)
                            ) {
                                Icon(
                                    if (state.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Next sentence button
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.advanceInBatch() },
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
                }
            }
        }
    }
}
