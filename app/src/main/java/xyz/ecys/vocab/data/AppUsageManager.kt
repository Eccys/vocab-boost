package xyz.ecys.vocab.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppUsageManager private constructor(private val appUsageDao: AppUsageDao) {
    private var sessionStartTime: Long = 0
    private var isQuizSession: Boolean = false

    companion object {
        @Volatile
        private var INSTANCE: AppUsageManager? = null
        
        fun getInstance(context: Context): AppUsageManager {
            return INSTANCE ?: synchronized(this) {
                val database = WordDatabase.getDatabase(context)
                AppUsageManager(database.appUsageDao()).also {
                    INSTANCE = it
                }
            }
        }

        private fun getStartOfDayTimestamp(): Long {
            return LocalDateTime.now()
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }

    fun startQuizSession() {
        isQuizSession = true
        sessionStartTime = System.currentTimeMillis()
    }

    fun startSession() {
        isQuizSession = false
        sessionStartTime = System.currentTimeMillis()
    }

    suspend fun recordCorrectAnswer() = withContext(Dispatchers.IO) {
        val today = getStartOfDayTimestamp()
        appUsageDao.recordUsage(AppUsage(date = today))
        appUsageDao.incrementCorrectAnswers(today)
    }

    suspend fun endSession() = withContext(Dispatchers.IO) {
        if (sessionStartTime == 0L) return@withContext

        val now = System.currentTimeMillis()
        val duration = now - sessionStartTime
        val today = getStartOfDayTimestamp()

        // Record usage for today
        appUsageDao.recordUsage(AppUsage(date = today))
        // Update duration
        appUsageDao.updateSessionDuration(today, duration)
        // If this was a quiz session, increment the session count
        if (isQuizSession) {
            appUsageDao.incrementSessionCount(today)
        }
        
        sessionStartTime = 0
        isQuizSession = false
    }

    suspend fun getTotalTimeSpent(): Long = withContext(Dispatchers.IO) {
        appUsageDao.getTotalTimeSpent()
    }

    suspend fun getTimeSpentToday(): Long = withContext(Dispatchers.IO) {
        val today = getStartOfDayTimestamp()
        appUsageDao.getTimeSpentSince(today)
    }

    suspend fun getTotalCorrectAnswers(): Int = withContext(Dispatchers.IO) {
        appUsageDao.getTotalCorrectAnswers()
    }

    suspend fun getCorrectAnswersToday(): Int = withContext(Dispatchers.IO) {
        val today = getStartOfDayTimestamp()
        appUsageDao.getCorrectAnswersForDate(today)
    }

    fun getUsageBetweenDates(startDate: Long, endDate: Long): Flow<List<AppUsage>> {
        return appUsageDao.getUsageBetweenDates(startDate, endDate)
    }

    suspend fun getCurrentStreak(): Int = withContext(Dispatchers.IO) {
        try {
            val today = getStartOfDayTimestamp()
            val yesterday = today - (24 * 60 * 60 * 1000)
            
            val todayAnswers = try {
                appUsageDao.getCorrectAnswersForDate(today)
            } catch (e: Exception) {
                0
            }
            
            val yesterdayAnswers = try {
                appUsageDao.getCorrectAnswersForDate(yesterday)
            } catch (e: Exception) {
                0
            }

            android.util.Log.d("StreakDebug", "Today's timestamp: $today, answers: $todayAnswers")
            android.util.Log.d("StreakDebug", "Yesterday's timestamp: $yesterday, answers: $yesterdayAnswers")

            var streak = 0
            
            // First check if both yesterday and today are active
            if (yesterdayAnswers > 0 && todayAnswers > 0) {
                streak = 2
                android.util.Log.d("StreakDebug", "Both days active, starting streak at 2")
                var checkDay = yesterday - (24 * 60 * 60 * 1000) // Start checking from 2 days ago
                while (true) {
                    val answers = try {
                        appUsageDao.getCorrectAnswersForDate(checkDay)
                    } catch (e: Exception) {
                        0
                    }
                    android.util.Log.d("StreakDebug", "Checking day: $checkDay, answers: $answers")
                    if (answers > 0) {
                        streak++
                        checkDay -= 24 * 60 * 60 * 1000
                    } else {
                        break
                    }
                }
            }
            // Then check if either today or yesterday is active
            else if (todayAnswers > 0 || yesterdayAnswers > 0) {
                streak = 1
                android.util.Log.d("StreakDebug", "One day active, streak is 1")
            }

            android.util.Log.d("StreakDebug", "Final streak: $streak")
            return@withContext streak
        } catch (e: Exception) {
            android.util.Log.e("StreakDebug", "Error calculating streak", e)
            return@withContext 0
        }
    }

    suspend fun getBestStreak(): Int = withContext(Dispatchers.IO) {
        try {
            // Get all usage data
            val usages = appUsageDao.getUsageBetweenDates(0, System.currentTimeMillis()).first()
            
            // Sort by date and filter for days with correct answers
            val activeDates = usages
                .filter { usage -> usage.correctAnswers > 0 }
                .map { usage -> usage.date }
                .sorted()
                .toList()

            if (activeDates.isEmpty()) return@withContext 0

            var bestStreak = 1
            var streakCount = 1
            var previousDate = activeDates[0]

            for (i in 1 until activeDates.size) {
                val currentDate = activeDates[i]
                val dayDiff = (currentDate - previousDate) / (24 * 60 * 60 * 1000)

                if (dayDiff == 1L) {
                    streakCount++
                    bestStreak = maxOf(bestStreak, streakCount)
                } else {
                    streakCount = 1
                }

                previousDate = currentDate
            }

            // Check if current streak is the best streak
            val ongoingStreak = getCurrentStreak()
            return@withContext maxOf(bestStreak, ongoingStreak)
        } catch (e: Exception) {
            // If anything goes wrong, return 0 as a safe default
            return@withContext 0
        }
    }
} 