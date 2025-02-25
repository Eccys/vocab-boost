package xyz.ecys.vocab.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WordRepository private constructor(private val wordDao: WordDao) {
    
    companion object {
        @Volatile
        private var INSTANCE: WordRepository? = null
        
        fun getInstance(context: Context): WordRepository {
            return INSTANCE ?: synchronized(this) {
                val database = WordDatabase.getDatabase(context)
                WordRepository(database.wordDao()).also {
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
        // First, delete all existing words
        wordDao.deleteAllWords()
        // Reset the SQLite sequence so IDs start from 1 again
        wordDao.resetWordsSequence()
        
        val initialWords = listOf(
            Word(
                word = "ephemeral",
                definition = "lasting for a very short time",
                
                synonym1 = "temporary",
                synonym1Definition = "existing only for a limited time, not permanent",
                synonym1ExampleSentence = "The temporary exhibit will only be at the museum for one month.",
                
                synonym2 = "fleeting",
                synonym2Definition = "passing swiftly; lasting for a very brief time",
                synonym2ExampleSentence = "She caught a fleeting glimpse of the rare bird before it flew away.",
                
                synonym3 = "transient",
                synonym3Definition = "lasting only for a short time; impermanent",
                synonym3ExampleSentence = "The transient nature of fashion trends makes it hard to keep up."
            ),
            Word(
                word = "ubiquitous",
                definition = "present, appearing, or found everywhere",
                
                synonym1 = "omnipresent",
                synonym1Definition = "present everywhere at the same time",
                synonym1ExampleSentence = "The omnipresent smell of coffee filled every corner of the café.",
                
                synonym2 = "pervasive",
                synonym2Definition = "spreading widely throughout an area or group of people",
                synonym2ExampleSentence = "Social media has become a pervasive influence in modern life.",
                
                synonym3 = "universal",
                synonym3Definition = "present or occurring everywhere",
                synonym3ExampleSentence = "The universal appeal of music transcends cultural boundaries."
            ),
            Word(
                word = "serendipity",
                definition = "the occurrence of finding pleasant things by chance",
                
                synonym1 = "chance",
                synonym1Definition = "a random or unplanned fortunate discovery",
                synonym1ExampleSentence = "By chance, she discovered her favorite book in a small bookstore.",
                
                synonym2 = "fortune",
                synonym2Definition = "a happy accident or pleasant surprise",
                synonym2ExampleSentence = "It was pure fortune that led him to meet his future business partner.",
                
                synonym3 = "luck",
                synonym3Definition = "success or good fortune by chance rather than design",
                synonym3ExampleSentence = "Through sheer luck, they found the perfect house within their budget."
            ),
            Word(
                word = "aberration",
                definition = "a state or condition markedly different from the norm",
                
                synonym1 = "anomaly",
                synonym1Definition = "something that deviates from the standard pattern",
                synonym1ExampleSentence = "Scientists detected an anomaly in the data that required further investigation.",
                
                synonym2 = "deviation",
                synonym2Definition = "departure from the usual or expected course",
                synonym2ExampleSentence = "The sudden drop in temperature was a deviation from typical summer weather.",
                
                synonym3 = "irregularity",
                synonym3Definition = "something that varies from the normal arrangement or development",
                synonym3ExampleSentence = "The irregularity in his heartbeat prompted a visit to the doctor."
            ),
            Word(
                word = "abhor",
                definition = "to feel hatred or disgust toward",
                
                synonym1 = "detest",
                synonym1Definition = "to dislike intensely; to feel repugnance toward",
                synonym1ExampleSentence = "He detests the sound of nails on a chalkboard.",
                
                synonym2 = "loathe",
                synonym2Definition = "to feel intense dislike or disgust for",
                synonym2ExampleSentence = "She loathes getting up early on cold winter mornings.",
                
                synonym3 = "despise",
                synonym3Definition = "to regard with deep contempt or aversion",
                synonym3ExampleSentence = "They despise any form of dishonesty in their organization."
            ),
            Word(
                word = "acquiesce",
                definition = "to agree or express agreement reluctantly",
                
                synonym1 = "comply",
                synonym1Definition = "to accept or go along with something without protest",
                synonym1ExampleSentence = "The students complied with the new dress code regulations.",
                
                synonym2 = "consent",
                synonym2Definition = "to give permission or approval",
                synonym2ExampleSentence = "After much discussion, she consented to the proposed changes.",
                
                synonym3 = "yield",
                synonym3Definition = "to give way to or submit to pressure or demands",
                synonym3ExampleSentence = "The committee finally yielded to public pressure and changed their policy."
            )
        )
        wordDao.insertWords(initialWords)
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