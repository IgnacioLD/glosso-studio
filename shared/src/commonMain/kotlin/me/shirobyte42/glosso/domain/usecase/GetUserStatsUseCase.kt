package me.shirobyte42.glosso.domain.usecase

import me.shirobyte42.glosso.domain.model.UserStats
import me.shirobyte42.glosso.domain.repository.StatsRepository

class GetUserStatsUseCase(private val statsRepository: StatsRepository) {
    suspend operator fun invoke(): UserStats = UserStats(
        timeline = statsRepository.getMasteryTimeline(),
        activityDates = statsRepository.getActivityDates(),
        weakPhonemes = statsRepository.getPhonemeStats(),
        reviewBacklog = statsRepository.getReviewBacklog(),
        totalMastered = statsRepository.getTotalMastered(),
        totalActivityDays = statsRepository.getTotalActivityDays()
    )
}
