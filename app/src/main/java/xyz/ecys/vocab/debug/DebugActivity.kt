package xyz.ecys.vocab.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.ui.theme.AppIcons
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
class DebugActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)

        setContent {
            VocabularyBoosterTheme {
                val words by wordRepository.getAllWordsFlow().collectAsState(initial = emptyList())
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                var isSearchVisible by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                val coroutineScope = rememberCoroutineScope()

                // Handle back press when search is visible
                BackHandler(enabled = isSearchVisible) {
                    isSearchVisible = false
                    searchQuery = ""
                }

                // Filter words based on search query
                val filteredWords = remember(words, searchQuery) {
                    if (searchQuery.isEmpty()) {
                        words
                    } else {
                        words.filter { it.word.contains(searchQuery, ignoreCase = true) }
                    }
                }

                Scaffold(
                    topBar = {
                        if (!isSearchVisible) {
                            CenterAlignedTopAppBar(
                                title = { 
                                    Text(
                                        text = "Machine Learning",
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
                                actions = {
                                    IconButton(onClick = { isSearchVisible = true }) {
                                        Icon(
                                            painter = AppIcons.magnifyingGlass(),
                                            contentDescription = "Search",
                                            tint = Color(0xFFFCFCFC)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        } else {
                            TopAppBar(
                                title = {
                                    var focusRequester = remember { FocusRequester() }
                                    
                                    LaunchedEffect(Unit) {
                                        focusRequester.requestFocus()
                                    }
                                    
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 8.dp)
                                            .focusRequester(focusRequester),
                                        placeholder = { Text("Search word") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = Color(0xFFFCFCFC).copy(alpha = 0.3f),
                                            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                            unfocusedLeadingIconColor = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { 
                                            isSearchVisible = false
                                            searchQuery = ""
                                        }
                                    ) {
                                        Icon(
                                            painter = AppIcons.arrowLeft(),
                                            contentDescription = "Close search",
                                            tint = Color(0xFFFCFCFC)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
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
                        items(filteredWords) { word ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF18191E)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = word.word.lowercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = Color(0xFFFCFCFC)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    wordRepository.updateBookmark(word.id, !word.isBookmarked)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                            painter = if (word.isBookmarked) AppIcons.bookmarkSolid() else AppIcons.bookmarkOutline(),
                                            contentDescription = if (word.isBookmarked) "Remove bookmark" else "Add bookmark",
                                            tint = if (word.isBookmarked) MaterialTheme.colorScheme.tertiary else Color(0xFFFCFCFC)
                                            )
                                        }
                                    }
                                    // Performance Stats
                                    Text(
                                        "Performance",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        """
                                        Times Reviewed: ${word.timesReviewed}
                                        Times Correct: ${word.timesCorrect}
                                        Success Rate: ${if (word.timesReviewed > 0) 
                                            "%.1f%%".format(word.timesCorrect.toFloat() / word.timesReviewed * 100)
                                        else "N/A"}
                                        Quality: ${word.quality}
                                        """.trimIndent(),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Learning Algorithm Stats
                                    Text(
                                        "Algorithm",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        """
                                        Ease Factor: ${String.format("%.2f", word.easeFactor)}
                                        Interval: ${word.interval} days
                                        Repetition Count: ${word.repetitionCount}
                                        """.trimIndent(),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Timing Stats
                                    Text(
                                        "Schedule",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        """
                                        Last Reviewed: ${if (word.lastReviewed > 0) dateFormat.format(Date(word.lastReviewed)) else "Never"}
                                        Next Review: ${if (word.nextReviewDate > 0) dateFormat.format(Date(word.nextReviewDate)) else "Not set"}
                                        """.trimIndent(),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                                        )
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