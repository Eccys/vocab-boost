package xyz.ecys.vocab.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class CorrectAnswerTracker private constructor(private val appUsageDao: AppUsageDao) {
    companion object {
        @Volatile
        private var INSTANCE: CorrectAnswerTracker? = null
        
        fun getInstance(context: Context): CorrectAnswerTracker {
            return INSTANCE ?: synchronized(this) {
                val database = WordDatabase.getDatabase(context)
                CorrectAnswerTracker(database.appUsageDao()).also {
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

    suspend fun recordCorrectAnswer() = withContext(Dispatchers.IO) {
        val today = getStartOfDayTimestamp()
        appUsageDao.recordUsage(AppUsage(date = today))
        appUsageDao.incrementCorrectAnswers(today)
    }

    suspend fun getCorrectAnswersToday(): Int = withContext(Dispatchers.IO) {
        val today = getStartOfDayTimestamp()
        appUsageDao.getCorrectAnswersForDate(today) ?: 0
    }

    suspend fun getTotalCorrectAnswers(): Int = withContext(Dispatchers.IO) {
        appUsageDao.getTotalCorrectAnswers()
    }

    suspend fun getCorrectAnswersForDate(date: Long): Int = withContext(Dispatchers.IO) {
        appUsageDao.getCorrectAnswersForDate(date) ?: 0
    }
} 