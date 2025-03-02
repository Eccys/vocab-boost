package xyz.ecys.vocab.ui.components.stats

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.utils.FormatUtils.toSentenceCase
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun CompactWordCard(
    word: Word,
    expandedWordId: Int?,
    onExpandedWordIdChange: (Int?) -> Unit,
    onBookmarkClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isExpanded = expandedWordId == word.id
    val interactionSource = remember { MutableInteractionSource() }

    val successRate = if (word.timesReviewed > 0) {
        ((word.timesCorrect.toFloat() / word.timesReviewed) * 100).toInt()
    } else {
        0
    }

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

    // Custom colors for CompactWordCard
    val cardBackground = Color(0xFF19181E)  // Specific card background color
    val textColor = Color(0xFFFCFCFC)       // White text
    val dimmedText = textColor.copy(alpha = 0.7f)


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
            containerColor = cardBackground
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
                    modifier = Modifier.weight(1f),
                    color = textColor  // Using custom text color
                )
                Text(
                    text = "$successRate%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = if (word.isBookmarked) AppIcons.bookmarkSolid() else AppIcons.bookmarkOutline(),
                        contentDescription = if (word.isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (word.isBookmarked) MaterialTheme.colorScheme.tertiary else textColor,
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
