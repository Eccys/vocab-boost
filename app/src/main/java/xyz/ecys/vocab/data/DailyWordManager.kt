package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.random.Random
import java.util.*

class DailyWordManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("daily_word_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var allWords: List<DailyWord> = emptyList()
    private var previousWords: MutableList<DailyWord> = mutableListOf()
    
    // Cache for today's word to avoid repeated calculations
    private var cachedTodayWord: DailyWord? = null
    private var cachedDateStr: String? = null
    
    // Reference date for deterministic shuffling (2023-01-01)
    // Using Calendar API for compatibility with API level 24
    private val referenceCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2023)
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private val referenceTimestamp = referenceCalendar.timeInMillis
    
    init {
        loadAllWords()
        loadPreviousWords()
        
        // If there are no previous words, initialize with all words except today's word
        if (previousWords.isEmpty()) {
            initializePreviousWords()
        }
    }
    
    /**
     * Returns a lightweight version of today's word for quick display
     * This method is optimized for performance in the UI
     */
    fun getWordPreview(): DailyWord {
        // Get today's date using Calendar
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val todayStr = String.format("%04d-%02d-%02d", year, month, day)
        
        // Check if we have a cached word for today using string comparison
        if (cachedTodayWord != null && cachedDateStr == todayStr) {
            return cachedTodayWord!!
        }
        
        // Try to get from preferences first (fastest approach)
        val wordJson = prefs.getString("current_word", null)
        if (wordJson != null) {
            try {
                val word = gson.fromJson(wordJson, DailyWord::class.java)
                
                // Cache the word with date string
                cachedTodayWord = word
                cachedDateStr = todayStr
                
                return word
            } catch (e: Exception) {
                // If parsing fails, continue to next approach
            }
        }
        
        // If not in preferences, calculate it using the string-based method
        val word = getWordForSpecificDate(todayStr)
        
        cachedTodayWord = word
        cachedDateStr = todayStr
        
        return word
    }
    
    private fun initializePreviousWords() {
        // Get today's word based on date using Calendar
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val todayStr = String.format("%04d-%02d-%02d", year, month, day)
        val todayWord = getWordForSpecificDate(todayStr)
        
        // Add all words except today's word to previous words
        previousWords.clear()
        allWords.forEach { word ->
            if (word.word != todayWord.word) {
                previousWords.add(word)
            }
        }
        
        // Save the previous words
        prefs.edit()
            .putString("previous_words", gson.toJson(previousWords))
            .apply()
    }
    
    /**
     * Creates a deterministically shuffled sequence of indices based on cycle number.
     * This ensures a complete cycle through all words before any repeats.
     * 
     * @param cycleNumber The current cycle number (increases each time we go through all words)
     * @return A shuffled list of indices (0 to allWords.size-1)
     */
    private fun createDeterministicShuffle(cycleNumber: Int): List<Int> {
        // Create a sequence 0..allWords.size-1
        val indices = (0 until allWords.size).toList()
        
        // Use the cycle number as a seed for shuffling
        val random = Random(cycleNumber)
        
        // Shuffle the indices deterministically
        return indices.shuffled(random)
    }
    
    /**
     * Gets a word for a specific date using a deterministic shuffling algorithm.
     * This ensures all users see the same word on the same date, and that
     * all words appear exactly once before any word repeats.
     * 
     * This version uses a Calendar-based approach for API level 24 compatibility.
     */
    private fun getWordForDate(date: LocalDate): DailyWord {
        if (allWords.isEmpty()) {
            return getDefaultWord()
        }

        try {
            // Extract date components using reflection to avoid direct API 26+ dependencies
            val calendar = Calendar.getInstance()
            
            // Use reflection to get year, month, and day of month
            val yearMethod = date.javaClass.getMethod("getYear")
            val year = yearMethod.invoke(date) as Int
            
            val monthMethod = date.javaClass.getMethod("getMonthValue")
            val month = monthMethod.invoke(date) as Int
            
            val dayMethod = date.javaClass.getMethod("getDayOfMonth")
            val day = dayMethod.invoke(date) as Int
            
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1) // Calendar months are 0-based
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // Calculate days since reference date
            val diff = calendar.timeInMillis - referenceTimestamp
            val daysSinceReference = (diff / (24 * 60 * 60 * 1000)).toInt()
            
            // Calculate which cycle we're in and position within cycle
            val cycleNumber = daysSinceReference / allWords.size
            val positionInCycle = daysSinceReference % allWords.size
            
            // Create a deterministically shuffled sequence for this cycle
            val shuffledIndices = createDeterministicShuffle(cycleNumber)
            
            // Get the word at the shuffled position
            val adjustedPosition = if (positionInCycle < 0) positionInCycle + allWords.size else positionInCycle
            return allWords[shuffledIndices[adjustedPosition]]
        } catch (e: Exception) {
            // If anything fails, return the default word
            return getDefaultWord()
        }
    }
    
    fun getTodaysWord(): DailyWord {
        // Get today's date using Calendar instead of LocalDate.now()
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val todayStr = String.format("%04d-%02d-%02d", year, month, day)
        
        val lastUpdatedDate = getLastUpdatedDate()
        
        // If we haven't updated today based on string comparison
        if (lastUpdatedDate == null || lastUpdatedDate.toString() != todayStr) {
            // Update the daily word
            updateDailyWord(todayStr)
        }
        
        // Try to get the current word from preferences
        val wordJson = prefs.getString("current_word", null)
        if (wordJson != null) {
            try {
                return gson.fromJson(wordJson, DailyWord::class.java)
            } catch (e: Exception) {
                // If parsing fails, continue to calculate a new word
            }
        }
        
        // If no word is stored in preferences, calculate it
        return getWordForSpecificDate(todayStr)
    }
    
    fun getPreviousWords(): List<DailyWord> {
        return previousWords
    }
    
    private fun updateDailyWord(year: Int, month: Int, day: Int) {
        // Get the current word
        val currentWordJson = prefs.getString("current_word", null)
        val currentWord = if (currentWordJson != null) {
            gson.fromJson(currentWordJson, DailyWord::class.java)
        } else {
            null
        }
        
        // If there's a current word, add it to previous words
        if (currentWord != null) {
            addToPreviousWords(currentWord)
        }
        
        // Get a new word for today using the string date format
        val todayStr = String.format("%04d-%02d-%02d", year, month, day)
        val newWord = getWordForSpecificDate(todayStr)
        
        // Save the new word
        prefs.edit()
            .putString("current_word", gson.toJson(newWord))
            .putString("last_updated", todayStr)
            .apply()
        
        // Remove the new word from the previous words list if it exists
        previousWords.removeIf { it.word.equals(newWord.word, ignoreCase = true) }
    }
    
    private fun updateDailyWord(today: LocalDate) {
        // Call the new implementation using reflection to avoid direct API 26+ references
        try {
            // Use reflection to get year, month, and day from LocalDate
            val yearMethod = today.javaClass.getMethod("getYear")
            val year = yearMethod.invoke(today) as Int
            
            val monthValueMethod = today.javaClass.getMethod("getMonthValue")
            val monthValue = monthValueMethod.invoke(today) as Int
            
            val dayOfMonthMethod = today.javaClass.getMethod("getDayOfMonth")
            val dayOfMonth = dayOfMonthMethod.invoke(today) as Int
            
            updateDailyWord(year, monthValue, dayOfMonth)
        } catch (e: Exception) {
            // Fallback to basic implementation if LocalDate API access fails
            val currentWordJson = prefs.getString("current_word", null)
            val currentWord = if (currentWordJson != null) {
                gson.fromJson(currentWordJson, DailyWord::class.java)
            } else {
                null
            }
            
            if (currentWord != null) {
                addToPreviousWords(currentWord)
            }
            
            // Instead of directly accessing getWordForDate, get a string version of the date
            val dateStr = today.toString()
            val newWord = getWordForSpecificDate(dateStr)
            
            prefs.edit()
                .putString("current_word", gson.toJson(newWord))
                .putString("last_updated", dateStr)
                .apply()
            
            previousWords.removeIf { it.word.equals(newWord.word, ignoreCase = true) }
        }
    }
    
    private fun addToPreviousWords(word: DailyWord) {
        // Add to previous words if not already there
        if (previousWords.none { it.word.equals(word.word, ignoreCase = true) }) {
            previousWords.add(word)
            
            // Save the updated list
            prefs.edit()
                .putString("previous_words", gson.toJson(previousWords))
                .apply()
        }
    }
    
    // Custom date class for API compatibility
    private class DateInfo(val year: Int, val month: Int, val day: Int) {
        override fun toString(): String {
            return String.format("%04d-%02d-%02d", year, month, day)
        }
        
        fun equals(other: DateInfo): Boolean {
            return year == other.year && month == other.month && day == other.day
        }
    }
    
    private fun getLastUpdatedDate(): DateInfo? {
        val dateStr = prefs.getString("last_updated", null)
        return if (dateStr != null) {
            try {
                // Parse date string
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    
                    DateInfo(year, month, day)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    private fun loadAllWords() {
        try {
            val inputStream = context.assets.open("dailywords.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()
            
            val type = object : TypeToken<List<DailyWord>>() {}.type
            allWords = gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            e.printStackTrace()
            allWords = listOf(getDefaultWord())
        }
    }
    
    private fun loadPreviousWords() {
        val previousWordsJson = prefs.getString("previous_words", null)
        if (previousWordsJson != null) {
            val type = object : TypeToken<List<DailyWord>>() {}.type
            previousWords = gson.fromJson(previousWordsJson, type)
        }
    }
    
    private fun getDefaultWord(): DailyWord {
        return DailyWord(
            word = "Cryptic",
            pronunciation = "KRIP-tik",
            short_definition = "Having a hidden meaning",
            meaning = "Having a meaning that is mysterious or obscure, often intentionally.",
            category = "Adjective",
            example = "The detective found a cryptic note at the crime scene.",
            context = "Cryptic is often used to describe puzzles, messages, or statements that are deliberately difficult to understand or interpret.",
            did_you_know = "The word 'cryptic' comes from the Greek 'kryptikos,' meaning 'hidden' or 'secret.' It shares its roots with words like 'crypt' and 'encrypt,' all relating to concealment or secrecy."
        )
    }
    
    /**
     * Gets the word for a specific date, which can be in the past, present, or future.
     * This is useful for browsing past or future daily words.
     * 
     * @param date The date to get the word for
     * @return The DailyWord for the specified date
     */
    fun getWordForSpecificDate(date: LocalDate): DailyWord {
        return getWordForDate(date)
    }
    
    /**
     * Gets the word for a specific date using a string date format (yyyy-MM-dd).
     * Compatible with API level 24.
     * 
     * @param dateStr The date string in format "yyyy-MM-dd"
     * @return The DailyWord for the specified date
     */
    fun getWordForSpecificDate(dateStr: String): DailyWord {
        try {
            // Parse the date string into calendar components
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                
                // Calculate days since reference date
                val daysSinceReference = getDayOfEpoch(year, month, day)
                
                if (allWords.isEmpty()) {
                    return getDefaultWord()
                }
                
                // Calculate which cycle we're in and position within cycle
                val cycleNumber = daysSinceReference / allWords.size
                val positionInCycle = daysSinceReference % allWords.size
                
                // Create a deterministically shuffled sequence for this cycle
                val shuffledIndices = createDeterministicShuffle(cycleNumber)
                
                // Get the word at the shuffled position
                return allWords[shuffledIndices[positionInCycle]]
            } else {
                // Fallback to today's word if parsing fails
                return getTodaysWord()
            }
        } catch (e: Exception) {
            // Fallback to today's word if an error occurs
            return getTodaysWord()
        }
    }
    
    /**
     * Gets the daily words for a range of dates.
     * 
     * @param startDateStr The start date string in format "yyyy-MM-dd" (inclusive)
     * @param endDateStr The end date string in format "yyyy-MM-dd" (inclusive)
     * @return A list of DailyWords for the specified date range
     */
    fun getWordsForDateRange(startDateStr: String, endDateStr: String): List<DailyWord> {
        try {
            // Parse the dates
            val startDateParts = startDateStr.split("-")
            val endDateParts = endDateStr.split("-")
            
            if (startDateParts.size != 3 || endDateParts.size != 3) {
                return emptyList()
            }
            
            val startCalendar = Calendar.getInstance()
            startCalendar.set(
                startDateParts[0].toInt(),
                startDateParts[1].toInt() - 1, // Calendar months are 0-based
                startDateParts[2].toInt(),
                0, 0, 0
            )
            startCalendar.set(Calendar.MILLISECOND, 0)
            
            val endCalendar = Calendar.getInstance()
            endCalendar.set(
                endDateParts[0].toInt(),
                endDateParts[1].toInt() - 1, // Calendar months are 0-based
                endDateParts[2].toInt(),
                0, 0, 0
            )
            endCalendar.set(Calendar.MILLISECOND, 0)
            
            // Check if start date is after end date
            if (startCalendar.timeInMillis > endCalendar.timeInMillis) {
                return emptyList()
            }
            
            val words = mutableListOf<DailyWord>()
            val currentCalendar = startCalendar.clone() as Calendar
            
            while (currentCalendar.timeInMillis <= endCalendar.timeInMillis) {
                val year = currentCalendar.get(Calendar.YEAR)
                val month = currentCalendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
                val day = currentCalendar.get(Calendar.DAY_OF_MONTH)
                val dateStr = String.format("%04d-%02d-%02d", year, month, day)
                
                words.add(getWordForSpecificDate(dateStr))
                
                // Move to next day
                currentCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            return words
        } catch (e: Exception) {
            return emptyList()
        }
    }
    
    /**
     * Gets the daily words for a range of dates.
     * This overload maintains compatibility with code expecting LocalDate parameters.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A list of DailyWords for the specified date range
     */
    fun getWordsForDateRange(startDate: LocalDate, endDate: LocalDate): List<DailyWord> {
        try {
            // Use reflection to avoid direct API 26+ calls
            val toStringMethod = startDate.javaClass.getMethod("toString")
            val startDateStr = toStringMethod.invoke(startDate) as String
            val endDateStr = toStringMethod.invoke(endDate) as String
            
            return getWordsForDateRange(startDateStr, endDateStr)
        } catch (e: Exception) {
            return emptyList()
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: DailyWordManager? = null
        
        fun getInstance(context: Context): DailyWordManager {
            return INSTANCE ?: synchronized(this) {
                val instance = DailyWordManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * Calculates the number of days between the reference date (2023-01-01) and the specified date.
     * This method is used to determine which word to display for a given date.
     * 
     * @param year The year (e.g., 2023)
     * @param month The month (1-12)
     * @param day The day of month (1-31)
     * @return The number of days since the reference date
     */
    private fun getDayOfEpoch(year: Int, month: Int, day: Int): Int {
        // Reference date is 2023-01-01
        val refYear = 2023
        val refMonth = 1
        val refDay = 1
        
        // Function to check if year is leap year
        fun isLeapYear(year: Int): Boolean {
            return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        }
        
        // Function to get days in a month
        fun getDaysInMonth(year: Int, month: Int): Int {
            return when (month) {
                4, 6, 9, 11 -> 30
                2 -> if (isLeapYear(year)) 29 else 28
                else -> 31
            }
        }
        
        // Calculate days between years
        var days = 0
        if (year > refYear) {
            for (y in refYear until year) {
                days += if (isLeapYear(y)) 366 else 365
            }
        } else if (year < refYear) {
            for (y in year until refYear) {
                days -= if (isLeapYear(y)) 366 else 365
            }
        }
        
        // Calculate days in current year to current month
        if (month > refMonth || (month == refMonth && year > refYear)) {
            for (m in refMonth until month) {
                days += getDaysInMonth(year, m)
            }
        } else if (month < refMonth || (month == refMonth && year < refYear)) {
            for (m in month until refMonth) {
                days -= getDaysInMonth(year, m)
            }
        }
        
        // Add days of the month
        days += day - refDay
        
        return days
    }
    
    // Update method using custom DateInfo class
    private fun updateDailyWord(date: DateInfo) {
        updateDailyWord(date.toString())
    }
    
    // Update daily word using string date format (yyyy-mm-dd)
    private fun updateDailyWord(dateStr: String) {
        try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                
                // Get the current word
                val currentWordJson = prefs.getString("current_word", null)
                val currentWord = if (currentWordJson != null) {
                    gson.fromJson(currentWordJson, DailyWord::class.java)
                } else {
                    null
                }
                
                // Add current word to previous words if it exists
                if (currentWord != null) {
                    addToPreviousWords(currentWord)
                }
                
                // Get new word for today
                val newWord = getWordForSpecificDate(dateStr)
                
                // Save the new word in preferences
                prefs.edit()
                    .putString("current_word", gson.toJson(newWord))
                    .putString("last_updated", dateStr)
                    .apply()
                
                // Cache the word
                cachedTodayWord = newWord
                cachedDateStr = dateStr
                
                // Remove the new word from previous words list if it exists
                val index = previousWords.indexOfFirst { it.word == newWord.word }
                if (index >= 0) {
                    previousWords.removeAt(index)
                    prefs.edit().putString("previous_words", gson.toJson(previousWords)).apply()
                }
            }
        } catch (e: Exception) {
            // If anything fails, just log it and continue
            e.printStackTrace()
        }
    }
} 