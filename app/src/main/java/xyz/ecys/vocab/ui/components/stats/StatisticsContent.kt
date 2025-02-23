package xyz.ecys.vocab.ui.components.stats

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.utils.FormatUtils.formatNumber
import xyz.ecys.vocab.utils.FormatUtils.formatTime
import xyz.ecys.vocab.utils.FormatUtils.formatStreak
import androidx.compose.foundation.interaction.MutableInteractionSource
import java.util.*
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView

@Composable
fun StatisticsContent(
    totalReviewed: Int,
    timeSpentToday: Long,
    totalTimeSpent: Long,
    bestStreak: Int,
    currentStreak: Int,
    showingCurrentStreak: Boolean,
    onStreakClick: () -> Unit,
    masteredWords: List<Word>,
    toReviewWords: List<Word>,
    wordsToday: Int,
    dailyGoal: Int,
    words: List<Word>,
    onGoalClick: () -> Unit,
    onResetClick: () -> Unit,
    onStudiedClick: () -> Unit,
    onMasteredClick: () -> Unit,
    onReviewClick: () -> Unit,
    onWordsClick: () -> Unit,
    correctWords: List<Word>,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .then(modifier),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Daily Goal Progress Circle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { 
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onWordsClick()
                            },
                            onLongPress = { 
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onGoalClick()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = (wordsToday.toFloat() / dailyGoal).coerceIn(0f, 1f),
                    modifier = Modifier.size(200.dp),
                    strokeWidth = 12.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$wordsToday/$dailyGoal",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Daily Goal",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBoxWithIcon(
                    icon = AppIcons.boltSolid(),
                    value = formatNumber(totalReviewed),
                    label = "Words Studied",
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onStudiedClick)
                )
                StatBoxWithIcon(
                    icon = AppIcons.stopwatchSolid(),
                    value = formatTime(totalTimeSpent),
                    label = "Time Spent",
                    modifier = Modifier.weight(1f)
                )
                StatBoxWithIcon(
                    icon = AppIcons.medalSolid(),
                    value = if (showingCurrentStreak) formatStreak(currentStreak) else formatStreak(bestStreak),
                    label = if (showingCurrentStreak) "Current Streak" else "Best Streak",
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onStreakClick)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WordListCard(
                    count = masteredWords.size,
                    label = "Mastered",
                    words = masteredWords,
                    onClick = onMasteredClick,
                    modifier = Modifier.weight(1f)
                )
                WordListCard(
                    count = wordsToday,
                    label = "Today",
                    words = words.filter { word ->
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        word.lastReviewed >= today
                    },
                    onClick = onWordsClick,
                    modifier = Modifier.weight(1f)
                )
                WordListCard(
                    count = toReviewWords.size,
                    label = "To Review",
                    words = toReviewWords,
                    onClick = onReviewClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onResetClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF44336)
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFF44336).copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Learning Progress")
            }
        }
    }
} 