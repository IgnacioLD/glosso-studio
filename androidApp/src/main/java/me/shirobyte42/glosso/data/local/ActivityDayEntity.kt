package me.shirobyte42.glosso.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Stores a record for each day the user has mastered at least one sentence.
 * Date format is "YYYY-MM-DD".
 */
@Entity(tableName = "activity_days", primaryKeys = ["dateString", "language"])
data class ActivityDayEntity(
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "en") val language: String = "en"
)
