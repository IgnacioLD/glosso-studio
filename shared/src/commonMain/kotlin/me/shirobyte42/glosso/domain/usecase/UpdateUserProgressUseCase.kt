package me.shirobyte42.glosso.domain.usecase

import me.shirobyte42.glosso.domain.repository.PreferenceRepository

class UpdateMasteryUseCase(
    private val prefs: PreferenceRepository
) {
    /**
     * [mastered] is decided by the scorer, which is the only place that can see
     * per-word results and completeness. This use case just persists that
     * decision - it must not re-derive it from [score] or the two can disagree.
     */
    suspend operator fun invoke(
        score: Int,
        mastered: Boolean,
        sentenceText: String,
        category: Int,
        topic: String = ""
    ): MasteryResult {
        val wasAlreadyMastered = prefs.isSentenceMastered(sentenceText)

        if (mastered) {
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
            isNewMastery = mastered && !wasAlreadyMastered,
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
