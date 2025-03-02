package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.random.Random

class DailyWordManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("daily_word_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var allWords: List<DailyWord> = emptyList()
    private var previousWords: MutableList<DailyWord> = mutableListOf()
    
    // Cache for today's word to avoid repeated calculations
    private var cachedTodayWord: DailyWord? = null
    private var cachedDate: LocalDate? = null
    
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
        // Check if we have a cached word for today
        val today = LocalDate.now()
        if (cachedTodayWord != null && cachedDate?.isEqual(today) == true) {
            return cachedTodayWord!!
        }
        
        // Try to get from preferences first (fastest approach)
        val wordJson = prefs.getString("current_word", null)
        if (wordJson != null) {
            try {
                return gson.fromJson(wordJson, DailyWord::class.java).also { 
                    cachedTodayWord = it
                    cachedDate = today
                }
            } catch (e: Exception) {
                // If parsing fails, continue to next approach
            }
        }
        
        // If not in preferences, calculate it (more expensive operation)
        return getWordForDate(today).also {
            cachedTodayWord = it
            cachedDate = today
        }
    }
    
    private fun initializePreviousWords() {
        // Get today's word based on date
        val today = LocalDate.now()
        val todayWord = getWordForDate(today)
        
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
    
    private fun getWordIndexForDate(date: LocalDate): Int {
        if (allWords.isEmpty()) return 0
        
        // Use the date to determine the index using our deterministic algorithm
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val hash = consistentHash(dateString)
        return abs(hash) % allWords.size
    }
    
    fun getTodaysWord(): DailyWord {
        val today = LocalDate.now()
        val lastUpdatedDate = getLastUpdatedDate()
        
        // If we haven't updated today, get a new word
        if (lastUpdatedDate == null || !lastUpdatedDate.isEqual(today)) {
            updateDailyWord(today)
        }
        
        // Get the current word from preferences
        val wordJson = prefs.getString("current_word", null)
        return if (wordJson != null) {
            gson.fromJson(wordJson, DailyWord::class.java)
        } else {
            // Fallback to a default word
            getDefaultWord()
        }
    }
    
    fun getPreviousWords(): List<DailyWord> {
        return previousWords
    }
    
    private fun updateDailyWord(today: LocalDate) {
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
        
        // Get a new word for today using the deterministic algorithm
        val newWord = getWordForDate(today)
        
        // Save the new word
        prefs.edit()
            .putString("current_word", gson.toJson(newWord))
            .putString("last_updated", today.toString())
            .apply()
        
        // Remove the new word from the previous words list if it exists
        previousWords.removeIf { it.word.equals(newWord.word, ignoreCase = true) }
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
    
    private fun getLastUpdatedDate(): LocalDate? {
        val dateStr = prefs.getString("last_updated", null)
        return if (dateStr != null) {
            LocalDate.parse(dateStr)
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
    
    /**
     * Gets a word for a specific date using a deterministic algorithm.
     * This ensures all users see the same word on the same date.
     */
    private fun getWordForDate(date: LocalDate): DailyWord {
        if (allWords.isEmpty()) {
            return getDefaultWord()
        }

        // Use a deterministic algorithm to get a consistent word index
        // for all users on the same day, regardless of device
        val algorithmVersion = 1 // Increment this if the algorithm changes
        
        // Format the date consistently
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE) // YYYY-MM-DD format
        
        // Create a seed from the date + algorithm version for consistency
        val seed = "$dateString:v$algorithmVersion"
        
        // Use Java's String.hashCode() which is consistent across platforms
        var hash = seed.hashCode()
        
        // If you need more entropy, you can also use a more complex hash function:
        for (char in seed) {
            hash = 31 * hash + char.code
        }
        
        // Ensure a positive index and apply modulo to get within range of available words
        val wordIndex = Math.abs(hash) % allWords.size
        
        // Return the word at the computed index
        return allWords[wordIndex]
    }
    
    /**
     * Consistent hash function to ensure the same words are selected across devices
     */
    private fun consistentHash(input: String): Int {
        var h = 0
        for (i in input.indices) {
            h = 31 * h + input[i].code
        }
        return h
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
     * Gets the words for a range of dates, useful for preloading or browsing a calendar.
     * 
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return A list of DailyWords for the specified date range
     */
    fun getWordsForDateRange(startDate: LocalDate, endDate: LocalDate): List<DailyWord> {
        if (startDate.isAfter(endDate)) {
            return emptyList()
        }
        
        val words = mutableListOf<DailyWord>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            words.add(getWordForDate(currentDate))
            currentDate = currentDate.plusDays(1)
        }
        
        return words
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
} 