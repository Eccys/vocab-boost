package xyz.ecys.vocab.quiz

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.ui.theme.AppIcons
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizTopBar(
    onBackClick: () -> Unit,
    currentWord: Word?,
    onBookmarkClick: (Word) -> Unit,
    hintUsedForCurrentQuestion: Boolean,
    selectedAnswer: String?,
    onHintClick: () -> Unit
) {
    android.util.Log.d("HintDebug", "QuizTopBar recomposed with hintUsedForCurrentQuestion=$hintUsedForCurrentQuestion, selectedAnswer=$selectedAnswer")
    
    // Debug hint button state
    val hintButtonEnabled = !hintUsedForCurrentQuestion && selectedAnswer == null
    android.util.Log.e("CRITICAL_HINT", "=== QUIZ TOP BAR RECOMPOSED ===")
    android.util.Log.e("CRITICAL_HINT", "Hint button enabled: $hintButtonEnabled")
    android.util.Log.e("CRITICAL_HINT", "hintUsedForCurrentQuestion: $hintUsedForCurrentQuestion")
    android.util.Log.e("CRITICAL_HINT", "selectedAnswer: $selectedAnswer")
    android.util.Log.e("CRITICAL_HINT", "currentWord: ${currentWord?.id}")
    
    TopAppBar(
        title = { 
            Text(
                text = "Synonyms",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCFCFC)
                )
            ) 
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = AppIcons.arrowLeft(),
                    contentDescription = "Back",
                    tint = Color(0xFFFCFCFC)
                )
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentWord != null) {
                    IconButton(
                        onClick = { 
                            onHintClick() 
                        },
                        enabled = !hintUsedForCurrentQuestion && selectedAnswer == null
                    ) {
                        Icon(
                            painter = AppIcons.lightbulbSolid(),
                            contentDescription = "Show hint",
                            tint = if (!hintUsedForCurrentQuestion && selectedAnswer == null)
                                Color(0xFFFDC500)
                            else
                                Color(0xFFFCFCFC).copy(alpha = 0.3f)
                        )
                    }
                    
                    IconButton(
                        onClick = { onBookmarkClick(currentWord) }
                    ) {
                        Icon(
                            painter = if (currentWord.isBookmarked) AppIcons.bookmarkSolid() else AppIcons.bookmarkOutline(),
                            contentDescription = if (currentWord.isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (currentWord.isBookmarked) MaterialTheme.colorScheme.tertiary else Color(0xFFFCFCFC)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
} 