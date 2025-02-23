package xyz.ecys.vocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.ui.theme.AppIcons
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.WordRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            VocabularyBoosterTheme {
                val context = LocalContext.current
                var showNeuralInfo by remember { mutableStateOf(false) }
                var showRefreshConfirmation by remember { mutableStateOf(false) }
                
                // Get current settings
                val prefs = context.getSharedPreferences("vocab_settings", Context.MODE_PRIVATE)
                var reducedAnimations by remember { 
                    mutableStateOf(prefs.getBoolean("reduced_animations", false))
                }
                var spacedRepetition by remember { 
                    mutableStateOf(prefs.getBoolean("spaced_repetition", true))
                }
                var neuralProcessing by remember { 
                    mutableStateOf(prefs.getBoolean("neural_processing", false))
                }

                if (showNeuralInfo) {
                    AlertDialog(
                        onDismissRequest = { showNeuralInfo = false },
                        title = { Text("Neural Processing") },
                        text = { 
                            Text(
                                """
                                Difficulty-based word selection uses an advanced algorithm to:
                                
                                • Track your success rate with each word
                                • Adjust difficulty based on performance
                                • Prioritize words you find challenging
                                • Optimize your learning path
                                
                                The system uses spaced repetition and performance metrics to create a personalized learning experience.
                                """.trimIndent()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showNeuralInfo = false }) {
                                Text("Got it")
                            }
                        }
                    )
                }

                if (showRefreshConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showRefreshConfirmation = false },
                        title = { Text("Refresh Database") },
                        text = { 
                            Text(
                                """
                                This will reset the database to its initial state with the default word set.
                                All learning progress and custom words will be lost.
                                
                                Are you sure you want to continue?
                                """.trimIndent()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { 
                                    showRefreshConfirmation = false
                                    lifecycleScope.launch {
                                        val wordRepository = WordRepository.getInstance(context)
                                        wordRepository.insertInitialWords()
                                    }
                                }
                            ) {
                                Text("Refresh", color = Color(0xFFED333B))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRefreshConfirmation = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = "Settings",
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Learning Settings Section
                        Text(
                            text = "Learning",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Spaced Repetition")
                            Switch(
                                checked = spacedRepetition,
                                onCheckedChange = { 
                                    spacedRepetition = it
                                    prefs.edit().putBoolean("spaced_repetition", it).apply()
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Neural Processing")
                                Icon(
                                    painter = AppIcons.circleInfoSolid(),
                                    contentDescription = "Info",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { showNeuralInfo = true },
                                    tint = Color(0xFFFCFCFC)
                                )
                            }
                            Switch(
                                checked = neuralProcessing,
                                onCheckedChange = { 
                                    neuralProcessing = it
                                    prefs.edit().putBoolean("neural_processing", it).apply()
                                }
                            )
                        }

                        // UI Settings Section
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "User Experience",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reduced Animations")
                            Switch(
                                checked = reducedAnimations,
                                onCheckedChange = { 
                                    reducedAnimations = it
                                    prefs.edit().putBoolean("reduced_animations", it).apply()
                                }
                            )
                        }

                        // Database Management Section
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Database",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Button(
                            onClick = { showRefreshConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF18191E),
                                contentColor = Color(0xFFFCFCFC)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Refresh Database")
                            }
                        }
                    }
                }
            }
        }
    }
} 