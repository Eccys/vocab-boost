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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTopBar(
    onBackClick: () -> Unit,
    currentWord: Word?,
    onBookmarkClick: (Word) -> Unit,
    lives: Int,
    hintsRemaining: Int = 0,
    hintUsedForCurrentQuestion: Boolean = false,
    selectedAnswer: String? = null,
    onHintClick: () -> Unit = {}
) {
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
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Icon(
                        painter = if (index < lives) AppIcons.heartSolid() else AppIcons.heartCrackSolid(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFED333B)
                    )
                }
                if (currentWord != null) {
                    if (hintsRemaining > 0) {
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = { onHintClick() },
                                enabled = hintsRemaining > 0 && !hintUsedForCurrentQuestion && selectedAnswer == null
                            ) {
                                Icon(
                                    painter = AppIcons.lightbulbSolid(),
                                    contentDescription = "Show hint",
                                    tint = if (hintsRemaining > 0 && !hintUsedForCurrentQuestion && selectedAnswer == null) 
                                        Color(0xFFFFC107) else Color.Gray
                                )
                            }
                            
                            Text(
                                text = hintsRemaining.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color.Black,
                                modifier = Modifier.offset(x = 0.dp, y = 1.dp),
                                textAlign = TextAlign.Center
                            )
                        }
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