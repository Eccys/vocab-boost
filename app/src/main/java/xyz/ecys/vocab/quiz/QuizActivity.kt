package xyz.ecys.vocab.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.*
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.quiz.components.QuizContent
import xyz.ecys.vocab.data.SettingsManager

class QuizActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var correctAnswerTracker: CorrectAnswerTracker
    private lateinit var settingsManager: SettingsManager
    private var isBookmarkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)
        appUsageManager = AppUsageManager.getInstance(this)
        correctAnswerTracker = CorrectAnswerTracker.getInstance(this)
        settingsManager = SettingsManager.getInstance(this)
        isBookmarkMode = intent.getStringExtra("mode") == "bookmarks"

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
                    onFinish = { finish() },
                    settingsManager = settingsManager
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