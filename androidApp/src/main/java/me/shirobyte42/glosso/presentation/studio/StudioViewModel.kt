package me.shirobyte42.glosso.presentation.studio

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.shirobyte42.glosso.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.shirobyte42.glosso.domain.model.PronunciationFeedback
import me.shirobyte42.glosso.domain.model.Sentence
import me.shirobyte42.glosso.domain.repository.GlossoRepository
import me.shirobyte42.glosso.domain.repository.PreferenceRepository
import me.shirobyte42.glosso.domain.repository.SpeechController
import me.shirobyte42.glosso.domain.usecase.UpdateMasteryUseCase
import me.shirobyte42.glosso.data.local.SentenceDao
import me.shirobyte42.glosso.data.audio.PhonemeRecognizer
import me.shirobyte42.glosso.data.audio.GlossoTtsController
import me.shirobyte42.glosso.data.audio.ModelState

class StudioViewModel(
    private val repository: GlossoRepository,
    private val sentenceDao: SentenceDao,
    private val speechController: SpeechController,
    private val prefs: PreferenceRepository,
    private val updateMastery: UpdateMasteryUseCase,
    private val recognizer: PhonemeRecognizer,
    private val ttsController: GlossoTtsController,
    private val appContext: Context
) : ViewModel() {
    private val TAG = "StudioViewModel"

    companion object {
        const val BATCH_SIZE = 10
    }

    private val _uiState = MutableStateFlow(StudioUiState(
        selectedVoiceIndex = prefs.getSelectedVoice(),
        currentStreak = prefs.getMasteryCombo(),
        isTutorialVisible = !prefs.isTutorialShown(),
        playbackSpeed = prefs.getPlaybackSpeed(),
        isIpaVisible = prefs.isIpaVisible(),
        isTranslationVisible = prefs.isTranslationVisible(),
        targetLanguage = prefs.getTargetLanguage(),
        uiLanguage = prefs.getUiLanguage().ifEmpty { "en" }
    ))
    val uiState: StateFlow<StudioUiState> = _uiState

    private var lastAudioBase64: String? = null

    init {
        viewModelScope.launch {
            recognizer.modelState.collect { state ->
                if (state == ModelState.FAILED) {
                    val msg = recognizer.modelError
                        ?: appContext.getString(R.string.studio_err_model_failed_default)
                    _uiState.update { it.copy(
                        modelError = "$msg " + appContext.getString(R.string.studio_err_model_redownload_suffix)
                    ) }
                }
            }
        }
        viewModelScope.launch {
            prefs.getIpaVisibleFlow().collect { visible ->
                _uiState.update { it.copy(isIpaVisible = visible) }
            }
        }
        viewModelScope.launch {
            prefs.getTranslationVisibleFlow().collect { visible ->
                _uiState.update { it.copy(isTranslationVisible = visible) }
            }
        }
        viewModelScope.launch {
            prefs.getTargetLanguageFlow().collect { lang ->
                _uiState.update { it.copy(targetLanguage = lang) }
            }
        }
    }

    fun dismissTutorial() {
        prefs.setTutorialShown(true)
        _uiState.update { it.copy(isTutorialVisible = false) }
    }

    fun showTutorial() {
        _uiState.update { it.copy(isTutorialVisible = true) }
    }

    fun loadTopics(levelIndex: Int) {
        viewModelScope.launch {
            try {
                val lang = prefs.getTargetLanguage()
                val topics = repository.getTopics(levelIndex, lang)
                val hasBatch = prefs.hasSavedBatch(levelIndex)
                val topicCounts = topics.associateWith { topic ->
                    prefs.getMasteryCountForCategoryAndTopic(levelIndex, topic)
                }
                val topicTotals = topics.associateWith { topic ->
                    repository.getSentenceCountByTopic(levelIndex, lang, topic)
                }
                _uiState.update { it.copy(
                    topics = topics,
                    hasSavedBatch = hasBatch,
                    savedBatchMasteredCount = if (hasBatch) prefs.getSavedBatchMasteredCount() else 0,
                    savedBatchTotalSize = if (hasBatch) prefs.getSavedBatchTotalSize() else 0,
                    topicMasteryCounts = topicCounts,
                    topicTotalCounts = topicTotals
                ) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load topics", e)
            }
        }
    }

    fun toggleTopic(topic: String) {
        _uiState.update { state ->
            val current = state.selectedTopics
            if (current.contains(topic)) {
                state.copy(selectedTopics = current - topic)
            } else {
                state.copy(selectedTopics = current + topic)
            }
        }
    }

    fun setTopics(levelIndex: Int, topics: List<String>) {
        _uiState.update { it.copy(selectedTopics = topics) }
        loadBatch(levelIndex)
    }

    fun loadBatch(levelIndex: Int) {
        Log.d(TAG, "Loading batch for level $levelIndex")
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                error = null,
                feedback = null,
                isMastered = false,
                hasRecordedVoice = false,
                isBatchComplete = false,
                batchMasteredCount = 0,
                batchQueue = emptyList(),
                batchTotalSize = 0
            ) }
            prefs.setLastLevel(levelIndex)
            lastAudioBase64 = null

            try {
                val currentTopics = _uiState.value.selectedTopics
                val masteredSentences = prefs.getMasteredSentences().toList()

                // Prepend up to 3 due reviews
                val dueReviewTexts = prefs.getDueReviews(levelIndex).take(3)
                val reviewSentences = if (dueReviewTexts.isNotEmpty()) {
                    repository.getSentencesByTexts(levelIndex, prefs.getTargetLanguage(), dueReviewTexts)
                } else emptyList()

                val freshBatch = repository.getBatch(
                    category = levelIndex,
                    language = prefs.getTargetLanguage(),
                    topics = if (currentTopics.isEmpty()) null else currentTopics,
                    exclude = masteredSentences,
                    count = BATCH_SIZE - reviewSentences.size
                )

                val batch = reviewSentences + freshBatch
                val reviewTexts = reviewSentences.map { it.text }.toSet()

                if (batch.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = appContext.getString(R.string.studio_no_sentences)) }
                    return@launch
                }

                // Persist batch
                prefs.saveBatch(levelIndex, batch.map { it.text }, 0, batch.size)

                val first = batch.first()
                _uiState.update { it.copy(
                    isLoading = false,
                    batchQueue = batch,
                    batchTotalSize = batch.size,
                    batchMasteredCount = 0,
                    currentSentence = first,
                    isMastered = prefs.isSentenceMastered(first.text),
                    feedback = null,
                    hasRecordedVoice = false,
                    isBatchComplete = false,
                    hasSavedBatch = true,
                    savedBatchMasteredCount = 0,
                    savedBatchTotalSize = batch.size,
                    reviewSentenceTexts = reviewTexts
                ) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load batch", e)
                _uiState.update { it.copy(isLoading = false, error = appContext.getString(R.string.studio_err_load_failed, e.message ?: "")) }
            }
        }
    }

    fun resumeBatch(levelIndex: Int) {
        Log.d(TAG, "Resuming saved batch for level $levelIndex")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            prefs.setLastLevel(levelIndex)
            lastAudioBase64 = null

            try {
                val queueTexts = prefs.getSavedBatchQueueTexts()
                val masteredCount = prefs.getSavedBatchMasteredCount()
                val totalSize = prefs.getSavedBatchTotalSize()

                if (queueTexts.isEmpty()) {
                    loadBatch(levelIndex)
                    return@launch
                }

                val sentences = repository.getSentencesByTexts(levelIndex, prefs.getTargetLanguage(), queueTexts)
                // Restore original queue order
                val sentenceMap = sentences.associateBy { it.text }
                val queue = queueTexts.mapNotNull { sentenceMap[it] }

                if (queue.isEmpty()) {
                    loadBatch(levelIndex)
                    return@launch
                }

                val first = queue.first()
                _uiState.update { it.copy(
                    isLoading = false,
                    batchQueue = queue,
                    batchTotalSize = totalSize,
                    batchMasteredCount = masteredCount,
                    currentSentence = first,
                    isMastered = prefs.isSentenceMastered(first.text),
                    feedback = null,
                    hasRecordedVoice = false,
                    isBatchComplete = false
                ) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume batch, starting fresh", e)
                loadBatch(levelIndex)
            }
        }
    }

    fun advanceInBatch() {
        val state = _uiState.value
        val current = state.currentSentence ?: return
        val score = state.feedback?.score ?: 0
        val mastered = score >= 85

        val remaining = state.batchQueue.drop(1)
        val newQueue = if (mastered) remaining else remaining + current
        val newMasteredCount = state.batchMasteredCount + if (mastered) 1 else 0

        lastAudioBase64 = null

        val levelIndex = prefs.getLastLevel()

        if (newQueue.isEmpty()) {
            prefs.clearSavedBatch()
            val weakPhonemes = prefs.getWeakPhonemes(minMissed = 5)
            viewModelScope.launch {
                val totalMastery = prefs.getTotalMasteryCount()
                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                val shouldReview = totalMastery >= 5 &&
                    System.currentTimeMillis() - prefs.getLastReviewPromptMs() > thirtyDaysMs
                if (shouldReview) {
                    prefs.setLastReviewPromptMs(System.currentTimeMillis())
                    _uiState.update { it.copy(shouldPromptReview = true) }
                }
            }
            _uiState.update { it.copy(
                isBatchComplete = true,
                batchMasteredCount = newMasteredCount,
                batchQueue = emptyList(),
                feedback = null,
                hasRecordedVoice = false,
                error = null,
                currentStreak = prefs.getMasteryCombo(),
                hasSavedBatch = false,
                suggestedDrillPhoneme = weakPhonemes.firstOrNull()
            ) }
        } else {
            prefs.saveBatch(levelIndex, newQueue.map { it.text }, newMasteredCount, state.batchTotalSize)
            val next = newQueue.first()
            viewModelScope.launch {
                val isMastered = prefs.isSentenceMastered(next.text)
                _uiState.update { it.copy(
                    batchQueue = newQueue,
                    batchMasteredCount = newMasteredCount,
                    currentSentence = next,
                    isMastered = isMastered,
                    feedback = null,
                    hasRecordedVoice = false,
                    error = null,
                    savedBatchMasteredCount = newMasteredCount,
                    savedBatchTotalSize = state.batchTotalSize
                ) }
            }
        }
    }

    fun setVoiceIndex(index: Int, autoPlay: Boolean = true) {
        _uiState.update { it.copy(selectedVoiceIndex = index) }
        prefs.setSelectedVoice(index)
        if (autoPlay) playReference()
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            try {
                val base64 = speechController.stopRecording()
                _uiState.update { it.copy(isRecording = false) }
                if (base64 != null) {
                    lastAudioBase64 = base64
                    _uiState.update { it.copy(hasRecordedVoice = true) }
                    analyzeSpeech(base64)
                } else {
                    _uiState.update { it.copy(error = appContext.getString(R.string.error_recording_failed)) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRecording = false, error = appContext.getString(R.string.studio_err_stop_failed, e.message ?: "")) }
            }
        } else {
            try {
                speechController.startRecording()
                _uiState.update { it.copy(isRecording = true, error = null, feedback = null, hasRecordedVoice = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRecording = false, error = appContext.getString(R.string.studio_err_start_failed, e.message ?: "")) }
            }
        }
    }

    private fun analyzeSpeech(base64Audio: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            try {
                val sentence = _uiState.value.currentSentence ?: return@launch
                val startTime = System.currentTimeMillis()

                val feedback = withContext(Dispatchers.Default) {
                    Log.d(TAG, "Attempting on-device recognition...")
                    val recognizedIpa = speechController.recognize(base64Audio)

                    if (recognizedIpa != null) {
                        Log.d(TAG, "On-device recognition success: $recognizedIpa")
                        val result = speechController.calculateScore(sentence.text, sentence.ipa, recognizedIpa)
                        Log.d(TAG, "On-device scoring result: ${result.score}")
                        result
                    } else {
                        Log.e(TAG, "On-device recognition failed.")
                        throw Exception(appContext.getString(R.string.studio_err_recognition_failed_setup))
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 300) delay(300 - elapsed)

                val levelIndex = prefs.getLastLevel()
                val topic = sentence.topic
                val result = updateMastery(feedback.score, sentence.text, levelIndex, topic)

                // Track phoneme stats
                feedback.alignment.forEach { match ->
                    if (match.expected != "-") {
                        prefs.incrementPhonemeStats(match.expected, missed = match.status.name == "MISSED")
                    }
                }

                _uiState.update { it.copy(
                    isAnalyzing = false,
                    feedback = feedback,
                    isMastered = result.isNewMastery || _uiState.value.isMastered,
                    currentStreak = result.currentStreak
                ) }

                // Schedule for review if newly mastered; update review if it was a review sentence
                val isReview = _uiState.value.reviewSentenceTexts.contains(sentence.text)
                if (result.isNewMastery) {
                    prefs.scheduleReview(sentence.text, levelIndex)
                    checkMilestone()
                } else if (isReview) {
                    prefs.updateReviewResult(sentence.text, mastered = feedback.score >= 85)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed", e)
                _uiState.update { it.copy(isAnalyzing = false, error = appContext.getString(R.string.error_analysis_failed)) }
            }
        }
    }

    fun startDrillBatch(levelIndex: Int, phoneme: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true, error = null, feedback = null,
                isMastered = false, hasRecordedVoice = false,
                isBatchComplete = false, batchMasteredCount = 0,
                batchQueue = emptyList(), batchTotalSize = 0,
                suggestedDrillPhoneme = null
            ) }
            lastAudioBase64 = null
            try {
                val masteredSentences = prefs.getMasteredSentences().toList()
                val batch = repository.getBatchForPhoneme(levelIndex, prefs.getTargetLanguage(), phoneme, masteredSentences, BATCH_SIZE)
                if (batch.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = appContext.getString(R.string.studio_err_no_drill_sentences, phoneme)) }
                    return@launch
                }
                val first = batch.first()
                _uiState.update { it.copy(
                    isLoading = false, batchQueue = batch, batchTotalSize = batch.size,
                    batchMasteredCount = 0, currentSentence = first,
                    isMastered = prefs.isSentenceMastered(first.text),
                    feedback = null, hasRecordedVoice = false, isBatchComplete = false
                ) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load drill batch", e)
                _uiState.update { it.copy(isLoading = false, error = appContext.getString(R.string.studio_err_drill_load_failed, e.message ?: "")) }
            }
        }
    }

    private suspend fun checkMilestone() {
        val milestones = listOf(10, 25, 50, 100, 250, 500, 1000)
        val totalMastery = prefs.getTotalMasteryCount()
        val acknowledged = prefs.getAcknowledgedMilestone()
        val nextMilestone = milestones.firstOrNull { it > acknowledged && it <= totalMastery }
        if (nextMilestone != null) {
            prefs.setAcknowledgedMilestone(nextMilestone)
            _uiState.update { it.copy(pendingMilestone = nextMilestone) }
        }
    }

    fun acknowledgeMilestone() {
        _uiState.update { it.copy(pendingMilestone = null) }
    }

    fun consumeReviewPrompt() {
        _uiState.update { it.copy(shouldPromptReview = false) }
    }

    fun togglePlaybackSpeed() {
        val newSpeed = if (_uiState.value.playbackSpeed == 1.0f) 0.75f else 1.0f
        prefs.setPlaybackSpeed(newSpeed)
        _uiState.update { it.copy(playbackSpeed = newSpeed) }
    }

    fun playReference() {
        val sentence = _uiState.value.currentSentence ?: return
        val voiceIndex = _uiState.value.selectedVoiceIndex
        val audio = if (voiceIndex == 0) sentence.audio1 else sentence.audio2 ?: sentence.audio1
        audio?.let {
            speechController.playAudio(it, prefs.getPlaybackSpeed())
        }
    }

    fun playRecordedVoice() {
        lastAudioBase64?.let { recorded ->
            speechController.playNormalized(recorded) {
                playReference()
            }
        }
    }

    fun speakWord(word: String) {
        val sentence = _uiState.value.currentSentence
        val cleanWord = word.trim('.', ',', '!', '?', ';', ':')
        val offset = sentence?.wordOffsets?.firstOrNull {
            it.w.equals(cleanWord, ignoreCase = true)
        }
        val audio = sentence?.let {
            if (_uiState.value.selectedVoiceIndex == 0) it.audio1 else it.audio2
        }
        if (offset != null && audio != null) {
            speechController.playWordSlice(audio, offset.s, offset.e)
        } else {
            ttsController.speak(cleanWord)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechController.stopPlayback()
        ttsController.shutdown()
    }
}

data class StudioUiState(
    val currentSentence: Sentence? = null,
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val isAnalyzing: Boolean = false,
    val feedback: PronunciationFeedback? = null,
    val selectedVoiceIndex: Int = 0,
    val isMastered: Boolean = false,
    val currentStreak: Int = 0,
    val topics: List<String> = emptyList(),
    val selectedTopics: List<String> = emptyList(),
    val hasRecordedVoice: Boolean = false,
    val isTutorialVisible: Boolean = false,
    val error: String? = null,
    val playbackSpeed: Float = 1.0f,
    val isIpaVisible: Boolean = false,
    val isTranslationVisible: Boolean = true,
    val modelError: String? = null,
    val pendingMilestone: Int? = null,
    val topicMasteryCounts: Map<String, Int> = emptyMap(),
    val topicTotalCounts: Map<String, Int> = emptyMap(),
    val suggestedDrillPhoneme: String? = null,
    val reviewSentenceTexts: Set<String> = emptySet(),
    // Active batch state
    val batchQueue: List<Sentence> = emptyList(),
    val batchTotalSize: Int = 0,
    val batchMasteredCount: Int = 0,
    val isBatchComplete: Boolean = false,
    // Saved batch info (for TopicSelectionScreen)
    val hasSavedBatch: Boolean = false,
    val savedBatchMasteredCount: Int = 0,
    val savedBatchTotalSize: Int = 0,
    val targetLanguage: String = "en_GB",
    val uiLanguage: String = "en",
    val shouldPromptReview: Boolean = false
)
