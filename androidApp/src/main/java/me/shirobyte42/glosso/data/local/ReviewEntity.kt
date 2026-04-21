package me.shirobyte42.glosso.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "review_queue", primaryKeys = ["text", "language"])
data class ReviewEntity(
    val text: String,
    val levelIndex: Int,
    val nextReviewAt: Long,
    val intervalDays: Int = 1,
    @ColumnInfo(defaultValue = "en") val language: String = "en"
)
