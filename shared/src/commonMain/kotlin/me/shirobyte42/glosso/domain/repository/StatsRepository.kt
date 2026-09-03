package me.shirobyte42.glosso.domain.repository

import me.shirobyte42.glosso.domain.model.MasteryTimelinePoint
import me.shirobyte42.glosso.domain.model.PhonemeStat
import me.shirobyte42.glosso.domain.model.ReviewBacklog

/**
 * Read-only access to user learning statistics.
 * All data is scoped to the currently selected target language.
 */
interface StatsRepository {
    /** Mastered-sentence counts grouped by day, ascending by date. */
    suspend fun getMasteryTimeline(): List<MasteryTimelinePoint>

    /** Days (as "YYYY-MM-DD") on which at least one sentence was mastered. */
    suspend fun getActivityDates(): List<String>

    /** Phonemes sorted by miss count, descending. */
    suspend fun getPhonemeStats(): List<PhonemeStat>

    suspend fun getReviewBacklog(): ReviewBacklog

    suspend fun getTotalMastered(): Int

    suspend fun getTotalActivityDays(): Int
}
