package xyz.ecys.vocab.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words")
    suspend fun getAllWords(): List<Word>

    @Query("SELECT * FROM words")
    fun getAllWordsFlow(): Flow<List<Word>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<Word>)

    @Update
    suspend fun updateWord(word: Word)

    @Query("""
        SELECT * FROM words 
        WHERE nextReviewDate <= :currentTime 
        ORDER BY easeFactor ASC, lastReviewed ASC
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
        SET timesReviewed = CASE 
                WHEN :wasCorrect = 0 THEN 0 
                ELSE timesReviewed + 1 
            END,
            timesCorrect = timesCorrect + :wasCorrect,
            lastReviewed = :timestamp,
            easeFactor = CASE
                WHEN :wasCorrect = 1 THEN 
                    MAX(1.3, easeFactor + (0.1 - (5 - :quality) * (0.08 + (5 - :quality) * 0.02)))
                ELSE easeFactor
                END,
            interval = CASE
                WHEN :wasCorrect = 0 THEN 1
                WHEN timesReviewed = 0 THEN 1
                WHEN timesReviewed = 1 THEN 2
                ELSE CAST(interval * CASE
                        WHEN :wasCorrect = 1 THEN easeFactor
                        ELSE 1
                        END AS INTEGER)
                END,
            nextReviewDate = :timestamp + (
                CASE
                    WHEN :wasCorrect = 0 THEN 86400000  -- 1 day in milliseconds
                    WHEN timesReviewed = 0 THEN 86400000
                    WHEN timesReviewed = 1 THEN 172800000  -- 2 days in milliseconds
                    ELSE CAST(interval * CASE
                            WHEN :wasCorrect = 1 THEN easeFactor
                            ELSE 1
                            END * 86400000 AS INTEGER)
                    END
            ),
            quality = CASE
                WHEN :wasCorrect = 0 THEN 0
                WHEN :responseTime < 3000 THEN 5  -- Fast: < 3 seconds
                WHEN :responseTime < 5000 THEN 4  -- Medium: 3-5 seconds
                ELSE 3                            -- Slow: > 5 seconds
            END
        WHERE id = :wordId
    """)
    suspend fun updateWordStats(
        wordId: Int,
        wasCorrect: Int,
        timestamp: Long,
        responseTime: Long,
        quality: Int
    )

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