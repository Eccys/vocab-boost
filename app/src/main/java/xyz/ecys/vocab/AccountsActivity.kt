package xyz.ecys.vocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.ecys.vocab.data.AuthViewModel
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.PressInteraction
import kotlinx.coroutines.flow.collect
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut

@OptIn(ExperimentalMaterial3Api::class)
class AccountsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VocabularyBoosterTheme {
                val authViewModel: AuthViewModel = viewModel()
                val scope = rememberCoroutineScope()
                var showPasswordDialog by remember { mutableStateOf(false) }
                var showEmailDialog by remember { mutableStateOf(false) }
                var showSetPasswordDialog by remember { mutableStateOf(false) }
                var newPassword by remember { mutableStateOf("") }
                var newEmail by remember { mutableStateOf("") }
                var currentPassword by remember { mutableStateOf("") }
                val authState by authViewModel.authState.collectAsState()
                val authError by authViewModel.authError.collectAsState()
                val message by authViewModel.message.collectAsState()
                var lastSync by remember { mutableStateOf(authViewModel.getLastSyncTime()) }
                var showSnackbar by remember { mutableStateOf(false) }
                var snackbarMessage by remember { mutableStateOf("") }
                var showForgotPasswordDialog by remember { mutableStateOf(false) }
                var resetEmail by remember { mutableStateOf("") }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = "Account",
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
                                containerColor = Color(0xFF05080D)
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
                        // Account Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF18191E)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Email",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFAAAAAA)
                                )
                                Text(
                                    text = authState?.email ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFFCFCFC)
                                )
                            }
                        }

                        // Account Actions
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF18191E)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ListItem(
                                    headlineContent = { Text("Change Email") },
                                    leadingContent = {
                                        Icon(
                                            painter = AppIcons.envelopeSolid(),
                                            contentDescription = "Email",
                                            tint = Color(0xFFFCFCFC)
                                        )
                                    },
                                    modifier = Modifier.clickable { showEmailDialog = true }
                                )
                                
                                HorizontalDivider(color = Color(0xFF2C2E33))

                                ListItem(
                                    headlineContent = { Text("Change Password") },
                                    leadingContent = {
                                        Icon(
                                            painter = AppIcons.lockSolid(),
                                            contentDescription = "Password",
                                            tint = Color(0xFFFCFCFC)
                                        )
                                    },
                                    modifier = Modifier.clickable { showPasswordDialog = true }
                                )

                                HorizontalDivider(color = Color(0xFF2C2E33))

                                // Add Set Password option for Google users without a password
                                val isGoogleUser = authState?.providerData?.any { it.providerId == "google.com" } ?: false
                                val hasPassword = authViewModel.hasPassword()
                                
                                if (isGoogleUser && !hasPassword) {
                                    ListItem(
                                        headlineContent = { Text("Set Password") },
                                        supportingContent = { Text("Add password to your Google account", color = Color(0xFFAAAAAA)) },
                                        leadingContent = {
                                            Icon(
                                                painter = AppIcons.lockSolid(),
                                                contentDescription = "Set Password",
                                                tint = Color(0xFFFCFCFC)
                                            )
                                        },
                                        modifier = Modifier.clickable { showSetPasswordDialog = true }
                                    )
                                    
                                    HorizontalDivider(color = Color(0xFF2C2E33))
                                }

                                ListItem(
                                    headlineContent = { Text("Sync Data") },
                                    supportingContent = { Text("Last synced: $lastSync", color = Color(0xFFAAAAAA)) },
                                    leadingContent = {
                                        Icon(
                                            painter = AppIcons.cloudArrowUpSolid(),
                                            contentDescription = "Sync",
                                            tint = Color(0xFFFCFCFC)
                                        )
                                    },
                                    modifier = Modifier.clickable { 
                                        authViewModel.syncData { success ->
                                            if (success) {
                                                lastSync = authViewModel.getLastSyncTime()
                                                showSnackbar = true
                                                snackbarMessage = "Data synced successfully"
                                            } else {
                                                showSnackbar = true
                                                snackbarMessage = authError ?: "Sync failed"
                                            }
                                        }
                                    }
                                )
                                
                                HorizontalDivider(color = Color(0xFF2C2E33))
                                
                                ListItem(
                                    headlineContent = { Text("Download Data") },
                                    supportingContent = { Text("Get data from other devices", color = Color(0xFFAAAAAA)) },
                                    leadingContent = {
                                        Icon(
                                            painter = AppIcons.cloudArrowDownSolid(),
                                            contentDescription = "Download",
                                            tint = Color(0xFFFCFCFC)
                                        )
                                    },
                                    modifier = Modifier.clickable { 
                                        authViewModel.downloadDataFromCloud { success ->
                                            if (success) {
                                                lastSync = authViewModel.getLastSyncTime()
                                                showSnackbar = true
                                                snackbarMessage = "Data downloaded successfully"
                                            } else {
                                                showSnackbar = true
                                                snackbarMessage = authError ?: "Download failed"
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Sign Out Button
                        var isSignOutPressed by remember { mutableStateOf(false) }
                        val signOutScale by animateFloatAsState(
                            targetValue = if (isSignOutPressed) 0.97f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.75f,
                                stiffness = 300f
                            )
                        )

                        OutlinedButton(
                            onClick = { 
                                scope.launch {
                                    authViewModel.signOut()
                                    finish()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .graphicsLayer {
                                    scaleX = signOutScale
                                    scaleY = signOutScale
                                },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFED333B)
                            ),
                            border = BorderStroke(
                                1.dp,
                                Color(0xFFED333B).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            interactionSource = remember { MutableInteractionSource() }
                                .also { interactionSource ->
                                    LaunchedEffect(interactionSource) {
                                        interactionSource.interactions.collect { interaction ->
                                            when (interaction) {
                                                is PressInteraction.Press -> isSignOutPressed = true
                                                is PressInteraction.Release -> isSignOutPressed = false
                                                is PressInteraction.Cancel -> isSignOutPressed = false
                                            }
                                        }
                                    }
                                }
                        ) {
                            Text("Sign Out")
                        }
                    }
                }

                // Add Snackbar to show messages
                if (showSnackbar) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth(0.9f),
                            shape = RoundedCornerShape(24.dp), // More rounded corners
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

                // Password Change Dialog
                if (showPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { showPasswordDialog = false },
                        title = { Text("Change Password") },
                        containerColor = Color(0xFF19181E),
                        titleContentColor = Color(0xFFFCFCFC),
                        textContentColor = Color(0xFFFCFCFC),
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentPassword,
                                    onValueChange = { currentPassword = it },
                                    label = { Text("Current Password") },
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
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("New Password") },
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
                                
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF90CAF9),
                                    modifier = Modifier
                                        .clickable { 
                                            showPasswordDialog = false
                                            showForgotPasswordDialog = true
                                            resetEmail = authState?.email ?: ""
                                        }
                                        .padding(vertical = 4.dp)
                                )
                                
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
                                    if (currentPassword.isNotBlank() && newPassword.isNotBlank()) {
                                        authViewModel.updatePassword(currentPassword, newPassword) { success ->
                                            if (success) {
                                                showPasswordDialog = false
                                                currentPassword = ""
                                                newPassword = ""
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Change", color = Color(0xFF90CAF9))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showPasswordDialog = false }
                            ) {
                                Text("Cancel", color = Color(0xFF90CAF9))
                            }
                        }
                    )
                }

                // Email Change Dialog
                if (showEmailDialog) {
                    AlertDialog(
                        onDismissRequest = { showEmailDialog = false },
                        title = { Text("Change Email") },
                        containerColor = Color(0xFF19181E),
                        titleContentColor = Color(0xFFFCFCFC),
                        textContentColor = Color(0xFFFCFCFC),
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentPassword,
                                    onValueChange = { currentPassword = it },
                                    label = { Text("Current Password") },
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
                                OutlinedTextField(
                                    value = newEmail,
                                    onValueChange = { newEmail = it },
                                    label = { Text("New Email") },
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
                                    if (currentPassword.isNotBlank() && newEmail.isNotBlank()) {
                                        authViewModel.updateEmail(newEmail, currentPassword) { success ->
                                            if (success) {
                                                showEmailDialog = false
                                                currentPassword = ""
                                                newEmail = ""
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Change", color = Color(0xFF90CAF9))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showEmailDialog = false }
                            ) {
                                Text("Cancel", color = Color(0xFF90CAF9))
                            }
                        }
                    )
                }
                
                // Set Password Dialog for Google Users
                if (showSetPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { showSetPasswordDialog = false },
                        title = { Text("Set Password") },
                        containerColor = Color(0xFF19181E),
                        titleContentColor = Color(0xFFFCFCFC),
                        textContentColor = Color(0xFFFCFCFC),
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Adding a password to your Google account will allow you to sign in with email and password, and will be required for sensitive operations.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFAAAAAA)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("New Password") },
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
                                
                                Text(
                                    "Password must be at least 8 characters with at least one uppercase letter, one lowercase letter, and one number.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFAAAAAA)
                                )
                                
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
                                    if (newPassword.isNotBlank()) {
                                        // Validate password
                                        if (newPassword.length < 8 || 
                                            !newPassword.any { it.isDigit() } ||
                                            !newPassword.any { it.isUpperCase() } ||
                                            !newPassword.any { it.isLowerCase() }) {
                                            authViewModel.showMessage("Password does not meet requirements")
                                        } else {
                                            authViewModel.setPasswordForGoogleUser(newPassword) { success ->
                                                if (success) {
                                                    showSetPasswordDialog = false
                                                    newPassword = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Set Password", color = Color(0xFF90CAF9))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { 
                                    showSetPasswordDialog = false
                                    newPassword = ""
                                }
                            ) {
                                Text("Cancel", color = Color(0xFF90CAF9))
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
                        containerColor = Color(0xFF19181E),
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
                                                    showSnackbar = true
                                                    snackbarMessage = "Password reset link sent to your email"
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

                // Custom Snackbar
                Box(modifier = Modifier.fillMaxSize()) {
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
            }
        }
    }
} 
