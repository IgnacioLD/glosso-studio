package me.shirobyte42.glosso.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shirobyte42.glosso.data.local.ActivityDayDao
import me.shirobyte42.glosso.data.local.MasteredSentenceDao
import me.shirobyte42.glosso.data.local.ReviewDao
import me.shirobyte42.glosso.domain.model.MasteryTimelinePoint
import me.shirobyte42.glosso.domain.model.PhonemeStat
import me.shirobyte42.glosso.domain.model.ReviewBacklog
import me.shirobyte42.glosso.domain.repository.PreferenceRepository
import me.shirobyte42.glosso.domain.repository.StatsRepository

class RoomStatsRepository(
    private val masteredSentenceDao: MasteredSentenceDao,
    private val activityDayDao: ActivityDayDao,
    private val reviewDao: ReviewDao,
    private val prefs: PreferenceRepository
) : StatsRepository {

    override suspend fun getMasteryTimeline(): List<MasteryTimelinePoint> = withContext(Dispatchers.IO) {
        masteredSentenceDao.getMasteryCountsByDay(prefs.getTargetLanguage())
            .map { MasteryTimelinePoint(date = it.date, count = it.count) }
    }

    override suspend fun getActivityDates(): List<String> = withContext(Dispatchers.IO) {
        activityDayDao.getAllActivityDates(prefs.getTargetLanguage())
    }

    override suspend fun getPhonemeStats(): List<PhonemeStat> {
        return prefs.getPhonemeStats()
    }

    override suspend fun getReviewBacklog(): ReviewBacklog = withContext(Dispatchers.IO) {
        val language = prefs.getTargetLanguage()
        ReviewBacklog(
            dueCount = reviewDao.getDueCount(language, System.currentTimeMillis()),
            scheduledCount = reviewDao.getScheduledCount(language)
        )
    }

    override suspend fun getTotalMastered(): Int = withContext(Dispatchers.IO) {
        masteredSentenceDao.getTotalCount(prefs.getTargetLanguage())
    }

    override suspend fun getTotalActivityDays(): Int = withContext(Dispatchers.IO) {
        activityDayDao.getTotalActivityDays(prefs.getTargetLanguage())
    }
}
