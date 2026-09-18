package me.shirobyte42.glosso.domain.model

/**
 * The single source of truth for how a pronunciation attempt is judged.
 *
 * A single averaged score is a poor gate for mastery: errors dilute as a
 * sentence gets longer, so a ten-word sentence with one destroyed word used to
 * score higher than a two-word sentence with one wrong sound. These rules
 * combine the overall score with structural requirements, so "mastered" means
 * the learner pronounced the whole sentence, not that they averaged well.
 *
 * Tuning guidance: [MASTERY_THRESHOLD] is the overall quality bar,
 * [COMPLETENESS_FLOOR] stops a fluent half-sentence passing, and [WORD_FLOOR]
 * stops one destroyed word hiding behind a good average. Change nothing else.
 */
object ScoringConfig {

    /** Overall score at or above this is mastery-quality. */
    const val MASTERY_THRESHOLD = 85

    /** Scores at or above this (but below mastery) were close. */
    const val CLOSE_THRESHOLD = 70

    /** Percent of the reference sounds the learner must actually produce. */
    const val COMPLETENESS_FLOOR = 80

    /** No individual word may score below this, however good the average is. */
    const val WORD_FLOOR = 50

    /** Score band only - drives colour, haptics and wording. */
    fun isMasteryBand(score: Int): Boolean = score >= MASTERY_THRESHOLD

    fun isCloseBand(score: Int): Boolean = score >= CLOSE_THRESHOLD

    /**
     * The named level for a single word. [perfect] wins outright: a word with
     * no missed or merely-close sound is never described as just "mastered".
     */
    fun levelFor(score: Int, perfect: Boolean = false): MasteryLevel = when {
        perfect -> MasteryLevel.PERFECT
        score >= MASTERY_THRESHOLD -> MasteryLevel.MASTERED
        score >= CLOSE_THRESHOLD -> MasteryLevel.ALMOST
        score >= WORD_FLOOR -> MasteryLevel.ROUGH
        else -> MasteryLevel.NOT_YET
    }

    /**
     * The named level for a whole sentence. Mastery is a gate, not a band, so a
     * high average with a skipped or destroyed word cannot claim MASTERED.
     */
    fun sentenceLevel(score: Int, isMastery: Boolean, perfect: Boolean = false): MasteryLevel = when {
        perfect -> MasteryLevel.PERFECT
        isMastery -> MasteryLevel.MASTERED
        score >= CLOSE_THRESHOLD -> MasteryLevel.ALMOST
        score >= WORD_FLOOR -> MasteryLevel.ROUGH
        else -> MasteryLevel.NOT_YET
    }

    /**
     * The full mastery decision: a good overall score, most of the sentence
     * actually attempted, and no word left destroyed.
     */
    fun qualifiesForMastery(
        score: Int,
        completeness: Int,
        wordScores: List<Int>
    ): Boolean =
        isMasteryBand(score) &&
            completeness >= COMPLETENESS_FLOOR &&
            wordScores.all { it >= WORD_FLOOR }

    /** The weakest word, or null when there are none to compare. */
    fun weakestWordIndex(wordScores: List<Int>): Int? =
        wordScores.withIndex().minByOrNull { it.value }?.index
}
