package xyz.ecys.vocab

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.AuthRepository
import xyz.ecys.vocab.data.GlobalQuizState
import xyz.ecys.vocab.data.SyncManager
import androidx.work.Configuration

class VocabApplication : Application(), Configuration.Provider {
    private val TAG = "VocabApplication"
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure Firestore for offline persistence
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
        
        // Initialize repositories/managers
        AuthRepository.initialize(this)
        GlobalQuizState.initialize(this)
        
        // Get the SyncManager instance to initialize it
        val syncManager = SyncManager.getInstance(this)
        
        // Start the initial data load from Firestore
        // This happens once at app startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting initial data load from cloud")
                syncManager.initialLoadFromCloud()
            } catch (e: Exception) {
                Log.e(TAG, "Error during initial data load", e)
            }
        }
    }
} 