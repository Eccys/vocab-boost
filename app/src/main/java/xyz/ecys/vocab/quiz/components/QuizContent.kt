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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuizContent(
    wordRepository: WordRepository,
    appUsageManager: AppUsageManager,
    isBookmarkMode: Boolean,
    onFinish: () -> Unit
) {
    val currentWord = remember { mutableStateOf<Word?>(null) }
    val lives = remember { mutableStateOf(3) }
    val coroutineScope = rememberCoroutineScope()

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
                lives = lives.value
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
            }
        )
    }
} 