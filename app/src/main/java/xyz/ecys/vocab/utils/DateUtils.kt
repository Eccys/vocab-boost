package xyz.ecys.vocab.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility functions for working with dates
 */
object DateUtils {
    private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Convert a Date object to an ISO-8601 string
     */
    fun toIsoString(date: Date): String {
        return ISO_FORMAT.format(date)
    }
    
    /**
     * Parse an ISO-8601 string to a Date object
     */
    fun parseIsoString(dateString: String): Date? {
        return try {
            ISO_FORMAT.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Extension function to convert a Date to an ISO-8601 string
 */
fun Date.toIsoString(): String {
    return DateUtils.toIsoString(this)
} 