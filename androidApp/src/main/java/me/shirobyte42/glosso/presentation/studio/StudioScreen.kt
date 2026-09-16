package me.shirobyte42.glosso.presentation.studio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import org.koin.androidx.compose.koinViewModel
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.presentation.LocalWindowWidthClass

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
    val isExpandedWidth = LocalWindowWidthClass.current == WindowWidthSizeClass.Expanded

    // In-app review prompt (no-op in F-Droid flavor)
    if (state.shouldPromptReview) {
        viewModel.consumeReviewPrompt()
        LaunchedEffect(Unit) {
            me.shirobyte42.glosso.util.launchInAppReview(context)
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
    var showMicrophoneDisclosure by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    if (showMicrophoneDisclosure) {
        AlertDialog(
            onDismissRequest = { showMicrophoneDisclosure = false },
            title = { Text(stringResource(R.string.studio_microphone_title)) },
            text = { Text(stringResource(R.string.studio_microphone_message)) },
            confirmButton = {
                Button(onClick = {
                    showMicrophoneDisclosure = false
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text(stringResource(R.string.studio_microphone_continue))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showMicrophoneDisclosure = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            StudioTopBar(
                category = category,
                targetLanguage = state.targetLanguage,
                currentStreak = state.currentStreak,
                onBack = onNavigateBack,
                onShowTutorial = { viewModel.showTutorial() },
                onSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        // Model load failure - full-screen error overlay
        if (state.modelError != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                ModelLoadErrorCard(
                    message = state.modelError,
                    onGoToSettings = onNavigateToSettings
                )
            }
            return@Scaffold
        }

        if (isExpandedWidth && !state.isBatchComplete) {
            // Expanded (tablet/landscape) - two-column layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
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
                        SentenceCard(
                            sentenceText = sentence.text,
                            translation = sentence.translationFor(state.uiLanguage)?.takeIf { state.isTranslationVisible && it.isNotBlank() },
                            ipa = sentence.ipa,
                            letterFeedback = state.feedback?.letterFeedback,
                            isMastered = state.isMastered,
                            isReview = sentence.text in state.reviewSentenceTexts,
                            showIpa = state.isIpaVisible && sentence.ipa.isNotBlank(),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 40.sp),
                            onWordClick = { viewModel.speakWord(it) }
                        )
                        val pairHints = state.feedback?.pairHints
                        if (!pairHints.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PairHintsColumn(pairHints)
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
                        ScoreDisplay(
                            feedbackScore = state.feedback?.score,
                            animatedScore = animatedScore,
                            animatedScoreProgress = animatedScoreProgress,
                            modifier = Modifier
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FeedbackSummary(state.feedback?.score, state.isMastered)
                    Spacer(modifier = Modifier.height(16.dp))
                    PracticeActionCard(
                        isRecording = state.isRecording,
                        isAnalyzing = state.isAnalyzing,
                        hasRecordedVoice = state.hasRecordedVoice,
                        onRecord = {
                            if (hasPermission) viewModel.toggleRecording()
                            else showMicrophoneDisclosure = true
                        },
                        onPlayRecording = { viewModel.playRecordedVoice() },
                        onNext = { viewModel.advanceInBatch() }
                    )
                }
            }
        } else Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
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
                Spacer(modifier = Modifier.height(12.dp))

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
                    BatchCompleteCard(
                        batchTotalSize = state.batchTotalSize,
                        suggestedDrillPhoneme = state.suggestedDrillPhoneme,
                        onPracticePhoneme = { viewModel.startDrillBatch(category, it) }
                    )
                } else {
                    // Sentence card
                    state.currentSentence?.let { sentence ->
                        SentenceCard(
                            sentenceText = sentence.text,
                            translation = sentence.translationFor(state.uiLanguage)?.takeIf { state.isTranslationVisible && it.isNotBlank() },
                            ipa = sentence.ipa,
                            letterFeedback = state.feedback?.letterFeedback,
                            isMastered = state.isMastered,
                            isReview = sentence.text in state.reviewSentenceTexts,
                            showIpa = state.isIpaVisible && sentence.ipa.isNotBlank(),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 30.sp
                            ),
                            onWordClick = { viewModel.speakWord(it) }
                        )

                        // Pair hints - show after feedback when there are CLOSE phoneme confusions
                        val pairHints = state.feedback?.pairHints
                        if (!pairHints.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PairHintsColumn(pairHints)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasAudio) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setVoiceIndex(0, autoPlay = true) },
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.studio_btn_play),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.studio_btn_play),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        PlaybackSpeedToggle(
                            isSlow = state.playbackSpeed < 1.0f,
                            onToggle = { viewModel.togglePlaybackSpeed() }
                        )
                    }

                    // Idle guidance fills the space before the first take so the
                    // screen never reads as empty.
                    if (state.feedback == null && !state.isRecording && !state.isAnalyzing && !state.hasRecordedVoice) {
                        IdlePracticeHint()
                    }

                    // Score display with count-up animation
                    Spacer(modifier = Modifier.height(8.dp))
                    ScoreDisplay(
                        feedbackScore = state.feedback?.score,
                        animatedScore = animatedScore,
                        animatedScoreProgress = animatedScoreProgress,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    FeedbackSummary(state.feedback?.score, state.isMastered)
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Control bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isBatchComplete) {
                    Button(
                        onClick = { viewModel.loadBatch(category) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.studio_btn_next_batch), fontWeight = FontWeight.Bold)
                    }
                } else {
                    PracticeActionCard(
                        isRecording = state.isRecording,
                        isAnalyzing = state.isAnalyzing,
                        hasRecordedVoice = state.hasRecordedVoice,
                        onRecord = {
                            if (hasPermission) viewModel.toggleRecording()
                            else showMicrophoneDisclosure = true
                        },
                        onPlayRecording = { viewModel.playRecordedVoice() },
                        onNext = { viewModel.advanceInBatch() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdlePracticeHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.studio_practice_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
