package xyz.ecys.vocab.ui.components.stats

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.activity.compose.BackHandler
import xyz.ecys.vocab.data.Word
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.ui.components.EmptyStateNotification
import xyz.ecys.vocab.ui.components.stats.CompactWordCard
import xyz.ecys.vocab.ui.theme.AppIcons
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.background
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

@Composable
fun GoalDialog(
    showDialog: Boolean,
    goalInput: String,
    onGoalInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set Daily Goal") },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { onGoalInputChange(it.filter { char -> char.isDigit() }) },
                    label = { Text("Words per day") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(goalInput.toIntOrNull() ?: 20)
                        onDismiss()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ResetDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Reset Progress") },
            text = { Text("This will reset all learning statistics. Your bookmarks will be preserved. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListDialog(
    words: List<Word>,
    showDialog: Boolean,
    onDismiss: () -> Unit,
    wordRepository: WordRepository,
    coroutineScope: CoroutineScope,
    title: String
) {
    if (showDialog) {
        var expandedWordId by remember { mutableStateOf<Int?>(null) }
        var isSearchVisible by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        BackHandler(enabled = isSearchVisible) {
            isSearchVisible = false
            searchQuery = ""
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.background(Color(0xFF05080D), shape = RoundedCornerShape(28.dp)),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = Color(0xFF05080D),
            titleContentColor = Color(0xFFFCFCFC),
            textContentColor = Color(0xFFFCFCFC),
            title = {
                Box(
                    modifier = Modifier.height(56.dp)
                ) {
                    if (!isSearchVisible) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title)
                            IconButton(
                                onClick = { isSearchVisible = true }
                            ) {
                                Icon(
                                    painter = AppIcons.magnifyingGlass(),
                                    contentDescription = "Search",
                                    tint = Color(0xFFFCFCFC)
                                )
                            }
                        }
                    } else {
                        val focusRequester = remember { FocusRequester() }
                        
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    isSearchVisible = false
                                    searchQuery = ""
                                }
                            ) {
                                Icon(
                                    painter = AppIcons.arrowLeft(),
                                    contentDescription = "Back",
                                    tint = Color(0xFFFCFCFC)
                                )
                            }
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                placeholder = { Text("Search") },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                painter = AppIcons.xSolid(),
                                                contentDescription = "Clear search",
                                                tint = Color(0xFFFCFCFC).copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFFFCFCFC),
                                    unfocusedTextColor = Color(0xFFFCFCFC),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFFFCFCFC).copy(alpha = 0.3f),
                                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLeadingIconColor = Color(0xFFFCFCFC).copy(alpha = 0.7f),
                                    focusedTrailingIconColor = Color(0xFFFCFCFC).copy(alpha = 0.7f),
                                    unfocusedTrailingIconColor = Color(0xFFFCFCFC).copy(alpha = 0.7f),
                                    focusedPlaceholderColor = Color(0xFFFCFCFC).copy(alpha = 0.5f),
                                    unfocusedPlaceholderColor = Color(0xFFFCFCFC).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            },
            text = {
                val filteredWords = words.filter { word ->
                    if (searchQuery.isEmpty()) true
                    else {
                        word.word.contains(searchQuery, ignoreCase = true) ||
                        word.synonym1.contains(searchQuery, ignoreCase = true) ||
                        word.synonym2.contains(searchQuery, ignoreCase = true) ||
                        word.synonym3.contains(searchQuery, ignoreCase = true) ||
                        word.definition.contains(searchQuery, ignoreCase = true) ||
                        word.synonym1ExampleSentence.contains(searchQuery, ignoreCase = true) ||
                        word.synonym2ExampleSentence.contains(searchQuery, ignoreCase = true) ||
                        word.synonym3ExampleSentence.contains(searchQuery, ignoreCase = true)
                    }
                }.sortedWith(
                    if (searchQuery.isEmpty()) {
                        compareBy { it.word.lowercase() }
                    } else {
                        compareBy<Word> { !it.word.contains(searchQuery, ignoreCase = true) }
                            .thenBy { !it.synonym1.contains(searchQuery, ignoreCase = true) }
                            .thenBy { !it.synonym2.contains(searchQuery, ignoreCase = true) }
                            .thenBy { !it.synonym3.contains(searchQuery, ignoreCase = true) }
                            .thenBy { !it.definition.contains(searchQuery, ignoreCase = true) }
                            .thenBy { !it.synonym1ExampleSentence.contains(searchQuery, ignoreCase = true) }
                            .thenBy { !it.synonym2ExampleSentence.contains(searchQuery, ignoreCase = true) }
                            .thenBy { !it.synonym3ExampleSentence.contains(searchQuery, ignoreCase = true) }
                            .thenBy { it.word.lowercase() }
                    }
                )

                if (filteredWords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateNotification(
                            message = if (searchQuery.isEmpty()) {
                                "No words available"
                            } else {
                                "No words found matching '${searchQuery}'"
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        items(filteredWords) { word ->
                            CompactWordCard(
                                word = word,
                                expandedWordId = expandedWordId,
                                onExpandedWordIdChange = { expandedWordId = it },
                                onBookmarkClick = {
                                    coroutineScope.launch {
                                        wordRepository.updateBookmark(word.id, !word.isBookmarked)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun WordInfoDialog(
    word: Word?,
    onDismiss: () -> Unit
) {
    if (word != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(word.word) },
            text = { Text(word.definition) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
} 