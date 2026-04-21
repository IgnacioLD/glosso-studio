package me.shirobyte42.glosso.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    fun getSelectedVoice(): Int
    fun setSelectedVoice(index: Int)
    
    fun getLastLevel(): Int
    fun setLastLevel(level: Int)
    
    // Professional Progress
    fun getMasteryStreakFlow(): Flow<Int>
    suspend fun getMasteryStreak(): Int
    fun setMasteryStreak(streak: Int)
    
    fun getMasteryCombo(): Int
    fun setMasteryCombo(combo: Int)
    
    fun getBestMasteryStreak(): Int
    fun setBestMasteryStreak(streak: Int)

    // Mastered Sentences Tracking
    suspend fun isSentenceMastered(text: String): Boolean
    suspend fun markSentenceAsMastered(text: String, category: Int, topic: String = "")
    suspend fun getMasteredSentences(): Set<String>
    suspend fun getMasteryCountForCategory(category: Int): Int
    suspend fun getMasteryCountForCategoryAndTopic(category: Int, topic: String): Int
    suspend fun getTotalMasteryCount(): Int
    suspend fun resetProgress()

    // Mastery milestones
    fun getAcknowledgedMilestone(): Int
    fun setAcknowledgedMilestone(milestone: Int)

    // Phoneme statistics
    fun incrementPhonemeStats(phoneme: String, missed: Boolean)
    fun getWeakPhonemes(minMissed: Int = 5): List<String>

    // Spaced repetition
    suspend fun getDueReviews(levelIndex: Int): List<String>
    suspend fun scheduleReview(text: String, levelIndex: Int)
    suspend fun updateReviewResult(text: String, mastered: Boolean)

    fun isTutorialShown(): Boolean
    fun setTutorialShown(shown: Boolean)

    fun isOnboardingShown(): Boolean
    fun setOnboardingShown(shown: Boolean)

    fun getLastReviewPromptMs(): Long
    fun setLastReviewPromptMs(ms: Long)

    // Playback settings
    fun getPlaybackSpeed(): Float
    fun setPlaybackSpeed(speed: Float)

    // Learning display settings
    fun isIpaVisible(): Boolean
    fun setIpaVisible(visible: Boolean)
    fun getIpaVisibleFlow(): kotlinx.coroutines.flow.Flow<Boolean>

    // Theme (0=system, 1=light, 2=dark)
    fun getThemeMode(): Int
    fun setThemeMode(mode: Int)

    // Target learning language ("en", "fr", ...)
    fun getTargetLanguage(): String
    fun setTargetLanguage(code: String)
    fun getTargetLanguageFlow(): kotlinx.coroutines.flow.Flow<String>

    // Interface / translation language (what we translate sentences into). Defaults to "en".
    fun getUiLanguage(): String
    fun setUiLanguage(code: String)

    // Show sentence translation under the text in StudioScreen
    fun isTranslationVisible(): Boolean
    fun setTranslationVisible(visible: Boolean)
    fun getTranslationVisibleFlow(): kotlinx.coroutines.flow.Flow<Boolean>

    // Latin experimental warning acknowledgment
    fun isLatinWarningAcknowledged(): Boolean
    fun setLatinWarningAcknowledged(acknowledged: Boolean)

    // One-shot flag: true once v2.0 → v2.1 cleanup has executed
    fun isMigratedToV10(): Boolean
    fun setMigratedToV10(migrated: Boolean)

    // Batch persistence
    fun saveBatch(levelIndex: Int, queueTexts: List<String>, masteredCount: Int, totalSize: Int)
    fun hasSavedBatch(levelIndex: Int): Boolean
    fun getSavedBatchQueueTexts(): List<String>
    fun getSavedBatchMasteredCount(): Int
    fun getSavedBatchTotalSize(): Int
    fun clearSavedBatch()
}
