package xyz.ecys.vocab

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.data.AppUsageManager
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.components.stats.*
import xyz.ecys.vocab.utils.FormatUtils.toSentenceCase
import java.util.Locale
import android.content.Context as AndroidContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
class StatsActivity : ComponentActivity() {
    private lateinit var wordRepository: WordRepository
    private lateinit var appUsageManager: AppUsageManager

    companion object {
        private const val DEFAULT_DAILY_GOAL = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordRepository = WordRepository.getInstance(this)
        appUsageManager = AppUsageManager.getInstance(this)

        setContent {
            VocabularyBoosterTheme {
                var totalReviewed by remember { mutableStateOf(0) }
                var timeSpentToday by remember { mutableStateOf(0L) }
                var totalTimeSpent by remember { mutableStateOf(0L) }
                var bestStreak by remember { mutableStateOf(0) }
                var currentStreak by remember { mutableStateOf(0) }
                var showingCurrentStreak by remember { mutableStateOf(false) }
                var masteredWords by remember { mutableStateOf<List<Word>>(emptyList()) }
                var toReviewWords by remember { mutableStateOf<List<Word>>(emptyList()) }
                var showResetDialog by remember { mutableStateOf(false) }
                var showMasteredWords by remember { mutableStateOf(false) }
                var showReviewWords by remember { mutableStateOf(false) }
                var showStudiedWords by remember { mutableStateOf(false) }
                var showWordsToday by remember { mutableStateOf(false) }
                var showWordInfo by remember { mutableStateOf<Word?>(null) }
                var showGoalDialog by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                // Search state
                var masteredSearchQuery by remember { mutableStateOf("") }
                var reviewSearchQuery by remember { mutableStateOf("") }
                var studiedSearchQuery by remember { mutableStateOf("") }
                val words by wordRepository.getAllWordsFlow().collectAsState(initial = emptyList())

                // Get daily goal from preferences
                val prefs = getSharedPreferences("vocab_settings", AndroidContext.MODE_PRIVATE)
                var dailyGoal by remember { 
                    mutableStateOf(prefs.getInt("daily_goal", DEFAULT_DAILY_GOAL))
                }
                var wordsToday by remember { mutableStateOf(0) }
                var goalInput by remember { mutableStateOf(dailyGoal.toString()) }

                // Add this variable to track correct words
                var correctWords = words.filter { it.timesCorrect > 0 }

                fun updateWordLists(wordList: List<Word>) {
                    masteredWords = wordList.filter { 
                        it.timesReviewed > 0 && it.timesCorrect.toFloat() / it.timesReviewed >= 0.9
                    }
                    toReviewWords = wordList.filter {
                        it.timesReviewed > 0 && it.timesCorrect.toFloat() / it.timesReviewed < 0.9
                    }
                }

                fun handleBookmarkClick(word: Word) {
                    coroutineScope.launch {
                        wordRepository.updateBookmark(word.id, !word.isBookmarked)
                    }
                }

                // Load statistics
                LaunchedEffect(words) {
                    totalReviewed = words.count { it.timesReviewed > 0 }
                    timeSpentToday = appUsageManager.getTimeSpentToday()
                    totalTimeSpent = appUsageManager.getTotalTimeSpent()
                    bestStreak = appUsageManager.getBestStreak()
                    wordsToday = words.count { word ->
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        word.lastReviewed >= today
                    }
                    updateWordLists(words)
                }

                // Load current streak independently
                LaunchedEffect(Unit) {
                    currentStreak = appUsageManager.getCurrentStreak()
                }

                // Dialogs
                GoalDialog(
                    showDialog = showGoalDialog,
                    goalInput = goalInput,
                    onGoalInputChange = { goalInput = it },
                    onDismiss = { showGoalDialog = false },
                    onSave = { newGoal ->
                        dailyGoal = newGoal
                        prefs.edit().putInt("daily_goal", dailyGoal).apply()
                    }
                )

                ResetDialog(
                    showDialog = showResetDialog,
                    onDismiss = { showResetDialog = false },
                    onConfirm = {
                        coroutineScope.launch {
                            wordRepository.resetAllStats()
                            snackbarHostState.showSnackbar("Learning progress has been reset")
                        }
                    }
                )

                WordListDialog(
                    words = masteredWords,
                    showDialog = showMasteredWords,
                    onDismiss = { showMasteredWords = false },
                    wordRepository = wordRepository,
                    coroutineScope = coroutineScope,
                    title = "Words Mastered"
                )

                WordListDialog(
                    words = toReviewWords,
                    showDialog = showReviewWords,
                    onDismiss = { showReviewWords = false },
                    wordRepository = wordRepository,
                    coroutineScope = coroutineScope,
                    title = "Words to Review"
                )

                WordListDialog(
                    words = words.filter { it.timesReviewed > 0 },
                    showDialog = showStudiedWords,
                    onDismiss = { showStudiedWords = false },
                    wordRepository = wordRepository,
                    coroutineScope = coroutineScope,
                    title = "Words Studied"
                )

                WordListDialog(
                    words = words.filter { word ->
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        word.lastReviewed >= today
                    },
                    showDialog = showWordsToday,
                    onDismiss = { showWordsToday = false },
                    wordRepository = wordRepository,
                    coroutineScope = coroutineScope,
                    title = "Today's Words"
                )

                WordInfoDialog(
                    word = showWordInfo,
                    onDismiss = { showWordInfo = null }
                )

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = "Statistics",
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
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    StatisticsContent(
                        totalReviewed = totalReviewed,
                        timeSpentToday = timeSpentToday,
                        totalTimeSpent = totalTimeSpent,
                        bestStreak = bestStreak,
                        currentStreak = currentStreak,
                        showingCurrentStreak = showingCurrentStreak,
                        onStreakClick = { 
                            showingCurrentStreak = !showingCurrentStreak
                            android.util.Log.d("StreakDebug", "Streak button clicked. Showing current streak: $showingCurrentStreak")
                            if (showingCurrentStreak) {
                                android.util.Log.d("StreakDebug", "Refreshing current streak...")
                                coroutineScope.launch {
                                    currentStreak = appUsageManager.getCurrentStreak()
                                    android.util.Log.d("StreakDebug", "Current streak refreshed to: $currentStreak")
                                }
                            }
                        },
                        masteredWords = masteredWords,
                        toReviewWords = toReviewWords,
                        wordsToday = wordsToday,
                        dailyGoal = dailyGoal,
                        words = words,
                        onGoalClick = { showGoalDialog = true },
                        onResetClick = { showResetDialog = true },
                        onStudiedClick = { showStudiedWords = true },
                        onMasteredClick = { showMasteredWords = true },
                        onReviewClick = { showReviewWords = true },
                        onWordsClick = { showWordsToday = true },
                        correctWords = correctWords,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBoxWithIcon(
    icon: Painter,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF18191E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFFFCFCFC)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatNumber(number: Int): String = when {
    number >= 1000000 -> "%.1fM".format(number / 1000000f)
    number >= 1000 -> "%.1fk".format(number / 1000f)
    else -> number.toString()
}

private fun formatTime(timeInMillis: Long): String {
    val seconds = timeInMillis / 1000
    val minutes = (seconds + 59) / 60  // Round up minutes
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    
    return when {
        hours > 0 -> "%.1fh".format(minutes / 60f)  // Show decimal hours
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "0s"
    }
}

private fun formatStreak(days: Int): String = when {
    days >= 365 -> "%.1fy".format(days / 365f)
    else -> "${days}d"
}

@Composable
private fun CompactWordCard(
    word: Word,
    onBookmarkClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .pointerInput(word.id) {
                detectTapGestures(
                    onLongPress = { lookupWord(word.word) },
                    onTap = { isExpanded = !isExpanded }
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
                    text = "${((word.timesCorrect.toFloat() / word.timesReviewed) * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = if (word.isBookmarked) AppIcons.bookmarkSolid() else AppIcons.bookmarkOutline(),
                        contentDescription = if (word.isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (word.isBookmarked) MaterialTheme.colorScheme.tertiary else Color(0xFFFCFCFC),
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCFCFC).copy(alpha = 0.7f),
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = word.synonym1ExampleSentence,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Reviewed ${word.timesReviewed} times",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
} 