package xyz.ecys.vocab

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.Parcelize
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.data.WordRepository
import kotlinx.coroutines.launch
import android.content.ActivityNotFoundException
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import xyz.ecys.vocab.data.QuizResult

@OptIn(ExperimentalMaterial3Api::class)
class QuizResultsActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)
        val results = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("results", QuizResult::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<QuizResult>("results") ?: emptyList()
        }

        setContent {
            VocabularyBoosterTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var bookmarkedWords by remember { mutableStateOf(emptySet<String>()) }
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val view = LocalView.current

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
                    bookmarkedWords = words.filter { it.isBookmarked }.map { it.word }.toSet()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = "Quiz Results",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCFCFC)
                                    )
                                ) 
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
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
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                startActivity(Intent(this@QuizResultsActivity, QuizActivity::class.java))
                                finish()
                            }
                        ) {
                            Icon(
                                painter = AppIcons.repeatSolid(),
                                contentDescription = "Try again",
                                tint = Color(0xFFFCFCFC)
                            )
                        }
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { result ->
                            val isBookmarked = bookmarkedWords.contains(result.word)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 2.dp,
                                        color = if (result.isCorrect) {
                                            Color(0xFF4CAF50) // Green border
                                        } else {
                                            Color(0xFFF44336) // Red border
                                        },
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF18191E) // Updated background color
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
                                            text = result.word,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            modifier = Modifier
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onLongPress = { lookupWord(result.word) }
                                                    )
                                                }
                                        )
                                        Text(
                                            text = result.definition,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        if (!result.isCorrect) {
                                            Text(
                                                text = "Your answer: ${result.userChoice}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFF44336) // Red text
                                            )
                                        }
                                        Text(
                                            text = if (result.isCorrect) {
                                                "Correct: ${result.correctChoice}"
                                            } else {
                                                "Correct answer: ${result.correctChoice}"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (result.isCorrect) {
                                                Color(0xFF4CAF50) // Green text
                                            } else {
                                                Color.White.copy(alpha = 0.7f)
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val word = wordRepository.getAllWords().find { it.word == result.word }
                                                if (word != null) {
                                                    wordRepository.updateBookmark(word.id, !isBookmarked)
                                                    bookmarkedWords = if (isBookmarked) {
                                                        bookmarkedWords - result.word
                                                    } else {
                                                        bookmarkedWords + result.word
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
                    }
                }
            }
        }
    }
} 