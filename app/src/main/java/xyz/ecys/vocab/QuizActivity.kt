@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package xyz.ecys.vocab

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.flow.first
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.data.AppUsageManager
import xyz.ecys.vocab.data.CorrectAnswerTracker
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.ui.theme.AppIcons
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.animation.animateContentSize
import xyz.ecys.vocab.ui.theme.AppAnimations
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import xyz.ecys.vocab.ui.theme.Primary80
import xyz.ecys.vocab.ui.theme.Background
import xyz.ecys.vocab.ui.theme.Surface
import xyz.ecys.vocab.ui.theme.White
import xyz.ecys.vocab.ui.theme.Success
import xyz.ecys.vocab.ui.theme.Error
import xyz.ecys.vocab.data.QuizResult as FirestoreQuizResult
import xyz.ecys.vocab.data.SettingsManager
import xyz.ecys.vocab.quiz.generateOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import xyz.ecys.vocab.data.QuizResultRepository
import xyz.ecys.vocab.data.QuizQuestion
import java.util.Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import xyz.ecys.vocab.quiz.QuizResult
import android.content.Context
import xyz.ecys.vocab.utils.TransitionUtils

class QuizActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var correctAnswerTracker: CorrectAnswerTracker
    private lateinit var settingsManager: SettingsManager
    private lateinit var quizResultRepository: QuizResultRepository
    private var isBookmarkMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)
        appUsageManager = AppUsageManager.getInstance(this)
        correctAnswerTracker = CorrectAnswerTracker.getInstance(this)
        settingsManager = SettingsManager.getInstance(this)
        quizResultRepository = QuizResultRepository.getInstance(this)
        isBookmarkMode = intent.getStringExtra("mode") == "bookmarks"

        // Start tracking quiz session
        appUsageManager.startQuizSession()

        // Initialize database with sample words if empty
        lifecycleScope.launch {
            if (wordRepository.getAllWords().isEmpty()) {
                wordRepository.insertInitialWords()
            }
        }

        setContent {
            VocabularyBoosterTheme {
                val currentWord = remember { mutableStateOf<Word?>(null) }
                val coroutineScope = rememberCoroutineScope()
                val lives = remember { mutableStateOf(3) }
                val hintsRemaining = remember { mutableStateOf(3) }
                
                // Add these state variables
                var selectedAnswerState by remember { mutableStateOf<String?>(null) }
                var showNextButton by remember { mutableStateOf(false) }
                var expandedExamples by remember { mutableStateOf(setOf<String>()) }
                // Add a state to track if hint was used for current question
                var hintUsedForCurrentQuestionState by remember { mutableStateOf(false) }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        QuizTopBar(
                            onBackClick = { 
                                lifecycleScope.launch {
                                    appUsageManager.endSession()
                                }
                                finish() 
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
                            hintUsedForCurrentQuestion = hintUsedForCurrentQuestionState,
                            selectedAnswer = selectedAnswerState,
                            onHintClick = {
                                // Hints are unlimited now
                                // Only allow hint if it hasn't been used for this question and user hasn't answered
                                if (currentWord.value != null && 
                                    !hintUsedForCurrentQuestionState &&
                                    selectedAnswerState == null) {
                                    // Don't decrease hint count anymore
                                    hintUsedForCurrentQuestionState = true
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
                        hintsRemaining = hintsRemaining,
                        selectedAnswer = selectedAnswerState,
                        setSelectedAnswer = { newValue -> selectedAnswerState = newValue },
                        hintUsedForCurrentQuestion = hintUsedForCurrentQuestionState,
                        setHintUsedForCurrentQuestion = { newValue -> hintUsedForCurrentQuestionState = newValue },
                        onGameOver = {
                            lifecycleScope.launch {
                                appUsageManager.endSession()
                            }
                        },
                        settingsManager = settingsManager,
                        quizResultRepository = quizResultRepository
                    )
                }
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

    override fun onBackPressed() {
        super.onBackPressed()
        TransitionUtils.applyStandardTransitionOnFinish(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizTopBar(
    onBackClick: () -> Unit,
    currentWord: Word?,
    onBookmarkClick: (Word) -> Unit,
    lives: Int,
    hintsRemaining: Int,
    hintUsedForCurrentQuestion: Boolean,
    selectedAnswer: String?,
    onHintClick: () -> Unit
) {
    TopAppBar(
        title = { 
            Text(
                text = "Synonyms",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCFCFC)
                )
            ) 
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = AppIcons.arrowLeft(),
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hearts/lives removed - quiz is now endless
                
                // Hint button - now unlimited and without counter
                if (currentWord != null) {
                    IconButton(
                        onClick = { onHintClick() },
                        // Only disabled if hint already used for this question or user has answered
                        enabled = !hintUsedForCurrentQuestion && selectedAnswer == null
                    ) {
                        Icon(
                            painter = AppIcons.lightbulbSolid(),
                            contentDescription = "Show hint",
                            // Gray out if disabled for any reason
                            tint = if (!hintUsedForCurrentQuestion && selectedAnswer == null) 
                                Color(0xFFFFC107) else Color.Gray
                        )
                    }
                    
                    // Bookmark button
                    IconButton(
                        onClick = { onBookmarkClick(currentWord) }
                    ) {
                        Icon(
                            painter = if (currentWord.isBookmarked) AppIcons.bookmarkSolid() else AppIcons.bookmarkOutline(),
                            contentDescription = if (currentWord.isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (currentWord.isBookmarked) MaterialTheme.colorScheme.tertiary else Color(0xFFFCFCFC)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    wordRepository: WordRepository,
    appUsageManager: AppUsageManager,
    isBookmarkMode: Boolean,
    currentWord: MutableState<Word?>,
    @Suppress("UNUSED_PARAMETER") lives: MutableState<Int>,
    @Suppress("UNUSED_PARAMETER") hintsRemaining: MutableState<Int>,
    selectedAnswer: String?,
    setSelectedAnswer: (String?) -> Unit,
    hintUsedForCurrentQuestion: Boolean,
    setHintUsedForCurrentQuestion: (Boolean) -> Unit,
    @Suppress("UNUSED_PARAMETER") onGameOver: () -> Unit,
    @Suppress("UNUSED_PARAMETER") settingsManager: SettingsManager,
    quizResultRepository: QuizResultRepository
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // State variables
    var quizResults by remember { mutableStateOf<List<QuizResult>>(emptyList()) }
    var currentSynonymSet by remember { mutableStateOf(1) }
    var questionStartTime by remember { mutableStateOf(0L) }
    @Suppress("UNUSED_VARIABLE") 
    var expandedExamples by remember { mutableStateOf(setOf<String>()) }
    var currentBatch by remember { mutableStateOf<List<Word>>(emptyList()) }
    var nextBatch by remember { mutableStateOf<List<Word>>(emptyList()) }
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalBookmarkedWords by remember { mutableStateOf(0) }
    var showHint by remember { mutableStateOf(false) }
    var showNextButton by remember { mutableStateOf(false) }

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
            setSelectedAnswer(selectedSynonym)
            
            // Find the correct answer based on currentSynonymSet
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
            
            // Update word statistics with timing information
            coroutineScope.launch {
                wordRepository.updateWordStats(
                    wordId = currentWord.value!!.id,
                    wasCorrect = isCorrect,
                    timestamp = now,
                    responseTime = responseTime
                )
            }
            
            // Create a QuizQuestion object for the current question
            val newQuizQuestion = QuizQuestion(
                word = currentWord.value!!.word,
                correctDefinition = currentWord.value!!.definition,
                userAnswer = selectedSynonym,
                isCorrect = isCorrect
            )
            
            // Add to the legacy results list for backwards compatibility
            val legacyResult = QuizResult(
                word = currentWord.value!!.word,
                definition = currentWord.value!!.definition,
                userChoice = selectedSynonym,
                correctChoice = correctSynonym,
                isCorrect = isCorrect
            )
            quizResults = quizResults + legacyResult

            // Update the next button state
            showNextButton = true
            showHint = false // Reset hint when answering

            // Save this single question to Firestore immediately for quiz history
            coroutineScope.launch {
                // Create a Firestore QuizResult with just this question
                val firestoreQuizResult = FirestoreQuizResult(
                    timestamp = Date(),
                    correctAnswers = if (isCorrect) 1 else 0,
                    totalQuestions = 1,
                    questions = listOf(newQuizQuestion),
                    score = if (isCorrect) 1f else 0f,
                    durationInSeconds = responseTime / 1000
                )
                
                // Save to Firestore
                try {
                    quizResultRepository.saveQuizResult(firestoreQuizResult)
                    println("Saved single question to Firestore for quiz history")
                } catch (e: Exception) {
                    println("Error saving to Firestore: ${e.message}")
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
            // Get the next word using the existing selection logic
            val nextWord = if (isBookmarkMode) {
                if (totalBookmarkedWords > 1) {
                    val bookmarkedWords = wordRepository.getBookmarkedWordsFlow().first()
                    val filteredWords = bookmarkedWords.filter { it.id != currentWord.value?.id }
                    if (filteredWords.isNotEmpty()) filteredWords.random() else bookmarkedWords.random()
                } else {
                    wordRepository.getRandomBookmarkedWords(1).firstOrNull() ?: return@launch
                }
            } else {
                wordRepository.getNextWord(currentWord.value)
            }
            
            // Get other words for options using the settings for number of options
            val totalOptionsCount = settingsManager.getMultipleChoiceOptionsCount()
            val wrongOptionsCount = totalOptionsCount - 1 // Subtract 1 for the correct answer
            
            val otherWords = if (isBookmarkMode) {
                wordRepository.getRandomBookmarkedWordsExcluding(10, nextWord.id)
                    .filter { it.category == nextWord.category }
                    .take(wrongOptionsCount)  // Use the number of wrong options from settings
            } else {
                wordRepository.getRandomWordsByCategory(nextWord.category, totalOptionsCount)  // Get total words needed
                    .filter { word -> word.id != nextWord.id }
                    .take(wrongOptionsCount)  // Use the number of wrong options from settings
            }
            
            nextBatch = listOf(nextWord) + otherWords
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
                // Get the next word using the existing selection logic
                val nextWord = if (isBookmarkMode) {
                    if (totalBookmarkedWords > 1) {
                        val bookmarkedWords = wordRepository.getBookmarkedWordsFlow().first()
                        val filteredWords = bookmarkedWords.filter { it.id != currentWord.value?.id }
                        if (filteredWords.isNotEmpty()) filteredWords.random() else bookmarkedWords.random()
                    } else {
                        wordRepository.getRandomBookmarkedWords(1).firstOrNull() ?: return@launch
                    }
                } else {
                    wordRepository.getNextWord(currentWord.value)
                }
                
                // Get other words for options using the settings for number of options
                val totalOptionsCount = settingsManager.getMultipleChoiceOptionsCount()
                val wrongOptionsCount = totalOptionsCount - 1 // Subtract 1 for the correct answer
                
                // Get other words of the same category
                val otherWords = if (isBookmarkMode) {
                    wordRepository.getRandomBookmarkedWordsExcluding(10, nextWord.id)
                        .filter { it.category == nextWord.category }
                        .take(wrongOptionsCount)  // Use the number of wrong options from settings
                } else {
                    wordRepository.getRandomWordsByCategory(nextWord.category, totalOptionsCount)  // Get total words needed
                        .filter { word -> word.id != nextWord.id }
                        .take(wrongOptionsCount)  // Use the number of wrong options from settings
                }
                
                currentBatch = listOf(nextWord) + otherWords
                currentWord.value = nextWord
                val (newOptions, newSynonymSet) = generateOptions(currentBatch, currentWord.value!!)
                options = newOptions
                currentSynonymSet = newSynonymSet
                loadNextBatch()
            }
        }
        setSelectedAnswer(null)
        // No need to set showNextButton since we're using a local variable in handleAnswer
        // Reset expandedExamples
        showHint = false
        setHintUsedForCurrentQuestion(false)  // Reset the hint used flag for the new question
        questionStartTime = System.currentTimeMillis()
    }

    // Initial load
    LaunchedEffect(Unit) {
        // Get the initial word
        val initialWord = if (isBookmarkMode) {
            wordRepository.getRandomBookmarkedWords(1).firstOrNull() ?: return@LaunchedEffect
        } else {
            wordRepository.getNextWord(null)
        }

        // Get other words for options using the settings for number of options
        val totalOptionsCount = settingsManager.getMultipleChoiceOptionsCount()
        val wrongOptionsCount = totalOptionsCount - 1 // Subtract 1 for the correct answer
        
        // Get other words of the same category
        val otherWords = if (isBookmarkMode) {
            wordRepository.getRandomBookmarkedWordsExcluding(10, initialWord.id)
                .filter { it.category == initialWord.category }
                .take(wrongOptionsCount)  // Use the number of wrong options from settings
        } else {
            wordRepository.getRandomWordsByCategory(initialWord.category, totalOptionsCount)  // Get total words needed
                .filter { word -> word.id != initialWord.id }
                .take(wrongOptionsCount)  // Use the number of wrong options from settings
        }

        currentBatch = listOf(initialWord) + otherWords
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
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Word and definition section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentWord.value!!.word.lowercase(),
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Show definition after answering - modified to prevent jitter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = if (selectedAnswer != null) 4.dp else 0.dp)
                        .then(
                            if (selectedAnswer != null) {
                                Modifier.wrapContentSize()
                            } else {
                                Modifier.height(0.dp)
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentWord.value!!.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    // Add example section
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Track if the example is expanded
                    var isExampleExpanded by remember { mutableStateOf(false) }
                    
                    Text(
                        text = "Example",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.clickable {
                            isExampleExpanded = !isExampleExpanded
                        }
                    )
                    
                    AnimatedVisibility(
                        visible = isExampleExpanded,
                        enter = expandVertically(
                            animationSpec = AppAnimations.tweenSpec()
                        ) + fadeIn(
                            animationSpec = AppAnimations.tweenSpec()
                        ),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = currentWord.value!!.exampleSentence,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Show hint when requested - modified to prevent jitter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = if (showHint) 4.dp else 0.dp)
                        .then(
                            if (showHint) {
                                Modifier.wrapContentSize()
                            } else {
                                Modifier.height(0.dp)
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentWord.value!!.exampleSentence,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Add a divider between the word section and options - modified to prevent jitter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-8).dp)  // Reduced negative offset to position divider more evenly
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(vertical = 0.dp),  // Keeping vertical padding at 0.dp
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // Update the hint button to show/hide the hint
        LaunchedEffect(hintUsedForCurrentQuestion) {
            // Show hint when hint is used for current question
            if (hintUsedForCurrentQuestion && !showHint) {
                showHint = true
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
                val shouldShowDefinition = selectedAnswer != null && (isSelected || isCorrectAnswer || expandedExamples.contains(optionSynonym))
                
                // Updated button styling
                Button(
                    onClick = { 
                        if (selectedAnswer == null) {
                            handleAnswer(optionSynonym)
                        } else {
                            // Allow clicking other options after answering to see their definitions
                            if (!isSelected && !isCorrectAnswer) {
                                expandedExamples = if (expandedExamples.contains(optionSynonym)) {
                                    expandedExamples - optionSynonym
                                } else {
                                    expandedExamples + optionSynonym
                                }
                            }
                        }
                    },
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
                    enabled = true, // Always enabled to allow clicking after answering
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp,
                        disabledElevation = 0.dp
                    ),
                    border = if (selectedAnswer == null) {
                        BorderStroke(1.dp, White.copy(alpha = 0.12f))
                    } else null
                ) {
                    Text(
                        text = optionSynonym.lowercase(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Show definition for answer choices - modified to prevent jitter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (shouldShowDefinition) 8.dp else 0.dp)
                            .then(
                                if (shouldShowDefinition) {
                                    Modifier.wrapContentSize()
                                } else {
                                    Modifier.height(0.dp)
                                }
                            ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "$optionSynonym means $synonymDefinition",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        // Create a separate state for example expansion
                        var isExampleVisible by remember { mutableStateOf(false) }
                        
                        // For correct/incorrect answers, use a local state
                        // For other options, use the local isExampleVisible state
                        val showExample = if (isSelected || isCorrectAnswer) {
                            // Use expandedExamples for backward compatibility
                            expandedExamples.contains(optionSynonym)
                        } else {
                            isExampleVisible
                        }
                        
                        Text(
                            text = "Example",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2196F3),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable {
                                    if (isSelected || isCorrectAnswer) {
                                        // For correct/incorrect answers, use expandedExamples
                                        expandedExamples = if (expandedExamples.contains(optionSynonym)) {
                                            expandedExamples - optionSynonym
                                        } else {
                                            expandedExamples + optionSynonym
                                        }
                                    } else {
                                        // For other options, toggle the local state
                                        isExampleVisible = !isExampleVisible
                                    }
                                }
                        )
                        
                        // Use Box with animateContentSize instead of AnimatedVisibility
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = 0.6f,  // Lower value = more bouncy
                                        stiffness = Spring.StiffnessMediumLow,  // Faster animation
                                        visibilityThreshold = null
                                    )
                                )
                        ) {
                            if (showExample) {
                                Text(
                                    text = synonymExampleSentence,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Update "Next" button styling to match
        if (showNextButton) {
            Button(
                onClick = { 
                    setSelectedAnswer(null)
                    showNextButton = false
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
        
        // Add some padding at the bottom to ensure scrollability
        Spacer(modifier = Modifier.height(32.dp))
    }
}

