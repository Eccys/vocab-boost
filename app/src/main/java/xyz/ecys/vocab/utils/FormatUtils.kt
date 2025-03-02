package xyz.ecys.vocab.utils

object FormatUtils {
    fun formatNumber(number: Int): String = when {
        number >= 1000000 -> "%.1fM".format(number / 1000000f)
        number >= 1000 -> "%.1fk".format(number / 1000f)
        else -> number.toString()
    }

    fun formatTime(timeInMillis: Long): String {
        val seconds = timeInMillis / 1000
        val minutes = (seconds + 59) / 60  // Round up minutes
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        
        return when {
            hours > 0 -> "%.1fh".format(minutes / 60f)  // Show decimal hours
            minutes > 0 -> "${minutes}m"
            seconds > 0 -> "${seconds}s"
            else -> "0s"
        }
    }

    fun formatStreak(days: Int): String = when {
        days >= 365 -> "%.1fy".format(days / 365f)
        else -> "${days}d"
    }

    fun toSentenceCase(text: String): String {
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
} 