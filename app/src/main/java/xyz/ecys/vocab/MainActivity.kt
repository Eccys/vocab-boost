@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.ecys.vocab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.ui.theme.AppIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.Instant
import java.time.ZoneOffset
import androidx.lifecycle.lifecycleScope
import xyz.ecys.vocab.data.WordDatabase
import xyz.ecys.vocab.data.AppUsage
import xyz.ecys.vocab.data.AppUsageManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.offset

data class CalendarDay(
    val date: LocalDate,
    var isActive: Boolean,
    val isToday: Boolean
) {
    val dayName: String = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2).uppercase()
}

class MainActivity : ComponentActivity() {
    private lateinit var wordDatabase: WordDatabase
    private lateinit var appUsageManager: AppUsageManager

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
                var dailyGoal by remember { 
                    mutableStateOf(prefs.getInt("daily_goal", 10))
                }
                var wordsToday by remember { mutableStateOf(0) }
                var goalInput by remember { mutableStateOf(dailyGoal.toString()) }
                var totalReviewed by remember { mutableStateOf(0) }
                var timeSpentToday by remember { mutableStateOf(0L) }
                var totalTimeSpent by remember { mutableStateOf(0L) }
                var bestStreak by remember { mutableStateOf(0) }

                // Load statistics
                LaunchedEffect(Unit) {
                    try {
                        wordsToday = appUsageManager.getCorrectAnswersToday()
                        totalReviewed = wordDatabase.wordDao().countWordsWithReviews()
                        timeSpentToday = appUsageManager.getTimeSpentToday()
                        totalTimeSpent = appUsageManager.getTotalTimeSpent()
                        bestStreak = appUsageManager.getBestStreak()
                    } catch (e: Exception) {
                        println("Error loading statistics: ${e.message}")
                        wordsToday = 0
                        totalReviewed = 0
                        timeSpentToday = 0
                        totalTimeSpent = 0
                        bestStreak = 0
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = "Vocabulary Booster",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCFCFC)
                                    )
                                ) 
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    startActivity(Intent(this@MainActivity, StatsActivity::class.java))
                                }) {
                                    Icon(
                                        painter = AppIcons.chartSimpleSolid(),
                                        contentDescription = "Statistics",
                                        tint = Color(0xFFFCFCFC)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                }) {
                                    Icon(
                                        painter = AppIcons.cogSolid(),
                                        contentDescription = "Settings",
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(0.5f))

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

                        // Play Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(
                                    color = Color(0xFFFCFCFC),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Gray
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.Start,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "$wordsToday/$dailyGoal",
                                                    style = MaterialTheme.typography.headlineLarge,
                                                    color = Color(0xFF1A1A1A),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                
                                                if (wordsToday < dailyGoal) {
                                                    val remainingWords = dailyGoal - wordsToday
                                                    val timeToComplete = remainingWords * 5L * 1000
                                                    Text(
                                                        text = "${formatTime(timeToComplete)} to complete",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                            
                                            Box(
                                                modifier = Modifier.size(64.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val strokeWidth = 10.dp.toPx()
                                                    val progress = (wordsToday.toFloat() / dailyGoal).coerceIn(0f, 1f)
                                                    
                                                    drawArc(
                                                        color = Color(0xFFE8E8E8),
                                                        startAngle = -90f,
                                                        sweepAngle = 360f,
                                                        useCenter = false,
                                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                                    )
                                                    
                                                    drawArc(
                                                        color = Color(0xFF1A1A1A),
                                                        startAngle = -90f,
                                                        sweepAngle = 360f * progress,
                                                        useCenter = false,
                                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                                    )
                                                }
                                                
                                                Box(
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${(wordsToday.toFloat() / dailyGoal * 100).toInt()}%",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = Color(0xFF1A1A1A),
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, QuizActivity::class.java))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1A1A1A)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            wordsToday == 0 -> "Play"
                                            wordsToday >= dailyGoal -> "Daily target achieved"
                                            else -> "Keep going"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                startActivity(Intent(this@MainActivity, BookmarksActivity::class.java))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Bookmarks",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

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