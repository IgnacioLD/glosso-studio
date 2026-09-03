package me.shirobyte42.glosso.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.shirobyte42.glosso.domain.model.MasteryTimelinePoint
import me.shirobyte42.glosso.domain.model.PhonemeStat
import me.shirobyte42.glosso.domain.repository.PreferenceRepository
import me.shirobyte42.glosso.domain.usecase.GetUserStatsUseCase

class StatsViewModel(
    private val getUserStats: GetUserStatsUseCase,
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.getTargetLanguageFlow().collect {
                load()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _uiState.update { it.copy(isLoading = true) }
        val stats = getUserStats()

        val countsByDate = stats.timeline.associate { it.date to it.count }
        val today = LocalDate.now()
        val filledTimeline = (TIMELINE_DAYS - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong()).toString()
            MasteryTimelinePoint(date = date, count = countsByDate[date] ?: 0)
        }

        val weakPhonemes = stats.weakPhonemes
            .filter { it.missed >= MIN_MISSES }
            .sortedWith(compareByDescending<PhonemeStat> { it.missRatio }.thenByDescending { it.missed })
            .take(WEAK_PHONEME_LIMIT)

        _uiState.update {
            it.copy(
                isLoading = false,
                timeline = filledTimeline,
                activityDates = stats.activityDates.toSet(),
                weakPhonemes = weakPhonemes,
                reviewDue = stats.reviewBacklog.dueCount,
                reviewScheduled = stats.reviewBacklog.scheduledCount,
                totalMastered = stats.totalMastered,
                totalActivityDays = stats.totalActivityDays,
                bestCombo = prefs.getBestMasteryStreak()
            )
        }
    }

    companion object {
        private const val TIMELINE_DAYS = 30
        private const val WEAK_PHONEME_LIMIT = 5
        private const val MIN_MISSES = 3
    }
}

data class StatsUiState(
    val isLoading: Boolean = true,
    val timeline: List<MasteryTimelinePoint> = emptyList(),
    val activityDates: Set<String> = emptySet(),
    val weakPhonemes: List<PhonemeStat> = emptyList(),
    val reviewDue: Int = 0,
    val reviewScheduled: Int = 0,
    val totalMastered: Int = 0,
    val totalActivityDays: Int = 0,
    val bestCombo: Int = 0
) {
    val hasAnyTimelineActivity: Boolean get() = timeline.any { it.count > 0 }
    val hasAnyActivity: Boolean get() = activityDates.isNotEmpty()
}
