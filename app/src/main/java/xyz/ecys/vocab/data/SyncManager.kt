package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

/**
 * FirestoreAccessMonitor tracks and restricts Firestore operations to ensure they only happen
 * at approved times according to our optimization strategy.
 */
object FirestoreAccessMonitor {
    private val TAG = "FirestoreMonitor"
    private var enabled = true
    private var initialLoadComplete = false
    
    // When set to true, all operations will be allowed (for admin purposes only)
    var bypassRestrictions = false
    
    fun markInitialLoadComplete() {
        initialLoadComplete = true
        Log.d(TAG, "Initial Firestore load marked as complete")
    }
    
    fun validateRead(source: String, reason: String): Boolean {
        if (bypassRestrictions) return true
        if (!initialLoadComplete) {
            // Allow reads during initial app startup
            Log.d(TAG, "Allowing Firestore read during initial load from: $source, reason: $reason")
            return true
        }
        
        // Log unauthorized read attempt
        Log.w(TAG, "⚠️ UNAUTHORIZED FIRESTORE READ from: $source, reason: $reason")
        SyncTestLogger.logUnauthorizedReadAttempt(source, reason)
        
        if (enabled) {
            // For debugging purposes, log the stack trace to identify the caller
            Exception("Trace for unauthorized Firestore read").printStackTrace()
            return false
        }
        return true
    }
    
    fun validateWrite(source: String, reason: String, isExplicitSync: Boolean = false): Boolean {
        if (bypassRestrictions) return true
        if (isExplicitSync) {
            // Allow writes during explicit sync (app closing or threshold met)
            Log.d(TAG, "Allowing Firestore write during explicit sync from: $source, reason: $reason")
            return true
        }
        
        // Log unauthorized write attempt
        Log.w(TAG, "⚠️ UNAUTHORIZED FIRESTORE WRITE from: $source, reason: $reason")
        SyncTestLogger.logUnauthorizedWriteAttempt(source, reason)
        
        if (enabled) {
            // For debugging purposes, log the stack trace to identify the caller
            Exception("Trace for unauthorized Firestore write").printStackTrace()
            return false
        }
        return true
    }
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        Log.d(TAG, "FirestoreAccessMonitor ${if (enabled) "enabled" else "disabled"}")
    }
}

/**
 * SyncManager handles batched synchronization with Firestore to reduce read/write operations
 */
class SyncManager private constructor(private val context: Context) {
    private val TAG = "SyncManager"
    
    // Repository references
    private val authRepository: AuthRepository = AuthRepository.getInstance()
    private lateinit var quizResultRepository: QuizResultRepository
    
    // Shared preferences for sync state
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    
    // State tracking
    private var lastSyncTime: Long = 0
    private var questionsSinceLastSync: Int = 0
    private var isSyncing: Boolean = false
    
    init {
        // Load last sync time from preferences
        lastSyncTime = preferences.getLong(KEY_LAST_SYNC_TIME, 0)
        questionsSinceLastSync = preferences.getInt(KEY_QUESTIONS_SINCE_SYNC, 0)
        
        // Initialize repositories
        try {
            quizResultRepository = QuizResultRepository.getInstance(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing repositories", e)
        }
        
        Log.d(TAG, "SyncManager initialized. Last sync: $lastSyncTime, " +
                "Questions since sync: $questionsSinceLastSync")
    }
    
    /**
     * Call when the app starts to load all data from Firestore once
     */
    suspend fun initialLoadFromCloud(): Boolean {
        if (!isUserSignedIn()) {
            Log.d(TAG, "User not signed in, skipping initial load")
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting initial data load from cloud")
                // Log test event
                SyncTestLogger.logInitialLoad()
                
                val downloadTask = authRepository.downloadDataFromCloud()
                
                // Wait for completion
                val result = try {
                    downloadTask.await()
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading data", e)
                    false
                }
                
                // Update last sync time if successful
                if (result) {
                    lastSyncTime = System.currentTimeMillis()
                    savePreferences()
                    Log.d(TAG, "Initial data load complete")
                    
                    // Mark initial load as complete in monitor
                    FirestoreAccessMonitor.markInitialLoadComplete()
                }
                
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error in initial data load", e)
                false
            }
        }
    }
    
    /**
     * Record that a quiz question was answered (for tracking sync conditions)
     */
    fun recordQuestionAnswered() {
        questionsSinceLastSync++
        savePreferences()
        
        // Check if we should sync
        checkIfSyncNeeded()
    }
    
    /**
     * Force a sync to Firebase now (suspending version)
     */
    suspend fun performSync(forceSync: Boolean = false): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isSyncing) {
                    Log.d(TAG, "Sync already in progress, ignoring request")
                    return@withContext false
                }
                
                if (!isUserSignedIn()) {
                    Log.d(TAG, "User not signed in, skipping sync")
                    return@withContext false
                }
                
                isSyncing = true
                Log.d(TAG, "Starting data sync to cloud (forced=${forceSync})")
                
                // First, sync quiz results from the local cache to Firestore
                try {
                    // Log test event
                    SyncTestLogger.logFirestoreSync()
                    Log.d(TAG, "Syncing cached quiz results to Firestore")
                    
                    // First count how many results are pending sync
                    val localItems = quizResultRepository.getLocalItemsPendingSync()
                    Log.d(TAG, "Found ${localItems.quizResults} quiz results and ${localItems.historyItems} " +
                           "history items pending sync")
                    
                    // Now sync them
                    val quizSyncResult = quizResultRepository.syncCachedResultsToFirestore(isExplicitSync = true)
                    Log.d(TAG, "Quiz results sync ${if (quizSyncResult) "successful" else "failed"}")
                    
                    // Check if there are still items pending sync after the sync attempt
                    val remainingItems = quizResultRepository.getLocalItemsPendingSync()
                    Log.d(TAG, "After sync: ${remainingItems.quizResults} quiz results and " +
                           "${remainingItems.historyItems} history items still pending sync")
                    
                    if (remainingItems.quizResults > 0 || remainingItems.historyItems > 0) {
                        Log.w(TAG, "⚠️ Some items failed to sync! This may indicate a Firestore permissions issue.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing cached quiz results", e)
                    // Continue with the rest of the sync even if this part fails
                }
                
                // Then perform the regular sync from AuthRepository
                Log.d(TAG, "Starting data sync through AuthRepository")
                val syncTask = authRepository.syncData()
                
                // Wait for completion
                val result = try {
                    syncTask.await()
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing data through AuthRepository", e)
                    false
                }
                
                // Reset counters and update sync time if successful
                if (result) {
                    lastSyncTime = System.currentTimeMillis()
                    questionsSinceLastSync = 0
                    savePreferences()
                    Log.d(TAG, "Data sync complete - updated last sync time and reset counters")
                }
                
                isSyncing = false
                return@withContext result
            } catch (e: Exception) {
                Log.e(TAG, "Error in syncNow", e)
                isSyncing = false
                return@withContext false
            }
        }
    }
    
    /**
     * Non-suspending version of syncNow for Java/callback compatibility
     */
    fun syncNow(onComplete: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = performSync(false)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result)
            }
        }
    }
    
    /**
     * Force sync now regardless of criteria (for use when app/activity is closing)
     */
    fun forceSyncNow(onComplete: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = performSync(true)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result)
            }
        }
    }
    
    /**
     * Check if we should sync based on our criteria
     * (2+ questions and 2+ minutes since last sync)
     */
    private fun checkIfSyncNeeded() {
        if (isSyncing) return
        if (!isUserSignedIn()) return
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSync = currentTime - lastSyncTime
        
        // Sync if 2+ questions answered AND 2+ minutes since last sync
        if (questionsSinceLastSync >= MIN_QUESTIONS_FOR_SYNC && 
            timeSinceLastSync >= MIN_TIME_BETWEEN_SYNCS) {
            Log.d(TAG, "Sync criteria met: $questionsSinceLastSync questions, " +
                    "${timeSinceLastSync / 1000 / 60} minutes since last sync")
            
            // Launch a coroutine to perform the sync
            CoroutineScope(Dispatchers.IO).launch {
                performSync()
            }
        }
    }
    
    private fun savePreferences() {
        preferences.edit()
            .putLong(KEY_LAST_SYNC_TIME, lastSyncTime)
            .putInt(KEY_QUESTIONS_SINCE_SYNC, questionsSinceLastSync)
            .apply()
    }
    
    private fun isUserSignedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }
    
    companion object {
        private const val PREF_NAME = "sync_manager_prefs"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_QUESTIONS_SINCE_SYNC = "questions_since_sync"
        
        // Sync criteria constants
        private const val MIN_QUESTIONS_FOR_SYNC = 2
        private const val MIN_TIME_BETWEEN_SYNCS = 2 * 60 * 1000 // 2 minutes in milliseconds
        
        @Volatile
        private var INSTANCE: SyncManager? = null
        
        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
} 