@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.ecys.vocab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.AppUsage
import xyz.ecys.vocab.data.AppUsageManager
import xyz.ecys.vocab.data.WordDatabase
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

data class CalendarDay(
    val date: LocalDate,
    var isActive: Boolean,
    val isToday: Boolean
) {
    val dayName: String = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2).uppercase()
}

class MainActivity : ComponentActivity() { // Calendar Card
    private lateinit var wordDatabase: WordDatabase
    private lateinit var appUsageManager: AppUsageManager

    // Add state variables at class level
    private var wordsToday = mutableStateOf(0)
    private var dailyGoal = mutableStateOf(20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wordDatabase = WordDatabase.getDatabase(this)
        appUsageManager = AppUsageManager.getInstance(this)

        // Start session
        appUsageManager.startSession()

        enableEdgeToEdge()
        setContent {
            VocabularyBoosterTheme {
                // Generate activity days
                val activityDays = remember {
                    mutableStateOf(mutableListOf<CalendarDay>()).apply {
                        val today = LocalDate.now()
                        val days = mutableListOf<CalendarDay>()
                        
                        // Past 15 days
                        for (i in 15 downTo 1) {
                            days.add(CalendarDay(
                                date = today.minusDays(i.toLong()),
                                isActive = false, // Will be updated from database
                                isToday = false
                            ))
                        }
                        
                        // Today
                        days.add(CalendarDay(
                            date = today,
                            isActive = true,
                            isToday = true
                        ))
                        
                        // Next 15 days
                        for (i in 1..15) {
                            days.add(CalendarDay(
                                date = today.plusDays(i.toLong()),
                                isActive = false,
                                isToday = false
                            ))
                        }
                        value = days
                    }
                }

                // Load activity data
                LaunchedEffect(Unit) {
                    try {
                        val startDate = activityDays.value.first().date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                        val endDate = activityDays.value.last().date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                        
                        wordDatabase.appUsageDao().getUsageBetweenDates(startDate, endDate)
                            .collect { usages ->
                                val usageDates = usages.map { usage ->
                                    LocalDate.ofInstant(
                                        Instant.ofEpochMilli(usage.date),
                                        ZoneOffset.UTC
                                    )
                                }.toSet()
                                
                                val updatedDays = activityDays.value.map { day ->
                                    day.copy(isActive = usageDates.contains(day.date) || day.isToday)
                                }
                                activityDays.value = updatedDays.toMutableList()
                            }
                    } catch (e: Exception) {
                        println("Error loading activity data: ${e.message}")
                    }
                }

                // Get daily goal from preferences
                val prefs = getSharedPreferences("vocab_settings", Context.MODE_PRIVATE)
                dailyGoal = remember { 
                    mutableStateOf(prefs.getInt("daily_goal", 20))
                }
                wordsToday = remember { mutableStateOf(0) }
                var goalInput by remember { mutableStateOf(dailyGoal.value.toString()) }
                var totalReviewed by remember { mutableStateOf(0) }
                var timeSpentToday by remember { mutableStateOf(0L) }
                var totalTimeSpent by remember { mutableStateOf(0L) }
                var bestStreak by remember { mutableStateOf(0) }

                // Load statistics
                LaunchedEffect(Unit) {
                    try {
                        updateStatistics()
                    } catch (e: Exception) {
                        println("Error loading statistics: ${e.message}")
                        wordsToday.value = 0
                        totalReviewed = 0
                        timeSpentToday = 0
                        totalTimeSpent = 0
                        bestStreak = 0
                    }
                }

                // Play Card
                var isPlayCardPressed by remember { mutableStateOf(false) }
                val playCardScale by animateFloatAsState(
                    targetValue = if (isPlayCardPressed) 0.97f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,    // More bounce
                        stiffness = 300f         // Slower, more natural movement
                    )
                )

                // Add these state variables alongside the playCardPressed state
                var isKeepGoingPressed by remember { mutableStateOf(false) }
                var isBookmarksPressed by remember { mutableStateOf(false) }

                // Add these animations alongside the playCardScale animation
                val keepGoingScale by animateFloatAsState(
                    targetValue = if (isKeepGoingPressed) 0.97f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 300f
                    )
                )

                val bookmarksScale by animateFloatAsState(
                    targetValue = if (isBookmarksPressed) 0.97f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 300f
                    )
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Vocabulary Booster",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCFCFC)
                                        )
                                    )
                                    Text(
                                        text = "Powered by AI",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCFCFC).copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            },
                            navigationIcon = {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val alpha by animateFloatAsState(
                                    targetValue = if (isPressed) 0.4f else 1f,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "iconAlpha"
                                )
                                IconButton(
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, StatsActivity::class.java))
                                    },
                                    modifier = Modifier.size(48.dp),
                                    interactionSource = interactionSource
                                ) {
                                    Icon(
                                        painter = AppIcons.chartSimpleSolid(),
                                        contentDescription = "Statistics",
                                        tint = Color(0xFFFCFCFC).copy(alpha = alpha)
                                    )
                                }
                            },
                            actions = {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val alpha by animateFloatAsState(
                                    targetValue = if (isPressed) 0.4f else 1f,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "iconAlpha"
                                )
                                IconButton(
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                    },
                                    modifier = Modifier.size(48.dp),
                                    interactionSource = interactionSource
                                ) {
                                    Icon(
                                        painter = AppIcons.cogSolid(),
                                        contentDescription = "Settings",
                                        tint = Color(0xFFFCFCFC).copy(alpha = alpha)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Activity Calendar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    color = Color(0xFF18191E),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                                val itemWidth = (screenWidth - 32.dp) / 5 - 4.dp  // Account for padding and spacing
                                val visibleWidth = screenWidth - 32.dp  // Total width minus horizontal padding
                                val scrollToCenter = remember {
                                    // Today is at index 15 (middle of 31 days)
                                    // We want today's card (index 15) in the center of 5 visible items
                                    // So we start 2 items before today: 15 - 2 = 13
                                    13
                                }
                                
                                LazyRow(
                                    userScrollEnabled = true,
                                    state = rememberLazyListState(
                                        initialFirstVisibleItemIndex = scrollToCenter
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(activityDays.value) { day ->
                                        Box(
                                            modifier = Modifier
                                                .width(itemWidth)
                                                .height(104.dp)
                                                .padding(vertical = 10.dp)
                                        ) {
                                            // Draw connection to next day if both are active
                                            val currentIndex = activityDays.value.indexOf(day)
                                            val nextDay = if (currentIndex < activityDays.value.size - 1) {
                                                activityDays.value[currentIndex + 1]
                                            } else null
                                            
                                            if (day.isActive && nextDay?.isActive == true) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .offset(x = (itemWidth/2), y = 15.dp)  // Moved down a bit more
                                                        .width(itemWidth + 4.dp)  // Extended to fully connect circles
                                                        .height(3.dp)
                                                        .background(
                                                            color = Color(0xFFFCFCFC)
                                                        )
                                                )
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        color = if (day.isToday) Color(0xFFF0F0F0)
                                                               else Color.Transparent,
                                                        shape = RoundedCornerShape(16.dp)
                                                    ),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = day.dayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (day.isToday) Color(0xFF1A1A1A)
                                                           else Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(
                                                            color = if (day.isToday) Color(0xFFF0F0F0)
                                                                   else if (day.isActive) Color(0xFFFCFCFC)
                                                                   else Color.Transparent,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = day.date.dayOfMonth.toString(),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (day.isToday) Color(0xFF1A1A1A)
                                                               else if (day.isActive) Color(0xFF1A1A1A)
                                                               else Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Play Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = playCardScale
                                    scaleY = playCardScale
                                }
                                .background(
                                    color = Color(0xFFFCFCFC),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() }
                                        .also { interactionSource ->
                                            LaunchedEffect(interactionSource) {
                                                interactionSource.interactions.collect { interaction ->
                                                    when (interaction) {
                                                        is PressInteraction.Press -> isPlayCardPressed = true
                                                        is PressInteraction.Release -> isPlayCardPressed = false
                                                        is PressInteraction.Cancel -> isPlayCardPressed = false
                                                    }
                                                }
                                            }
                                        },
                                    indication = null,  // Remove default ripple
                                    onClick = { }  // Empty click handler - just for the animation
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Row #1: Content
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Column: Text content
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Gray
                                        )
                                        
                                        Text(
                                            text = "${wordsToday.value}/${dailyGoal.value}",
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = Color(0xFF1A1A1A),
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Text(
                                            text = if (wordsToday.value >= dailyGoal.value) {
                                                "Daily target achieved"
                                            } else {
                                                val remainingWords = dailyGoal.value - wordsToday.value
                                                val timeToComplete = remainingWords * 5L * 1000
                                                "${formatTime(timeToComplete)} to complete"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    // Right Column: Progress Circle
                                    Box(
                                        modifier = Modifier.size(90.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val strokeWidth = 11.dp.toPx()
                                            val progress = (wordsToday.value.toFloat() / dailyGoal.value).coerceIn(0f, 1f)
                                            
                                            val arcSize = size.width - strokeWidth
                                            val topLeft = strokeWidth / 2
                                            
                                            drawArc(
                                                color = Color(0xFFE8E8E8),
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                                                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft)
                                            )
                                            
                                            drawArc(
                                                color = Color(0xFF1A1A1A),
                                                startAngle = -90f,
                                                sweepAngle = 360f * progress,
                                                useCenter = false,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                                                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft)
                                            )
                                        }
                                        
                                        Text(
                                            text = "${((wordsToday.value.toFloat() / dailyGoal.value) * 100).toInt()}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF1A1A1A),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                
                                // Row #2: Button - This is where the quiz should start
                                Button(
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, QuizActivity::class.java))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .graphicsLayer {
                                            scaleX = keepGoingScale
                                            scaleY = keepGoingScale
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1A1A1A)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                    ),
                                    interactionSource = remember { MutableInteractionSource() }
                                        .also { interactionSource ->
                                            LaunchedEffect(interactionSource) {
                                                interactionSource.interactions.collect { interaction ->
                                                    when (interaction) {
                                                        is PressInteraction.Press -> isKeepGoingPressed = true
                                                        is PressInteraction.Release -> isKeepGoingPressed = false
                                                        is PressInteraction.Cancel -> isKeepGoingPressed = false
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Text(
                                        text = when {
                                            wordsToday.value == 0 -> "Play"
                                            else -> "Keep going"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Bookmarks Button
                        Button(
                            onClick = {
                                startActivity(Intent(this@MainActivity, BookmarksActivity::class.java))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .graphicsLayer {
                                    scaleX = bookmarksScale
                                    scaleY = bookmarksScale
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF18191E)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            interactionSource = remember { MutableInteractionSource() }
                                .also { interactionSource ->
                                    LaunchedEffect(interactionSource) {
                                        interactionSource.interactions.collect { interaction ->
                                            when (interaction) {
                                                is PressInteraction.Press -> isBookmarksPressed = true
                                                is PressInteraction.Release -> isBookmarksPressed = false
                                                is PressInteraction.Cancel -> isBookmarksPressed = false
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = AppIcons.bookmarkSolid(),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFCFCFC)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bookmarks",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFCFCFC)
                                )
                            }
                        }
                        
                        // Add remaining space at the bottom
                        Spacer(modifier = Modifier.weight(1f))
                    }
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
        appUsageManager.startSession()
        
        // Update statistics when returning to the activity
        lifecycleScope.launch {
            updateStatistics()
        }
    }

    private suspend fun updateStatistics() {
        wordsToday.value = wordDatabase.wordDao().countWordsReviewedToday()
        val prefs = getSharedPreferences("vocab_settings", Context.MODE_PRIVATE)
        dailyGoal.value = prefs.getInt("daily_goal", 20)
    }
}

// Number formatting utilities
private fun formatNumber(number: Int): String = when {
    number >= 1000000 -> "%.1fM".format(number / 1000000f)
    number >= 1000 -> "%.1fk".format(number / 1000f)
    else -> number.toString()
}

private fun formatDuration(minutes: Long): String = when {
    minutes >= 1440 -> "%.1fh".format(minutes / 60f)  // More than 24 hours
    minutes >= 60 -> "%.1fh".format(minutes / 60f)    // More than 1 hour
    else -> "${minutes}m"
}

private fun formatStreak(days: Int): String = when {
    days >= 365 -> "%.1fy".format(days / 365f)
    else -> "${days}d"
}

private fun formatTime(timeInMillis: Long): String {
    val seconds = timeInMillis / 1000
    val minutes = (seconds + 59) / 60  // Round up minutes
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}min"
        else -> "${seconds}s"
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
            .height(100.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF18191E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}