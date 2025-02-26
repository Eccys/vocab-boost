package xyz.ecys.vocab.quiz

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import xyz.ecys.vocab.QuizResultsActivity
import xyz.ecys.vocab.data.*
import xyz.ecys.vocab.ui.theme.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    wordRepository: WordRepository,
    appUsageManager: AppUsageManager,
    isBookmarkMode: Boolean,
    currentWord: MutableState<Word?>,
    lives: MutableState<Int>,
    onGameOver: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    
    // State variables
    var quizResults by remember { mutableStateOf<List<QuizResult>>(emptyList()) }
    var currentSynonymSet by remember { mutableStateOf(1) }
    var questionStartTime by remember { mutableStateOf(0L) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showNextButton by remember { mutableStateOf(false) }
    var expandedExamples by remember { mutableStateOf(setOf<String>()) }
    var currentBatch by remember { mutableStateOf<List<Word>>(emptyList()) }
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalBookmarkedWords by remember { mutableStateOf(0) }

    // Debug state
    var showDebugInfo by remember { mutableStateOf(true) }
    var allWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    var overdueWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    var unseenWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    var recentlyReviewedWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    
    // Load all words for debugging
    LaunchedEffect(Unit) {
        allWords = wordRepository.getAllWords()
        val currentTime = System.currentTimeMillis()
        overdueWords = wordRepository.getWordsForLearning(100).filter { it.nextReviewDate <= currentTime && it.nextReviewDate > 0 }
        unseenWords = wordRepository.getAllWords().filter { it.timesReviewed == 0 }
        recentlyReviewedWords = wordRepository.getRecentlyReviewedWords(5)
    }
    
    // Update debug info when current word changes
    LaunchedEffect(currentWord.value) {
        if (currentWord.value != null) {
            val currentTime = System.currentTimeMillis()
            allWords = wordRepository.getAllWords()
            overdueWords = wordRepository.getWordsForLearning(100).filter { it.nextReviewDate <= currentTime && it.nextReviewDate > 0 }
            unseenWords = allWords.filter { it.timesReviewed == 0 }
            recentlyReviewedWords = wordRepository.getRecentlyReviewedWords(5)
        }
    }

    // Add the lookupWord function
    fun lookupWord(word: String) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        try {
            val dictionaryIntent = Intent(Intent.ACTION_VIEW)
            dictionaryIntent.data = Uri.parse("dictionary:$word")
            context.startActivity(dictionaryIntent)
        } catch (e: ActivityNotFoundException) {
            val searchIntent = Intent(Intent.ACTION_VIEW)
            searchIntent.data = Uri.parse("https://www.google.com/search?q=define+$word")
            context.startActivity(searchIntent)
        }
    }

    // Move handleAnswer function here
    fun handleAnswer(selectedSynonym: String) {
        if (selectedAnswer == null) {
            selectedAnswer = selectedSynonym
            
            // Use the tracked currentSynonymSet to determine the correct answer
            val correctSynonym = when (currentSynonymSet) {
                1 -> currentWord.value!!.synonym1
                2 -> currentWord.value!!.synonym2
                else -> currentWord.value!!.synonym3
            }
            
            val isCorrect = selectedSynonym == correctSynonym
            
            val now = System.currentTimeMillis()
            val responseTime = now - questionStartTime
            android.util.Log.d("QuizTimer", "Question start time: $questionStartTime")
            android.util.Log.d("QuizTimer", "Answer time: $now")
            android.util.Log.d("QuizTimer", "Response time: $responseTime")
            
            if (isCorrect) {
                coroutineScope.launch {
                    appUsageManager.recordCorrectAnswer()
                }
            }
            
            if (!isCorrect) {
                lives.value--
            }
            
            // Update word statistics with timing information
            coroutineScope.launch {
                wordRepository.updateWordStats(
                    wordId = currentWord.value!!.id,
                    wasCorrect = isCorrect,
                    timestamp = now,
                    responseTime = responseTime
                )
            }
            
            val newResult = QuizResult(
                word = currentWord.value!!.word,
                definition = currentWord.value!!.definition,
                userChoice = selectedSynonym,
                correctChoice = correctSynonym,
                isCorrect = isCorrect
            )
            quizResults = quizResults + newResult

            showNextButton = true

            if (lives.value <= 0) {
                // Game over, show results
                coroutineScope.launch {
                    // End the session before transitioning
                    appUsageManager.endSession()
                    
                    // Create intent with results
                    val intent = Intent(context, QuizResultsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putParcelableArrayListExtra("results", ArrayList(quizResults))
                    }
                    
                    // Start the activity and notify game over
                    context.startActivity(intent)
                    onGameOver()
                }
            }
        }
    }

    // Get total bookmarked words count
    LaunchedEffect(Unit) {
        if (isBookmarkMode) {
            totalBookmarkedWords = wordRepository.getBookmarkedWordsFlow().first().size
        }
    }

    // Function to advance to next question
    fun advanceToNextQuestion() {
        coroutineScope.launch {
            // Get the next word prioritizing overdue words by their overdue ratio
            val nextWord = if (isBookmarkMode) {
                // In bookmark mode, get a random bookmarked word
                if (totalBookmarkedWords > 1) {
                    val bookmarkedWords = wordRepository.getBookmarkedWordsFlow().first()
                    val filteredWords = bookmarkedWords.filter { it.id != currentWord.value?.id }
                    if (filteredWords.isNotEmpty()) filteredWords.random() else bookmarkedWords.random()
                } else {
                    wordRepository.getRandomBookmarkedWords(1).firstOrNull() ?: return@launch
                }
            } else {
                // In normal mode, get the next word prioritizing overdue words
                wordRepository.getNextWord(currentWord.value)
            }

            // Build a batch with the selected word and 3 other random words
            val otherWords = if (isBookmarkMode) {
                wordRepository.getRandomBookmarkedWordsExcluding(3, nextWord.id)
            } else {
                wordRepository.getRandomWordsExcluding(3, nextWord)
            }
            
            // Create the new batch with the prioritized word first
            currentBatch = listOf(nextWord) + otherWords
            currentWord.value = nextWord
            
            // Generate options and synonym set for the quiz
            val (newOptions, newSynonymSet) = generateOptions(currentBatch, nextWord)
            options = newOptions
            currentSynonymSet = newSynonymSet
            
            // Reset UI state for the new question
            selectedAnswer = null
            showNextButton = false
            expandedExamples = emptySet()
            questionStartTime = System.currentTimeMillis()
            
            // Update debug info
            val currentTime = System.currentTimeMillis()
            allWords = wordRepository.getAllWords()
            overdueWords = wordRepository.getWordsForLearning(100).filter { it.nextReviewDate <= currentTime && it.nextReviewDate > 0 }
            unseenWords = allWords.filter { it.timesReviewed == 0 }
            recentlyReviewedWords = wordRepository.getRecentlyReviewedWords(5)
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        // Get the initial word prioritizing overdue words by their overdue ratio
        val initialWord = if (isBookmarkMode) {
            // In bookmark mode, get a random bookmarked word
            wordRepository.getRandomBookmarkedWords(1).firstOrNull()
        } else {
            // In normal mode, get the next word prioritizing overdue words
            wordRepository.getNextWord(null)
        }
        
        if (initialWord != null) {
            // Build a batch with the selected word and 3 other random words
            val otherWords = if (isBookmarkMode) {
                wordRepository.getRandomBookmarkedWordsExcluding(3, initialWord.id)
            } else {
                wordRepository.getRandomWordsExcluding(3, initialWord)
            }
            
            // Create the batch with the prioritized word first
            currentBatch = listOf(initialWord) + otherWords
            currentWord.value = initialWord
            
            // Generate options and synonym set for the quiz
            val (newOptions, newSynonymSet) = generateOptions(currentBatch, initialWord)
            options = newOptions
            currentSynonymSet = newSynonymSet
            
            questionStartTime = System.currentTimeMillis()
            android.util.Log.d("QuizTimer", "Setting initial question start time: $questionStartTime")
        }
    }

    if (currentBatch.isEmpty() || currentWord.value == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isBookmarkMode) {
                Text("No bookmarked words available")
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Debug panel
        if (showDebugInfo && currentWord.value != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2D2D3A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Debug Info",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Switch(
                            checked = showDebugInfo,
                            onCheckedChange = { showDebugInfo = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val word = currentWord.value!!
                    val currentTime = System.currentTimeMillis()
                    val isOverdue = word.nextReviewDate > 0 && word.nextReviewDate <= currentTime
                    val isUnseen = word.timesReviewed == 0
                    
                    Text(
                        "Word ID: ${word.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        "Times Reviewed: ${word.timesReviewed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        "Next Review: ${if (word.nextReviewDate > 0) 
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(word.nextReviewDate)) 
                            else "Not scheduled"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        "Status: ${when {
                            isOverdue -> "OVERDUE"
                            isUnseen -> "UNSEEN"
                            else -> "SCHEDULED"
                        }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isOverdue -> Color(0xFFFF9800)
                            isUnseen -> Color(0xFF2196F3)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                    
                    Text(
                        "Interval: ${word.interval} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        "Ease Factor: ${String.format("%.2f", word.easeFactor)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Overdue Words: ${overdueWords.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                    
                    Text(
                        "Unseen Words: ${unseenWords.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2196F3)
                    )
                    
                    if (overdueWords.isNotEmpty()) {
                        Text(
                            "Overdue IDs: ${overdueWords.take(5).map { it.id }}${if (overdueWords.size > 5) "..." else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Word Selection Logic:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    
                    Text(
                        "1. Overdue words (highest priority)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdueWords.isNotEmpty()) Color(0xFFFF9800) else Color.White.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        "2. Unseen words (if no overdue words)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdueWords.isEmpty() && unseenWords.isNotEmpty()) Color(0xFF2196F3) else Color.White.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        "3. Words without future review dates (last resort)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdueWords.isEmpty() && unseenWords.isEmpty()) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Recently Reviewed:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    
                    recentlyReviewedWords.forEach { recentWord ->
                        val reviewTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(recentWord.lastReviewed))
                        val nextReview = if (recentWord.nextReviewDate > 0) {
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(java.util.Date(recentWord.nextReviewDate))
                        } else "Not scheduled"
                        
                        Text(
                            "${recentWord.word} (ID: ${recentWord.id}) - Reviewed at $reviewTime, Next: $nextReview",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Add a button to force refresh the word selection
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val nextWord = wordRepository.getNextWord(currentWord.value)
                                val otherWords = wordRepository.getRandomWordsExcluding(3, nextWord)
                                currentBatch = listOf(nextWord) + otherWords
                                currentWord.value = nextWord
                                val (newOptions, newSynonymSet) = generateOptions(currentBatch, nextWord)
                                options = newOptions
                                currentSynonymSet = newSynonymSet
                                selectedAnswer = null
                                showNextButton = false
                                expandedExamples = emptySet()
                                questionStartTime = System.currentTimeMillis()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3F51B5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Force Next Word Selection")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Add a section to show all words and their status
                    var showAllWords by remember { mutableStateOf(false) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "All Words Status",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Switch(
                            checked = showAllWords,
                            onCheckedChange = { showAllWords = it }
                        )
                    }
                    
                    if (showAllWords) {
                        val currentTime = System.currentTimeMillis()
                        val sortedWords = allWords.sortedWith(
                            compareBy<Word> { 
                                // First sort by status: overdue, unseen, future review
                                when {
                                    it.nextReviewDate > 0 && it.nextReviewDate <= currentTime -> 0
                                    it.timesReviewed == 0 -> 1
                                    else -> 2
                                }
                            }.thenBy { it.word }
                        )
                        
                        sortedWords.forEach { word ->
                            val isOverdue = word.nextReviewDate > 0 && word.nextReviewDate <= currentTime
                            val isUnseen = word.timesReviewed == 0
                            val isFutureReview = word.nextReviewDate > currentTime
                            
                            val statusColor = when {
                                isOverdue -> Color(0xFFFF9800)
                                isUnseen -> Color(0xFF2196F3)
                                isFutureReview -> Color(0xFF4CAF50)
                                else -> Color.White.copy(alpha = 0.7f)
                            }
                            
                            val statusText = when {
                                isOverdue -> "OVERDUE"
                                isUnseen -> "UNSEEN"
                                isFutureReview -> "FUTURE"
                                else -> "REVIEWED"
                            }
                            
                            val nextReview = if (word.nextReviewDate > 0) {
                                java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                                    .format(java.util.Date(word.nextReviewDate))
                            } else "N/A"
                            
                            Text(
                                "${word.word} (ID: ${word.id}) - $statusText - Next: $nextReview",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }

        // Add state for tooltip visibility
        var showTooltip by remember { mutableStateOf(false) }

        Text(
            text = currentWord.value!!.word.lowercase(),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { 
                            // Show tooltip with example sentence instead of looking up the word
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            showTooltip = true
                        }
                    )
                }
        )

        // Tooltip for example sentence
        AnimatedVisibility(
            visible = showTooltip,
            enter = fadeIn() + expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 300f
                ),
                expandFrom = Alignment.Top
            ),
            exit = fadeOut() + shrinkVertically(
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 300f
                ),
                shrinkTowards = Alignment.Top
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showTooltip = false },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2D2D3A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Example:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF90CAF9)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentWord.value!!.exampleSentence,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        options.forEach { optionSynonym ->
            Column(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = AppAnimations.contentSizeSpec()
                    )
            ) {
                val selectedWord = currentBatch.first { word ->
                    optionSynonym == word.synonym1 ||
                    optionSynonym == word.synonym2 ||
                    optionSynonym == word.synonym3
                }
                
                // Get the correct definition and example for this synonym
                val (synonymDefinition, synonymExampleSentence) = when (optionSynonym) {
                    selectedWord.synonym1 -> Pair(selectedWord.synonym1Definition, selectedWord.synonym1ExampleSentence)
                    selectedWord.synonym2 -> Pair(selectedWord.synonym2Definition, selectedWord.synonym2ExampleSentence)
                    selectedWord.synonym3 -> Pair(selectedWord.synonym3Definition, selectedWord.synonym3ExampleSentence)
                    else -> Pair("", "") // shouldn't happen
                }
                
                val isSelected = selectedAnswer == optionSynonym
                val isCorrectAnswer = optionSynonym == when (currentSynonymSet) {
                    1 -> currentWord.value!!.synonym1
                    2 -> currentWord.value!!.synonym2
                    else -> currentWord.value!!.synonym3
                }
                val shouldShowDefinition = selectedAnswer != null && (isSelected || isCorrectAnswer)
                
                // Updated button styling
                Button(
                    onClick = { if (selectedAnswer == null) handleAnswer(optionSynonym) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            selectedAnswer != null && isSelected && !isCorrectAnswer -> Error
                            selectedAnswer != null && isCorrectAnswer -> Success
                            else -> Color(0xFF18191E)
                        },
                        contentColor = White,
                        disabledContainerColor = when {
                            selectedAnswer != null && isSelected && !isCorrectAnswer -> Error
                            selectedAnswer != null && isCorrectAnswer -> Success
                            else -> Color(0xFF18191E)
                        },
                        disabledContentColor = White
                    ),
                    shape = MaterialTheme.shapes.medium,
                    enabled = selectedAnswer == null,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Text(
                        text = optionSynonym.lowercase(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                AnimatedVisibility(
                    visible = shouldShowDefinition,
                    enter = expandVertically(
                        animationSpec = AppAnimations.tweenSpec()
                    ) + fadeIn(
                        animationSpec = AppAnimations.tweenSpec()
                    ),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$optionSynonym means $synonymDefinition"
                                .capitalize(Locale.current),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Example",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.clickable {
                                expandedExamples = if (expandedExamples.contains(optionSynonym)) {
                                    expandedExamples - optionSynonym
                                } else {
                                    expandedExamples + optionSynonym
                                }
                            }
                        )
                        AnimatedVisibility(
                            visible = expandedExamples.contains(optionSynonym),
                            enter = expandVertically(
                                animationSpec = AppAnimations.tweenSpec()
                            ) + fadeIn(
                                animationSpec = AppAnimations.tweenSpec()
                            ),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Text(
                                text = synonymExampleSentence,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Update "Next" button styling to match
        if (showNextButton) {
            Button(
                onClick = { 
                    selectedAnswer = null
                    showNextButton = false
                    expandedExamples = emptySet()
                    advanceToNextQuestion() 
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .height(56.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary80,
                    contentColor = Background
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "Next",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
} 