package me.shirobyte42.glosso.presentation.topic

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.presentation.LocalWindowWidthClass
import me.shirobyte42.glosso.presentation.studio.StudioViewModel
import me.shirobyte42.glosso.presentation.util.TopicEmojiMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSelectionScreen(
    levelIndex: Int,
    onNavigateBack: () -> Unit,
    onStartPractice: (Int, List<String>) -> Unit,
    onContinueBatch: (Int) -> Unit = {},
    viewModel: StudioViewModel = koinViewModel { org.koin.core.parameter.parametersOf(levelIndex) }
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
    val levelFallback = stringResource(R.string.topics_level_fallback, levelIndex + 1)

    LaunchedEffect(levelIndex) {
        viewModel.loadTopics(levelIndex)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.topics_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            levelNames.getOrElse(levelIndex) { levelFallback }.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.topics_back))
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 12.dp, bottom = 16.dp)
                    ) {
                        if (state.hasSavedBatch) {
                            OutlinedButton(
                                onClick = { onContinueBatch(levelIndex) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.topics_continue_batch_label,
                                        state.savedBatchMasteredCount,
                                        state.savedBatchTotalSize
                                    ),
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { onStartPractice(levelIndex, state.selectedTopics) },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.selectedTopics.isEmpty())
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (state.selectedTopics.isEmpty())
                                    stringResource(R.string.topics_practice_all_cta)
                                else
                                    stringResource(R.string.topics_practice_selected_cta, state.selectedTopics.size),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
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

            Text(
                text = stringResource(R.string.topics_select_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            val widthClass = LocalWindowWidthClass.current
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (widthClass == WindowWidthSizeClass.Expanded) 180.dp else 150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(state.topics) { index, topic ->
                    val isSelected = state.selectedTopics.contains(topic)

                    // Staggered entry animation
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(topic) {
                        kotlinx.coroutines.delay(index * 30L)
                        visible = true
                    }
                    val alpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(200),
                        label = "topicAlpha"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (visible) 1f else 0.92f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        label = "topicScale"
                    )

                    TopicCard(
                        topic = topic,
                        emoji = TopicEmojiMap.getEmoji(topic),
                        isSelected = isSelected,
                        masteredCount = state.topicMasteryCounts[topic] ?: 0,
                        totalCount = state.topicTotalCounts[topic] ?: 0,
                        modifier = Modifier.alpha(alpha).scale(scale),
                        onClick = { viewModel.toggleTopic(topic) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCard(
    topic: String,
    emoji: String,
    isSelected: Boolean,
    masteredCount: Int = 0,
    totalCount: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.height(130.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Larger emoji
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = topic.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (totalCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.topics_mastered_count, masteredCount, totalCount),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
