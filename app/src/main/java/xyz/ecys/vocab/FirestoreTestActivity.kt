package xyz.ecys.vocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.FirestoreAccessMonitor
import xyz.ecys.vocab.data.QuizQuestion
import xyz.ecys.vocab.data.QuizResult
import xyz.ecys.vocab.data.QuizResultRepository
import xyz.ecys.vocab.data.SyncManager
import xyz.ecys.vocab.data.SyncTestLogger
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.utils.TransitionUtils
import java.util.Date

/**
 * Test Activity to verify Firebase/Firestore optimization.
 * This activity allows you to:
 * 1. Reset test counter
 * 2. Simulate answering questions (saves to local cache)
 * 3. View history (should use local cache first)
 * 4. Force sync to Firestore
 * 5. See test stats
 * 6. Toggle Firestore access restrictions
 */
@OptIn(ExperimentalMaterial3Api::class)
class FirestoreTestActivity : ComponentActivity() {
    private lateinit var quizResultRepository: QuizResultRepository
    private lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repositories
        quizResultRepository = QuizResultRepository.getInstance(this)
        syncManager = SyncManager.getInstance(this)
        
        setContent {
            VocabularyBoosterTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Firestore Optimization Test") },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    finish()
                                    TransitionUtils.applyStandardTransitionOnFinish(this@FirestoreTestActivity)
                                }) {
                                    Icon(
                                        painter = AppIcons.arrowLeft(),
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    FirestoreTestContent(
                        modifier = Modifier.padding(innerPadding),
                        quizResultRepository = quizResultRepository,
                        syncManager = syncManager
                    )
                }
            }
        }
    }
    
    @Composable
    fun FirestoreTestContent(
        modifier: Modifier = Modifier,
        quizResultRepository: QuizResultRepository,
        syncManager: SyncManager
    ) {
        var stats by remember { mutableStateOf(SyncTestLogger.getStats()) }
        var monitorLog by remember { mutableStateOf("") }
        var accessRestricted by remember { mutableStateOf(true) }
        val scrollState = rememberScrollState()
        
        // Function to update the stats and log information
        fun updateStats() {
            stats = SyncTestLogger.getStats()
        }
        
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    SyncTestLogger.reset()
                    updateStats()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Test Counters")
            }
            
            Button(
                onClick = {
                    lifecycleScope.launch {
                        simulateQuestionAnswer()
                        updateStats()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simulate Answer Question")
            }
            
            Button(
                onClick = {
                    lifecycleScope.launch {
                        viewHistory(false)
                        updateStats()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View History (Use Cache)")
            }
            
            Button(
                onClick = {
                    lifecycleScope.launch {
                        viewHistory(true) 
                        updateStats()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Force History Refresh")
            }
            
            Button(
                onClick = {
                    syncManager.syncNow { success ->
                        updateStats()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Force Sync to Firestore")
            }
            
            // Access restriction toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Firestore Access Restrictions: ",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = accessRestricted,
                    onCheckedChange = { checked ->
                        accessRestricted = checked
                        FirestoreAccessMonitor.bypassRestrictions = !checked
                    }
                )
            }
            
            // Test unauthorized access
            Button(
                onClick = {
                    lifecycleScope.launch {
                        testUnauthorizedAccess()
                        updateStats()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Test Unauthorized Access")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stats,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    private suspend fun simulateQuestionAnswer() {
        try {
            // Create a mock quiz question
            val question = QuizQuestion(
                word = "test_word_${System.currentTimeMillis() % 100}",
                correctDefinition = "Test definition",
                userAnswer = "Test answer",
                isCorrect = true
            )
            
            // Create a quiz result with this question
            val quizResult = QuizResult(
                timestamp = Date(),
                correctAnswers = 1,
                totalQuestions = 1,
                questions = listOf(question),
                score = 1f,
                durationInSeconds = 3
            )
            
            // Save the result (should only save locally)
            quizResultRepository.saveQuizResult(quizResult)
            
        } catch (e: Exception) {
            android.util.Log.e("FirestoreTest", "Error simulating question answer", e)
        }
    }
    
    private suspend fun viewHistory(forceRefresh: Boolean) {
        try {
            quizResultRepository.getQuizHistory(forceRefresh = forceRefresh)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreTest", "Error viewing history", e)
        }
    }
    
    private suspend fun testUnauthorizedAccess() {
        try {
            // This should be blocked unless restrictions are turned off
            quizResultRepository.syncCachedResultsToFirestore(isExplicitSync = false)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreTest", "Error in unauthorized access test", e)
        }
    }
} 