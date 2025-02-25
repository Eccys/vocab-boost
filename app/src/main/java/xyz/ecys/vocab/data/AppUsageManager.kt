package xyz.ecys.vocab.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppUsageManager private constructor(
    private val appUsageDao: AppUsageDao,
    private val correctAnswerTracker: CorrectAnswerTracker
) {
    private var sessionStartTime: Long = 0
    private var isQuizSession: Boolean = false

    companion object {
        @Volatile
        private var INSTANCE: AppUsageManager? = null
        
        fun getInstance(context: Context): AppUsageManager {
            return INSTANCE ?: synchronized(this) {
                val database = WordDatabase.getDatabase(context)
                AppUsageManager(
                    database.appUsageDao(),
                    CorrectAnswerTracker.getInstance(context)
                ).also {
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

    suspend fun recordCorrectAnswer() = correctAnswerTracker.recordCorrectAnswer()

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

    suspend fun getTotalCorrectAnswers() = correctAnswerTracker.getTotalCorrectAnswers()

    suspend fun getCorrectAnswersToday() = correctAnswerTracker.getCorrectAnswersToday()

    fun getUsageBetweenDates(startDate: Long, endDate: Long): Flow<List<AppUsage>> {
        return appUsageDao.getUsageBetweenDates(startDate, endDate)
    }

    suspend fun getCurrentStreak(): Int = withContext(Dispatchers.IO) {
        // Checks consecutive days with activity
        // Returns the current streak count
        try {
            android.util.Log.d("StreakDebug", "Starting getCurrentStreak calculation...")
            val today = getStartOfDayTimestamp()
            val yesterday = today - (24 * 60 * 60 * 1000)
            
            android.util.Log.d("StreakDebug", "Today's timestamp: $today")
            android.util.Log.d("StreakDebug", "Yesterday's timestamp: $yesterday")
            
            // Get answers for today and yesterday
            val todayAnswers = correctAnswerTracker.getCorrectAnswersForDate(today)
            val yesterdayAnswers = correctAnswerTracker.getCorrectAnswersForDate(yesterday)
            
            android.util.Log.d("StreakDebug", "Today's answers: $todayAnswers")
            android.util.Log.d("StreakDebug", "Yesterday's answers: $yesterdayAnswers")
            
            // If no activity in the last two days, no streak
            if (todayAnswers == 0 && yesterdayAnswers == 0) {
                android.util.Log.d("StreakDebug", "No activity in last two days, returning 0")
                return@withContext 0
            }
            
            var streak = 0
            var checkDay = if (todayAnswers > 0) {
                android.util.Log.d("StreakDebug", "Starting streak check from today")
                today
            } else {
                android.util.Log.d("StreakDebug", "Starting streak check from yesterday")
                yesterday
            }
            
            // Count consecutive days with activity
            while (true) {
                val answers = correctAnswerTracker.getCorrectAnswersForDate(checkDay)
                android.util.Log.d("StreakDebug", "Checking day: ${checkDay}, answers: $answers")
                
                if (answers > 0) {
                    streak++
                    android.util.Log.d("StreakDebug", "Found active day, streak now: $streak")
                    checkDay -= 24 * 60 * 60 * 1000
                } else {
                    android.util.Log.d("StreakDebug", "Found inactive day, breaking loop")
                    break
                }
            }
            
            android.util.Log.d("StreakDebug", "Final streak count: $streak")
            return@withContext streak
            
        } catch (e: Exception) {
            android.util.Log.e("StreakDebug", "Error calculating streak", e)
            android.util.Log.e("StreakDebug", "Stack trace: ${e.stackTraceToString()}")
            return@withContext 0
        }
    }

    suspend fun getBestStreak(): Int = withContext(Dispatchers.IO) {
        // Gets all usage data and calculates the best streak
        // Returns the highest streak achieved
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
    
    suspend fun resetAllUsageData() = withContext(Dispatchers.IO) {
        appUsageDao.deleteAllUsageData()
    }
} 