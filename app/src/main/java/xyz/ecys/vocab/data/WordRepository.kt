package xyz.ecys.vocab.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WordRepository private constructor(
    private val wordDao: WordDao,
    private val context: Context
) {
    
    companion object {
        @Volatile
        private var INSTANCE: WordRepository? = null
        
        fun getInstance(context: Context): WordRepository {
            return INSTANCE ?: synchronized(this) {
                val database = WordDatabase.getDatabase(context)
                WordRepository(database.wordDao(), context.applicationContext).also {
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

    // Learning and statistics operations
    suspend fun updateWordStats(
        wordId: Int, 
        wasCorrect: Boolean, 
        timestamp: Long = System.currentTimeMillis(),
        responseTime: Long = 0
    ) = withContext(Dispatchers.IO) {
        // Get current word to access its repetition count
        // val word = wordDao.getAllWords().find { it.id == wordId } ?: return@withContext
        // New efficient approach: fetching the word directly by its ID
        val word = wordDao.getWordById(wordId) ?: return@withContext

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

        // Update the word stats in the database
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
        
        // PRIORITY 1: ALL OVERDUE WORDS
        // Get ALL overdue words, sorted by their overdue ratio
        val overdueWords = wordDao.getOverdueWords(currentTime)
        
        // Filter out the excluded word if any
        val availableOverdueWords = overdueWords.filter { it.id != excludeWord?.id }
        
        // If there are ANY overdue words, always return the one with highest overdue ratio
        if (availableOverdueWords.isNotEmpty()) {
            android.util.Log.d("WordPriority", "Selected an overdue word with ratio: " + 
                ((currentTime - availableOverdueWords.first().nextReviewDate) / 
                (Math.max(1, availableOverdueWords.first().interval) * 86400000.0)).toString())
            return availableOverdueWords.first()
        }
        
        // PRIORITY 2: ONLY IF NO OVERDUE WORDS, USE UNSEEN WORDS
        // Get completely unseen words (timesReviewed = 0)
        val unseenWords = wordDao.getUnseenWords()
            .filter { it.id != excludeWord?.id }
        
        // If there are ANY unseen words, always return one of them
        if (unseenWords.isNotEmpty()) {
            android.util.Log.d("WordPriority", "Selected an unseen word")
            // Take the first one, don't randomize
            return unseenWords.first()
        }
        
        // PRIORITY 3: ONLY AS LAST RESORT, USE OTHER WORDS
        // We only reach here if there are NO overdue words AND NO unseen words
        android.util.Log.d("WordPriority", "No overdue or unseen words, selecting random word")
        
        // Get all words that are not excluded and don't have a future review date
        val allWords = wordDao.getAllWords()
        val availableWords = allWords
            .filter { it.id != excludeWord?.id }
            .filter { it.nextReviewDate == 0L || it.nextReviewDate <= currentTime }
        
        // If there are any available words, return a random one
        if (availableWords.isNotEmpty()) {
            return availableWords.random()
        }
        
        // If all words have future review dates, log this situation and return a random word
        // excluding the current one (this should be a rare fallback)
        android.util.Log.d("WordPriority", "All words have future review dates, selecting random word anyway")
        return if (excludeWord == null) {
            wordDao.getRandomWords(1).first()
        } else {
            wordDao.getRandomWordsExcluding(1, excludeWord.id).first()
        }
    }
} 