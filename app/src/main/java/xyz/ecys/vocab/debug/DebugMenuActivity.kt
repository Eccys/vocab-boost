package xyz.ecys.vocab.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.ecys.vocab.FirestoreTestActivity
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.utils.TransitionUtils

@OptIn(ExperimentalMaterial3Api::class)
class DebugMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VocabularyBoosterTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Debug Menu") },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    finish()
                                    TransitionUtils.applyStandardTransitionOnFinish(this@DebugMenuActivity)
                                }) {
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
                    DebugMenuContent(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    @Composable
    fun DebugMenuContent(modifier: Modifier = Modifier) {
        val menuItems = listOf(
            DebugMenuItem("Machine Learning Debug", "View word stats for machine learning") { 
                startActivity(Intent(this, DebugActivity::class.java))
                TransitionUtils.applyStandardTransition(this@DebugMenuActivity)
            },
            DebugMenuItem("Firestore Test", "Test Firebase/Firestore optimization") {
                startActivity(Intent(this, FirestoreTestActivity::class.java))
                TransitionUtils.applyStandardTransition(this@DebugMenuActivity)
            }
        )
        
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(menuItems) { menuItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF18191E)
                    ),
                    onClick = menuItem.onClick
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = menuItem.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = menuItem.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

data class DebugMenuItem(
    val title: String,
    val description: String,
    val onClick: () -> Unit
) 