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
    var nextBatch by remember { mutableStateOf<List<Word>>(emptyList()) }
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalBookmarkedWords by remember { mutableStateOf(0) }

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
            
            // Find which word this synonym belongs to
            val selectedWord = currentBatch.first { word ->
                selectedSynonym == word.synonym1 ||
                selectedSynonym == word.synonym2 ||
                selectedSynonym == word.synonym3
            }
            
            // Use the tracked currentSynonymSet to determine the correct answer
            val correctSynonym = when (currentSynonymSet) {
                1 -> currentWord.value!!.synonym1
                2 -> currentWord.value!!.synonym2
                else -> currentWord.value!!.synonym3
            }
            
            val isCorrect = selectedSynonym == correctSynonym
            
            val now = System.currentTimeMillis()
            val responseTime = now - questionStartTime
            
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

    // Function to load the next batch of words
    fun loadNextBatch() {
        coroutineScope.launch {
            val newBatch = if (isBookmarkMode) {
                if (totalBookmarkedWords > 1) {
                    wordRepository.getRandomBookmarkedWordsExcluding(4, currentWord.value?.id ?: 0)
                } else {
                    wordRepository.getRandomBookmarkedWords(4)
                }
            } else {
                wordRepository.getRandomWordsExcluding(4, currentWord.value)
            }
            nextBatch = newBatch
        }
    }

    // Function to advance to next question
    fun advanceToNextQuestion() {
        // If we have a preloaded batch, use it
        if (nextBatch.isNotEmpty()) {
            currentBatch = nextBatch
            currentWord.value = currentBatch[0]
            val (newOptions, newSynonymSet) = generateOptions(currentBatch, currentWord.value!!)
            options = newOptions
            currentSynonymSet = newSynonymSet
            nextBatch = emptyList()
            // Start loading the next batch immediately
            loadNextBatch()
        } else {
            // Fallback in case nextBatch isn't ready
            coroutineScope.launch {
                val newBatch = if (isBookmarkMode) {
                    if (totalBookmarkedWords > 1) {
                        wordRepository.getRandomBookmarkedWordsExcluding(4, currentWord.value?.id ?: 0)
                    } else {
                        wordRepository.getRandomBookmarkedWords(4)
                    }
                } else {
                    wordRepository.getRandomWordsExcluding(4, currentWord.value)
                }
                currentBatch = newBatch
                currentWord.value = currentBatch[0]
                val (newOptions, newSynonymSet) = generateOptions(currentBatch, currentWord.value!!)
                options = newOptions
                currentSynonymSet = newSynonymSet
                loadNextBatch()
            }
        }
        selectedAnswer = null
        showNextButton = false
        expandedExamples = emptySet()
        questionStartTime = System.currentTimeMillis()
    }

    // Initial load
    LaunchedEffect(Unit) {
        currentBatch = if (isBookmarkMode) {
            wordRepository.getRandomBookmarkedWords(4)
        } else {
            wordRepository.getRandomWords(4)
        }
        if (currentBatch.isNotEmpty()) {
            currentWord.value = currentBatch[0]
            val (newOptions, newSynonymSet) = generateOptions(currentBatch, currentWord.value!!)
            options = newOptions
            currentSynonymSet = newSynonymSet
            loadNextBatch()
            questionStartTime = System.currentTimeMillis()
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
        Text(
            text = currentWord.value!!.word.lowercase(),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { lookupWord(currentWord.value!!.word) }
                    )
                }
        )

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