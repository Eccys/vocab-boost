package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.collections.ArrayList

class QuizResultRepository private constructor(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "QuizResultRepository"

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

    // Save a quiz result locally only, don't immediately sync to Firestore
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
                
                // Log test event
                SyncTestLogger.logLocalSave()
                
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
                
                // Notify SyncManager that a question was answered (for tracking sync conditions)
                try {
                    val syncManager = SyncManager.getInstance(context)
                    syncManager.recordQuestionAnswered()
                    } catch (e: Exception) {
                    Log.w(TAG, "Failed to notify SyncManager: ${e.message}")
                }
                
                // Return the local ID
                return@withContext localId
            } catch (e: Exception) {
                println("Error in saveQuizResult: ${e.message}")
                e.printStackTrace()
                "error_${System.currentTimeMillis()}" // Return a unique error ID
            }
        }
    }

    // Method to sync all cached results to Firestore (called by SyncManager)
    suspend fun syncCachedResultsToFirestore(isExplicitSync: Boolean = false): Boolean {
        if (!isUserSignedIn()) {
            println("User not signed in, skipping sync")
            return false
        }
        
        // Validate Firestore write access
        if (!FirestoreAccessMonitor.validateWrite("QuizResultRepository.syncCachedResultsToFirestore", 
            "Sync quiz results to cloud", isExplicitSync)) {
            Log.w(TAG, "Unauthorized attempt to write to Firestore, sync aborted")
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                var success = true
                
                // Get proper collection references for the current user
                val userId = getUserId()
                // Store quiz_results in the user document directly
                val userDocument = firestore.collection("users").document(userId)
                
                // Sync quiz results that don't have a Firebase ID yet
                val localOnlyResults = localQuizResultsCache.filter { it.id.startsWith("local_") }
                
                for (result in localOnlyResults) {
                    try {
                        // Store in app_usage field as a map entry
                        val resultMap = mapOf(
                            "timestamp" to result.timestamp,
                            "correctAnswers" to result.correctAnswers,
                            "totalQuestions" to result.totalQuestions,
                            "score" to result.score,
                            "durationInSeconds" to result.durationInSeconds
                        )
                        
                        // Use transaction to update or create the document
                        firestore.runTransaction { transaction ->
                            // Get the current user document
                            val userDocSnapshot = transaction.get(userDocument)
                            
                            // Check if app_usage field exists
                            val appUsageMap = userDocSnapshot.get("app_usage") as? Map<String, Any> ?: hashMapOf()
                            
                            // Get or create quiz_results list
                            val quizResultsList = appUsageMap["quiz_results"] as? List<Map<String, Any>> ?: listOf()
                            
                            // Create updated list with new result
                            val updatedResults = quizResultsList + resultMap
                            
                            // Limit the number of stored results
                            val limitedResults = if (updatedResults.size > MAX_FIRESTORE_RESULTS) {
                                updatedResults.takeLast(MAX_FIRESTORE_RESULTS)
                            } else {
                                updatedResults
                            }
                            
                            // Create updated app_usage map
                            val updatedAppUsage = appUsageMap + ("quiz_results" to limitedResults)
                            
                            // Update the document
                            transaction.set(userDocument, mapOf("app_usage" to updatedAppUsage), SetOptions.merge())
                        }.await()
                        
                        // Update the local cache with a Firestore-style ID to mark it as synced
                        val index = localQuizResultsCache.indexOfFirst { it.id == result.id }
                        if (index >= 0) {
                            val updatedResult = localQuizResultsCache[index].copy(id = "synced_${System.currentTimeMillis()}")
                            localQuizResultsCache[index] = updatedResult
                        }
                    } catch (e: Exception) {
                        println("Error syncing quiz result to Firestore: ${e.message}")
                        success = false
                    }
                }
                
                // Sync history items that don't have a Firebase ID yet
                // Only sync the 30 most recent items to conserve Firestore operations
                val localOnlyHistory = localHistoryCache.filter { it.id.startsWith("local_") }
                    .sortedByDescending { it.timestamp }
                    .take(MAX_FIRESTORE_HISTORY)
                
                for (item in localOnlyHistory) {
                    try {
                        // Store in words field as a map entry
                        val historyItemMap = mapOf(
                            "timestamp" to item.timestamp,
                            "word" to item.word,
                            "correctDefinition" to item.correctDefinition,
                            "userAnswer" to item.userAnswer,
                            "isCorrect" to item.isCorrect
                        )
                        
                        // Use transaction to update or create the document
                        firestore.runTransaction { transaction ->
                            // Get the current user document
                            val userDocSnapshot = transaction.get(userDocument)
                            
                            // Check if words field exists
                            val wordsMap = userDocSnapshot.get("words") as? Map<String, Any> ?: hashMapOf()
                            
                            // Get or create history list
                            val historyList = wordsMap["history"] as? List<Map<String, Any>> ?: listOf()
                            
                            // Create updated list with new history item
                            val updatedHistory = historyList + historyItemMap
                            
                            // Limit the number of stored history items
                            val limitedHistory = if (updatedHistory.size > MAX_FIRESTORE_HISTORY) {
                                updatedHistory.takeLast(MAX_FIRESTORE_HISTORY)
                            } else {
                                updatedHistory
                            }
                            
                            // Create updated words map
                            val updatedWords = wordsMap + ("history" to limitedHistory)
                            
                            // Update the document
                            transaction.set(userDocument, mapOf("words" to updatedWords), SetOptions.merge())
                        }.await()
                        
                        // Update the local cache with a Firestore-style ID to mark it as synced
                        val index = localHistoryCache.indexOfFirst { it.id == item.id }
                        if (index >= 0) {
                            val updatedItem = localHistoryCache[index].copy(id = "synced_${System.currentTimeMillis()}")
                            localHistoryCache[index] = updatedItem
                        }
                    } catch (e: Exception) {
                        println("Error syncing history item to Firestore: ${e.message}")
                        success = false
                    }
                }
                
                // Save updated local caches with the new IDs
                if (success) {
                    saveLocalCache()
                    saveHistoryCache()
                }
                
                return@withContext success
            } catch (e: Exception) {
                println("Error in syncCachedResultsToFirestore: ${e.message}")
                e.printStackTrace()
                return@withContext false
            }
        }
    }
    
    // Get quiz history (use local cache unless forced refresh)
    suspend fun getQuizHistory(
        pageSize: Int = 50,
        forceRefresh: Boolean = false
    ): List<QuizHistoryItem> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if we should use the local cache or do a refresh
                val shouldRefresh = forceRefresh || isHistoryRefreshNeeded()
                
                if (!shouldRefresh) {
                    // Log test event for local read
                    SyncTestLogger.logLocalRead()
                    
                    // Use local cache data
                    println("Using cached history data")
                    return@withContext localHistoryCache
                        .sortedByDescending { it.timestamp }
                        .distinctBy { it.word.lowercase() } // Keep only the most recent attempt for each word
                        .take(pageSize)
                }
                
                // We need to refresh from Firestore
                if (!isUserSignedIn()) {
                    println("User not signed in, using only local history")
                    return@withContext localHistoryCache
                        .sortedByDescending { it.timestamp }
                        .distinctBy { it.word.lowercase() }
                        .take(pageSize)
                }
                
                // Check if we're allowed to read from Firestore
                if (!FirestoreAccessMonitor.validateRead("QuizResultRepository.getQuizHistory", 
                    "Fetch history from cloud with forceRefresh=$forceRefresh")) {
                    Log.w(TAG, "Unauthorized attempt to read from Firestore, using local cache instead")
                    return@withContext localHistoryCache
                        .sortedByDescending { it.timestamp }
                        .distinctBy { it.word.lowercase() }
                        .take(pageSize)
                }
                
                try {
                    Log.d(TAG, "Fetching history from Firestore due to refresh condition")
                    // Log test event for Firestore read
                    SyncTestLogger.logFirestoreRead()
                    
                    val userId = getUserId()
                    val userDocument = firestore.collection("users").document(userId)
                    
                    // Get the user document
                    val userDocSnapshot = userDocument.get().await()
                    
                    // Get words map and history list
                    val wordsMap = userDocSnapshot.get("words") as? Map<String, Any> ?: hashMapOf()
                    val historyList = wordsMap["history"] as? List<Map<String, Any>> ?: listOf()
                    
                    // Convert each item to QuizHistoryItem
                    val firestoreHistory = historyList.map { item ->
                        QuizHistoryItem(
                            id = "synced_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}",
                            userId = userId,
                            timestamp = (item["timestamp"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                            word = (item["word"] as? String) ?: "",
                            correctDefinition = (item["correctDefinition"] as? String) ?: "",
                            userAnswer = (item["userAnswer"] as? String) ?: "",
                            isCorrect = (item["isCorrect"] as? Boolean) ?: false
                        )
                    }
                    
                    println("Retrieved ${firestoreHistory.size} history items from Firestore")
                    
                    // Merge with local history (keeping newer versions of each word)
                    val mergedHistory = mergeHistory(localHistoryCache, firestoreHistory)
                    
                    // Update the local cache with the merged history
                    localHistoryCache.clear()
                    localHistoryCache.addAll(mergedHistory)
                    saveHistoryCache()
                    
                    // Update last refresh time
                    updateLastHistoryRefreshTime()
                    
                    // Return sorted history limited to page size
                    return@withContext mergedHistory
                        .sortedByDescending { it.timestamp }
                        .distinctBy { it.word.lowercase() } // Keep only the most recent attempt for each word
                        .take(pageSize)
                } catch (e: Exception) {
                    println("Error retrieving history from Firestore: ${e.message}")
                    e.printStackTrace()
            }
            
                // Fall back to local cache if Firestore fails
            return@withContext localHistoryCache
                .sortedByDescending { it.timestamp }
                    .distinctBy { it.word.lowercase() }
                .take(pageSize)
            } catch (e: Exception) {
                println("Error in getQuizHistory: ${e.message}")
                e.printStackTrace()
                return@withContext emptyList()
            }
        }
    }
    
    // Check if we need to refresh history from Firestore
    private fun isHistoryRefreshNeeded(): Boolean {
        val lastRefreshTime = sharedPreferences.getLong(KEY_LAST_HISTORY_REFRESH, 0)
        val currentTime = System.currentTimeMillis()
        return currentTime - lastRefreshTime > HISTORY_REFRESH_INTERVAL
    }
    
    // Update the timestamp of when history was last refreshed from Firestore
    private fun updateLastHistoryRefreshTime() {
        sharedPreferences.edit()
            .putLong(KEY_LAST_HISTORY_REFRESH, System.currentTimeMillis())
            .apply()
    }
    
    // Merge history from local storage and Firestore, giving preference to newer entries
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
            // Validate Firestore write access
            if (!FirestoreAccessMonitor.validateWrite("QuizResultRepository.clearAllQuizResults", 
                "Clear all quiz data from cloud", true)) {
                Log.w(TAG, "Unauthorized attempt to clear data from Firestore, skipping cloud clear")
                return
            }
            
            try {
                val userId = getUserId()
                val userQuizResultsCollection = firestore.collection("users")
                    .document(userId)
                    .collection("quiz_results")
                    
                val userHistoryCollection = firestore.collection("users")
                    .document(userId)
                    .collection("history")
                
                // Clear quiz results
                val results = userQuizResultsCollection
                    .get()
                    .await()
                
                for (document in results.documents) {
                    userQuizResultsCollection.document(document.id).delete().await()
                }
                
                // Clear history
                val history = userHistoryCollection
                    .get()
                    .await()
                
                for (document in history.documents) {
                    userHistoryCollection.document(document.id).delete().await()
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

    // Get a count of local items pending sync
    fun getLocalItemsPendingSync(): PendingSyncCount {
        val pendingQuizResults = localQuizResultsCache.count { it.id.startsWith("local_") }
        val pendingHistoryItems = localHistoryCache.count { it.id.startsWith("local_") }
        return PendingSyncCount(pendingQuizResults, pendingHistoryItems)
    }

    companion object {
        private const val KEY_LOCAL_RESULTS = "local_quiz_results"
        private const val KEY_LOCAL_HISTORY = "local_quiz_history"
        private const val KEY_LAST_HISTORY_REFRESH = "last_history_refresh"
        private const val MAX_LOCAL_RESULTS = 100   // Store up to 100 results locally
        private const val MAX_LOCAL_HISTORY = 200   // Store up to 200 history items locally
        private const val MAX_FIRESTORE_RESULTS = 30 // Limit to 30 results in Firestore
        private const val MAX_FIRESTORE_HISTORY = 30 // Limit to 30 history items in Firestore
        private const val HISTORY_REFRESH_INTERVAL = 15 * 60 * 1000L // 15 minutes in milliseconds
        
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

// Data class to hold counts of pending sync items
data class PendingSyncCount(
    val quizResults: Int,
    val historyItems: Int
) 