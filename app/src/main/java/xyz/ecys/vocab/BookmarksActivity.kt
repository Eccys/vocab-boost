package xyz.ecys.vocab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.ui.theme.AppIcons
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import xyz.ecys.vocab.ui.theme.AppAnimations
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import xyz.ecys.vocab.ui.components.EmptyStateNotification
import xyz.ecys.vocab.ui.components.WordTooltip
import android.content.ActivityNotFoundException
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.activity.compose.BackHandler
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.clickable
import java.util.*

fun toSentenceCase(text: String): String {
    return text.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class)
class BookmarksActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)

        setContent {
            VocabularyBoosterTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val bookmarkedWords by wordRepository.getBookmarkedWordsFlow().collectAsState(initial = emptyList<Word>())
                val scope = rememberCoroutineScope()
                var isSearchVisible by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var expandedWordId by remember { mutableStateOf<Int?>(null) }
                
                // Handle back press when search is visible
                BackHandler(enabled = isSearchVisible) {
                    isSearchVisible = false
                    searchQuery = ""
                }
                
                // Store initial bookmarked words to maintain visibility
                var initialBookmarkedWords by remember { mutableStateOf(bookmarkedWords) }
                
                // Update initial bookmarked words when bookmarkedWords changes
                LaunchedEffect(bookmarkedWords) {
                    if (initialBookmarkedWords.isEmpty()) {
                        initialBookmarkedWords = bookmarkedWords
                    }
                }

                // Custom colors for CompactWordCard
                val cardBackground = Color(0xFF19181E)  // Specific card background color
                val textColor = Color(0xFFFCFCFC)       // White text
                val dimmedText = textColor.copy(alpha = 0.7f)
                val Success = MaterialTheme.colorScheme.primary
                val Error = MaterialTheme.colorScheme.error

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (!isSearchVisible) {
                            CenterAlignedTopAppBar(
                                title = { 
                                    Text(
                                        text = "Bookmarked Words",
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
                                            contentDescription = "Back"
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
                                        placeholder = { Text("Search") },
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
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (initialBookmarkedWords.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Bookmark some words first before testing",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    startActivity(
                                        Intent(this@BookmarksActivity, QuizActivity::class.java)
                                            .putExtra("mode", "bookmarks")
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = AppIcons.boltSolid(),
                                contentDescription = "Test bookmarks"
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    BookmarksScreen(
                        modifier = Modifier.padding(innerPadding),
                        wordRepository = wordRepository,
                        searchQuery = searchQuery,
                        isSearchVisible = isSearchVisible,
                        initialBookmarkedWords = initialBookmarkedWords,
                        expandedWordId = expandedWordId,
                        onExpandedWordIdChange = { expandedWordId = it }
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksScreen(
    modifier: Modifier = Modifier,
    wordRepository: WordRepository,
    searchQuery: String,
    isSearchVisible: Boolean,
    initialBookmarkedWords: List<Word>,
    expandedWordId: Int?,
    onExpandedWordIdChange: (Int?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bookmarkedWords by wordRepository.getBookmarkedWordsFlow().collectAsState(initial = emptyList<Word>())
    var displayedWords by remember { mutableStateOf(emptyList<Word>()) }
    var expandedTooltipWordId by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val view = LocalView.current

    // Custom colors for CompactWordCard
    val cardBackground = Color(0xFF19181E)  // Specific card background color
    val textColor = Color(0xFFFCFCFC)       // White text
    val dimmedText = textColor.copy(alpha = 0.7f)
    val Success = MaterialTheme.colorScheme.primary
    val Error = MaterialTheme.colorScheme.error

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

    // Update displayed words when bookmarked words or search query changes
    LaunchedEffect(initialBookmarkedWords, searchQuery) {
        displayedWords = if (searchQuery.isEmpty()) {
            initialBookmarkedWords.sortedBy { it.word.lowercase() }
        } else {
            initialBookmarkedWords.filter { word ->
                word.word.contains(searchQuery, ignoreCase = true) ||
                word.synonym1.contains(searchQuery, ignoreCase = true) ||
                word.synonym2.contains(searchQuery, ignoreCase = true) ||
                word.synonym3.contains(searchQuery, ignoreCase = true) ||
                word.definition.contains(searchQuery, ignoreCase = true)
            }.sortedWith(
                compareBy<Word> { !it.word.contains(searchQuery, ignoreCase = true) }
                    .thenBy { !it.synonym1.contains(searchQuery, ignoreCase = true) }
                    .thenBy { !it.synonym2.contains(searchQuery, ignoreCase = true) }
                    .thenBy { !it.synonym3.contains(searchQuery, ignoreCase = true) }
                    .thenBy { !it.definition.contains(searchQuery, ignoreCase = true) }
                    .thenBy { it.word.lowercase() }
            )
        }
    }

    if (initialBookmarkedWords.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateNotification(
                message = "No bookmarked words yet",
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05080D))  // Dialog/screen background color
    ) {
        if (displayedWords.isEmpty() && searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateNotification(
                    message = "No words found matching '${searchQuery}'"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = displayedWords,
                    key = { it.id }
                ) { word ->
                    val isBookmarked = bookmarkedWords.any { it.id == word.id }
                    val isExpanded = expandedWordId == word.id
                    val successRate = if (word.timesReviewed > 0) {
                        ((word.timesCorrect.toFloat() / word.timesReviewed) * 100).toInt()
                    } else {
                        0
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            .pointerInput(word.id, expandedWordId) {
                                detectTapGestures(
                                    onLongPress = { lookupWord(word.word) },
                                    onTap = { onExpandedWordIdChange(if (isExpanded) null else word.id) }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF18191E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$successRate%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            wordRepository.updateBookmark(word.id, !isBookmarked)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        painter = if (isBookmarked) AppIcons.bookmarkSolid() else AppIcons.bookmarkOutline(),
                                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.tertiary else Color(0xFFFCFCFC),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = toSentenceCase(word.definition),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dimmedText,
                                    )
                                    Text(
                                        text = word.synonym1ExampleSentence,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dimmedText
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