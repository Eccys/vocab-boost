package xyz.ecys.vocab.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*

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
            // Use Calendar API which is compatible with API level 24
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
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
            // Update quiz duration separately
            appUsageDao.updateQuizDuration(today, duration)
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

    // New methods to track quiz time specifically
    suspend fun getTotalQuizTimeSpent(): Long = withContext(Dispatchers.IO) {
        appUsageDao.getTotalQuizTimeSpent()
    }

    suspend fun getQuizTimeSpentToday(): Long = withContext(Dispatchers.IO) {
        val today = getStartOfDayTimestamp()
        appUsageDao.getQuizTimeSpentSince(today)
    }

    suspend fun getTotalCorrectAnswers() = correctAnswerTracker.getTotalCorrectAnswers()

    suspend fun getCorrectAnswersToday() = correctAnswerTracker.getCorrectAnswersToday()

    fun getUsageBetweenDates(startDate: Long, endDate: Long): Flow<List<AppUsage>> {
        return appUsageDao.getUsageBetweenDates(startDate, endDate)
    }

    suspend fun getCurrentStreak(): Int = withContext(Dispatchers.IO) {
        try {
            val usages = appUsageDao.getUsageBetweenDates(0, System.currentTimeMillis()).first()
            
            // Sort by date and get active dates
            val activeDates = usages
                .filter { usage -> usage.correctAnswers > 0 }
                .map { usage -> usage.date }
                .sorted()
                .toList()
            
            if (activeDates.isEmpty()) return@withContext 0
            
            val dayInMillis = 24 * 60 * 60 * 1000
            val today = getStartOfDayTimestamp()
            
            // Check if last active date was today or yesterday
            val lastActiveDate = activeDates.last()
            val dayDiff = (today - lastActiveDate) / dayInMillis
            
            if (dayDiff > 1) return@withContext 0 // Streak broken if last activity was before yesterday
            
            // Count consecutive days backward from today/yesterday
            var streak = 1 // Start with 1 for today/yesterday
            var prevDate = lastActiveDate
            
            for (i in activeDates.size - 2 downTo 0) {
                val currentDate = activeDates[i]
                val consecutiveDayDiff = (prevDate - currentDate) / dayInMillis
                
                if (consecutiveDayDiff == 1L) {
                    streak++
                    prevDate = currentDate
                } else if (consecutiveDayDiff > 1L) {
                    break // Break on gap in streak
                }
            }
            
            return@withContext streak
        } catch (e: Exception) {
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