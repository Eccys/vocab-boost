package xyz.ecys.vocab.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words") suspend fun getAllWords(): List<Word>

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    @Query("DELETE FROM sqlite_sequence WHERE name='words'")
    suspend fun resetWordsSequence()

    @Query("SELECT * FROM words") fun getAllWordsFlow(): Flow<List<Word>>

    @Query("""
        SELECT * FROM words 
        WHERE id != :excludeId 
        ORDER BY RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomWordsExcluding(count: Int, excludeId: Int): List<Word>

    @Query("""
        SELECT * FROM words 
        ORDER BY RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomWords(count: Int): List<Word>

    @Query("SELECT * FROM words WHERE isBookmarked = 1")
    fun getBookmarkedWordsFlow(): Flow<List<Word>>

    @Query("""
        SELECT * FROM words 
        WHERE isBookmarked = 1 
        ORDER BY RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomBookmarkedWords(count: Int): List<Word>

    @Query("""
        SELECT * FROM words 
        WHERE isBookmarked = 1 AND id != :excludeId 
        ORDER BY RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomBookmarkedWordsExcluding(count: Int, excludeId: Int): List<Word>

    @Query("UPDATE words SET isBookmarked = :isBookmarked WHERE id = :wordId")
    suspend fun updateBookmark(wordId: Int, isBookmarked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWord(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWords(words: List<Word>)

    @Update suspend fun updateWord(word: Word)

    @Query("""
    SELECT * FROM words 
    WHERE nextReviewDate <= :currentTime 
    ORDER BY 
      ((:currentTime - nextReviewDate) * 1.0 / ((CASE WHEN interval = 0 THEN 1 ELSE interval END) * 86400000)) DESC,
      easeFactor ASC, 
      lastReviewed ASC
    """)
    suspend fun getOverdueWords(currentTime: Long): List<Word>

    @Query("""
        SELECT * FROM words 
        WHERE timesReviewed = 0
        ORDER BY RANDOM()
    """)
    suspend fun getUnseenWords(): List<Word>

    @Query("""
        UPDATE words 
        SET timesReviewed = timesReviewed + 1,
            timesCorrect = timesCorrect + :wasCorrect,
            lastReviewed = :timestamp,
            easeFactor = :easeFactor,
            interval = :interval,
            repetitionCount = :repetitionCount,
            nextReviewDate = :nextReviewDate,
            quality = :quality
        WHERE id = :wordId
    """)
    suspend fun updateWordStats(
        wordId: Int,
        wasCorrect: Int,
        timestamp: Long,
        quality: Int,
        easeFactor: Float,
        interval: Int,
        repetitionCount: Int,
        nextReviewDate: Long
    )

    @Query("SELECT * FROM words WHERE id = :wordId LIMIT 1")
    suspend fun getWordById(wordId: Int): Word?

    @Query("""
        UPDATE words 
        SET timesReviewed = 0,
            timesCorrect = 0,
            lastReviewed = 0,
            easeFactor = 2.5,
            interval = 0,
            repetitionCount = 0,
            nextReviewDate = 0
    """)
    suspend fun resetAllStats()

    @Query("SELECT COUNT(*) FROM words WHERE timesReviewed > 0")
    suspend fun countWordsWithReviews(): Int

    @Query("""
        SELECT COUNT(DISTINCT id) FROM words 
        WHERE lastReviewed >= (
            strftime('%s', date('now', 'localtime', 'start of day')) * 1000
        )
    """)
    suspend fun countWordsReviewedToday(): Int

    @Query("SELECT * FROM words WHERE nextReviewDate <= :now AND nextReviewDate > 0 ORDER BY easeFactor ASC")
    fun getOverdueWordsFlow(now: Long = System.currentTimeMillis()): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE nextReviewDate = 0")
    fun getUnreviewedWords(): Flow<List<Word>>

    @Query("UPDATE words SET easeFactor = :easeFactor, interval = :interval, repetitionCount = :repetitionCount, nextReviewDate = :nextReviewDate WHERE id = :wordId")
    suspend fun updateSpacedRepetitionData(
        wordId: Int,
        easeFactor: Float,
        interval: Int,
        repetitionCount: Int,
        nextReviewDate: Long
    )
}

data class CategoryStats(
    val category: String,
    val total: Int,
    val studied: Int,
    val accuracy: Float
)
