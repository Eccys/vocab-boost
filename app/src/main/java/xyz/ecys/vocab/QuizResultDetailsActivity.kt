package xyz.ecys.vocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.ecys.vocab.data.QuizQuestion
import xyz.ecys.vocab.data.QuizResult as FirestoreQuizResult
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class QuizResultDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Get the quiz result from the intent
        val quizResult = intent.getParcelableExtra<FirestoreQuizResult>("QUIZ_RESULT")
        
        setContent {
            VocabularyBoosterTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = "Quiz Results",
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
                    if (quizResult != null) {
                        QuizResultDetailsContent(
                            quizResult = quizResult,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        // Show error state if quiz result is null
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error loading quiz results",
                                color = Color(0xFFFCFCFC),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizResultDetailsContent(
    quizResult: FirestoreQuizResult,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(quizResult.timestamp)
    
    // Calculate score percentage
    val scorePercentage = (quizResult.correctAnswers.toFloat() / quizResult.totalQuestions * 100).toInt()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF18191E)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$scorePercentage%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        scorePercentage >= 80 -> Color(0xFF4CAF50) // Green for good scores
                        scorePercentage >= 60 -> Color(0xFFFFC107) // Yellow for okay scores
                        else -> Color(0xFFE57373) // Red for poor scores
                    }
                )
                
                Text(
                    text = "${quizResult.correctAnswers} of ${quizResult.totalQuestions} correct",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFCFCFC)
                )
                
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                if (quizResult.durationInSeconds > 0) {
                    val minutes = quizResult.durationInSeconds / 60
                    val seconds = quizResult.durationInSeconds % 60
                    Text(
                        text = "Time: ${if (minutes > 0) "$minutes min " else ""}$seconds sec",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // Questions list
        Text(
            text = "Questions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFCFCFC),
            modifier = Modifier.padding(top = 8.dp)
        )
        
        if (quizResult.questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No question details available",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quizResult.questions) { question ->
                    QuestionResultItem(question)
                }
                
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun QuestionResultItem(question: QuizQuestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (question.isCorrect) Color(0xFF1E3B29) else Color(0xFF3B1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Word
            Text(
                text = question.word,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFCFCFC)
            )
            
            Divider(
                color = Color(0x22FFFFFF),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            // Correct definition
            Text(
                text = "Correct definition:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Text(
                text = question.correctDefinition,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4CAF50)
            )
            
            // User's answer if incorrect
            if (!question.isCorrect) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Your answer:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Text(
                    text = question.userAnswer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE57373)
                )
            }
            
            // Status icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    painter = if (question.isCorrect) AppIcons.heartSolid() else AppIcons.heartCrackSolid(),
                    contentDescription = if (question.isCorrect) "Correct" else "Incorrect",
                    tint = if (question.isCorrect) Color(0xFF4CAF50) else Color(0xFFE57373),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 