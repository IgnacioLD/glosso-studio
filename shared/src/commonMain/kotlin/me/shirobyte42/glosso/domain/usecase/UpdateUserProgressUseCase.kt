package me.shirobyte42.glosso.domain.usecase

import me.shirobyte42.glosso.domain.repository.PreferenceRepository

class UpdateMasteryUseCase(
    private val prefs: PreferenceRepository
) {
    suspend operator fun invoke(score: Int, sentenceText: String, category: Int, topic: String = ""): MasteryResult {
        val wasAlreadyMastered = prefs.isSentenceMastered(sentenceText)
        val isScoreMastery = score >= 85

        if (isScoreMastery) {
            if (!wasAlreadyMastered) {
                prefs.markSentenceAsMastered(sentenceText, category, topic)
            }
            val newCombo = prefs.getMasteryCombo() + 1
            prefs.setMasteryCombo(newCombo)
        } else {
            prefs.setMasteryCombo(0)
        }

        val currentCombo = prefs.getMasteryCombo()

        return MasteryResult(
            isNewMastery = isScoreMastery && !wasAlreadyMastered,
            currentStreak = currentCombo,
            score = score
        )
    }
}

data class MasteryResult(
    val isNewMastery: Boolean,
    val currentStreak: Int,
    val score: Int
)
