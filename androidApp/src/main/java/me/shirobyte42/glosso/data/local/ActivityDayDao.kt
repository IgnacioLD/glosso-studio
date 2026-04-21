package me.shirobyte42.glosso.data.local

import androidx.room.*

@Dao
interface ActivityDayDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDay(day: ActivityDayEntity)

    @Query("SELECT dateString FROM activity_days WHERE language = :language ORDER BY dateString DESC")
    suspend fun getAllActivityDates(language: String = "en"): List<String>

    @Query("SELECT COUNT(*) FROM activity_days WHERE language = :language")
    suspend fun getTotalActivityDays(language: String = "en"): Int

    @Query("DELETE FROM activity_days WHERE language = :language")
    suspend fun deleteAll(language: String = "en")
}
