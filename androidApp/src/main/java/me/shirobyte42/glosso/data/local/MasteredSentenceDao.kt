package me.shirobyte42.glosso.data.local

import androidx.room.*

@Dao
interface MasteredSentenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasteredSentence(sentence: MasteredSentenceEntity)

    @Query("SELECT text FROM mastered_sentences WHERE language = :language")
    suspend fun getAllMasteredTexts(language: String = "en"): List<String>

    @Query("SELECT COUNT(*) FROM mastered_sentences WHERE levelIndex = :levelIndex AND language = :language")
    suspend fun getCountByLevel(levelIndex: Int, language: String = "en"): Int

    @Query("SELECT COUNT(*) FROM mastered_sentences WHERE language = :language")
    suspend fun getTotalCount(language: String = "en"): Int

    @Query("SELECT EXISTS(SELECT 1 FROM mastered_sentences WHERE text = :text AND language = :language)")
    suspend fun isMastered(text: String, language: String = "en"): Boolean

    @Query("SELECT COUNT(*) FROM mastered_sentences WHERE levelIndex = :levelIndex AND topic = :topic AND language = :language")
    suspend fun getCountByLevelAndTopic(levelIndex: Int, topic: String, language: String = "en"): Int

    @Query("DELETE FROM mastered_sentences WHERE language = :language")
    suspend fun deleteAll(language: String = "en")
}
