package me.shirobyte42.glosso.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "mastered_sentences", primaryKeys = ["text", "language"])
data class MasteredSentenceEntity(
    val text: String,
    val levelIndex: Int,
    val masteredAt: Long = System.currentTimeMillis(),
    val topic: String = "",
    @ColumnInfo(defaultValue = "en") val language: String = "en"
)
