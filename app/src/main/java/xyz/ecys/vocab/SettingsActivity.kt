package xyz.ecys.vocab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.AuthViewModel
import xyz.ecys.vocab.data.WordRepository
import xyz.ecys.vocab.data.AppUsageManager
import xyz.ecys.vocab.debug.DebugActivity
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import xyz.ecys.vocab.ui.components.auth.AuthSheet
import xyz.ecys.vocab.ui.components.stats.GoalDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                lifecycleScope.launch {
                    authViewModel.handleGoogleSignInResult(token)
                }
            }
        } catch (e: ApiException) {
            // Handle error
            authViewModel.showMessage("Google sign in failed: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Observe sign-in intent
        lifecycleScope.launch {
            authViewModel.signInIntent.collect { intent ->
                intent?.let {
                    googleSignInLauncher.launch(intent)
                    authViewModel.clearSignInIntent()
                }
            }
        }

        // Create a variable to track auth sheet state changes
        var authSheetVisible = false
        
        // Observe auth state changes to dismiss auth sheet and show success message
        lifecycleScope.launch {
            authViewModel.authState.collect { user ->
                if (user != null && authSheetVisible) {
                    authSheetVisible = false
                    authViewModel.showMessage("Successfully signed in")
                }
            }
        }

        setContent {
            VocabularyBoosterTheme {
                val context = LocalContext.current
                var showNeuralInfo by remember { mutableStateOf(false) }
                var showRefreshConfirmation by remember { mutableStateOf(false) }
                var showAuthSheet by remember { mutableStateOf(authSheetVisible) }
                var showGoalDialog by remember { mutableStateOf(false) }
                var goalInput by remember { mutableStateOf("20") }
                
                // Password dialog state
                var showPasswordDialog by remember { mutableStateOf(false) }
                var passwordInput by remember { mutableStateOf("") }
                
                // Forgot password dialog state
                var showForgotPasswordDialog by remember { mutableStateOf(false) }
                
                // Custom snackbar state
                var showSnackbar by remember { mutableStateOf(false) }
                var snackbarMessage by remember { mutableStateOf("") }
                
                val authError by authViewModel.authError.collectAsState()
                val authState by authViewModel.authState.collectAsState()
                val message by authViewModel.message.collectAsState()
                val scope = rememberCoroutineScope()
                
                // Show snackbar when message changes
                LaunchedEffect(message) {
                    message?.let {
                        if (it.isNotEmpty()) {
                            snackbarMessage = it
                            showSnackbar = true
                            // Auto-dismiss after 3 seconds
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                showSnackbar = false
                                authViewModel.showMessage(null) // Clear the message
                            }
                        }
                    }
                }
                
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

                // Update the external variable when showAuthSheet changes
                LaunchedEffect(showAuthSheet) {
                    authSheetVisible = showAuthSheet
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
                                All learning progress, custom words, streaks, and time spent data will be lost.
                                
                                Are you sure you want to continue?
                                """.trimIndent()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { 
                                    showRefreshConfirmation = false
                                    
                                    // Only show password dialog if user is signed in
                                    if (authViewModel.currentUser != null) {
                                        showPasswordDialog = true
                                    } else {
                                        // If user is not signed in, proceed without password
                                        lifecycleScope.launch {
                                            val wordRepository = WordRepository.getInstance(context)
                                            wordRepository.insertInitialWords()
                                            
                                            // Clear app usage data (streaks and time spent)
                                            val appUsageManager = AppUsageManager.getInstance(context)
                                            appUsageManager.resetAllUsageData()
                                            
                                            authViewModel.showMessage("Database reset successfully")
                                        }
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
                
                // Password confirmation dialog
                if (showPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { showPasswordDialog = false },
                        title = { Text("Confirm Password") },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Check if user is signed in with Google
                                val isGoogleSignIn = authViewModel.currentUser?.providerData?.any { 
                                    it.providerId == "google.com" 
                                } ?: false
                                
                                // Check if user has a password
                                val hasPassword = authViewModel.hasPassword()
                                
                                if (isGoogleSignIn && !hasPassword) {
                                    Text(
                                        "You're signed in with Google. Are you sure you want to reset the database? All learning progress, custom words, streaks, and time spent data will be lost."
                                    )
                                } else {
                                    Text("Please enter your password to confirm database reset")
                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Password") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedTextColor = Color(0xFFFCFCFC),
                                            focusedTextColor = Color(0xFFFCFCFC),
                                            cursorColor = Color(0xFF90CAF9),
                                            focusedBorderColor = Color(0xFF90CAF9),
                                            unfocusedBorderColor = Color(0xFF546E7A),
                                            focusedLabelColor = Color(0xFF90CAF9),
                                            unfocusedLabelColor = Color(0xFF546E7A)
                                        )
                                    )
                                }
                                
                                authError?.let {
                                    Text(
                                        text = it,
                                        color = Color(0xFFED333B),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    // Check if user is signed in with Google
                                    val isGoogleSignIn = authViewModel.currentUser?.providerData?.any { 
                                        it.providerId == "google.com" 
                                    } ?: false
                                    
                                    // Check if user has a password
                                    val hasPassword = authViewModel.hasPassword()
                                    
                                    if ((isGoogleSignIn && !hasPassword) || (hasPassword && passwordInput.isNotBlank())) {
                                        if (isGoogleSignIn && !hasPassword) {
                                            // For Google users without a password, proceed without verification
                                            showPasswordDialog = false
                                            // Proceed with database reset
                                            lifecycleScope.launch {
                                                val wordRepository = WordRepository.getInstance(context)
                                                wordRepository.insertInitialWords()
                                                
                                                // Clear app usage data (streaks and time spent)
                                                val appUsageManager = AppUsageManager.getInstance(context)
                                                appUsageManager.resetAllUsageData()
                                                
                                                // Sync changes to the cloud
                                                authViewModel.syncData { syncSuccess ->
                                                    if (syncSuccess) {
                                                        authViewModel.showMessage("Database reset and synced to cloud")
                                                    } else {
                                                        authViewModel.showMessage("Database reset but sync failed")
                                                    }
                                                }
                                            }
                                        } else {
                                            // For users with a password, verify password
                                            lifecycleScope.launch {
                                                try {
                                                    authViewModel.verifyPassword(passwordInput) { success ->
                                                        if (success) {
                                                            showPasswordDialog = false
                                                            passwordInput = ""
                                                            // Proceed with database reset
                                                            lifecycleScope.launch {
                                                                val wordRepository = WordRepository.getInstance(context)
                                                                wordRepository.insertInitialWords()
                                                                
                                                                // Clear app usage data (streaks and time spent)
                                                                val appUsageManager = AppUsageManager.getInstance(context)
                                                                appUsageManager.resetAllUsageData()
                                                                
                                                                // Sync changes to the cloud
                                                                authViewModel.syncData { syncSuccess ->
                                                                    if (syncSuccess) {
                                                                        authViewModel.showMessage("Database reset and synced to cloud")
                                                                    } else {
                                                                        authViewModel.showMessage("Database reset but sync failed")
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    authViewModel.showMessage("Error: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Confirm", color = Color(0xFFED333B))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                showPasswordDialog = false
                                passwordInput = ""
                                authViewModel.clearError()
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Forgot Password Dialog
                if (showForgotPasswordDialog) {
                    var emailError by remember { mutableStateOf<String?>(null) }
                    var emailSuccess by remember { mutableStateOf<String?>(null) }
                    
                    // Get current user email
                    val userEmail = authViewModel.getCurrentUserEmail() ?: ""
                    // Check if password reset is allowed (24h limit)
                    val canRequestReset = authViewModel.canRequestPasswordReset()
                    
                    // Format time until next reset is allowed
                    val timeUntilNextReset = if (!canRequestReset) {
                        val millisRemaining = authViewModel.getTimeUntilNextPasswordReset()
                        val hoursRemaining = millisRemaining / (1000 * 60 * 60)
                        val minutesRemaining = (millisRemaining % (1000 * 60 * 60)) / (1000 * 60)
                        "${hoursRemaining}h ${minutesRemaining}m"
                    } else ""
                    
                    AlertDialog(
                        onDismissRequest = { showForgotPasswordDialog = false },
                        title = { Text("Reset Password") },
                        containerColor = Color(0xFF18191E),
                        titleContentColor = Color(0xFFFCFCFC),
                        textContentColor = Color(0xFFFCFCFC),
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Email input screen
                                if (canRequestReset) {
                                    Text(
                                        "A password reset link will be sent to your email address ($userEmail).",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFAAAAAA)
                                    )
                                    
                                    Text(
                                        "Note: You can only request one password reset every 24 hours.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF9800)
                                    )
                                } else {
                                    Text(
                                        "You have already requested a password reset recently.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFAAAAAA)
                                    )
                                    
                                    Text(
                                        "You can request another reset in $timeUntilNextReset.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                                
                                emailError?.let {
                                    Text(
                                        text = it,
                                        color = Color(0xFFED333B),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                emailSuccess?.let {
                                    Text(
                                        text = it,
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                authError?.let {
                                    Text(
                                        text = it,
                                        color = Color(0xFFED333B),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (canRequestReset) {
                                        authViewModel.sendPasswordResetEmail(userEmail) { success ->
                                            if (success) {
                                                emailSuccess = "Password reset link sent!"
                                                // Close dialog after a short delay
                                                scope.launch {
                                                    kotlinx.coroutines.delay(1500)
                                                    showForgotPasswordDialog = false
                                                    authViewModel.showMessage("Password reset link sent to your email")
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = canRequestReset
                            ) {
                                Text(
                                    "Send Reset Link", 
                                    color = if (canRequestReset) Color(0xFF90CAF9) else Color(0xFF546E7A)
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { 
                                    showForgotPasswordDialog = false
                                    authViewModel.clearError()
                                }
                            ) {
                                Text("Cancel", color = Color(0xFF90CAF9))
                            }
                        }
                    )
                }

                if (showAuthSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { 
                            showAuthSheet = false
                            authViewModel.clearError()
                        },
                        containerColor = Color(0xFF05080D),
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                        windowInsets = WindowInsets(0),
                        sheetState = rememberModalBottomSheetState(
                            skipPartiallyExpanded = true
                        ),
                        shape = RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .navigationBarsPadding()
                        ) {
                            AuthSheet(
                                onDismiss = { 
                                    showAuthSheet = false
                                    authViewModel.clearError()
                                },
                                authViewModel = authViewModel
                            )
                        }
                    }
                }

                // Add GoalDialog
                GoalDialog(
                    showDialog = showGoalDialog,
                    goalInput = goalInput,
                    onGoalInputChange = { goalInput = it },
                    onDismiss = { showGoalDialog = false },
                    onSave = { newGoal ->
                        prefs.edit().putInt("daily_goal", newGoal).apply()
                    }
                )

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
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                startActivity(Intent(context, DebugActivity::class.java))
                            }
                        ) {
                            Icon(
                                painter = AppIcons.sparklesSolid(),
                                contentDescription = "Debug"
                            )
                        }
                    },
                    floatingActionButtonPosition = FabPosition.End
                ) { innerPadding ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Authentication Section
                            Text(
                                text = "Authentication",
                                style = MaterialTheme.typography.titleLarge
                            )
                            
                            var isAccountCardPressed by remember { mutableStateOf(false) }
                            val accountCardScale by animateFloatAsState(
                                targetValue = if (isAccountCardPressed) 0.97f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.75f,
                                    stiffness = 300f
                                )
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = accountCardScale
                                        scaleY = accountCardScale
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF18191E)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                onClick = { 
                                    if (authState != null) {
                                        val intent = Intent(context, AccountsActivity::class.java)
                                        context.startActivity(intent)
                                    } else {
                                        showAuthSheet = true
                                    }
                                },
                                interactionSource = remember { MutableInteractionSource() }
                                    .also { interactionSource ->
                                        LaunchedEffect(interactionSource) {
                                            interactionSource.interactions.collect { interaction ->
                                                when (interaction) {
                                                    is PressInteraction.Press -> isAccountCardPressed = true
                                                    is PressInteraction.Release -> isAccountCardPressed = false
                                                    is PressInteraction.Cancel -> isAccountCardPressed = false
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = AppIcons.circleUserSolid(),
                                            contentDescription = "Account",
                                            tint = Color(0xFFFCFCFC),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("Account")
                                            Text(
                                                text = authState?.email ?: "Not signed in",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFAAAAAA)
                                            )
                                        }
                                    }
                                    Icon(
                                        painter = AppIcons.arrowRight(),
                                        contentDescription = "Open",
                                        tint = Color(0xFFFCFCFC)
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFF18191E),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Learning Settings Section
                            Text(
                                text = "Learning",
                                style = MaterialTheme.typography.titleLarge
                            )
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF18191E)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 16.dp),
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 16.dp),
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
                                }
                            }

                            // Daily Goal Setting
                            var isDailyGoalPressed by remember { mutableStateOf(false) }
                            val dailyGoalScale by animateFloatAsState(
                                targetValue = if (isDailyGoalPressed) 0.97f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.75f,
                                    stiffness = 300f
                                )
                            )
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = dailyGoalScale
                                        scaleY = dailyGoalScale
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF18191E)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                onClick = { 
                                    showGoalDialog = true 
                                },
                                interactionSource = remember { MutableInteractionSource() }
                                    .also { interactionSource ->
                                        LaunchedEffect(interactionSource) {
                                            interactionSource.interactions.collect { interaction ->
                                                when (interaction) {
                                                    is PressInteraction.Press -> isDailyGoalPressed = true
                                                    is PressInteraction.Release -> isDailyGoalPressed = false
                                                    is PressInteraction.Cancel -> isDailyGoalPressed = false
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = AppIcons.targetSolid(),
                                            contentDescription = "Daily Goal",
                                            tint = Color(0xFFFCFCFC),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("Daily Goal")
                                            Text(
                                                text = "${prefs.getInt("daily_goal", 20)} words per day",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFAAAAAA)
                                            )
                                        }
                                    }
                                    Icon(
                                        painter = AppIcons.arrowRight(),
                                        contentDescription = "Open",
                                        tint = Color(0xFFFCFCFC)
                                    )
                                }
                            }

                            // UI Settings Section
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "User Experience",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF18191E)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reduce Animations")
                                    Switch(
                                        checked = reducedAnimations,
                                        onCheckedChange = { 
                                            reducedAnimations = it
                                            prefs.edit().putBoolean("reduced_animations", it).apply()
                                        }
                                    )
                                }
                            }

                            // Database Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF18191E)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                onClick = { showRefreshConfirmation = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = AppIcons.triangleExclamationSolid(),
                                            contentDescription = "Warning",
                                            tint = Color(0xFFED333B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("Reset Database")
                                            Text(
                                                text = "Reset to initial word set",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFAAAAAA)
                                            )
                                        }
                                    }
                                    Icon(
                                        painter = AppIcons.arrowRight(),
                                        contentDescription = "Open",
                                        tint = Color(0xFFFCFCFC)
                                    )
                                }
                            }
                        }
                        
                        // Custom Snackbar
                        AnimatedVisibility(
                            visible = showSnackbar,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(
                                    dampingRatio = 0.6f,
                                    stiffness = 300f
                                )
                            ) + fadeIn(
                                animationSpec = spring(
                                    dampingRatio = 0.6f,
                                    stiffness = 300f
                                )
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = spring(
                                    dampingRatio = 0.6f,
                                    stiffness = 300f
                                )
                            ) + fadeOut(
                                animationSpec = spring(
                                    dampingRatio = 0.6f,
                                    stiffness = 300f
                                )
                            ),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 100.dp) // Position further below the top bar
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .fillMaxWidth(0.9f), // Make it less wide
                                shape = RoundedCornerShape(16.dp), // Less rounded corners
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF18191E)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 6.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painter = AppIcons.circleInfoSolid(),
                                        contentDescription = "Info",
                                        tint = Color(0xFF90CAF9)
                                    )
                                    Text(
                                        text = snackbarMessage,
                                        color = Color(0xFFFCFCFC),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showSnackbar = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = AppIcons.xSolid(),
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFFAAAAAA)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 
