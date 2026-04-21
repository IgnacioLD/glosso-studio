package me.shirobyte42.glosso.domain.model

/**
 * Metadata for the unified eSpeak-fine-tuned wav2vec2 phoneme model
 * shared across every practice language.
 */
object EspeakModelConfig {
    const val MODEL_FILE = "wav2vec2_espeak_cv_ft_int8.onnx"
    const val VOCAB_FILE = "espeak_vocab.json"
    const val MIN_MODEL_BYTES = 1024L * 1024L * 200L  // >=200 MB sanity floor (real size ~303 MB)
    const val MIN_VOCAB_BYTES = 1024L                 // tiny JSON, but never zero
}

data class LanguageConfig(
    val code: String,
    val displayName: String,
    val flag: String,
    val downloadRepo: String = "shirobyte421/glosso-studio",
    /** espeak-ng voice id used offline to phonemize sentence text. Not used at runtime. */
    val espeakPhonemizer: String,
    /** True when recognition accuracy is unvalidated (e.g., Latin is not in training data). */
    val experimental: Boolean = false,
)

val SUPPORTED_LANGUAGES = listOf(
    LanguageConfig(
        code = "en_GB",
        displayName = "English (UK)",
        flag = "\uD83C\uDDEC\uD83C\uDDE7",
        espeakPhonemizer = "en-gb",
    ),
    LanguageConfig(
        code = "en_US",
        displayName = "English (US)",
        flag = "\uD83C\uDDFA\uD83C\uDDF8",
        espeakPhonemizer = "en-us",
    ),
    LanguageConfig(
        code = "fr",
        displayName = "Français",
        flag = "\uD83C\uDDEB\uD83C\uDDF7",
        espeakPhonemizer = "fr-fr",
    ),
    LanguageConfig(
        code = "es",
        displayName = "Español",
        flag = "\uD83C\uDDEA\uD83C\uDDF8",
        espeakPhonemizer = "es",
    ),
    LanguageConfig(
        code = "de",
        displayName = "Deutsch",
        flag = "\uD83C\uDDE9\uD83C\uDDEA",
        espeakPhonemizer = "de",
    ),
    LanguageConfig(
        code = "la",
        displayName = "Latina",
        flag = "\uD83D\uDCDC",
        espeakPhonemizer = "la",
        experimental = true,
    ),
)

fun languageConfigFor(code: String): LanguageConfig {
    val normalized = if (code == "en") "en_GB" else code
    return SUPPORTED_LANGUAGES.firstOrNull { it.code == normalized } ?: SUPPORTED_LANGUAGES.first()
}
