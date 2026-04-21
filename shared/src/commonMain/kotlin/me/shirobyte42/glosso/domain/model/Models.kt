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
    val pairHints: List<PairHint> = emptyList()
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
