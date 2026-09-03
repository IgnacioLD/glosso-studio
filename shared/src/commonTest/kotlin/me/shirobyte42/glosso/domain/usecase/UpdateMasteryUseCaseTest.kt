package me.shirobyte42.glosso.domain.usecase

import kotlinx.coroutines.test.runTest
import me.shirobyte42.glosso.domain.model.PhonemeStat
import me.shirobyte42.glosso.domain.repository.PreferenceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateMasteryUseCaseTest {

    private class FakePreferenceRepository : PreferenceRepository {
        val masteredSentences = mutableSetOf<String>()
        val markedTopics = mutableMapOf<String, String>()
        var combo = 0
        var streak = 0

        override suspend fun isSentenceMastered(text: String): Boolean = text in masteredSentences
        override suspend fun markSentenceAsMastered(text: String, category: Int, topic: String) {
            masteredSentences.add(text)
            markedTopics[text] = topic
        }
        override fun getMasteryCombo(): Int = combo
        override fun setMasteryCombo(combo: Int) { this.combo = combo }
        override fun setMasteryStreak(streak: Int) { this.streak = streak }

        // Unused by the use case under test; fail loudly if reached
        override fun getSelectedVoice(): Int = TODO()
        override fun setSelectedVoice(index: Int) = TODO()
        override fun getLastLevel(): Int = TODO()
        override fun setLastLevel(level: Int) = TODO()
        override fun getMasteryStreakFlow() = TODO()
        override suspend fun getMasteryStreak(): Int = TODO()
        override fun getBestMasteryStreak(): Int = TODO()
        override fun setBestMasteryStreak(streak: Int) = TODO()
        override suspend fun getMasteredSentences(): Set<String> = TODO()
        override suspend fun getMasteryCountForCategory(category: Int): Int = TODO()
        override suspend fun getMasteryCountForCategoryAndTopic(category: Int, topic: String): Int = TODO()
        override suspend fun getTotalMasteryCount(): Int = TODO()
        override suspend fun resetProgress() = TODO()
        override fun getAcknowledgedMilestone(): Int = TODO()
        override fun setAcknowledgedMilestone(milestone: Int) = TODO()
        override fun incrementPhonemeStats(phoneme: String, missed: Boolean) = TODO()
        override fun getWeakPhonemes(minMissed: Int): List<String> = TODO()
        override fun getPhonemeStats(): List<PhonemeStat> = TODO()
        override suspend fun getDueReviews(levelIndex: Int): List<String> = TODO()
        override suspend fun scheduleReview(text: String, levelIndex: Int) = TODO()
        override suspend fun updateReviewResult(text: String, mastered: Boolean) = TODO()
        override fun isTutorialShown(): Boolean = TODO()
        override fun setTutorialShown(shown: Boolean) = TODO()
        override fun isOnboardingShown(): Boolean = TODO()
        override fun setOnboardingShown(shown: Boolean) = TODO()
        override fun getLastReviewPromptMs(): Long = TODO()
        override fun setLastReviewPromptMs(ms: Long) = TODO()
        override fun getPlaybackSpeed(): Float = TODO()
        override fun setPlaybackSpeed(speed: Float) = TODO()
        override fun isIpaVisible(): Boolean = TODO()
        override fun setIpaVisible(visible: Boolean) = TODO()
        override fun getIpaVisibleFlow() = TODO()
        override fun getThemeMode(): Int = TODO()
        override fun setThemeMode(mode: Int) = TODO()
        override fun getTargetLanguage(): String = TODO()
        override fun setTargetLanguage(code: String) = TODO()
        override fun getTargetLanguageFlow() = TODO()
        override fun getUiLanguage(): String = TODO()
        override fun setUiLanguage(code: String) = TODO()
        override fun isTranslationVisible(): Boolean = TODO()
        override fun setTranslationVisible(visible: Boolean) = TODO()
        override fun getTranslationVisibleFlow() = TODO()
        override fun isLatinWarningAcknowledged(): Boolean = TODO()
        override fun setLatinWarningAcknowledged(acknowledged: Boolean) = TODO()
        override fun isMigratedToV10(): Boolean = TODO()
        override fun setMigratedToV10(migrated: Boolean) = TODO()
        override fun saveBatch(levelIndex: Int, queueTexts: List<String>, masteredCount: Int, totalSize: Int) = TODO()
        override fun hasSavedBatch(levelIndex: Int): Boolean = TODO()
        override fun getSavedBatchQueueTexts(): List<String> = TODO()
        override fun getSavedBatchMasteredCount(): Int = TODO()
        override fun getSavedBatchTotalSize(): Int = TODO()
        override fun clearSavedBatch() = TODO()
    }

    @Test
    fun `first mastery marks sentence and increments combo`() = runTest {
        val prefs = FakePreferenceRepository()
        val useCase = UpdateMasteryUseCase(prefs)

        val result = useCase(score = 90, sentenceText = "hello world", category = 0, topic = "greetings")

        assertTrue(result.isNewMastery)
        assertEquals(1, result.currentStreak)
        assertEquals(1, prefs.combo)
        assertTrue("hello world" in prefs.masteredSentences)
        assertEquals("greetings", prefs.markedTopics["hello world"])
    }

    @Test
    fun `repeat mastery increments combo but is not new`() = runTest {
        val prefs = FakePreferenceRepository()
        prefs.masteredSentences.add("hello world")
        val useCase = UpdateMasteryUseCase(prefs)

        val result = useCase(score = 95, sentenceText = "hello world", category = 0)

        assertFalse(result.isNewMastery)
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `score below threshold resets combo`() = runTest {
        val prefs = FakePreferenceRepository()
        prefs.combo = 4
        val useCase = UpdateMasteryUseCase(prefs)

        val result = useCase(score = 84, sentenceText = "hello world", category = 0)

        assertFalse(result.isNewMastery)
        assertEquals(0, result.currentStreak)
        assertEquals(0, prefs.combo)
        assertTrue("hello world" !in prefs.masteredSentences)
    }

    @Test
    fun `score exactly 85 counts as mastery`() = runTest {
        val prefs = FakePreferenceRepository()
        val useCase = UpdateMasteryUseCase(prefs)

        val result = useCase(score = 85, sentenceText = "boundary", category = 0)

        assertTrue(result.isNewMastery)
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `consecutive masteries accumulate combo`() = runTest {
        val prefs = FakePreferenceRepository()
        val useCase = UpdateMasteryUseCase(prefs)

        useCase(score = 90, sentenceText = "one", category = 0)
        val second = useCase(score = 90, sentenceText = "two", category = 0)

        assertTrue(second.isNewMastery)
        assertEquals(2, second.currentStreak)
    }

    @Test
    fun `failed attempt after masteries resets streak to zero`() = runTest {
        val prefs = FakePreferenceRepository()
        val useCase = UpdateMasteryUseCase(prefs)

        useCase(score = 90, sentenceText = "one", category = 0)
        val failed = useCase(score = 10, sentenceText = "two", category = 0)

        assertFalse(failed.isNewMastery)
        assertEquals(0, failed.currentStreak)
    }
}
