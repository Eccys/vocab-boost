package xyz.ecys.vocab

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.QuizQuestion
import xyz.ecys.vocab.data.QuizResult
import xyz.ecys.vocab.data.QuizResultRepository
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.data.QuizHistoryItem
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.ui.theme.Primary80
import xyz.ecys.vocab.ui.theme.Background
import java.util.Date
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import xyz.ecys.vocab.utils.TransitionUtils
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
class QuizHistoryActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository
    private lateinit var quizResultRepository: QuizResultRepository
    
    companion object {
        private const val HISTORY_CACHE_PREFS = "history_cache_prefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)
        quizResultRepository = QuizResultRepository.getInstance(this)
        
        // Initialize the history cache
        val historyCache = getSharedPreferences(HISTORY_CACHE_PREFS, Context.MODE_PRIVATE)

        setContent {
            VocabularyBoosterTheme {
                val coroutineScope = rememberCoroutineScope()
                
                // Load bookmarked words from cache first
                var bookmarkedWordsSet by remember { 
                    val cachedBookmarksJson = historyCache.getString("bookmarkedWords", "[]")
                    val type = object : TypeToken<Set<String>>() {}.type
                    try {
                        mutableStateOf(Gson().fromJson<Set<String>>(cachedBookmarksJson, type) ?: emptySet())
                    } catch (e: Exception) {
                        mutableStateOf(emptySet<String>())
                    }
                }
                
                val context = LocalContext.current
                val view = LocalView.current
                
                // State for quiz history - load from cache first
                var historyQuestions by remember { 
                    val cachedHistoryJson = historyCache.getString("historyItems", "[]")
                    val type = object : TypeToken<List<QuizHistoryItem>>() {}.type
                    try {
                        mutableStateOf(Gson().fromJson<List<QuizHistoryItem>>(cachedHistoryJson, type) ?: emptyList())
                    } catch (e: Exception) {
                        mutableStateOf<List<QuizHistoryItem>>(emptyList())
                    }
                }
                var isLoading by remember { mutableStateOf(historyQuestions.isEmpty()) }
                var pageSize by remember { mutableStateOf(30) }
                
                fun lookupWord(wordText: String) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    try {
                        val dictionaryIntent = Intent(Intent.ACTION_VIEW)
                        dictionaryIntent.data = Uri.parse("dictionary:$wordText")
                        context.startActivity(dictionaryIntent)
                    } catch (e: ActivityNotFoundException) {
                        val searchIntent = Intent(Intent.ACTION_VIEW)
                        searchIntent.data = Uri.parse("https://www.google.com/search?q=define+$wordText")
                        context.startActivity(searchIntent)
                    }
                }

                // Load initial bookmark states
                LaunchedEffect(Unit) {
                    val words = wordRepository.getAllWords()
                    val freshBookmarkedWords = words.filter { it.isBookmarked }.map { it.word }.toSet()
                    bookmarkedWordsSet = freshBookmarkedWords
                    
                    // Cache the bookmarked words
                    val bookmarksJson = Gson().toJson(freshBookmarkedWords)
                    historyCache.edit()
                        .putString("bookmarkedWords", bookmarksJson)
                        .apply()
                }

                // Load quiz history
                LaunchedEffect(pageSize) {
                    isLoading = historyQuestions.isEmpty() // Only show loading if we have no cached data
                    try {
                        println("Loading quiz history, page size: $pageSize")
                        
                        // Get quiz history using the new dedicated method
                        val freshHistoryItems = quizResultRepository.getQuizHistory(pageSize)
                        
                        println("Retrieved ${freshHistoryItems.size} history items")
                        if (freshHistoryItems.isNotEmpty()) {
                            println("First few words: ${freshHistoryItems.take(5).map { it.word }}")
                            
                            // Update the UI
                            historyQuestions = freshHistoryItems
                            
                            // Cache the history items
                            val historyJson = Gson().toJson(freshHistoryItems)
                            historyCache.edit()
                                .putString("historyItems", historyJson)
                                .apply()
                        } else {
                            println("No history items found")
                        }
                    } catch (e: Exception) {
                        println("Error loading quiz history: ${e.message}")
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = "History",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCFCFC)
                                    )
                                ) 
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    finish() 
                                    TransitionUtils.applyStandardTransitionOnFinish(this@QuizHistoryActivity)
                                }) {
                                    Icon(
                                        painter = AppIcons.arrowLeft(),
                                        contentDescription = "Back",
                                        tint = Color(0xFFFCFCFC)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (historyQuestions.isEmpty()) {
                            Text(
                                text = "No quiz history available",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.White
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(historyQuestions) { historyItem ->
                                    val isBookmarked = bookmarkedWordsSet.contains(historyItem.word)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 2.dp,
                                                color = if (historyItem.isCorrect) {
                                                    Color(0xFF4CAF50) // Green border
                                                } else {
                                                    Color(0xFFF44336) // Red border
                                                },
                                                shape = MaterialTheme.shapes.medium
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF18191E)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = historyItem.word,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Color.White,
                                                    modifier = Modifier.pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onLongPress = { lookupWord(historyItem.word) }
                                                        )
                                                    }
                                                )
                                                Text(
                                                    text = historyItem.correctDefinition,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                                if (!historyItem.isCorrect) {
                                                    Text(
                                                        text = "Your answer: ${historyItem.userAnswer}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color(0xFFF44336) // Red text
                                                    )
                                                }
                                                Text(
                                                    text = if (historyItem.isCorrect) {
                                                        "Correct: ${historyItem.userAnswer}"
                                                    } else {
                                                        "Correct answer: ${historyItem.correctDefinition}"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (historyItem.isCorrect) {
                                                        Color(0xFF4CAF50) // Green text
                                                    } else {
                                                        Color.White.copy(alpha = 0.7f)
                                                    }
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val word = wordRepository.getAllWords().find { it.word == historyItem.word }
                                                        if (word != null) {
                                                            wordRepository.updateBookmark(word.id, !isBookmarked)
                                                            bookmarkedWordsSet = if (isBookmarked) {
                                                                bookmarkedWordsSet - historyItem.word
                                                            } else {
                                                                bookmarkedWordsSet + historyItem.word
                                                            }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painter = if (isBookmarked) AppIcons.bookmarkSolid() else AppIcons.bookmarkOutline(),
                                                    contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                                    tint = if (isBookmarked) MaterialTheme.colorScheme.tertiary else Color(0xFFFCFCFC)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Load more button
                                item {
                                    if (historyQuestions.size == pageSize) {
                                        // Add state for button animation
                                        var isLoadMorePressed by remember { mutableStateOf(false) }
                                        val loadMoreScale by animateFloatAsState(
                                            targetValue = if (isLoadMorePressed) 0.97f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.75f,
                                                stiffness = 300f
                                            )
                                        )
                                        
                                        Button(
                                            onClick = {
                                                pageSize += 30
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .graphicsLayer {
                                                    scaleX = loadMoreScale
                                                    scaleY = loadMoreScale
                                                }
                                                .animateContentSize(
                                                    animationSpec = spring(
                                                        dampingRatio = 0.8f,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                ),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF18191E)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = ButtonDefaults.buttonElevation(
                                                defaultElevation = 0.dp,
                                                pressedElevation = 0.dp
                                            ),
                                            interactionSource = remember { MutableInteractionSource() }
                                                .also { interactionSource ->
                                                    LaunchedEffect(interactionSource) {
                                                        interactionSource.interactions.collect { interaction ->
                                                            when (interaction) {
                                                                is PressInteraction.Press -> isLoadMorePressed = true
                                                                is PressInteraction.Release -> isLoadMorePressed = false
                                                                is PressInteraction.Cancel -> isLoadMorePressed = false
                                                            }
                                                        }
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = "Load More",
                                                color = Color(0xFFFCFCFC),
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        TransitionUtils.applyStandardTransitionOnFinish(this)
    }
} 