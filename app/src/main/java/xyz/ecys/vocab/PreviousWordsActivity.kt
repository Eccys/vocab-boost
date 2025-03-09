package xyz.ecys.vocab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.ecys.vocab.data.DailyWord
import xyz.ecys.vocab.data.DailyWordManager
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.animation.animateContentSize

@OptIn(ExperimentalMaterial3Api::class)
class PreviousWordsActivity : ComponentActivity() {
    private lateinit var dailyWordManager: DailyWordManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize word manager
        dailyWordManager = DailyWordManager.getInstance(this)
        
        setContent {
            VocabularyBoosterTheme {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = "Previous Words of the Day",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCFCFC)
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
                    }
                ) { innerPadding ->
                    WordsListContainer(
                        dailyWordManager = dailyWordManager,
                        modifier = Modifier.padding(innerPadding),
                        onWordClick = { _, date ->
                            // Launch DailyWordActivity with the specific date
                            val intent = Intent(this, DailyWordActivity::class.java)
                            // Pass the date as a string rather than just an index
                            intent.putExtra("SELECTED_DATE", date.toString())
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WordsListContainer(
    dailyWordManager: DailyWordManager,
    modifier: Modifier = Modifier,
    onWordClick: (Int, LocalDate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var wordsWithDates by remember { mutableStateOf<List<Pair<DailyWord, LocalDate>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var daysToLoad by remember { mutableStateOf(30) } // Initial load of 30 days
    var hasMoreWords by remember { mutableStateOf(true) }
    
    // Load words asynchronously
    LaunchedEffect(daysToLoad) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val newWordsWithDates = mutableListOf<Pair<DailyWord, LocalDate>>()
            
            // Load words for the past N days
            for (i in 1..daysToLoad) {
                val date = today.minusDays(i.toLong())
                val word = dailyWordManager.getWordForSpecificDate(date)
                newWordsWithDates.add(Pair(word, date))
            }
            
            // Sort newest first
            newWordsWithDates.sortByDescending { it.second }
            
            wordsWithDates = newWordsWithDates
            
            // Check if we potentially have more words to load
            // We'll assume if we're under 365 days, there might be more
            hasMoreWords = daysToLoad < 365
            
            isLoading = false
        }
    }
    
    if (isLoading && wordsWithDates.isEmpty()) {
        // Show loading indicator only on initial load
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFFFCFCFC)
            )
        }
    } else if (wordsWithDates.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No previous words yet.\nCheck back tomorrow!",
                color = Color(0xFFFCFCFC),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(wordsWithDates.withIndex().toList()) { (index, pair) ->
                val (word, date) = pair
                WordItem(
                    word = word,
                    date = date,
                    onClick = { onWordClick(index, date) }
                )
            }
            
            item {
                if (hasMoreWords) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFCFCFC),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
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
                                // Load 30 more words
                                coroutineScope.launch {
                                    daysToLoad += 30
                                }
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
                                text = "Load More Words",
                                color = Color(0xFFFCFCFC),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun WordItem(
    word: DailyWord,
    date: LocalDate,
    onClick: (LocalDate) -> Unit
) {
    // Add press state tracking and animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 300f
        )
    )

    // Use Button component like in MainActivity
    Button(
        onClick = { onClick(date) },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF18191E)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        contentPadding = PaddingValues(16.dp),
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> isPressed = true
                            is PressInteraction.Release -> isPressed = false
                            is PressInteraction.Cancel -> isPressed = false
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCFCFC)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = word.category.lowercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    // Add a date separator
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    // Add the date
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Icon(
                painter = AppIcons.arrowRight(),
                contentDescription = "View word",
                tint = Color(0xFFFCFCFC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
} 