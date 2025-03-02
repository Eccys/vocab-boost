package xyz.ecys.vocab.quiz.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.data.AppUsageManager
import xyz.ecys.vocab.data.QuizResult
import xyz.ecys.vocab.quiz.QuizScreen
import xyz.ecys.vocab.quiz.QuizTopBar
import xyz.ecys.vocab.quiz.generateOptions
import xyz.ecys.vocab.data.SettingsManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuizContent(
    wordRepository: WordRepository,
    appUsageManager: AppUsageManager,
    isBookmarkMode: Boolean,
    onFinish: () -> Unit,
    settingsManager: SettingsManager
) {
    val currentWord = remember { mutableStateOf<Word?>(null) }
    val lives = remember { mutableStateOf(3) }
    val coroutineScope = rememberCoroutineScope()
    
    // Add hint-related state variables
    val hintsRemaining = remember { mutableStateOf(3) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var hintUsedForCurrentQuestion by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            QuizTopBar(
                onBackClick = { 
                    coroutineScope.launch {
                        appUsageManager.endSession()
                    }
                    onFinish() 
                },
                currentWord = currentWord.value,
                onBookmarkClick = { word ->
                    coroutineScope.launch {
                        wordRepository.updateBookmark(word.id, !word.isBookmarked)
                        currentWord.value = word.copy(isBookmarked = !word.isBookmarked)
                    }
                },
                lives = lives.value,
                hintsRemaining = hintsRemaining.value,
                hintUsedForCurrentQuestion = hintUsedForCurrentQuestion,
                selectedAnswer = selectedAnswer,
                onHintClick = {
                    // Only allow hint if:
                    // 1. Hints are remaining
                    // 2. We have a current word
                    // 3. Hint hasn't been used for this question
                    // 4. User hasn't answered yet
                    if (hintsRemaining.value > 0 && 
                        currentWord.value != null && 
                        !hintUsedForCurrentQuestion &&
                        selectedAnswer == null) {
                        hintsRemaining.value--
                        hintUsedForCurrentQuestion = true
                    }
                }
            )
        }
    ) { innerPadding ->
        QuizScreen(
            modifier = Modifier.padding(innerPadding),
            wordRepository = wordRepository,
            appUsageManager = appUsageManager,
            isBookmarkMode = isBookmarkMode,
            currentWord = currentWord,
            lives = lives,
            onGameOver = {
                coroutineScope.launch {
                    appUsageManager.endSession()
                }
                onFinish()
            },
            settingsManager = settingsManager
        )
    }
} 