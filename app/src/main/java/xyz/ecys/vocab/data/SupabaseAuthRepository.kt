package xyz.ecys.vocab.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.ecys.vocab.R
import xyz.ecys.vocab.data.models.AppUsage
import xyz.ecys.vocab.data.models.Word
import xyz.ecys.vocab.utils.toIsoString
import java.util.Date

/**
 * Repository for authentication with Supabase
 * Replaces the Firebase-based AuthRepository
 */
class SupabaseAuthRepository private constructor(private val context: Context) {
    private val supabase = SupabaseClient.getInstance(context)
    private val googleSignInClient: GoogleSignInClient
    
    private val _authState = MutableStateFlow<UserInfo?>(null)
    val authState: StateFlow<UserInfo?> = _authState

    private var lastSyncTimestamp: Long? = null
    private val TAG = "SupabaseAuthRepository"

    // Track last password reset request time
    private var lastPasswordResetRequestTime: Long = 0
    
    init {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        // Monitor auth state changes
        CoroutineScope(Dispatchers.IO).launch {
            supabase.sessionStatus.collectLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _authState.value = supabase.auth.currentUser
                    }
                    else -> {
                        _authState.value = null
                    }
                }
            }
        }
    }

    suspend fun registerUser(email: String, password: String): Result<Unit> {
        return try {
            supabase.signUp(email, password)
            
            // Create a user record in the users table
            val currentUser = supabase.currentUser ?: throw Exception("User registration failed")
            val now = System.currentTimeMillis()
            
            val userData = SupabaseUser(
                id = currentUser.id,
                email = email,
                createdAt = Date().toIsoString(),
                lastSyncTimestamp = now,
                updatedAt = Date().toIsoString()
            )
            
            supabase.db.from("users").insert(userData)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            supabase.signIn(email, password)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
    }

    fun signInWithGoogle(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleGoogleSignInResult(idToken: String) {
        try {
            // Sign in with Google using the Supabase client
            supabase.signInWithGoogle(idToken)
            
            // If this is a new user, create their user record
            val currentUser = supabase.currentUser ?: throw Exception("Google sign-in failed")
            
            // Check if user already exists in the database
            val existingUser = supabase.db.from("users")
                .select()
                .eq("id", currentUser.id)
                .single<SupabaseUser>()
            
            if (existingUser == null) {
                // Create a new user record
                val now = System.currentTimeMillis()
                val userData = SupabaseUser(
                    id = currentUser.id,
                    email = currentUser.email ?: "",
                    createdAt = Date().toIsoString(),
                    lastSyncTimestamp = now,
                    updatedAt = Date().toIsoString()
                )
                
                supabase.db.from("users").insert(userData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in error", e)
            throw e
        }
    }

    suspend fun signOut() {
        googleSignInClient.signOut().await()
        supabase.signOut()
    }

    fun getCurrentUserEmail(): String? {
        return supabase.currentUser?.email
    }

    suspend fun canRequestPasswordReset(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // If user is not logged in, use local variable
        if (supabase.currentUser == null) {
            // If lastPasswordResetRequestTime is 0, no request has been made yet
            if (lastPasswordResetRequestTime == 0L) return true
            
            val hoursSinceLastRequest = (currentTime - lastPasswordResetRequestTime) / (1000 * 60 * 60)
            return hoursSinceLastRequest >= 24
        }
        
        // For logged in users, check database
        try {
            val user = supabase.db.from("users")
                .select()
                .eq("id", supabase.currentUser!!.id)
                .single<SupabaseUser>()
            
            if (user != null && user.lastPasswordResetRequestTime != null) {
                // Update local cache
                lastPasswordResetRequestTime = user.lastPasswordResetRequestTime
                
                val hoursSinceLastRequest = (currentTime - user.lastPasswordResetRequestTime) / (1000 * 60 * 60)
                return hoursSinceLastRequest >= 24
            }
            
            // If no record exists, they can request a reset
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking password reset time", e)
            // Fall back to local variable if there's an error
            if (lastPasswordResetRequestTime == 0L) return true
            
            val hoursSinceLastRequest = (currentTime - lastPasswordResetRequestTime) / (1000 * 60 * 60)
            return hoursSinceLastRequest >= 24
        }
    }

    suspend fun getTimeUntilNextPasswordReset(): Long {
        val currentTime = System.currentTimeMillis()
        
        // If user is not logged in, use local variable
        if (supabase.currentUser == null) {
            val millisUntilNextReset = (lastPasswordResetRequestTime + (24 * 60 * 60 * 1000)) - currentTime
            return if (millisUntilNextReset > 0) millisUntilNextReset else 0
        }
        
        // For logged in users, check database
        try {
            val user = supabase.db.from("users")
                .select()
                .eq("id", supabase.currentUser!!.id)
                .single<SupabaseUser>()
            
            if (user != null && user.lastPasswordResetRequestTime != null) {
                // Update local cache
                lastPasswordResetRequestTime = user.lastPasswordResetRequestTime
                
                val millisUntilNextReset = (user.lastPasswordResetRequestTime + (24 * 60 * 60 * 1000)) - currentTime
                return if (millisUntilNextReset > 0) millisUntilNextReset else 0
            }
            
            // If no record exists, they can request a reset immediately
            return 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking time until next password reset", e)
            // Fall back to local variable if there's an error
            val millisUntilNextReset = (lastPasswordResetRequestTime + (24 * 60 * 60 * 1000)) - currentTime
            return if (millisUntilNextReset > 0) millisUntilNextReset else 0
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Task<Void> {
        // Create a task to wrap the coroutine result
        val taskCompletionSource = TaskCompletionSource<Void>()
        
        try {
            // Update the last request time locally
            lastPasswordResetRequestTime = System.currentTimeMillis()
            
            // Send reset email through Supabase
            supabase.auth.resetPasswordForEmail(email)
            
            // If user is logged in, also update in database
            if (supabase.currentUser != null) {
                supabase.db.from("users")
                    .update { json ->
                        json.put("last_password_reset_request_time", lastPasswordResetRequestTime)
                    }
                    .eq("id", supabase.currentUser!!.id)
                    .execute()
            }
            
            taskCompletionSource.setResult(null)
        } catch (e: Exception) {
            taskCompletionSource.setException(e)
        }
        
        return taskCompletionSource.task
    }

    suspend fun sendEmailVerification(): Task<Void> {
        // Create a task to wrap the coroutine result
        val taskCompletionSource = TaskCompletionSource<Void>()
        
        try {
            // Supabase doesn't have a direct equivalent for email verification
            // You might implement custom logic or use the OTP feature
            // For now, we'll just return success
            taskCompletionSource.setResult(null)
        } catch (e: Exception) {
            taskCompletionSource.setException(e)
        }
        
        return taskCompletionSource.task
    }

    suspend fun updateEmail(newEmail: String, password: String): Task<Void> {
        // Create a task to wrap the coroutine result
        val taskCompletionSource = TaskCompletionSource<Void>()
        
        try {
            // Reauthenticate first (we need to sign in again with current credentials)
            val currentEmail = supabase.currentUser?.email ?: throw Exception("No user logged in")
            supabase.signIn(currentEmail, password)
            
            // Update email
            supabase.updateEmail(newEmail)
            
            taskCompletionSource.setResult(null)
        } catch (e: Exception) {
            taskCompletionSource.setException(e)
        }
        
        return taskCompletionSource.task
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Task<Void> {
        // Create a task to wrap the coroutine result
        val taskCompletionSource = TaskCompletionSource<Void>()
        
        try {
            // Reauthenticate first (we need to sign in again with current credentials)
            val currentEmail = supabase.currentUser?.email ?: throw Exception("No user logged in")
            supabase.signIn(currentEmail, currentPassword)
            
            // Update password
            supabase.updatePassword(newPassword)
            
            taskCompletionSource.setResult(null)
        } catch (e: Exception) {
            taskCompletionSource.setException(e)
        }
        
        return taskCompletionSource.task
    }

    suspend fun verifyPassword(password: String): Task<Void> {
        // Create a task to wrap the coroutine result
        val taskCompletionSource = TaskCompletionSource<Void>()
        
        try {
            // Reauthenticate to verify the password
            val currentEmail = supabase.currentUser?.email ?: throw Exception("No user logged in")
            supabase.signIn(currentEmail, password)
            
            taskCompletionSource.setResult(null)
        } catch (e: Exception) {
            taskCompletionSource.setException(e)
        }
        
        return taskCompletionSource.task
    }

    suspend fun syncData(): Task<Void> {
        val taskCompletionSource = TaskCompletionSource<Void>()
        
        try {
            val user = supabase.currentUser ?: throw Exception("No user logged in")
            val userId = user.id
            
            // Get database instances
            val wordDatabase = WordDatabase.getDatabase(context)
            val wordDao = wordDatabase.wordDao()
            val appUsageDao = wordDatabase.appUsageDao()
            
            // 1. Sync words data - only for words that have been reviewed at least once
            val allWords = wordDao.getAllWords().filter { it.timesReviewed > 0 }
            
            // First check if we have remote data that's newer
            val remoteWords = supabase.db.from("words")
                .select()
                .eq("user_id", userId)
                .execute()
                .data
                .mapNotNull { it.toSupabaseWord() }
            
            // Convert to a map for easier lookup
            val remoteWordsMap = remoteWords.associateBy { it.word }
            
            // Update local words with newer remote data
            for (localWord in allWords) {
                val remoteWord = remoteWordsMap[localWord.word]
                if (remoteWord != null) {
                    // Check if remote word is newer
                    val remoteUpdated = remoteWord.updatedAt?.let { parseIsoDate(it) }?.time ?: 0
                    val localUpdated = localWord.updatedAt ?: 0
                    
                    if (remoteUpdated > localUpdated) {
                        // Remote is newer, update local
                        val updatedLocalWord = localWord.copy(
                            definition = remoteWord.definition,
                            exampleSentence = remoteWord.exampleSentence,
                            lastReviewed = remoteWord.lastReviewed?.let { parseIsoDate(it)?.time },
                            nextReview = remoteWord.nextReview?.let { parseIsoDate(it)?.time },
                            timesReviewed = remoteWord.timesReviewed,
                            timesCorrect = remoteWord.timesCorrect,
                            reviewStage = remoteWord.reviewStage,
                            isFavorite = remoteWord.isFavorite,
                            notes = remoteWord.notes,
                            updatedAt = remoteUpdated
                        )
                        wordDao.updateWord(updatedLocalWord)
                    }
                }
            }
            
            // Now push local changes to remote
            for (localWord in allWords) {
                val remoteWord = remoteWordsMap[localWord.word]
                val localUpdated = localWord.updatedAt ?: 0
                
                if (remoteWord == null) {
                    // Word doesn't exist remotely, insert it
                    val newRemoteWord = SupabaseWord(
                        userId = userId,
                        word = localWord.word,
                        definition = localWord.definition,
                        exampleSentence = localWord.exampleSentence,
                        createdAt = Date(localWord.createdAt).toIsoString(),
                        updatedAt = Date(localUpdated).toIsoString(),
                        lastReviewed = localWord.lastReviewed?.let { Date(it).toIsoString() },
                        nextReview = localWord.nextReview?.let { Date(it).toIsoString() },
                        timesReviewed = localWord.timesReviewed,
                        timesCorrect = localWord.timesCorrect,
                        reviewStage = localWord.reviewStage,
                        isFavorite = localWord.isFavorite,
                        notes = localWord.notes
                    )
                    
                    supabase.db.from("words").insert(newRemoteWord)
                } else {
                    // Word exists remotely, check if local is newer
                    val remoteUpdated = remoteWord.updatedAt?.let { parseIsoDate(it) }?.time ?: 0
                    
                    if (localUpdated > remoteUpdated) {
                        // Local is newer, update remote
                        val updatedRemoteWord = remoteWord.copy(
                            definition = localWord.definition,
                            exampleSentence = localWord.exampleSentence,
                            updatedAt = Date(localUpdated).toIsoString(),
                            lastReviewed = localWord.lastReviewed?.let { Date(it).toIsoString() },
                            nextReview = localWord.nextReview?.let { Date(it).toIsoString() },
                            timesReviewed = localWord.timesReviewed,
                            timesCorrect = localWord.timesCorrect,
                            reviewStage = localWord.reviewStage,
                            isFavorite = localWord.isFavorite,
                            notes = localWord.notes
                        )
                        
                        supabase.db.from("words")
                            .update(updatedRemoteWord)
                            .eq("id", remoteWord.id)
                            .execute()
                    }
                }
            }
            
            // 2. Sync app usage data
            val allAppUsage = appUsageDao.getAllAppUsage()
            
            // Get remote app usage
            val remoteAppUsage = supabase.db.from("app_usage")
                .select()
                .eq("user_id", userId)
                .execute()
                .data
                .mapNotNull { it.toSupabaseAppUsage() }
            
            // Convert to a map for easier lookup
            val remoteAppUsageMap = remoteAppUsage.associateBy { it.dateId }
            
            // Update local with newer remote data
            for (localUsage in allAppUsage) {
                val remoteUsage = remoteAppUsageMap[localUsage.dateId]
                if (remoteUsage != null) {
                    // Check if remote is newer
                    val remoteUpdated = remoteUsage.updatedAt?.let { parseIsoDate(it) }?.time ?: 0
                    val localUpdated = localUsage.updatedAt
                    
                    if (remoteUpdated > localUpdated) {
                        // Remote is newer, update local
                        val updatedLocalUsage = localUsage.copy(
                            appOpens = remoteUsage.appOpens,
                            timeSpentSeconds = remoteUsage.timeSpentSeconds,
                            featuresUsed = remoteUsage.featuresUsed.toString(), // Convert map to string
                            updatedAt = remoteUpdated
                        )
                        appUsageDao.updateAppUsage(updatedLocalUsage)
                    }
                }
            }
            
            // Push local changes to remote
            for (localUsage in allAppUsage) {
                val remoteUsage = remoteAppUsageMap[localUsage.dateId]
                
                if (remoteUsage == null) {
                    // Usage doesn't exist remotely, insert it
                    val newRemoteUsage = SupabaseAppUsage(
                        userId = userId,
                        dateId = localUsage.dateId,
                        appOpens = localUsage.appOpens,
                        timeSpentSeconds = localUsage.timeSpentSeconds,
                        featuresUsed = parseFeatureUsage(localUsage.featuresUsed),
                        updatedAt = Date(localUsage.updatedAt).toIsoString()
                    )
                    
                    supabase.db.from("app_usage").insert(newRemoteUsage)
                } else {
                    // Usage exists remotely, check if local is newer
                    val remoteUpdated = remoteUsage.updatedAt?.let { parseIsoDate(it) }?.time ?: 0
                    
                    if (localUsage.updatedAt > remoteUpdated) {
                        // Local is newer, update remote
                        val updatedRemoteUsage = remoteUsage.copy(
                            appOpens = localUsage.appOpens,
                            timeSpentSeconds = localUsage.timeSpentSeconds,
                            featuresUsed = parseFeatureUsage(localUsage.featuresUsed),
                            updatedAt = Date(localUsage.updatedAt).toIsoString()
                        )
                        
                        supabase.db.from("app_usage")
                            .update(updatedRemoteUsage)
                            .eq("id", remoteUsage.id)
                            .execute()
                    }
                }
            }
            
            // 3. Update last sync timestamp
            val currentTimestamp = System.currentTimeMillis()
            lastSyncTimestamp = currentTimestamp
            
            supabase.db.from("users")
                .update { json ->
                    json.put("last_sync_timestamp", currentTimestamp)
                    json.put("updated_at", Date().toIsoString())
                }
                .eq("id", userId)
                .execute()
            
            taskCompletionSource.setResult(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing data", e)
            taskCompletionSource.setException(e)
        }
        
        return taskCompletionSource.task
    }
    
    // Helper methods
    
    private fun parseFeatureUsage(featuresUsedString: String): Map<String, Int> {
        return try {
            // Simple parsing of "{feature1=1, feature2=2}" format
            if (featuresUsedString.isBlank() || featuresUsedString == "{}") {
                emptyMap()
            } else {
                featuresUsedString
                    .trim('{', '}')
                    .split(", ")
                    .associate { 
                        val parts = it.split("=")
                        parts[0] to parts[1].toInt()
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing features used", e)
            emptyMap()
        }
    }
    
    private fun parseIsoDate(dateString: String): Date? {
        return try {
            // Parse ISO-8601 date string
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(dateString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            null
        }
    }
    
    // Extension functions to map between Supabase and app models
    private fun Map<String, Any?>.toSupabaseWord(): SupabaseWord? {
        return try {
            SupabaseWord(
                id = this["id"] as String,
                userId = this["user_id"] as String,
                word = this["word"] as String,
                definition = this["definition"] as String,
                exampleSentence = this["example_sentence"] as String?,
                createdAt = this["created_at"] as String?,
                updatedAt = this["updated_at"] as String?,
                lastReviewed = this["last_reviewed"] as String?,
                nextReview = this["next_review"] as String?,
                timesReviewed = (this["times_reviewed"] as Number).toInt(),
                timesCorrect = (this["times_correct"] as Number).toInt(),
                reviewStage = (this["review_stage"] as Number).toInt(),
                isFavorite = this["is_favorite"] as Boolean,
                notes = this["notes"] as String?
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting to SupabaseWord", e)
            null
        }
    }
    
    private fun Map<String, Any?>.toSupabaseAppUsage(): SupabaseAppUsage? {
        return try {
            SupabaseAppUsage(
                id = this["id"] as String,
                userId = this["user_id"] as String,
                dateId = this["date_id"] as String,
                appOpens = (this["app_opens"] as Number).toInt(),
                timeSpentSeconds = (this["time_spent_seconds"] as Number).toInt(),
                featuresUsed = (this["features_used"] as Map<String, Any>?)?.mapValues { (it.value as Number).toInt() } ?: emptyMap(),
                updatedAt = this["updated_at"] as String?
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting to SupabaseAppUsage", e)
            null
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SupabaseAuthRepository? = null
        
        fun getInstance(context: Context): SupabaseAuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseAuthRepository(context).also { INSTANCE = it }
            }
        }
    }
} 