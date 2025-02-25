package xyz.ecys.vocab.ui.components.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import xyz.ecys.vocab.data.AuthViewModel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.BorderStroke
import xyz.ecys.vocab.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthSheet(
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(true) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val authError by authViewModel.authError.collectAsState()

    fun validatePassword(pass: String): Boolean {
        if (pass.length < 8) {
            passwordError = "Password must be at least 8 characters"
            return false
        }
        if (!pass.any { it.isDigit() }) {
            passwordError = "Password must contain at least one number"
            return false
        }
        if (!pass.any { it.isUpperCase() }) {
            passwordError = "Password must contain at least one uppercase letter"
            return false
        }
        if (!pass.any { it.isLowerCase() }) {
            passwordError = "Password must contain at least one lowercase letter"
            return false
        }
        passwordError = null
        return true
    }

    // Button animation states
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 300f
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFF05080D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFFCFCFC)
                )
                Text(
                    text = if (isSignUp) "Enter your details to sign up" else "Sign in to sync your data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAAAAA)
                )
            }

            // Form Fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
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
                    value = password,
                    onValueChange = { 
                        password = it
                        if (isSignUp) validatePassword(it)
                    },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
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

                passwordError?.let {
                    Text(
                        text = it,
                        color = Color(0xFFED333B),
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

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isSignUp && !validatePassword(password)) return@Button
                        
                        val action: (String, String, (Boolean) -> Unit) -> Unit = if (isSignUp) {
                            authViewModel::signUp
                        } else {
                            authViewModel::signIn
                        }
                        action(email, password) { success ->
                            if (success) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF18191E),
                        contentColor = Color(0xFFFCFCFC)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    interactionSource = interactionSource
                ) {
                    Text(
                        text = if (isSignUp) "Sign Up" else "Sign In",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                var isGooglePressed by remember { mutableStateOf(false) }
                val googleScale by animateFloatAsState(
                    targetValue = if (isGooglePressed) 0.97f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 300f
                    )
                )

                OutlinedButton(
                    onClick = { authViewModel.signInWithGoogle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = googleScale
                            scaleY = googleScale
                        },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFCFCFC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF546E7A)),
                    shape = RoundedCornerShape(12.dp),
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect { interaction ->
                                    when (interaction) {
                                        is PressInteraction.Press -> isGooglePressed = true
                                        is PressInteraction.Release -> isGooglePressed = false
                                        is PressInteraction.Cancel -> isGooglePressed = false
                                    }
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = AppIcons.googleLogo(),
                            contentDescription = "Google",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { isSignUp = !isSignUp },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF90CAF9)
                        )
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account?" else "Need an account?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (!isSignUp) {
                        TextButton(
                            onClick = {
                                if (email.isNotBlank()) {
                                    authViewModel.sendPasswordResetEmail(email) { success ->
                                        if (success) {
                                            // Show success message
                                            authViewModel.showMessage("Password reset email sent")
                                        }
                                    }
                                } else {
                                    authViewModel.showMessage("Please enter your email")
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF90CAF9)
                            )
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
} 