package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.collections.ArrayList

class QuizResultRepository private constructor(context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val quizResultsCollection = firestore.collection("quiz_results")
    private val historyCollection = firestore.collection("history")
    private val auth = FirebaseAuth.getInstance()

    // Local storage using SharedPreferences
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "quiz_results_local", Context.MODE_PRIVATE
    )
    private val historyPreferences: SharedPreferences = context.getSharedPreferences(
        "quiz_history_local", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    // Flag to track if we're in the process of signing in anonymously
    private var signingInAnonymously = false
    
    // Local cache for quiz results to handle permission issues
    private val localQuizResultsCache = ArrayList<QuizResult>()
    private val localHistoryCache = ArrayList<QuizHistoryItem>()
    
    init {
        // Load the local cache from SharedPreferences
        loadLocalCache()
        loadHistoryCache()
        
        // Try to ensure anonymous auth is available
        ensureAuthAvailable()
    }
    
    // Load stored results from SharedPreferences
    private fun loadLocalCache() {
        val resultsJson = sharedPreferences.getString(KEY_LOCAL_RESULTS, "[]")
        val type = object : TypeToken<List<QuizResult>>() {}.type
        try {
            val storedResults: List<QuizResult> = gson.fromJson(resultsJson, type)
            localQuizResultsCache.clear()
            localQuizResultsCache.addAll(storedResults)
            println("Loaded ${localQuizResultsCache.size} quiz results from local storage")
        } catch (e: Exception) {
            println("Error loading local results: ${e.message}")
            // Initialize with empty list if there's an error
            localQuizResultsCache.clear()
        }
    }
    
    // Load stored history from SharedPreferences
    private fun loadHistoryCache() {
        val historyJson = historyPreferences.getString(KEY_LOCAL_HISTORY, "[]")
        val type = object : TypeToken<List<QuizHistoryItem>>() {}.type
        try {
            val storedHistory: List<QuizHistoryItem> = gson.fromJson(historyJson, type)
            localHistoryCache.clear()
            localHistoryCache.addAll(storedHistory)
            println("Loaded ${localHistoryCache.size} history items from local storage")
        } catch (e: Exception) {
            println("Error loading local history: ${e.message}")
            // Initialize with empty list if there's an error
            localHistoryCache.clear()
        }
    }
    
    // Save the local cache to SharedPreferences
    private fun saveLocalCache() {
        try {
            val resultsJson = gson.toJson(localQuizResultsCache)
            sharedPreferences.edit()
                .putString(KEY_LOCAL_RESULTS, resultsJson)
                .apply()
            println("Saved ${localQuizResultsCache.size} quiz results to local storage")
        } catch (e: Exception) {
            println("Error saving local results: ${e.message}")
        }
    }
    
    // Save the history cache to SharedPreferences
    private fun saveHistoryCache() {
        try {
            val historyJson = gson.toJson(localHistoryCache)
            historyPreferences.edit()
                .putString(KEY_LOCAL_HISTORY, historyJson)
                .apply()
            println("Saved ${localHistoryCache.size} history items to local storage")
        } catch (e: Exception) {
            println("Error saving local history: ${e.message}")
        }
    }

    // Save a quiz result locally first, then to Firestore if signed in
    suspend fun saveQuizResult(quizResult: QuizResult): String {
        return withContext(Dispatchers.IO) {
            try {
                // Generate a unique local ID
                val localId = "local_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
                
                // Create a new result with the user ID and generated local ID
                val resultWithUserId = quizResult.copy(
                    id = localId,
                    userId = getUserId()
                )
                
                println("Saving quiz result with ${resultWithUserId.questions.size} questions for user ${resultWithUserId.userId}")
                
                // Add to local cache first
                localQuizResultsCache.add(resultWithUserId)
                
                // Also create history items from this quiz result
                val historyItems = resultWithUserId.questions.map { question ->
                    QuizHistoryItem(
                        id = "local_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}",
                        userId = resultWithUserId.userId,
                        timestamp = resultWithUserId.timestamp,
                        word = question.word,
                        correctDefinition = question.correctDefinition,
                        userAnswer = question.userAnswer,
                        isCorrect = question.isCorrect
                    )
                }
                
                // Add to history cache
                localHistoryCache.addAll(historyItems)
                
                // Enforce local cache limits
                if (localQuizResultsCache.size > MAX_LOCAL_RESULTS) {
                    localQuizResultsCache.sortByDescending { it.timestamp }
                    while (localQuizResultsCache.size > MAX_LOCAL_RESULTS) {
                        localQuizResultsCache.removeAt(localQuizResultsCache.size - 1)
                    }
                }
                
                if (localHistoryCache.size > MAX_LOCAL_HISTORY) {
                    localHistoryCache.sortByDescending { it.timestamp }
                    while (localHistoryCache.size > MAX_LOCAL_HISTORY) {
                        localHistoryCache.removeAt(localHistoryCache.size - 1)
                    }
                }
                
                // Save the updated caches to SharedPreferences
                saveLocalCache()
                saveHistoryCache()
                
                // Only attempt to sync with Firestore if the user is signed in
                if (isUserSignedIn()) {
                    try {
                        // Save the quiz result
            val docRef = quizResultsCollection.add(resultWithUserId).await()
                        println("Successfully synced quiz result to Firestore with ID: ${docRef.id}")
                        
                        // Update the local cache with the Firestore ID
                        val index = localQuizResultsCache.indexOfFirst { it.id == localId }
                        if (index >= 0) {
                            val updatedResult = localQuizResultsCache[index].copy(id = docRef.id)
                            localQuizResultsCache[index] = updatedResult
                            saveLocalCache()
                        }
                        
                        // Also save history items to the history collection
                        for (historyItem in historyItems) {
                            val historyRef = historyCollection.add(historyItem).await()
                            println("Saved history item to Firestore with ID: ${historyRef.id}")
                            
                            // Update the local history cache with the Firestore ID
                            val historyIndex = localHistoryCache.indexOfFirst { it.id == historyItem.id }
                            if (historyIndex >= 0) {
                                val updatedHistoryItem = localHistoryCache[historyIndex].copy(id = historyRef.id)
                                localHistoryCache[historyIndex] = updatedHistoryItem
                            }
                        }
                        saveHistoryCache()
                        
                        // Try to enforce the limit of quiz results in Firestore
                        enforceQuizResultLimit(MAX_FIRESTORE_RESULTS)
                        enforceHistoryLimit(MAX_FIRESTORE_HISTORY)
                        
                        return@withContext docRef.id
                    } catch (e: Exception) {
                        println("Error syncing to Firestore: ${e.message}")
                        return@withContext localId
                    }
                } else {
                    println("User not signed in, result saved locally only")
                    return@withContext localId
                }
            } catch (e: Exception) {
                println("Error in saveQuizResult: ${e.message}")
                e.printStackTrace()
                "error_${System.currentTimeMillis()}" // Return a unique error ID
            }
        }
    }

    // Method to enforce the limit of history items
    private suspend fun enforceHistoryLimit(maxItems: Int) {
        withContext(Dispatchers.IO) {
            try {
                // Only enforce limit if user is signed in
                if (isUserSignedIn()) {
                    // Get all history items for the current user, ordered by timestamp (oldest first)
                    val results = historyCollection
                        .whereEqualTo("userId", getUserId())
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .get()
                        .await()
                    
                    // If we have more than the maximum allowed items
                    if (results.size() > maxItems) {
                        // Calculate how many to delete
                        val numberToDelete = results.size() - maxItems
                        
                        // Get the IDs of the oldest items
                        val oldestIds = results.documents
                            .take(numberToDelete)
                            .map { it.id }
                        
                        // Delete each of the oldest items
                        for (id in oldestIds) {
                            historyCollection.document(id).delete().await()
                        }
                    }
                }
            } catch (e: Exception) {
                // Log the error but don't crash
                println("Error enforcing history limit: ${e.message}")
            }
        }
    }

    // Get quiz history specifically (focused format for history view)
    suspend fun getQuizHistory(pageSize: Int = MAX_LOCAL_HISTORY): List<QuizHistoryItem> {
        return withContext(Dispatchers.IO) {
            println("Fetching quiz history for user: ${getUserId()}")
            
            // If the user is signed in, try to sync with Firestore first
            if (isUserSignedIn()) {
                try {
                    val firestoreHistory = historyCollection
                        .whereEqualTo("userId", getUserId())
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .await()
                        .toObjects(QuizHistoryItem::class.java)
                    
                    println("Retrieved ${firestoreHistory.size} history items from Firestore")
                    
                    // Merge with local history (keeping newer versions of each word)
                    val mergedHistory = mergeHistory(localHistoryCache, firestoreHistory)
                    
                    // Update the local cache with the merged history
                    localHistoryCache.clear()
                    localHistoryCache.addAll(mergedHistory)
                    saveHistoryCache()
                    
                    // Return sorted history limited to page size
                    return@withContext mergedHistory
                        .sortedByDescending { it.timestamp }
                        .distinctBy { it.word.lowercase() } // Keep only the most recent attempt for each word
                        .take(pageSize)
                } catch (e: Exception) {
                    println("Error retrieving history from Firestore: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            // If we couldn't get results from Firestore or user isn't signed in,
            // return the local cache
            println("Using local history cache with ${localHistoryCache.size} items")
            return@withContext localHistoryCache
                .sortedByDescending { it.timestamp }
                .distinctBy { it.word.lowercase() } // Keep only the most recent attempt for each word
                .take(pageSize)
        }
    }
    
    // Helper method to merge local and Firestore history
    private fun mergeHistory(localHistory: List<QuizHistoryItem>, firestoreHistory: List<QuizHistoryItem>): List<QuizHistoryItem> {
        val historyMap = mutableMapOf<String, QuizHistoryItem>()
        
        // Add all local history to the map, keyed by word to identify duplicates
        for (item in localHistory) {
            val key = item.word.lowercase()
            // Only store if it doesn't exist or is newer
            if (!historyMap.containsKey(key) || historyMap[key]!!.timestamp.before(item.timestamp)) {
                historyMap[key] = item
            }
        }
        
        // Add or update with Firestore history
        for (item in firestoreHistory) {
            val key = item.word.lowercase()
            // Only store if it doesn't exist or is newer
            if (!historyMap.containsKey(key) || historyMap[key]!!.timestamp.before(item.timestamp)) {
                historyMap[key] = item
            }
        }
        
        // Convert back to a list
        return historyMap.values.toList()
    }

    // Method to clear all quiz results and history from both local storage and Firestore
    suspend fun clearAllQuizResults() {
        // Clear local caches
        localQuizResultsCache.clear()
        localHistoryCache.clear()
        saveLocalCache()
        saveHistoryCache()
        
        // Clear from Firestore if user is signed in
        if (isUserSignedIn()) {
            try {
                // Clear quiz results
                val results = quizResultsCollection
                    .whereEqualTo("userId", getUserId())
                    .get()
                    .await()
                
                for (document in results.documents) {
                    quizResultsCollection.document(document.id).delete().await()
                }
                
                // Clear history
                val history = historyCollection
                    .whereEqualTo("userId", getUserId())
                    .get()
                    .await()
                
                for (document in history.documents) {
                    historyCollection.document(document.id).delete().await()
                }
                
                println("Cleared all quiz results and history from Firestore for user: ${getUserId()}")
            } catch (e: Exception) {
                println("Error clearing data from Firestore: ${e.message}")
            }
        }
    }

    // Ensures some form of authentication is available
    private fun ensureAuthAvailable() {
        if (auth.currentUser == null && !signingInAnonymously) {
            signingInAnonymously = true
            auth.signInAnonymously()
                .addOnSuccessListener {
                    println("Anonymous authentication successful")
                    signingInAnonymously = false
                }
                .addOnFailureListener { e ->
                    println("Error with anonymous authentication: ${e.message}")
                    signingInAnonymously = false
                }
        }
    }

    // Get the current user ID or "local_user" if not logged in
    private fun getUserId(): String {
        val userId = auth.currentUser?.uid ?: "local_user"
        println("Using user ID: $userId")
        return userId
    }

    // Check if the user is signed in with a real account (not anonymous)
    private fun isUserSignedIn(): Boolean {
        val user = auth.currentUser
        return user != null && !user.isAnonymous
    }

    // Method to enforce the limit of quiz results
    private suspend fun enforceQuizResultLimit(maxResults: Int) {
        withContext(Dispatchers.IO) {
            try {
                // Only enforce limit if user is signed in
                if (isUserSignedIn()) {
                // Get all quiz results for the current user, ordered by timestamp (oldest first)
                val results = quizResultsCollection
                    .whereEqualTo("userId", getUserId())
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()
                
                // If we have more than the maximum allowed results
                if (results.size() > maxResults) {
                    // Calculate how many to delete
                    val numberToDelete = results.size() - maxResults
                    
                    // Get the IDs of the oldest quiz results
                    val oldestResultIds = results.documents
                        .take(numberToDelete)
                        .map { it.id }
                    
                    // Delete each of the oldest results
                    for (id in oldestResultIds) {
                        quizResultsCollection.document(id).delete().await()
                        }
                    }
                }
            } catch (e: Exception) {
                // Log the error but don't crash
                println("Error enforcing quiz result limit: ${e.message}")
            }
        }
    }

    companion object {
        private const val KEY_LOCAL_RESULTS = "local_quiz_results"
        private const val KEY_LOCAL_HISTORY = "local_quiz_history"
        private const val MAX_LOCAL_RESULTS = 100   // Store up to 100 results locally
        private const val MAX_LOCAL_HISTORY = 200   // Store up to 200 history items locally
        private const val MAX_FIRESTORE_RESULTS = 30 // Limit to 30 results in Firestore
        private const val MAX_FIRESTORE_HISTORY = 100 // Limit to 100 history items in Firestore
        
        @Volatile
        private var INSTANCE: QuizResultRepository? = null

        fun getInstance(context: Context): QuizResultRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = QuizResultRepository(context)
                INSTANCE = instance
                instance
            }
        }
        
        // Overloaded method for backward compatibility
        fun getInstance(): QuizResultRepository {
            return INSTANCE ?: throw IllegalStateException(
                "QuizResultRepository must be initialized with a context first"
            )
        }
    }
}

// New data class specifically for quiz history
data class QuizHistoryItem(
    val id: String = "",
    val userId: String = "",
    val timestamp: Date = Date(),
    val word: String = "",
    val correctDefinition: String = "",
    val userAnswer: String = "",
    val isCorrect: Boolean = false
) 