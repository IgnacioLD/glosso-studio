package me.shirobyte42.glosso.data.local

import androidx.room.*

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun scheduleReview(review: ReviewEntity)

    @Query("SELECT * FROM review_queue WHERE levelIndex = :levelIndex AND language = :language AND nextReviewAt <= :now")
    suspend fun getDueReviews(levelIndex: Int, now: Long, language: String = "en"): List<ReviewEntity>

    @Query("SELECT * FROM review_queue WHERE text = :text AND language = :language")
    suspend fun getReview(text: String, language: String = "en"): ReviewEntity?

    @Update
    suspend fun updateReview(review: ReviewEntity)

    @Query("DELETE FROM review_queue WHERE text = :text AND language = :language")
    suspend fun deleteReview(text: String, language: String = "en")

    @Query("DELETE FROM review_queue WHERE language = :language")
    suspend fun deleteAll(language: String = "en")
}
