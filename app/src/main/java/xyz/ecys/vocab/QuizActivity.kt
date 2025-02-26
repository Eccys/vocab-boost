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
import xyz.ecys.vocab.data.QuizResult

class QuizActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var correctAnswerTracker: CorrectAnswerTracker
    private var isBookmarkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)
        appUsageManager = AppUsageManager.getInstance(this)
        correctAnswerTracker = CorrectAnswerTracker.getInstance(this)
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
                
                // Add these state variables
                var selectedAnswer by remember { mutableStateOf<String?>(null) }
                var showNextButton by remember { mutableStateOf(false) }
                var expandedExamples by remember { mutableStateOf(setOf<String>()) }
                
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
                            lifecycleScope.launch {
                                appUsageManager.endSession()
                            }
                        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizTopBar(
    onBackClick: () -> Unit,
    currentWord: Word?,
    onBookmarkClick: (Word) -> Unit,
    lives: Int
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
                repeat(3) { index ->
                    Icon(
                        painter = if (index < lives) AppIcons.heartSolid() else AppIcons.heartCrackSolid(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFED333B)
                    )
                }
                if (currentWord != null) {
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
            
            quizResults = quizResults + QuizResult(
                word = currentWord.value!!.word,
                definition = currentWord.value!!.definition,
                userChoice = selectedSynonym,
                correctChoice = correctSynonym,
                isCorrect = isCorrect
            )

            showNextButton = true

            if (lives.value <= 0) {
                // Game over, show results
                onGameOver()
                val intent = Intent(context, QuizResultsActivity::class.java).apply {
                    putParcelableArrayListExtra("results", ArrayList(quizResults))
                }
                context.startActivity(intent)
                if (context is ComponentActivity) {
                    context.finish()
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
            // Use getNextWord for proper word selection
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
            
            // Get other words for options
            val otherWords = if (isBookmarkMode) {
                wordRepository.getRandomBookmarkedWordsExcluding(3, nextWord.id)
            } else {
                wordRepository.getRandomWordsExcluding(3, nextWord)
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
                // Use getNextWord for proper word selection
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
                
                // Get other words for options
                val otherWords = if (isBookmarkMode) {
                    wordRepository.getRandomBookmarkedWordsExcluding(3, nextWord.id)
                } else {
                    wordRepository.getRandomWordsExcluding(3, nextWord)
                }
                
                currentBatch = listOf(nextWord) + otherWords
                currentWord.value = nextWord
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
            // Use getNextWord for the first word to ensure proper word selection
            val initialWord = wordRepository.getNextWord(null)
            val otherWords = wordRepository.getRandomWordsExcluding(3, initialWord)
            listOf(initialWord) + otherWords
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

private fun generateOptions(words: List<Word>, currentWord: Word): Pair<List<String>, Int> {
    // Choose which synonym (1-3) to use for the correct answer
    val correctSynonymNumber = (1..3).random()
    
    // Get the correct answer based on the chosen synonym number
    val correctAnswer = when (correctSynonymNumber) {
        1 -> currentWord.synonym1
        2 -> currentWord.synonym2
        else -> currentWord.synonym3
    }
    
    // Create list of wrong answers by getting random synonyms from other words
    val wrongAnswers = words
        .filter { it.id != currentWord.id } // Exclude the current word
        .map { word ->
            // For each word, randomly select one of its synonyms
            val randomSynonymNumber = (1..3).random()
            when (randomSynonymNumber) {
                1 -> word.synonym1
                2 -> word.synonym2
                else -> word.synonym3
            }
        }
    
    // Combine all answers and shuffle
    val allOptions = (wrongAnswers + correctAnswer).shuffled()
    
    return Pair(allOptions, correctSynonymNumber)
}

