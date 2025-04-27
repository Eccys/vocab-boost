package xyz.ecys.vocab.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WordRepository private constructor(
    private val wordDao: WordDao,
    private val context: Context,
    private val settingsManager: SettingsManager? = null
) {
    
    companion object {
        @Volatile
        private var INSTANCE: WordRepository? = null
        
        fun getInstance(context: Context): WordRepository {
            return INSTANCE ?: synchronized(this) {
                val database = WordDatabase.getDatabase(context)
                val settingsManager = SettingsManager.getInstance(context)
                WordRepository(database.wordDao(), context.applicationContext, settingsManager).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Basic word operations
    suspend fun getRandomWordsExcluding(count: Int, excludeWord: Word?): List<Word> = withContext(Dispatchers.IO) {
        if (excludeWord == null) {
            wordDao.getRandomWords(count)
        } else {
            wordDao.getRandomWordsExcluding(count, excludeWord.id)
        }
    }

    suspend fun getRandomWords(count: Int): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getRandomWords(count)
    }

    fun getAllWordsFlow(): Flow<List<Word>> = wordDao.getAllWordsFlow()

    suspend fun getAllWords(): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getAllWords()
    }

    // Bookmark operations
    fun getBookmarkedWordsFlow(): Flow<List<Word>> = wordDao.getBookmarkedWordsFlow()

    suspend fun getRandomBookmarkedWords(count: Int): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getRandomBookmarkedWords(count)
    }

    suspend fun getRandomBookmarkedWordsExcluding(count: Int, excludeId: Int): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getRandomBookmarkedWordsExcluding(count, excludeId)
    }

    suspend fun updateBookmark(wordId: Int, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        wordDao.updateBookmark(wordId, isBookmarked)
    }

    // Add wrapper for getRandomWordsByCategory
    suspend fun getRandomWordsByCategory(category: String, count: Int): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getRandomWordsByCategory(category, count)
    }

    // Learning and statistics operations
    suspend fun updateWordStats(
        wordId: Int, 
        wasCorrect: Boolean, 
        timestamp: Long = System.currentTimeMillis(),
        responseTime: Long = 0
    ) = withContext(Dispatchers.IO) {
        // Get current word to access its repetition count
        val word = wordDao.getWordById(wordId) ?: return@withContext
        
        // Check if spaced repetition is enabled
        val isSpacedRepetitionEnabled = settingsManager?.isSpacedRepetitionEnabled() ?: true
        
        if (isSpacedRepetitionEnabled) {
            // Full spaced repetition algorithm
            // Calculate quality based on correctness and response time
            val quality = when {
                wasCorrect -> when {
                    responseTime < 3000 -> 5  // fast response
                    responseTime <= 5000 -> 4  // medium response
                    else -> 3  // slow but correct
                }
                else -> when {
                    word.repetitionCount == 1 -> 2  // failed but had one successful rep before
                    else -> 1  // complete fail
                }
            }

            // Calculate new ease factor
            val newEaseFactor = if (wasCorrect) {
                val adjustment = 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)
                kotlin.math.max(1.3f, word.easeFactor + adjustment)
            } else {
                val adjustment = 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)
                kotlin.math.max(1.3f, word.easeFactor + adjustment)
            }

            // Calculate new interval based on SM-2
            val newInterval = when {
                !wasCorrect -> 1  // Reset to 1 day on failure
                word.repetitionCount == 0 -> 1  // First successful repetition
                word.repetitionCount == 1 -> 3  // Second successful repetition
                else -> (word.interval * word.easeFactor).toInt()  // Subsequent repetitions
            }

            // Calculate next review date
            val nextReviewDate = timestamp + (newInterval * 24 * 60 * 60 * 1000L)

            // Update the word stats in the database with spaced repetition data
            wordDao.updateWordStats(
                wordId = wordId,
                wasCorrect = if (wasCorrect) 1 else 0,
                timestamp = timestamp,
                quality = quality,
                easeFactor = newEaseFactor,
                interval = newInterval,
                repetitionCount = if (wasCorrect) word.repetitionCount + 1 else 0,
                nextReviewDate = nextReviewDate
            )
        } else {
            // When spaced repetition is disabled, only update basic stats
            // Don't update easeFactor, interval, repetitionCount, nextReviewDate, or quality
            wordDao.updateBasicStats(
                wordId = wordId,
                wasCorrect = if (wasCorrect) 1 else 0,
                timestamp = timestamp
            )
        }
    }

    suspend fun getWordsForLearning(count: Int = 10): List<Word> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        
        // Get overdue words, now ordered by overdue ratio
        val overdueWords = wordDao.getOverdueWords(currentTime)
        
        if (overdueWords.size >= count) {
            // If there are enough overdue words, return the top count
            return@withContext overdueWords.take(count)
        }
        
        // If not enough, get unseen words
        val unseenWords = wordDao.getUnseenWords()
        val remainingCount = count - overdueWords.size
        if (unseenWords.size >= remainingCount) {
            return@withContext overdueWords + unseenWords.take(remainingCount)
        }
        
        // If still not enough, get random words
        val stillNeeded = count - overdueWords.size - unseenWords.size
        val randomWords = wordDao.getRandomWords(stillNeeded)
        return@withContext overdueWords + unseenWords + randomWords
    }

    // Database initialization
    suspend fun insertInitialWords() = withContext(Dispatchers.IO) {
        // Use the WordLoader to load words from JSON
        WordLoader.replaceAllWordsFromJson(context, wordDao)
    }

    // Add a method to add words without deleting existing ones
    suspend fun addWordsFromJson() = withContext(Dispatchers.IO) {
        WordLoader.addWordsFromJson(context, wordDao)
    }

    suspend fun resetAllStats() = withContext(Dispatchers.IO) {
        wordDao.resetAllStats()
    }

    suspend fun getRecentlyReviewedWords(limit: Int = 10): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getAllWords()
            .filter { it.lastReviewed > 0 }
            .sortedByDescending { it.lastReviewed }
            .take(limit)
    }

    suspend fun updateWordSynonyms(
        wordId: Int,
        synonym1: String? = null,
        synonym1Definition: String? = null,
        synonym1ExampleSentence: String? = null,
        synonym2: String? = null,
        synonym2Definition: String? = null,
        synonym2ExampleSentence: String? = null,
        synonym3: String? = null,
        synonym3Definition: String? = null,
        synonym3ExampleSentence: String? = null
    ) = withContext(Dispatchers.IO) {
        // Get the current word
        val word = wordDao.getAllWords().find { it.id == wordId } ?: return@withContext

        // Create updated word with new synonyms, keeping other fields unchanged
        val updatedWord = word.copy(
            synonym1 = synonym1 ?: word.synonym1,
            synonym1Definition = synonym1Definition ?: word.synonym1Definition,
            synonym1ExampleSentence = synonym1ExampleSentence ?: word.synonym1ExampleSentence,
            synonym2 = synonym2 ?: word.synonym2,
            synonym2Definition = synonym2Definition ?: word.synonym2Definition,
            synonym2ExampleSentence = synonym2ExampleSentence ?: word.synonym2ExampleSentence,
            synonym3 = synonym3 ?: word.synonym3,
            synonym3Definition = synonym3Definition ?: word.synonym3Definition,
            synonym3ExampleSentence = synonym3ExampleSentence ?: word.synonym3ExampleSentence
        )

        // Update the word in the database
        wordDao.updateWord(updatedWord)
    }

    suspend fun getNextWord(excludeWord: Word? = null): Word {
        val currentTime = System.currentTimeMillis()
        
        // Check if spaced repetition is enabled
        val isSpacedRepetitionEnabled = settingsManager?.isSpacedRepetitionEnabled() ?: true
        
        if (isSpacedRepetitionEnabled) {
            // PRIORITY 1: Get top overdue words
            // Get ALL overdue words
            val overdueWords = wordDao.getOverdueWords(currentTime)
            
            // Filter out the excluded word if any
            val availableOverdueWords = overdueWords.filter { it.id != excludeWord?.id }
            
            // If there are ANY overdue words, get top 5 (or fewer if less available)
            if (availableOverdueWords.isNotEmpty()) {
                // Calculate overdue ratio for each word
                val wordsWithRatio = availableOverdueWords.map { word ->
                    val dueDate = word.lastReviewed + (word.interval * 86400000L)
                    val overdueRatio = (currentTime - dueDate) / (Math.max(1, word.interval) * 86400000.0)
                    Pair(word, overdueRatio)
                }
                
                // Get the top 3 most overdue words
                val topOverdueWords = wordsWithRatio
                    .sortedByDescending { it.second }
                    .take(3)
                    .map { it.first }
                
                // Pick one of the top words randomly
                android.util.Log.d("WordPriority", "Selected from top ${topOverdueWords.size} overdue words")
                return topOverdueWords.random()
            }
            
            // PRIORITY 2: ONLY IF NO OVERDUE WORDS, USE UNSEEN WORDS
            // Get completely unseen words (timesReviewed = 0)
            val unseenWords = wordDao.getUnseenWords()
                .filter { it.id != excludeWord?.id }
            
            // If there are ANY unseen words, always return a random one of them
            if (unseenWords.isNotEmpty()) {
                android.util.Log.d("WordPriority", "Selected an unseen word")
                return unseenWords.random()
            }
            
            // PRIORITY 3: All words seen, none overdue - find closest to becoming overdue
            android.util.Log.d("WordPriority", "All words seen, none overdue - finding closest to becoming overdue")
            
            val allWords = wordDao.getAllWords().filter { it.id != excludeWord?.id && it.lastReviewed > 0 && it.interval > 0 }
            if (allWords.isNotEmpty()) {
                val wordsWithPseudoRatio = allWords.map { word ->
                    val dueDate = word.lastReviewed + (word.interval * 86400000L)
                    val pseudoRatio = (dueDate - currentTime) / (Math.max(1, word.interval) * 86400000.0)
                    Pair(word, pseudoRatio)
                }
                // Get the top 3 words closest to becoming overdue (smallest positive values)
                val topClosestWords = wordsWithPseudoRatio
                    .sortedBy { it.second }
                    .take(3)
                    .map { it.first }
                
                // Pick one of the top words randomly
                if (topClosestWords.isNotEmpty()) {
                    android.util.Log.d("WordPriority", "Selected from top ${topClosestWords.size} words closest to becoming overdue")
                    return topClosestWords.random()
                }
            }
            
            // LAST RESORT: Random word if something went wrong
            android.util.Log.d("WordPriority", "Falling back to completely random word")
            return if (excludeWord == null) {
                wordDao.getRandomWords(1).first()
            } else {
                wordDao.getRandomWordsExcluding(1, excludeWord.id).first()
            }
        } else {
            // When spaced repetition is disabled, simply return a random word
            android.util.Log.d("WordPriority", "Spaced repetition disabled, selecting fully random word")
            return if (excludeWord == null) {
                wordDao.getRandomWords(1).first()
            } else {
                wordDao.getRandomWordsExcluding(1, excludeWord.id).first()
            }
        }
    }
} 