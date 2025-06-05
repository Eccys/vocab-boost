package xyz.ecys.vocab.quiz

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.ecys.vocab.data.*
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.quiz.components.QuizContent
import xyz.ecys.vocab.data.SettingsManager
import xyz.ecys.vocab.data.QuizResultRepository
import xyz.ecys.vocab.data.GlobalQuizState
import xyz.ecys.vocab.data.SyncManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher

class QuizActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var correctAnswerTracker: CorrectAnswerTracker
    private lateinit var settingsManager: SettingsManager
    private lateinit var quizResultRepository: QuizResultRepository
    private lateinit var syncManager: SyncManager
    private var isBookmarkMode = false
    private var isSyncing = false
    
    companion object {
        private const val TAG = "QuizActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)
        appUsageManager = AppUsageManager.getInstance(this)
        correctAnswerTracker = CorrectAnswerTracker.getInstance(this)
        settingsManager = SettingsManager.getInstance(this)
        quizResultRepository = QuizResultRepository.getInstance(this)
        syncManager = SyncManager.getInstance(this)
        isBookmarkMode = intent.getBooleanExtra("bookmarkMode", false)
        
        // We no longer need to log or track lives and hints
        // since the quiz is now endless and hints are unlimited

        // Handle back button press to ensure state is saved and sync is triggered
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent multiple syncs
                if (isSyncing) {
                    Log.d(TAG, "Sync already in progress, ignoring back press")
                    return
                }
                
                isSyncing = true
                
                // This MUST be runBlocking to ensure sync completes before activity finishes
                runBlocking {
                    Log.d(TAG, "Back pressed - syncing data BEFORE finishing activity")
                    appUsageManager.endSession()
                    
                    try {
                        // Use suspending version directly in runBlocking context
                        val syncResult = syncManager.performSync(true)
                        Log.d(TAG, "Sync completed with result: $syncResult")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during sync on back pressed", e)
                    }
                }
                
                // Only finish after sync has completed
                finish()
            }
        })

        appUsageManager.startQuizSession()

        lifecycleScope.launch {
            if (wordRepository.getAllWords().isEmpty()) {
                wordRepository.insertInitialWords()
            }
        }

        setContent {
            VocabularyBoosterTheme {
                QuizContent(
                    wordRepository = wordRepository,
                    appUsageManager = appUsageManager,
                    isBookmarkMode = isBookmarkMode,
                    onFinish = { quizState -> 
                        // Prevent multiple syncs
                        if (isSyncing) {
                            Log.d(TAG, "Sync already in progress, ignoring finish request")
                            return@QuizContent
                        }
                        
                        isSyncing = true
                        
                        // Must be runBlocking to ensure sync completes before activity finishes
                        runBlocking {
                            Log.d(TAG, "Quiz finished - syncing data BEFORE finishing activity")
                            appUsageManager.endSession()
                            
                            try {
                                // Use suspending version directly in runBlocking context
                                val syncResult = syncManager.performSync(true)
                                Log.d(TAG, "Sync completed with result: $syncResult")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error during sync on quiz finish", e)
                            }
                        }
                        
                        // Only finish after sync has completed
                        finish()
                    },
                    settingsManager = settingsManager,
                    quizResultRepository = quizResultRepository
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        
        // If finishing, make sure to sync synchronously before the activity is destroyed
        if (isFinishing && !isSyncing) {
            isSyncing = true
            
            // Use runBlocking to ensure sync completes before the activity is destroyed
            runBlocking {
                Log.d(TAG, "Activity finishing - syncing data in onPause SYNCHRONOUSLY")
                appUsageManager.endSession()
                
                try {
                    // Use suspending version directly in runBlocking context
                    val syncResult = syncManager.performSync(true)
                    Log.d(TAG, "Sync completed with result: $syncResult")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during sync on pause", e)
                }
            }
        } else if (!isFinishing) {
            // Just end the session if not finishing
            lifecycleScope.launch {
                Log.d(TAG, "Activity paused but not finishing - just ending session")
                appUsageManager.endSession()
            }
        }
    }

    override fun onDestroy() {
        // Final attempt to ensure sync happens when activity is destroyed
        if (!isSyncing && isFinishing) {
            isSyncing = true
            
            try {
                // Use runBlocking as a last resort to ensure sync completes
                runBlocking {
                    Log.d(TAG, "Last resort sync in onDestroy - SYNCHRONOUSLY")
                    val syncResult = syncManager.performSync(true)
                    Log.d(TAG, "Final sync completed with result: $syncResult")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during final sync in onDestroy", e)
            }
        }
        
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        appUsageManager.startQuizSession()
    }
} 