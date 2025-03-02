package xyz.ecys.vocab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import xyz.ecys.vocab.data.DailyWord
import xyz.ecys.vocab.data.DailyWordManager
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
class DailyWordActivity : ComponentActivity() {
    private lateinit var dailyWordManager: DailyWordManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the daily word manager
        dailyWordManager = DailyWordManager.getInstance(this)
        
        setContent {
            VocabularyBoosterTheme {
                var isDailyWordCardPressed by remember { mutableStateOf(false) }
                // State for currently loaded words
                var wordsWithDates by remember { mutableStateOf<List<Pair<DailyWord, String>>>(emptyList()) }
                // Loading state
                var isLoading by remember { mutableStateOf(true) }
                // Store the selected date index once we find it
                var selectedDateIndex by remember { mutableStateOf(0) }
                // Track when initialization is complete
                var isInitialized by remember { mutableStateOf(false) }
                // Number of days to preload
                val preloadDays = 5
                
                val dailyWordCardScale by animateFloatAsState(
                    targetValue = if (isDailyWordCardPressed) 0.97f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 300f
                    )
                )
                
                // Get the selected date from the intent
                val selectedDateStr = intent.getStringExtra("SELECTED_DATE")
                val selectedDate = remember {
                    if (selectedDateStr != null) {
                        try {
                            // Instead of parsing to LocalDate, we'll just keep the string
                            // and create a Calendar object which is compatible with API level 24
                            selectedDateStr
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
                
                // Create pager state with key-based initialization to ensure it's properly reset
                // when we finally know the selected index
                val pagerState = rememberPagerState(
                    initialPage = selectedDateIndex,
                    pageCount = { wordsWithDates.size }
                )
                
                // Launch effect to load words when the component is first created
                LaunchedEffect(selectedDate) {
                    withContext(Dispatchers.IO) {
                        // Load daily words for a date range
                        isLoading = true
                        val initialWords = mutableListOf<Pair<DailyWord, String>>()
                        
                        if (selectedDate != null) {
                            // Load specific date
                            val word = dailyWordManager.getWordForSpecificDate(selectedDate)
                            initialWords.add(Pair(word, selectedDate))
                        } else {
                            // Load date range (e.g., today and 6 days before)
                            val today = Calendar.getInstance().let {
                                val year = it.get(Calendar.YEAR)
                                val month = it.get(Calendar.MONTH) + 1 // Calendar months are 0-based
                                val day = it.get(Calendar.DAY_OF_MONTH)
                                String.format("%04d-%02d-%02d", year, month, day)
                            }
                            
                            // Add today's word first
                            val todayWord = dailyWordManager.getTodaysWord()
                            initialWords.add(Pair(todayWord, today))
                            
                            // Add previous days
                            val calendar = Calendar.getInstance()
                            
                            // Get the 6 days before today
                            for (i in 1..6) {
                                calendar.add(Calendar.DAY_OF_MONTH, -1)
                                val prevDay = String.format("%04d-%02d-%02d", 
                                    calendar.get(Calendar.YEAR), 
                                    calendar.get(Calendar.MONTH) + 1,
                                    calendar.get(Calendar.DAY_OF_MONTH))
                                
                                val prevWord = dailyWordManager.getWordForSpecificDate(prevDay)
                                initialWords.add(Pair(prevWord, prevDay))
                            }
                        }
                        
                        // Sort words by date (newest first)
                        initialWords.sortByDescending { it.second }
                        
                        // Now find the index of our selected date
                        var indexToSelect = 0
                        if (selectedDate != null) {
                            val foundIndex = initialWords.indexOfFirst { it.second == selectedDate }
                            if (foundIndex >= 0) {
                                indexToSelect = foundIndex
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            wordsWithDates = initialWords
                            selectedDateIndex = indexToSelect
                            isLoading = false
                            isInitialized = true
                        }
                    }
                }
                
                // Jump to the selected date page once data is loaded
                LaunchedEffect(isInitialized, selectedDateIndex) {
                    if (isInitialized && wordsWithDates.isNotEmpty()) {
                        // This is crucial - we need to explicitly scroll to the right page
                        pagerState.scrollToPage(selectedDateIndex)
                    }
                }
                
                // Preload more words when needed
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage >= wordsWithDates.size - 2 && !isLoading) {
                        withContext(Dispatchers.IO) {
                            isLoading = true
                            
                            val lastDate = if (wordsWithDates.isNotEmpty()) {
                                wordsWithDates.minByOrNull { it.second }?.second
                            } else {
                                Calendar.getInstance().let {
                                    val year = it.get(Calendar.YEAR)
                                    val month = it.get(Calendar.MONTH) + 1
                                    val day = it.get(Calendar.DAY_OF_MONTH)
                                    String.format("%04d-%02d-%02d", year, month, day)
                                }
                            }
                            
                            if (lastDate != null) {
                                val newWords = mutableListOf<Pair<DailyWord, String>>()
                                
                                // Load more words
                                for (i in 1..5) {
                                    // Parse the last date string
                                    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    val lastCalendar = Calendar.getInstance()
                                    try {
                                        val parsedDate = simpleDateFormat.parse(lastDate)
                                        if (parsedDate != null) {
                                            lastCalendar.time = parsedDate
                                            // Subtract days
                                            lastCalendar.add(Calendar.DAY_OF_MONTH, -i)
                                            // Format back to string
                                            val prevDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(lastCalendar.time)
                                            val word = dailyWordManager.getWordForSpecificDate(prevDateStr)
                                            newWords.add(Pair(word, prevDateStr))
                                        }
                                    } catch (e: Exception) {
                                        // Skip this date if parsing fails
                                    }
                                }
                                
                                withContext(Dispatchers.Main) {
                                    wordsWithDates = (wordsWithDates + newWords).sortedByDescending { it.second }
                                    isLoading = false
                                }
                            } else {
                                isLoading = false
                            }
                        }
                    }
                }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        // Title should always be "Word of the Day" now
                                        text = "Word of the Day",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCFCFC)
                                        )
                                    )
                                    Text(
                                        text = if (wordsWithDates.isNotEmpty() && pagerState.currentPage < wordsWithDates.size) {
                                            // Show the actual date for this word
                                            val dateStr = wordsWithDates[pagerState.currentPage].second
                                            try {
                                                // Format the date string as "Month Day, Year"
                                                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                                val date = format.parse(dateStr)
                                                if (date != null) {
                                                    SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)
                                                } else {
                                                    dateStr
                                                }
                                            } catch (e: Exception) {
                                                dateStr
                                            }
                                        } else {
                                            ""
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                                        )
                                    )
                                }
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
                    }
                ) { innerPadding ->
                    if (isLoading && wordsWithDates.isEmpty()) {
                        // Show loading indicator when first loading
                        Box(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFCFCFC)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Date indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (wordsWithDates.isNotEmpty() && pagerState.currentPage < wordsWithDates.size) {
                                        val currentDate = wordsWithDates[pagerState.currentPage].second
                                        val today = Calendar.getInstance().let {
                                            val year = it.get(Calendar.YEAR)
                                            val month = it.get(Calendar.MONTH) + 1
                                            val day = it.get(Calendar.DAY_OF_MONTH)
                                            String.format("%04d-%02d-%02d", year, month, day)
                                        }
                                        
                                        Text(
                                            text = if (currentDate == today) {
                                                "Today"
                                            } else {
                                                // Calculate days between dates using Calendar
                                                val daysBetween = try {
                                                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                                    val date1 = format.parse(today)
                                                    val date2 = format.parse(currentDate)
                                                    
                                                    if (date1 != null && date2 != null) {
                                                        val diffInMillis = date1.time - date2.time
                                                        val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)
                                                        diffInDays
                                                    } else {
                                                        0L
                                                    }
                                                } catch (e: Exception) {
                                                    0L
                                                }
                                                
                                                "$daysBetween ${if (daysBetween == 1L) "day" else "days"} ago"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                
                                // Pager
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.weight(1f),
                                    reverseLayout = true  // Keep reverseLayout true but make sure data order is consistent with it
                                ) { page ->
                                    if (page < wordsWithDates.size) {
                                        DailyWordScreen(
                                            modifier = Modifier.fillMaxSize(),
                                            dailyWord = wordsWithDates[page].first,
                                            onSeeMoreClick = { 
                                                // Launch the PreviousWordsActivity
                                                val intent = Intent(this@DailyWordActivity, PreviousWordsActivity::class.java)
                                                startActivity(intent)
                                            }
                                        )
                                    } else {
                                        // Loading placeholder
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color(0xFFFCFCFC)
                                            )
                                        }
                                    }
                                }
                                
                                // Loading indicator at bottom when loading more words
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color(0xFFFCFCFC)
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

@Composable
fun DailyWordScreen(
    modifier: Modifier = Modifier,
    dailyWord: DailyWord,
    onSeeMoreClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Word of the day card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF18191E)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dailyWord.word,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = Color(0xFFFCFCFC),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dailyWord.category.lowercase(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.Gray
                    )
                    Text(
                        text = " | ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Text(
                        text = dailyWord.pronunciation,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = Color.Gray
                    )
                }
            }
        }
        
        // What it means section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "What It Means",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFCFCFC)
            )
            
            Text(
                text = dailyWord.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFCFCFC)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "//",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dailyWord.example,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCFCFC),
                        modifier = Modifier.weight(1f)  // Allow text to take available width
                    )
                }
            }
        }
        
        // Word in context section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${dailyWord.word} in context",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFCFCFC)
            )
            
            Text(
                text = dailyWord.context,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFCFCFC)
            )
        }
        
        // Did you know section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Did You Know?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFCFCFC)
            )
            
            Text(
                text = dailyWord.did_you_know,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFCFCFC)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // See more button
        var isSeeMorePressed by remember { mutableStateOf(false) }
        val seeMoreScale by animateFloatAsState(
            targetValue = if (isSeeMorePressed) 0.97f else 1f,
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 300f
            )
        )
        
        Button(
            onClick = onSeeMoreClick,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = seeMoreScale
                    scaleY = seeMoreScale
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF18191E)
            ),
            shape = RoundedCornerShape(12.dp),
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> isSeeMorePressed = true
                                is PressInteraction.Release -> isSeeMorePressed = false
                                is PressInteraction.Cancel -> isSeeMorePressed = false
                            }
                        }
                    }
                }
        ) {
            Text(
                text = "See More Words of the Day",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFCFCFC),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
} 