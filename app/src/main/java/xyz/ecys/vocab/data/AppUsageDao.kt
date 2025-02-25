package xyz.ecys.vocab.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordUsage(usage: AppUsage)

    @Query("UPDATE app_usage SET duration = duration + :additionalDuration WHERE date = :date")
    suspend fun updateSessionDuration(date: Long, additionalDuration: Long)

    @Query("UPDATE app_usage SET sessionCount = sessionCount + 1 WHERE date = :date")
    suspend fun incrementSessionCount(date: Long)

    @Query("UPDATE app_usage SET correctAnswers = correctAnswers + 1 WHERE date = :date")
    suspend fun incrementCorrectAnswers(date: Long)

    @Query("SELECT COALESCE(SUM(duration), 0) FROM app_usage")
    suspend fun getTotalTimeSpent(): Long

    @Query("SELECT COALESCE(SUM(duration), 0) FROM app_usage WHERE date >= :startDate")
    suspend fun getTimeSpentSince(startDate: Long): Long

    @Query("SELECT COALESCE(SUM(correctAnswers), 0) FROM app_usage")
    suspend fun getTotalCorrectAnswers(): Int

    @Query("SELECT COALESCE(correctAnswers, 0) FROM app_usage WHERE date = :date")
    suspend fun getCorrectAnswersForDate(date: Long): Int?

    @Query("SELECT * FROM app_usage WHERE date BETWEEN :startDate AND :endDate")
    fun getUsageBetweenDates(startDate: Long, endDate: Long): Flow<List<AppUsage>>

    @Query("SELECT * FROM app_usage WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getUsageBetweenDatesSync(startDate: Long, endDate: Long): List<AppUsage>

    @Query("SELECT * FROM app_usage WHERE date = :date LIMIT 1")
    suspend fun getUsageForDate(date: Long): AppUsage?

    @Query("""
        UPDATE app_usage 
        SET duration = :duration,
            sessionCount = :sessionCount,
            correctAnswers = :correctAnswers
        WHERE date = :date
    """)
    suspend fun updateUsage(date: Long, duration: Long, sessionCount: Int, correctAnswers: Int)

    @Query("DELETE FROM app_usage")
    suspend fun deleteAllUsageData()
} 