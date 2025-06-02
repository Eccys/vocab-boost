package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import xyz.ecys.vocab.utils.toIsoString

/**
 * Repository for quiz results using Supabase
 * Replaces the Firebase-based QuizResultRepository
 */
class SupabaseQuizResultRepository private constructor(private val context: Context) {
    private val supabase = SupabaseClient.getInstance(context)
    
    // Local storage using SharedPreferences
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "quiz_results_local", Context.MODE_PRIVATE
    )
    private val historyPreferences: SharedPreferences = context.getSharedPreferences(
        "quiz_history_local", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    // Local cache for quiz results to handle permission issues
    private val localQuizResultsCache = ArrayList<SupabaseQuizResult>()
    private val localHistoryCache = ArrayList<SupabaseHistoryItem>()
    
    private val TAG = "SupabaseQuizResultRepo"
    
    companion object {
        private const val KEY_LOCAL_RESULTS = "local_quiz_results"
        private const val KEY_LOCAL_HISTORY = "local_quiz_history"
        private const val MAX_LOCAL_RESULTS = 100
        private const val MAX_LOCAL_HISTORY = 500
        private const val MAX_FIRESTORE_RESULTS = 50
        private const val MAX_FIRESTORE_HISTORY = 200
        
        @Volatile
        private var INSTANCE: SupabaseQuizResultRepository? = null
        
        fun getInstance(context: Context): SupabaseQuizResultRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseQuizResultRepository(context).also { INSTANCE = it }
            }
        }
    }
    
    init {
        // Load the local cache from SharedPreferences
        loadLocalCache()
        loadHistoryCache()
    }
    
    // Load stored results from SharedPreferences
    private fun loadLocalCache() {
        val resultsJson = sharedPreferences.getString(KEY_LOCAL_RESULTS, "[]")
        val type = object : TypeToken<List<SupabaseQuizResult>>() {}.type
        try {
            val storedResults: List<SupabaseQuizResult> = gson.fromJson(resultsJson, type)
            localQuizResultsCache.clear()
            localQuizResultsCache.addAll(storedResults)
            Log.d(TAG, "Loaded ${localQuizResultsCache.size} quiz results from local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local results: ${e.message}")
            // Initialize with empty list if there's an error
            localQuizResultsCache.clear()
        }
    }
    
    // Load stored history from SharedPreferences
    private fun loadHistoryCache() {
        val historyJson = historyPreferences.getString(KEY_LOCAL_HISTORY, "[]")
        val type = object : TypeToken<List<SupabaseHistoryItem>>() {}.type
        try {
            val storedHistory: List<SupabaseHistoryItem> = gson.fromJson(historyJson, type)
            localHistoryCache.clear()
            localHistoryCache.addAll(storedHistory)
            Log.d(TAG, "Loaded ${localHistoryCache.size} history items from local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local history: ${e.message}")
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
            Log.d(TAG, "Saved ${localQuizResultsCache.size} quiz results to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local results: ${e.message}")
        }
    }
    
    // Save the history cache to SharedPreferences
    private fun saveHistoryCache() {
        try {
            val historyJson = gson.toJson(localHistoryCache)
            historyPreferences.edit()
                .putString(KEY_LOCAL_HISTORY, historyJson)
                .apply()
            Log.d(TAG, "Saved ${localHistoryCache.size} history items to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local history: ${e.message}")
        }
    }
    
    // Save a quiz result locally first, then to Supabase if signed in
    suspend fun saveQuizResult(quizResult: QuizResult): String {
        return withContext(Dispatchers.IO) {
            try {
                // Generate a unique local ID
                val localId = "local_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
                
                // Get the current user ID or use a guest ID
                val userId = supabase.currentUser?.id ?: "guest_${getDeviceId()}"
                
                // Convert QuizResult to SupabaseQuizResult
                val supabaseQuizResult = SupabaseQuizResult(
                    id = localId,
                    userId = userId,
                    timestamp = Date().toIsoString(),
                    correctAnswers = quizResult.correctAnswers,
                    totalQuestions = quizResult.totalQuestions,
                    questions = quizResult.questions.map { question ->
                        SupabaseQuizQuestion(
                            word = question.word,
                            correctDefinition = question.correctDefinition,
                            userAnswer = question.userAnswer,
                            isCorrect = question.isCorrect
                        )
                    },
                    score = quizResult.score,
                    durationInSeconds = quizResult.durationInSeconds
                )
                
                // Add to local cache first
                localQuizResultsCache.add(supabaseQuizResult)
                
                // Create history items from this quiz result
                val historyItems = supabaseQuizResult.questions.map { question ->
                    SupabaseHistoryItem(
                        id = "local_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}",
                        userId = userId,
                        timestamp = supabaseQuizResult.timestamp,
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
                
                // Only attempt to sync with Supabase if the user is signed in
                if (isUserSignedIn()) {
                    try {
                        // Save the quiz result to Supabase
                        val result = supabase.db.from("quiz_results")
                            .insert(supabaseQuizResult)
                            .execute()
                        
                        val newId = result.data.firstOrNull()?.let { it["id"] as String } ?: localId
                        Log.d(TAG, "Synced quiz result to Supabase with ID: $newId")
                        
                        // Update the local cache with the Supabase ID
                        val index = localQuizResultsCache.indexOfFirst { it.id == localId }
                        if (index >= 0) {
                            val updatedResult = localQuizResultsCache[index].copy(id = newId)
                            localQuizResultsCache[index] = updatedResult
                            saveLocalCache()
                        }
                        
                        // Save history items to Supabase
                        for (historyItem in historyItems) {
                            val historyResult = supabase.db.from("history")
                                .insert(historyItem)
                                .execute()
                            
                            val newHistoryId = historyResult.data.firstOrNull()?.let { it["id"] as String } ?: historyItem.id
                            Log.d(TAG, "Saved history item to Supabase with ID: $newHistoryId")
                            
                            // Update the local history cache with the Supabase ID
                            val historyIndex = localHistoryCache.indexOfFirst { it.id == historyItem.id }
                            if (historyIndex >= 0) {
                                val updatedHistoryItem = localHistoryCache[historyIndex].copy(id = newHistoryId)
                                localHistoryCache[historyIndex] = updatedHistoryItem
                            }
                        }
                        saveHistoryCache()
                        
                        // Enforce limits in Supabase
                        enforceQuizResultLimit(MAX_FIRESTORE_RESULTS)
                        enforceHistoryLimit(MAX_FIRESTORE_HISTORY)
                        
                        return@withContext newId
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing to Supabase: ${e.message}")
                        return@withContext localId
                    }
                } else {
                    Log.d(TAG, "User not signed in, result saved locally only")
                    return@withContext localId
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in saveQuizResult: ${e.message}")
                e.printStackTrace()
                return@withContext "error_${System.currentTimeMillis()}"
            }
        }
    }
    
    // Method to enforce the limit of quiz results
    private suspend fun enforceQuizResultLimit(maxItems: Int) {
        withContext(Dispatchers.IO) {
            try {
                // Only enforce limit if user is signed in
                if (isUserSignedIn()) {
                    val userId = supabase.currentUser?.id ?: return@withContext
                    
                    // Get all quiz results for the current user, ordered by timestamp (oldest first)
                    val results = supabase.db.from("quiz_results")
                        .select()
                        .eq("user_id", userId)
                        .order("timestamp", ascending = true)
                        .execute()
                    
                    val quizResults = results.data
                    
                    // If we have more than the maximum allowed results
                    if (quizResults.size > maxItems) {
                        // Calculate how many to delete
                        val numberToDelete = quizResults.size - maxItems
                        
                        // Get the IDs of the oldest items
                        val oldestIds = quizResults
                            .take(numberToDelete)
                            .mapNotNull { it["id"] as? String }
                        
                        // Delete each of the oldest items
                        for (id in oldestIds) {
                            supabase.db.from("quiz_results")
                                .delete()
                                .eq("id", id)
                                .execute()
                            
                            Log.d(TAG, "Deleted old quiz result with ID: $id")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enforcing quiz result limit: ${e.message}")
            }
        }
    }
    
    // Method to enforce the limit of history items
    private suspend fun enforceHistoryLimit(maxItems: Int) {
        withContext(Dispatchers.IO) {
            try {
                // Only enforce limit if user is signed in
                if (isUserSignedIn()) {
                    val userId = supabase.currentUser?.id ?: return@withContext
                    
                    // Get all history items for the current user, ordered by timestamp (oldest first)
                    val results = supabase.db.from("history")
                        .select()
                        .eq("user_id", userId)
                        .order("timestamp", ascending = true)
                        .execute()
                    
                    val historyItems = results.data
                    
                    // If we have more than the maximum allowed items
                    if (historyItems.size > maxItems) {
                        // Calculate how many to delete
                        val numberToDelete = historyItems.size - maxItems
                        
                        // Get the IDs of the oldest items
                        val oldestIds = historyItems
                            .take(numberToDelete)
                            .mapNotNull { it["id"] as? String }
                        
                        // Delete each of the oldest items
                        for (id in oldestIds) {
                            supabase.db.from("history")
                                .delete()
                                .eq("id", id)
                                .execute()
                            
                            Log.d(TAG, "Deleted old history item with ID: $id")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enforcing history limit: ${e.message}")
            }
        }
    }
    
    // Get quiz history specifically (focused format for history view)
    suspend fun getQuizHistory(pageSize: Int = MAX_LOCAL_HISTORY): List<QuizHistoryItem> {
        return withContext(Dispatchers.IO) {
            val userId = supabase.currentUser?.id ?: return@withContext localHistoryCache.map { it.toQuizHistoryItem() }
            
            try {
                // If user is signed in, try to sync with Supabase first
                if (isUserSignedIn()) {
                    val results = supabase.db.from("history")
                        .select()
                        .eq("user_id", userId)
                        .order("timestamp", ascending = false)
                        .limit(pageSize)
                        .execute()
                    
                    val historyItems = results.data.mapNotNull { item -> 
                        try {
                            SupabaseHistoryItem(
                                id = item["id"] as String,
                                userId = item["user_id"] as String,
                                timestamp = item["timestamp"] as String,
                                word = item["word"] as String,
                                correctDefinition = item["correct_definition"] as String,
                                userAnswer = item["user_answer"] as String,
                                isCorrect = item["is_correct"] as Boolean
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing history item: ${e.message}")
                            null
                        }
                    }
                    
                    // Update local cache
                    val existingIds = localHistoryCache.map { it.id }.toSet()
                    val newItems = historyItems.filter { !existingIds.contains(it.id) }
                    
                    if (newItems.isNotEmpty()) {
                        localHistoryCache.addAll(newItems)
                        saveHistoryCache()
                    }
                    
                    return@withContext historyItems.map { it.toQuizHistoryItem() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching history from Supabase: ${e.message}")
                // Fall back to local cache
            }
            
            // Return from local cache if we couldn't fetch from Supabase
            return@withContext localHistoryCache
                .filter { it.userId == userId }
                .sortedByDescending { it.timestamp }
                .take(pageSize)
                .map { it.toQuizHistoryItem() }
        }
    }
    
    // Get user's quiz results
    suspend fun getQuizResults(pageSize: Int = MAX_LOCAL_RESULTS): List<QuizResult> {
        return withContext(Dispatchers.IO) {
            val userId = supabase.currentUser?.id ?: return@withContext localQuizResultsCache.map { it.toQuizResult() }
            
            try {
                // If user is signed in, try to sync with Supabase first
                if (isUserSignedIn()) {
                    val results = supabase.db.from("quiz_results")
                        .select()
                        .eq("user_id", userId)
                        .order("timestamp", ascending = false)
                        .limit(pageSize)
                        .execute()
                    
                    val quizResults = results.data.mapNotNull { item ->
                        try {
                            // Parse the questions JSON array
                            val questionsJson = item["questions"] as? Map<*, *> ?: return@mapNotNull null
                            val questions = parseQuestions(questionsJson)
                            
                            SupabaseQuizResult(
                                id = item["id"] as String,
                                userId = item["user_id"] as String,
                                timestamp = item["timestamp"] as String,
                                correctAnswers = (item["correct_answers"] as Number).toInt(),
                                totalQuestions = (item["total_questions"] as Number).toInt(),
                                questions = questions,
                                score = (item["score"] as Number).toFloat(),
                                durationInSeconds = (item["duration_in_seconds"] as Number).toLong()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing quiz result: ${e.message}")
                            null
                        }
                    }
                    
                    // Update local cache
                    val existingIds = localQuizResultsCache.map { it.id }.toSet()
                    val newItems = quizResults.filter { !existingIds.contains(it.id) }
                    
                    if (newItems.isNotEmpty()) {
                        localQuizResultsCache.addAll(newItems)
                        saveLocalCache()
                    }
                    
                    return@withContext quizResults.map { it.toQuizResult() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching quiz results from Supabase: ${e.message}")
                // Fall back to local cache
            }
            
            // Return from local cache if we couldn't fetch from Supabase
            return@withContext localQuizResultsCache
                .filter { it.userId == userId }
                .sortedByDescending { it.timestamp }
                .take(pageSize)
                .map { it.toQuizResult() }
        }
    }
    
    // Convert SupabaseQuizResult to QuizResult
    private fun SupabaseQuizResult.toQuizResult(): QuizResult {
        return QuizResult(
            id = this.id,
            userId = this.userId,
            timestamp = parseIsoDate(this.timestamp) ?: Date(),
            correctAnswers = this.correctAnswers,
            totalQuestions = this.totalQuestions,
            questions = this.questions.map { it.toQuizQuestion() },
            score = this.score,
            durationInSeconds = this.durationInSeconds
        )
    }
    
    // Convert SupabaseQuizQuestion to QuizQuestion
    private fun SupabaseQuizQuestion.toQuizQuestion(): QuizQuestion {
        return QuizQuestion(
            word = this.word,
            correctDefinition = this.correctDefinition,
            userAnswer = this.userAnswer,
            isCorrect = this.isCorrect
        )
    }
    
    // Convert SupabaseHistoryItem to QuizHistoryItem
    private fun SupabaseHistoryItem.toQuizHistoryItem(): QuizHistoryItem {
        return QuizHistoryItem(
            id = this.id,
            userId = this.userId,
            timestamp = parseIsoDate(this.timestamp) ?: Date(),
            word = this.word,
            correctDefinition = this.correctDefinition,
            userAnswer = this.userAnswer,
            isCorrect = this.isCorrect
        )
    }
    
    // Parse questions from JSON
    private fun parseQuestions(questionsJson: Map<*, *>): List<SupabaseQuizQuestion> {
        return try {
            // This is a simplified implementation - you'd need to adapt this based on 
            // how the questions are actually stored in Supabase
            val questionsList = (questionsJson as? List<*>) ?: emptyList<Map<*, *>>()
            
            questionsList.mapNotNull { questionMap ->
                questionMap as? Map<*, *> ?: return@mapNotNull null
                
                SupabaseQuizQuestion(
                    word = questionMap["word"] as? String ?: "",
                    correctDefinition = questionMap["correct_definition"] as? String ?: "",
                    userAnswer = questionMap["user_answer"] as? String ?: "",
                    isCorrect = questionMap["is_correct"] as? Boolean ?: false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing questions JSON: ${e.message}")
            emptyList()
        }
    }
    
    // Helper methods
    private fun isUserSignedIn(): Boolean {
        return supabase.isLoggedIn
    }
    
    private fun getDeviceId(): String {
        // Simple device ID generator - you might want a more robust solution
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
    
    private fun parseIsoDate(dateString: String?): Date? {
        if (dateString == null) return null
        
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(dateString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            null
        }
    }
} 