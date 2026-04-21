package me.shirobyte42.glosso.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.shirobyte42.glosso.domain.repository.PreferenceRepository

import me.shirobyte42.glosso.data.local.MasteredSentenceDao
import me.shirobyte42.glosso.data.local.MasteredSentenceEntity
import me.shirobyte42.glosso.data.local.ActivityDayDao
import me.shirobyte42.glosso.data.local.ActivityDayEntity
import me.shirobyte42.glosso.data.local.ReviewDao
import me.shirobyte42.glosso.data.local.ReviewEntity
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

class AndroidPreferenceRepository(
    context: Context,
    private val masteredSentenceDao: MasteredSentenceDao,
    private val activityDayDao: ActivityDayDao,
    private val reviewDao: ReviewDao
) : PreferenceRepository {
    private val prefs = context.getSharedPreferences("glosso_prefs", Context.MODE_PRIVATE)
    private val _masteryStreakFlow = MutableStateFlow(0)

    init {
        // Flow is initialized to 0; refreshed via getMasteryStreakFlow() or getMasteryStreak()
    }

    override fun getSelectedVoice(): Int = prefs.getInt("selected_voice", 0)
    override fun setSelectedVoice(index: Int) {
        prefs.edit().putInt("selected_voice", index).apply()
    }

    override fun getLastLevel(): Int = prefs.getInt("last_level", -1)
    override fun setLastLevel(level: Int) {
        prefs.edit().putInt("last_level", level).apply()
    }

    override fun getMasteryStreakFlow(): Flow<Int> = _masteryStreakFlow.asStateFlow()
    
    override suspend fun getMasteryStreak(): Int = withContext(Dispatchers.IO) {
        calculateCurrentStreak()
    }

    override fun setMasteryStreak(streak: Int) {
        // No-op for DB-based streak, but we can update flow
        _masteryStreakFlow.value = streak
        
        // Update best streak in prefs for efficiency
        if (streak > getBestMasteryStreak()) {
            setBestMasteryStreak(streak)
        }
    }

    override fun getMasteryCombo(): Int = prefs.getInt("mastery_combo", 0)
    override fun setMasteryCombo(combo: Int) {
        prefs.edit().putInt("mastery_combo", combo).apply()
    }

    override fun getBestMasteryStreak(): Int = prefs.getInt("best_mastery_streak", 0)
    override fun setBestMasteryStreak(streak: Int) {
        prefs.edit().putInt("best_mastery_streak", streak).apply()
    }

    private fun currentLanguage(): String = prefs.getString("target_language", "en_GB") ?: "en_GB"

    override suspend fun isSentenceMastered(text: String): Boolean = withContext(Dispatchers.IO) {
        masteredSentenceDao.isMastered(text, currentLanguage())
    }

    override suspend fun markSentenceAsMastered(text: String, category: Int, topic: String) = withContext(Dispatchers.IO) {
        val lang = currentLanguage()
        // Only insert if not already mastered to avoid redundant activity logs
        if (!masteredSentenceDao.isMastered(text, lang)) {
            masteredSentenceDao.insertMasteredSentence(MasteredSentenceEntity(text, category, topic = topic, language = lang))

            // Mark today as active
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())
            activityDayDao.insertDay(ActivityDayEntity(todayStr, language = lang))

            // Update flow
            _masteryStreakFlow.value = calculateCurrentStreak()
        }
    }

    override suspend fun getMasteredSentences(): Set<String> = withContext(Dispatchers.IO) {
        masteredSentenceDao.getAllMasteredTexts(currentLanguage()).toSet()
    }

    override suspend fun getMasteryCountForCategory(category: Int): Int = withContext(Dispatchers.IO) {
        masteredSentenceDao.getCountByLevel(category, currentLanguage())
    }

    override suspend fun getMasteryCountForCategoryAndTopic(category: Int, topic: String): Int = withContext(Dispatchers.IO) {
        masteredSentenceDao.getCountByLevelAndTopic(category, topic, currentLanguage())
    }

    override fun getAcknowledgedMilestone(): Int = prefs.getInt("acknowledged_milestone", 0)
    override fun setAcknowledgedMilestone(milestone: Int) {
        prefs.edit().putInt("acknowledged_milestone", milestone).apply()
    }

    override fun incrementPhonemeStats(phoneme: String, missed: Boolean) {
        val totalKey = "ph_total_$phoneme"
        val missKey = "ph_miss_$phoneme"
        prefs.edit()
            .putInt(totalKey, prefs.getInt(totalKey, 0) + 1)
            .apply {
                if (missed) putInt(missKey, prefs.getInt(missKey, 0) + 1)
            }
            .apply()
    }

    override fun getWeakPhonemes(minMissed: Int): List<String> {
        return prefs.all.keys
            .filter { it.startsWith("ph_miss_") }
            .map { it.removePrefix("ph_miss_") }
            .filter { phoneme -> prefs.getInt("ph_miss_$phoneme", 0) >= minMissed }
            .sortedByDescending { phoneme -> prefs.getInt("ph_miss_$phoneme", 0) }
    }

    override suspend fun getTotalMasteryCount(): Int = withContext(Dispatchers.IO) {
        masteredSentenceDao.getTotalCount(currentLanguage())
    }

    override suspend fun getDueReviews(levelIndex: Int): List<String> = withContext(Dispatchers.IO) {
        reviewDao.getDueReviews(levelIndex, System.currentTimeMillis(), currentLanguage()).map { it.text }
    }

    override suspend fun scheduleReview(text: String, levelIndex: Int) = withContext(Dispatchers.IO) {
        val nextReviewAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L // 1 day
        reviewDao.scheduleReview(ReviewEntity(text, levelIndex, nextReviewAt, intervalDays = 1, language = currentLanguage()))
    }

    override suspend fun updateReviewResult(text: String, mastered: Boolean) = withContext(Dispatchers.IO) {
        val lang = currentLanguage()
        val existing = reviewDao.getReview(text, lang) ?: return@withContext
        if (mastered) {
            val nextInterval = nextReviewInterval(existing.intervalDays)
            val nextReviewAt = System.currentTimeMillis() + nextInterval * 24 * 60 * 60 * 1000L
            reviewDao.updateReview(existing.copy(nextReviewAt = nextReviewAt, intervalDays = nextInterval))
        } else {
            // Failure: reset to 1 day
            val nextReviewAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
            reviewDao.updateReview(existing.copy(nextReviewAt = nextReviewAt, intervalDays = 1))
        }
    }

    private fun nextReviewInterval(current: Int): Int = when {
        current < 3 -> 3
        current < 7 -> 7
        current < 14 -> 14
        current < 30 -> 30
        else -> 30
    }

    override suspend fun resetProgress() = withContext(Dispatchers.IO) {
        val lang = currentLanguage()
        masteredSentenceDao.deleteAll(lang)
        activityDayDao.deleteAll(lang)
        reviewDao.deleteAll(lang)
        withContext(Dispatchers.Main) {
            prefs.edit().apply {
                remove("mastery_combo")
                remove("best_mastery_streak")
                remove("batch_level")
                remove("batch_queue")
                remove("batch_mastered_count")
                remove("batch_total")
                remove("tutorial_shown")
                apply()
            }
            _masteryStreakFlow.value = 0
        }
    }

    override fun isTutorialShown(): Boolean = prefs.getBoolean("tutorial_shown", false)
    override fun setTutorialShown(shown: Boolean) {
        prefs.edit().putBoolean("tutorial_shown", shown).apply()
    }

    override fun isOnboardingShown(): Boolean = prefs.getBoolean("onboarding_shown", false)
    override fun setOnboardingShown(shown: Boolean) {
        prefs.edit().putBoolean("onboarding_shown", shown).apply()
    }

    override fun getLastReviewPromptMs(): Long = prefs.getLong("last_review_prompt_ms", 0L)
    override fun setLastReviewPromptMs(ms: Long) {
        prefs.edit().putLong("last_review_prompt_ms", ms).apply()
    }

    override fun getPlaybackSpeed(): Float = prefs.getFloat("playback_speed", 1.0f)
    override fun setPlaybackSpeed(speed: Float) {
        prefs.edit().putFloat("playback_speed", speed).apply()
    }

    private val _ipaVisibleFlow = MutableStateFlow(prefs.getBoolean("ipa_visible", false))

    override fun isIpaVisible(): Boolean = prefs.getBoolean("ipa_visible", false)
    override fun setIpaVisible(visible: Boolean) {
        prefs.edit().putBoolean("ipa_visible", visible).apply()
        _ipaVisibleFlow.value = visible
    }
    override fun getIpaVisibleFlow(): Flow<Boolean> = _ipaVisibleFlow.asStateFlow()

    override fun getThemeMode(): Int = prefs.getInt("theme_mode", 0)
    override fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        _themeModeFlow.value = mode
    }

    val _themeModeFlow = MutableStateFlow(prefs.getInt("theme_mode", 0))
    val themeModeFlow = _themeModeFlow.asStateFlow()

    private val _targetLanguageFlow = MutableStateFlow(prefs.getString("target_language", "") ?: "")
    override fun getTargetLanguage(): String = prefs.getString("target_language", "") ?: ""
    override fun setTargetLanguage(code: String) {
        prefs.edit().putString("target_language", code).apply()
        _targetLanguageFlow.value = code
    }
    override fun getTargetLanguageFlow(): Flow<String> = _targetLanguageFlow.asStateFlow()

    override fun getUiLanguage(): String = prefs.getString("ui_language", "en") ?: "en"
    override fun setUiLanguage(code: String) {
        prefs.edit().putString("ui_language", code).apply()
    }

    private val _translationVisibleFlow = MutableStateFlow(prefs.getBoolean("translation_visible", true))
    override fun isTranslationVisible(): Boolean = prefs.getBoolean("translation_visible", true)
    override fun setTranslationVisible(visible: Boolean) {
        prefs.edit().putBoolean("translation_visible", visible).apply()
        _translationVisibleFlow.value = visible
    }
    override fun getTranslationVisibleFlow(): Flow<Boolean> = _translationVisibleFlow.asStateFlow()

    override fun isLatinWarningAcknowledged(): Boolean = prefs.getBoolean("latin_warning_acknowledged", false)
    override fun setLatinWarningAcknowledged(acknowledged: Boolean) {
        prefs.edit().putBoolean("latin_warning_acknowledged", acknowledged).apply()
    }

    override fun isMigratedToV10(): Boolean = prefs.getBoolean("migrated_to_v10", false)
    override fun setMigratedToV10(migrated: Boolean) {
        prefs.edit().putBoolean("migrated_to_v10", migrated).apply()
    }

    override fun saveBatch(levelIndex: Int, queueTexts: List<String>, masteredCount: Int, totalSize: Int) {
        prefs.edit()
            .putInt("batch_level", levelIndex)
            .putString("batch_queue", queueTexts.joinToString("\u0000"))
            .putInt("batch_mastered_count", masteredCount)
            .putInt("batch_total", totalSize)
            .putString("batch_language", getTargetLanguage())
            .apply()
    }

    override fun hasSavedBatch(levelIndex: Int): Boolean {
        return prefs.getInt("batch_level", -1) == levelIndex &&
               prefs.getString("batch_queue", "")?.isNotEmpty() == true &&
               prefs.getString("batch_language", "") == getTargetLanguage()
    }

    override fun getSavedBatchQueueTexts(): List<String> {
        val raw = prefs.getString("batch_queue", "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split("\u0000")
    }

    override fun getSavedBatchMasteredCount(): Int = prefs.getInt("batch_mastered_count", 0)

    override fun getSavedBatchTotalSize(): Int = prefs.getInt("batch_total", 0)

    override fun clearSavedBatch() {
        prefs.edit()
            .remove("batch_level")
            .remove("batch_queue")
            .remove("batch_mastered_count")
            .remove("batch_total")
            .remove("batch_language")
            .apply()
    }

    private suspend fun calculateCurrentStreak(): Int {
        val dates = activityDayDao.getAllActivityDates(currentLanguage())
        if (dates.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        
        val todayStr = sdf.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(calendar.time)

        // If today is not in the list AND yesterday is not in the list, streak is 0
        if (!dates.contains(todayStr) && !dates.contains(yesterdayStr)) {
            return 0
        }

        var streak = 0
        val checkCalendar = Calendar.getInstance()
        
        // Start checking from the most recent active day
        if (!dates.contains(todayStr)) {
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val checkStr = sdf.format(checkCalendar.time)
            if (dates.contains(checkStr)) {
                streak++
                checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }
}
