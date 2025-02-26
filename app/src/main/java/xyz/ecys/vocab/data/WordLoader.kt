package xyz.ecys.vocab.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class to load vocabulary words from external sources
 */
object WordLoader {
    private const val TAG = "WordLoader"
    
    /**
     * Loads words from a JSON file in the assets folder
     * @param context Application context to access assets
     * @return List of Word objects or empty list if loading fails
     */
    suspend fun loadWordsFromJson(context: Context): List<Word> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val wordType = object : TypeToken<List<JsonWord>>() {}.type
            val jsonWords: List<JsonWord> = Gson().fromJson(jsonString, wordType)
            
            return@withContext jsonWords.map { jsonWord ->
                Word(
                    word = jsonWord.word,
                    definition = jsonWord.definition,
                    exampleSentence = jsonWord.exampleSentence ?: "",
                    synonym1 = jsonWord.synonym1,
                    synonym1Definition = jsonWord.synonym1Definition,
                    synonym1ExampleSentence = jsonWord.synonym1ExampleSentence,
                    synonym2 = jsonWord.synonym2,
                    synonym2Definition = jsonWord.synonym2Definition,
                    synonym2ExampleSentence = jsonWord.synonym2ExampleSentence,
                    synonym3 = jsonWord.synonym3,
                    synonym3Definition = jsonWord.synonym3Definition,
                    synonym3ExampleSentence = jsonWord.synonym3ExampleSentence
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading words from JSON", e)
            return@withContext emptyList<Word>()
        }
    }
    
    /**
     * Adds words from JSON to the database without deleting existing words
     * @param context Application context
     * @param wordDao Data access object for words
     */
    suspend fun addWordsFromJson(context: Context, wordDao: WordDao) = withContext(Dispatchers.IO) {
        try {
            val words = loadWordsFromJson(context)
            if (words.isNotEmpty()) {
                wordDao.insertWords(words)
                Log.d(TAG, "Successfully added ${words.size} words from JSON")
            } else {
                Log.w(TAG, "No words found in JSON file to add")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding words from JSON", e)
        }
    }
    
    /**
     * Replaces all words in the database with words from JSON
     * @param context Application context
     * @param wordDao Data access object for words
     */
    suspend fun replaceAllWordsFromJson(context: Context, wordDao: WordDao) = withContext(Dispatchers.IO) {
        try {
            val words = loadWordsFromJson(context)
            if (words.isNotEmpty()) {
                wordDao.deleteAllWords()
                wordDao.resetWordsSequence()
                wordDao.insertWords(words)
                Log.d(TAG, "Successfully replaced database with ${words.size} words from JSON")
            } else {
                Log.w(TAG, "No words found in JSON file for database replacement")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing words from JSON", e)
        }
    }
}

/**
 * Helper data class for JSON parsing
 */
private data class JsonWord(
    val word: String,
    val definition: String,
    val exampleSentence: String? = null,
    val synonym1: String,
    val synonym1Definition: String,
    val synonym1ExampleSentence: String,
    val synonym2: String,
    val synonym2Definition: String,
    val synonym2ExampleSentence: String,
    val synonym3: String,
    val synonym3Definition: String,
    val synonym3ExampleSentence: String
) 