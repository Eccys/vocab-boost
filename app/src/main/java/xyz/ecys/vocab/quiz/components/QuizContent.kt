package xyz.ecys.vocab.quiz.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.data.AppUsageManager
import xyz.ecys.vocab.data.QuizResult
import xyz.ecys.vocab.quiz.QuizScreen
import xyz.ecys.vocab.quiz.QuizTopBar
import xyz.ecys.vocab.quiz.generateOptions
import xyz.ecys.vocab.data.SettingsManager
import xyz.ecys.vocab.data.QuizResultRepository
import xyz.ecys.vocab.data.QuizStateManager
import xyz.ecys.vocab.data.GlobalQuizState
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.border

// Class to pass back state to the activity
data class QuizState(
    val timestamp: Long = System.currentTimeMillis() // Add a timestamp as we need at least one parameter
)

// Global hint state moved to HintManager.kt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuizContent(
    wordRepository: WordRepository,
    appUsageManager: AppUsageManager,
    isBookmarkMode: Boolean,
    onFinish: (QuizState) -> Unit,
    settingsManager: SettingsManager,
    quizResultRepository: QuizResultRepository
) {
    val currentWord = remember { mutableStateOf<Word?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var hintUsedForCurrentQuestion by remember { mutableStateOf(false) }
    
    // Track direct hint usage with a separate flag for the database update
    var directHintUsedForUpdate by remember { mutableStateOf(false) }
    
    // Add debug UI state
    var showDebugPanel = remember { mutableStateOf(false) }

    // Monitor state changes
    LaunchedEffect(selectedAnswer) {
        android.util.Log.d("StateTracker", "QuizContent: selectedAnswer changed to $selectedAnswer")
    }
    
    LaunchedEffect(hintUsedForCurrentQuestion) {
        android.util.Log.e("CRITICAL_HINT", "======== LaunchedEffect(hintUsedForCurrentQuestion) TRIGGERED ========")
        android.util.Log.e("CRITICAL_HINT", "BEFORE: directHintUsedForUpdate=$directHintUsedForUpdate")
        android.util.Log.e("CRITICAL_HINT", "QuizContent: hintUsedForCurrentQuestion changed to $hintUsedForCurrentQuestion")
        if (hintUsedForCurrentQuestion) {
            directHintUsedForUpdate = true
            android.util.Log.e("CRITICAL_HINT", "AFTER: directHintUsedForUpdate=$directHintUsedForUpdate")
            android.util.Log.e("CRITICAL_HINT", "QuizContent: Set directHintUsedForUpdate=true")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            QuizTopBar(
                onBackClick = { 
                    coroutineScope.launch {
                        appUsageManager.endSession()
                    }
                    onFinish(QuizState()) 
                },
                currentWord = currentWord.value,
                onBookmarkClick = { word ->
                    coroutineScope.launch {
                        wordRepository.updateBookmark(word.id, !word.isBookmarked)
                        currentWord.value = word.copy(isBookmarked = !word.isBookmarked)
                    }
                },
                hintUsedForCurrentQuestion = hintUsedForCurrentQuestion,
                selectedAnswer = selectedAnswer,
                onHintClick = {
                    // Always log that the button was pressed, regardless of conditions
                    android.util.Log.e("CRITICAL_HINT", "============ HINT BUTTON PRESSED ============")
                    
                    // Get the current word ID if available
                    val wordId = currentWord.value?.id
                    
                    // Always log the current state, whether we take action or not
                    android.util.Log.e("CRITICAL_HINT", "CURRENT STATE: currentWord=${wordId}, hintUsed=$hintUsedForCurrentQuestion, answer=$selectedAnswer")
                    android.util.Log.e("CRITICAL_HINT", "CURRENT GLOBALS: directHintUsed=$directHintUsedForUpdate, global=${HintManager.hintWasUsed}")

                    // Skip usual checks and forcibly set the hint state
                    if (currentWord.value != null) {
                        android.util.Log.e("CRITICAL_HINT", "FORCIBLY SETTING HINT STATE for word ${wordId}")
                        
                        // Set both the local and global hint state
                        hintUsedForCurrentQuestion = true
                        HintManager.hintWasUsed = true
                        
                        // Mark hint used for this specific word
                        if (wordId != null) {
                            HintManager.markHintUsedForWord(wordId)
                        }
                        
                        android.util.Log.e("CRITICAL_HINT", "AFTER FORCE: hintUsedForCurrentQuestion=$hintUsedForCurrentQuestion")
                        android.util.Log.e("CRITICAL_HINT", "AFTER FORCE: directHintUsedForUpdate=$directHintUsedForUpdate")
                        android.util.Log.e("CRITICAL_HINT", "AFTER FORCE: HintManager.hintWasUsed=${HintManager.hintWasUsed}")
                        
                        // Force log hint usage for debuggability
                        android.util.Log.e("CRITICAL_HINT_USAGE", "HINT BUTTON FORCE CLICKED for word: $wordId, hint will be used, global=${HintManager.hintWasUsed}")
                    } else {
                        android.util.Log.e("CRITICAL_HINT", "Cannot set hint state: currentWord is null")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            QuizScreen(
                modifier = Modifier.fillMaxSize(),
                wordRepository = wordRepository,
                appUsageManager = appUsageManager,
                isBookmarkMode = isBookmarkMode,
                currentWord = currentWord,
                selectedAnswer = selectedAnswer,
                setSelectedAnswer = { selectedAnswer = it },
                settingsManager = settingsManager,
                quizResultRepository = quizResultRepository,
                hintUsedForCurrentQuestion = hintUsedForCurrentQuestion,
                getHintUsed = { directHintUsedForUpdate },
                resetHintUsed = { 
                    android.util.Log.e("CRITICAL_HINT", "========== resetHintUsed CALLED ==========")
                    android.util.Log.e("CRITICAL_HINT", "QuizContent: resetHintUsed called, setting from hintUsed=$hintUsedForCurrentQuestion, directHint=$directHintUsedForUpdate, global=${HintManager.hintWasUsed} to FALSE")
                    
                    // Capture stack trace to see where this is called from
                    try {
                        throw Exception("resetHintUsed stacktrace capture")
                    } catch (e: Exception) {
                        android.util.Log.e("CRITICAL_HINT", "resetHintUsed called from:", e)
                    }
                    
                    // Also reset the word-specific hint state if we have a current word
                    if (currentWord.value != null) {
                        val wordId = currentWord.value!!.id
                        android.util.Log.e("CRITICAL_HINT", "Resetting hint for word ID: $wordId")
                        HintManager.resetHintForWord(wordId)
                    }
                    
                    hintUsedForCurrentQuestion = false 
                    directHintUsedForUpdate = false
                    android.util.Log.e("CRITICAL_HINT", "QuizContent: AFTER reset: hintUsed=$hintUsedForCurrentQuestion, directHint=$directHintUsedForUpdate, global=${HintManager.hintWasUsed}")
                }
            )
            
            // Debug content at the top of the Box, above everything (hidden by default now)
            if (showDebugPanel.value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.Red.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .border(2.dp, Color.Yellow, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .zIndex(999f),
                ) {
                    Column {
                        Text(
                            "🔍 DEBUG PANEL 🔍",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        // Debug buttons hidden but kept for development
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showDebugPanel.value = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("❌ Close", color = Color.White)
                        }
                    }
                }
            }
            
            // Debug button to reopen the panel
            if (!showDebugPanel.value) {
                // Hidden for production but can be re-enabled for debugging
                /* 
                Button(
                    onClick = { showDebugPanel.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .zIndex(999f)
                ) {
                    Text("DEBUG", color = Color.White, fontWeight = FontWeight.Bold)
                }
                */
            }
        }
    }
} 