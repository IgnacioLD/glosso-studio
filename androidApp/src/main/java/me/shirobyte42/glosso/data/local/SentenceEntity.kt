package me.shirobyte42.glosso.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import me.shirobyte42.glosso.domain.model.Sentence
import me.shirobyte42.glosso.domain.model.WordOffset
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private val wordOffsetJson = Json { ignoreUnknownKeys = true }

@Entity(
    tableName = "sentences",
    indices = [
        Index(value = ["id"], name = "ix_sentences_id"),
        Index(value = ["language"], name = "ix_sentences_language"),
        Index(value = ["level"], name = "ix_sentences_level"),
        Index(value = ["language", "level"], name = "ix_lang_level"),
        Index(value = ["topic"], name = "ix_sentences_topic"),
        Index(value = ["language", "level", "topic"], name = "ix_lang_level_topic")
    ]
)
data class SentenceEntity(
    @PrimaryKey val id: Int,
    val text: String,
    /** IPA — espeak-ng phonemization of the text, word-separated. */
    val ipa: String?,
    @ColumnInfo(name = "level") val level: String?,
    @ColumnInfo(name = "topic") val topic: String?,
    @ColumnInfo(name = "language") val language: String?,
    @ColumnInfo(name = "audio_b64") val audio1: String?,
    @ColumnInfo(name = "audio_b64_2") val audio2: String?,
    @ColumnInfo(name = "word_offsets", defaultValue = "") val wordOffsets: String? = null,
    @ColumnInfo(name = "translation_en", defaultValue = "NULL") val translationEn: String? = null,
    @ColumnInfo(name = "translation_es", defaultValue = "NULL") val translationEs: String? = null,
    @ColumnInfo(name = "translation_fr", defaultValue = "NULL") val translationFr: String? = null,
    @ColumnInfo(name = "translation_de", defaultValue = "NULL") val translationDe: String? = null,
)

fun SentenceEntity.toDomain() = Sentence(
    id = id,
    text = text,
    ipa = ipa ?: "",
    level = level ?: "A1",
    topic = topic ?: "General",
    language = language ?: "en",
    audio1 = audio1,
    audio2 = audio2,
    wordOffsets = wordOffsets?.takeIf { it.isNotBlank() }?.let {
        try { wordOffsetJson.decodeFromString<List<WordOffset>>(it) } catch (_: Exception) { emptyList() }
    } ?: emptyList(),
    translations = mapOf(
        "en" to translationEn,
        "es" to translationEs,
        "fr" to translationFr,
        "de" to translationDe,
    ).filterValues { !it.isNullOrBlank() }.mapValues { it.value!! }
)

fun Sentence.toEntity() = SentenceEntity(
    id = id ?: 0,
    text = text,
    ipa = ipa,
    level = level,
    topic = topic,
    language = language,
    audio1 = audio1,
    audio2 = audio2,
    translationEn = translations["en"],
    translationEs = translations["es"],
    translationFr = translations["fr"],
    translationDe = translations["de"],
)
