package xyz.ecys.vocab.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.SupabaseClient
import xyz.ecys.vocab.utils.MigrationUtils
import xyz.ecys.vocab.viewmodels.MigrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationScreen(
    onNavigateBack: () -> Unit,
    viewModel: MigrationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val isFirebaseSignedIn by viewModel.isFirebaseSignedIn.collectAsState()
    val isSupabaseSignedIn by viewModel.isSupabaseSignedIn.collectAsState()
    val migrationState by viewModel.migrationState.collectAsState()
    val isMigrating by viewModel.isMigrating.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.checkAuthStatus()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Migration to Supabase") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title and explanation
            Text(
                text = "Migrate Your Data",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            
            Text(
                text = "We're moving to a new database system to improve performance and reliability. " +
                       "This migration will transfer all your data to the new system.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
            
            // Prerequisites
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Prerequisites",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Firebase sign-in status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isFirebaseSignedIn) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isFirebaseSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Signed in to Firebase")
                        Spacer(modifier = Modifier.weight(1f))
                        if (!isFirebaseSignedIn) {
                            Button(
                                onClick = { 
                                    // Handle Firebase sign-in
                                    Toast.makeText(context, "Please sign in through the main login screen", Toast.LENGTH_LONG).show()
                                }
                            ) {
                                Text("Sign In")
                            }
                        }
                    }
                    
                    // Supabase sign-in status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSupabaseSignedIn) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isSupabaseSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Signed in to Supabase")
                        Spacer(modifier = Modifier.weight(1f))
                        if (!isSupabaseSignedIn) {
                            Button(
                                onClick = { viewModel.signInToSupabase() }
                            ) {
                                Text("Sign In")
                            }
                        }
                    }
                }
            }
            
            // Migration status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Migration Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // User data migration
                    MigrationStatusItem(
                        title = "User Profile",
                        isComplete = migrationState.userMigrated,
                        isInProgress = isMigrating
                    )
                    
                    // Words migration
                    MigrationStatusItem(
                        title = "Vocabulary Words",
                        isComplete = migrationState.wordsMigrated,
                        isInProgress = isMigrating && migrationState.userMigrated
                    )
                    
                    // App usage migration
                    MigrationStatusItem(
                        title = "App Usage Data",
                        isComplete = migrationState.appUsageMigrated,
                        isInProgress = isMigrating && migrationState.userMigrated && migrationState.wordsMigrated
                    )
                    
                    // Quiz results migration
                    MigrationStatusItem(
                        title = "Quiz Results",
                        isComplete = migrationState.quizResultsMigrated,
                        isInProgress = isMigrating && migrationState.userMigrated && migrationState.wordsMigrated && migrationState.appUsageMigrated
                    )
                    
                    // Quiz history migration
                    MigrationStatusItem(
                        title = "Quiz History",
                        isComplete = migrationState.quizHistoryMigrated,
                        isInProgress = isMigrating && migrationState.userMigrated && migrationState.wordsMigrated && migrationState.appUsageMigrated && migrationState.quizResultsMigrated
                    )
                    
                    // Transactions migration
                    MigrationStatusItem(
                        title = "Subscription Data",
                        isComplete = migrationState.transactionsMigrated,
                        isInProgress = isMigrating && migrationState.userMigrated && migrationState.wordsMigrated && migrationState.appUsageMigrated && migrationState.quizResultsMigrated && migrationState.quizHistoryMigrated
                    )
                }
            }
            
            // Migration button
            Button(
                onClick = {
                    scope.launch {
                        viewModel.startMigration()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = isFirebaseSignedIn && isSupabaseSignedIn && !isMigrating && !migrationState.success
            ) {
                Text("Start Migration")
            }
            
            // Migration completed message
            if (migrationState.success) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Migration Completed Successfully!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You can now continue using the app with your data on the new system.",
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Continue")
                }
            }
            
            // Migration message
            if (migrationState.message.isNotBlank() && !migrationState.success) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = migrationState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Loading indicator
            if (isMigrating) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Migrating your data...",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MigrationStatusItem(
    title: String,
    isComplete: Boolean,
    isInProgress: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (isComplete) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        } else if (isInProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(title)
        Spacer(modifier = Modifier.weight(1f))
        if (isComplete) {
            Text(
                text = "Completed",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        } else if (isInProgress) {
            Text(
                text = "In Progress",
                color = MaterialTheme.colorScheme.tertiary
            )
        } else {
            Text(
                text = "Pending",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 