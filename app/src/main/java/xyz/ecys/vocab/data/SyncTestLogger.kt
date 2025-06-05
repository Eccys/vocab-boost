package xyz.ecys.vocab.data

import android.util.Log
import android.widget.Toast
import android.content.Context

/**
 * Test utility to verify Firebase/Firestore optimization is working correctly.
 * Logs Firebase/Firestore operations and can be used to verify the optimization.
 */
object SyncTestLogger {
    private val TAG = "SyncTestLog"
    private val operations = mutableListOf<String>()
    private var firestoreReadCount = 0
    private var firestoreWriteCount = 0
    private var localReadCount = 0
    private var localWriteCount = 0
    private var initialLoadComplete = false
    
    // New counters for tracking validation failures
    private var unauthorizedReadAttempts = 0
    private var unauthorizedWriteAttempts = 0

    fun reset() {
        operations.clear()
        firestoreReadCount = 0
        firestoreWriteCount = 0
        localReadCount = 0
        localWriteCount = 0
        initialLoadComplete = false
        unauthorizedReadAttempts = 0
        unauthorizedWriteAttempts = 0
        Log.d(TAG, "SyncTestLogger reset")
    }
    
    fun logInitialLoad() {
        val timestamp = System.currentTimeMillis()
        operations.add("[$timestamp] INITIAL_LOAD from Firestore")
        firestoreReadCount++
        initialLoadComplete = true
        Log.d(TAG, "Initial load from Firestore recorded")
    }
    
    fun logLocalSave() {
        val timestamp = System.currentTimeMillis()
        operations.add("[$timestamp] LOCAL_SAVE")
        localWriteCount++
        Log.d(TAG, "Local save recorded")
    }
    
    fun logLocalRead() {
        val timestamp = System.currentTimeMillis()
        operations.add("[$timestamp] LOCAL_READ")
        localReadCount++
        Log.d(TAG, "Local read recorded")
    }
    
    fun logFirestoreSync() {
        val timestamp = System.currentTimeMillis()
        operations.add("[$timestamp] FIRESTORE_SYNC")
        firestoreWriteCount++
        Log.d(TAG, "Firestore sync recorded")
    }
    
    fun logFirestoreRead() {
        val timestamp = System.currentTimeMillis()
        operations.add("[$timestamp] FIRESTORE_READ")
        firestoreReadCount++
        Log.d(TAG, "Firestore read recorded")
    }
    
    fun logUnauthorizedReadAttempt(source: String, reason: String) {
        val timestamp = System.currentTimeMillis()
        operations.add("[$timestamp] ⚠️ UNAUTHORIZED_READ from $source: $reason")
        unauthorizedReadAttempts++
        Log.w(TAG, "Unauthorized Firestore read attempt from $source: $reason")
    }
    
    fun logUnauthorizedWriteAttempt(source: String, reason: String) {
        val timestamp = System.currentTimeMillis()
        operations.add("[$timestamp] ⚠️ UNAUTHORIZED_WRITE from $source: $reason")
        unauthorizedWriteAttempts++
        Log.w(TAG, "Unauthorized Firestore write attempt from $source: $reason")
    }
    
    fun getOperationsLog(): String {
        return operations.joinToString("\n")
    }
    
    fun getStats(): String {
        return """
            Firebase/Firestore Test Stats:
            - Initial Load Complete: $initialLoadComplete
            - Firestore Reads: $firestoreReadCount
            - Firestore Writes: $firestoreWriteCount
            - Local Reads: $localReadCount
            - Local Writes: $localWriteCount
            - Unauthorized Read Attempts: $unauthorizedReadAttempts
            - Unauthorized Write Attempts: $unauthorizedWriteAttempts
            
            ${getOperationsLog()}
        """.trimIndent()
    }
    
    fun showStats(context: Context) {
        Toast.makeText(context, getStats(), Toast.LENGTH_LONG).show()
        Log.i(TAG, getStats())
    }
} 