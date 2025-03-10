package xyz.ecys.vocab.quiz

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.*
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.quiz.components.QuizContent
import xyz.ecys.vocab.data.SettingsManager
import xyz.ecys.vocab.data.QuizResultRepository
import xyz.ecys.vocab.data.GlobalQuizState
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher

class QuizActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var correctAnswerTracker: CorrectAnswerTracker
    private lateinit var settingsManager: SettingsManager
    private lateinit var quizResultRepository: QuizResultRepository
    private var isBookmarkMode = false
    
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
        isBookmarkMode = intent.getBooleanExtra("bookmarkMode", false)
        
        // We no longer need to log or track lives and hints
        // since the quiz is now endless and hints are unlimited

        // Handle back button press to ensure state is saved
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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
        lifecycleScope.launch {
            appUsageManager.endSession()
        }
    }

    override fun onResume() {
        super.onResume()
        appUsageManager.startQuizSession()
    }
} 