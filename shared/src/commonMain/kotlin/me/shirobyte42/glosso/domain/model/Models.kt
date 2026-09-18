package me.shirobyte42.glosso.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WordOffset(
    val w: String, // word text
    val s: Float,  // start seconds
    val e: Float   // end seconds
)

@Serializable
data class Sentence(
    val id: Int? = null,
    val text: String,
    /** IPA (espeak-ng) — word-separated, canonical phonemization. */
    val ipa: String,
    val level: String,
    val topic: String,
    val language: String,
    val audio1: String? = null,
    val audio2: String? = null,
    val wordOffsets: List<WordOffset> = emptyList(),
    val translations: Map<String, String> = emptyMap(),
) {
    fun translationFor(uiLanguage: String): String? = translations[uiLanguage]
}

@Serializable
data class PronunciationFeedback(
    val score: Int = 0,
    val transcription: String = "",
    val feedback: String? = null,
    val normalizedActual: String? = null,
    val normalizedExpected: String? = null,
    val alignment: List<PhonemeMatchModel> = emptyList(),
    val letterFeedback: List<LetterFeedbackModel> = emptyList(),
    val pairHints: List<PairHint> = emptyList(),
    /** Quality of the sounds actually produced, independent of anything skipped. */
    val accuracy: Int = 0,
    /** Percent of the reference sounds the learner actually produced. */
    val completeness: Int = 0,
    /** Per-word results in sentence order; the weakest word drives the coaching. */
    val words: List<WordFeedbackModel> = emptyList(),
    /** The full mastery decision (score + completeness + no destroyed word). */
    val isMastery: Boolean = false,
    /** The named result shown to the learner, replacing the raw percentage. */
    val level: MasteryLevel = MasteryLevel.NOT_YET
)

/** A single word's result, so the learner can be told exactly what to work on. */
@Serializable
data class WordFeedbackModel(
    val text: String,
    val score: Int,
    val level: MasteryLevel = MasteryLevel.NOT_YET
)

/** Explains a CLOSE phoneme confusion to the learner. */
@Serializable
data class PairHint(
    val expected: String,
    val actual: String,
    val description: String
)

@Serializable
data class PhonemeMatchModel(
    val expected: String,
    val actual: String,
    val status: MatchStatusModel
)

@Serializable
data class LetterFeedbackModel(
    val char: String,
    val status: MatchStatusModel
)

@Serializable
enum class MatchStatusModel { PERFECT, CLOSE, MISSED }

/**
 * The named mastery level of a pronunciation attempt. This is the whole
 * user-facing result: learners are told what they achieved ("Mastered",
 * "Almost"), never a bare percentage that hides *what* to fix.
 *
 * Ordered best to worst so the enum's natural order is the ladder.
 */
@Serializable
enum class MasteryLevel {
    /** Every sound landed exactly - nothing to fix. */
    PERFECT,
    /** Correct and complete enough to count toward progress. */
    MASTERED,
    /** One clear slip; still understandable. */
    ALMOST,
    /** Several slips; practice needed. */
    ROUGH,
    /** Barely recognisable yet. */
    NOT_YET
}
