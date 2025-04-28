package xyz.ecys.vocab

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.SubscriptionManager
import xyz.ecys.vocab.ui.theme.AppIcons
import xyz.ecys.vocab.ui.theme.VocabularyBoosterTheme
import xyz.ecys.vocab.utils.TransitionUtils
import xyz.ecys.vocab.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter

@OptIn(ExperimentalMaterial3Api::class)
class SubscriptionActivity : ComponentActivity() {
    private lateinit var subscriptionManager: SubscriptionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the subscription manager
        subscriptionManager = SubscriptionManager.getInstance(this)
        
        setContent {
            VocabularyBoosterTheme {
                val isPremium by subscriptionManager.isPremium.collectAsState()
                val message by subscriptionManager.message.collectAsState()
                var showSnackbar by remember { mutableStateOf(false) }
                var snackbarMessage by remember { mutableStateOf("") }
                
                // Show snackbar when message changes
                LaunchedEffect(message) {
                    message?.let {
                        if (it.isNotEmpty()) {
                            snackbarMessage = it
                            showSnackbar = true
                            // Auto-dismiss after 3 seconds
                            kotlinx.coroutines.delay(3000)
                            showSnackbar = false
                            subscriptionManager.clearMessage()
                        }
                    }
                }
                
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    text = if (isPremium) "Premium" else "Upgrade to Premium",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCFCFC)
                                    )
                                ) 
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    finish() 
                                    TransitionUtils.applyStandardTransitionOnFinish(this@SubscriptionActivity)
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isPremium) {
                            PremiumStatusContent()
                        } else {
                            SubscriptionContent(innerPadding)
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
                                .padding(top = 100.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .fillMaxWidth(0.9f),
                                shape = RoundedCornerShape(16.dp),
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
    
    @Composable
    private fun PremiumStatusContent() {
        val expiryDate by subscriptionManager.expirationDate.collectAsState()
        val timeRemaining = remember(expiryDate) { subscriptionManager.getTimeRemaining() }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Additional spacer instead of padding on the icon
            Spacer(modifier = Modifier.height(24.dp))
            
            // Wrapped in Box to ensure proper centering and space allocation
            Box(
                modifier = Modifier
                    .fillMaxWidth()  // Use full width again
                    .height(140.dp), // Increased height
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.starSolid(),
                    contentDescription = null,
                    tint = Color(0xFFFFD700), // Gold color
                    modifier = Modifier.size(120.dp)
                )
            }
            
            Text(
                text = "You're a Premium Subscriber!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCFCFC)
                ),
                textAlign = TextAlign.Center
            )
            
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Subscription Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF90CAF9)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status:",
                            color = Color(0xFFAAAAAA)
                        )
                        Text(
                            text = "Active",
                            color = Color(0xFF4CAF50)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (timeRemaining != null) {
                        val (days, hours) = timeRemaining
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Time Remaining:",
                                color = Color(0xFFAAAAAA)
                            )
                            Text(
                                text = if (days > 365) "Lifetime" else "$days days, $hours hours",
                                color = Color(0xFFFCFCFC)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    expiryDate?.let { expiry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Expires:",
                                color = Color(0xFFAAAAAA)
                            )
                            Text(
                                text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(expiry),
                                color = Color(0xFFFCFCFC)
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Enjoy all premium features:",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFCFCFC),
                modifier = Modifier.padding(top = 16.dp)
            )
            
            PremiumFeaturesList()
        }
    }
    
    @Composable
    private fun SubscriptionContent(innerPadding: PaddingValues) {
        val scrollState = rememberScrollState()
        var selectedPlan by remember { mutableStateOf(SubscriptionManager.SubscriptionType.MONTHLY) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Monthly Plan
            SubscriptionPlanCard(
                title = "Monthly",
                price = "$8.99/mo",
                period = "Billed once per month. Cancel anytime you wish.",
                isSelected = selectedPlan == SubscriptionManager.SubscriptionType.MONTHLY,
                onSelect = {
                    selectedPlan = SubscriptionManager.SubscriptionType.MONTHLY
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Annual Plan
            SubscriptionPlanCard(
                title = "Annual",
                price = "$54.99/yr",
                period = "Save 💰 by paying the equivalent of",
                hasCustomSecondLine = true,
                isSelected = selectedPlan == SubscriptionManager.SubscriptionType.YEARLY,
                savePercentage = 49,
                onSelect = {
                    selectedPlan = SubscriptionManager.SubscriptionType.YEARLY
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Premium features list
            PremiumFeaturesList()
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Subscribe button with animation and consistent design
            var isSubscribeButtonPressed by remember { mutableStateOf(false) }
            val subscribeButtonScale by animateFloatAsState(
                targetValue = if (isSubscribeButtonPressed) 0.97f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 300f
                )
            )

            // Manual verification dialog state
            var showVerificationDialog by remember { mutableStateOf(false) }
            var transactionId by remember { mutableStateOf("") }

            Button(
                onClick = {
                    // Open external payment page for the selected subscription
                    subscriptionManager.openExternalPayment(selectedPlan, this@SubscriptionActivity)
                    // Show verification dialog after a delay
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(500)
                        showVerificationDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = subscribeButtonScale
                        scaleY = subscribeButtonScale
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                interactionSource = remember { MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect { interaction ->
                                when (interaction) {
                                    is PressInteraction.Press -> isSubscribeButtonPressed = true
                                    is PressInteraction.Release -> isSubscribeButtonPressed = false
                                    is PressInteraction.Cancel -> isSubscribeButtonPressed = false
                                }
                            }
                        }
                    }
            ) {
                Text(
                    text = "Subscribe for ${if (selectedPlan == SubscriptionManager.SubscriptionType.MONTHLY) "$8.99" else "$54.99"}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
            }
            
            // For testing/development: Add a button to directly activate premium
            // This should be removed or secured in production
            TextButton(
                onClick = { 
                    showVerificationDialog = true 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "I already paid, verify my purchase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF90CAF9)
                )
            }

            // Manual verification dialog
            if (showVerificationDialog) {
                AlertDialog(
                    onDismissRequest = { showVerificationDialog = false },
                    title = { Text("Verify Purchase") },
                    containerColor = Color(0xFF19181E),
                    titleContentColor = Color(0xFFFCFCFC),
                    textContentColor = Color(0xFFFCFCFC),
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "After completing your payment on Stripe, you'll receive a receipt with a payment ID. Enter it below to activate your premium subscription:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFAAAAAA)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = transactionId,
                                onValueChange = { transactionId = it },
                                label = { Text("Payment ID / Transaction ID") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedTextColor = Color(0xFFFCFCFC),
                                    focusedTextColor = Color(0xFFFCFCFC),
                                    cursorColor = Color(0xFF90CAF9),
                                    focusedBorderColor = Color(0xFF90CAF9),
                                    unfocusedBorderColor = Color(0xFF546E7A),
                                    focusedLabelColor = Color(0xFF90CAF9),
                                    unfocusedLabelColor = Color(0xFF546E7A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "The payment ID starts with 'pi_' followed by letters and numbers. You can find it in your email receipt or Stripe checkout confirmation page.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFAAAAAA)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                "Important: Each payment ID is linked to your account and cannot be used by others.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Launch verification process
                                lifecycleScope.launch {
                                    if (transactionId.isNotBlank()) {
                                        // Special handling for test IDs
                                        if (transactionId.startsWith("test_") || transactionId.startsWith("pi_test")) {
                                            subscriptionManager.activatePremiumForTesting(selectedPlan, transactionId)
                                            showVerificationDialog = false
                                        } else {
                                            // Normal verification flow
                                            val success = subscriptionManager.verifyTransactionId(
                                                transactionId,
                                                selectedPlan
                                            )
                                            if (success) {
                                                showVerificationDialog = false
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Verify", color = Color(0xFF90CAF9))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showVerificationDialog = false }
                        ) {
                            Text("Cancel", color = Color(0xFF90CAF9))
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Terms and Privacy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "By subscribing you agree to our",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terms of Use",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        color = Color.White,
                        modifier = Modifier.clickable { /* Handle terms click */ }
                    )
                    Text(
                        text = " and ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        color = Color.White,
                        modifier = Modifier.clickable { /* Handle privacy click */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    @Composable
    private fun PremiumFeaturesList() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            PremiumFeatureItem(text = "Cloud synchronization")
            PremiumFeatureItem(text = "Custom word packs")
            PremiumFeatureItem(text = "Machine learning statistics")
            PremiumFeatureItem(text = "Ad-free experience")
        }
    }
    
    @Composable
    private fun PremiumFeatureItem(text: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = AppIcons.checkSolid(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
    
    @Composable
    private fun SubscriptionPlanCard(
        title: String,
        price: String,
        period: String,
        secondLine: String? = null,
        hasCustomSecondLine: Boolean = false,
        thirdLine: String? = null,
        isSelected: Boolean,
        savePercentage: Int? = null, // For "SAVE 49%" label
        onSelect: () -> Unit
    ) {
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1f,
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 300f
            )
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Save percentage label if provided
            if (savePercentage != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-6).dp, x = (-6).dp)
                        .zIndex(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "SAVE ${savePercentage}%",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF18191E)
                ),
                shape = RoundedCornerShape(16.dp),
                onClick = onSelect,
                interactionSource = remember { MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect { interaction ->
                                when (interaction) {
                                    is PressInteraction.Press -> isPressed = true
                                    is PressInteraction.Release -> isPressed = false
                                    is PressInteraction.Cancel -> isPressed = false
                                }
                            }
                        }
                    }
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Price in top right
                    Text(
                        text = price,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 16.dp, top = 16.dp)
                    )
                    
                    // Content with aligned checkbox
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Row with checkbox, title/period at the same height as price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Checkbox - added padding to align with text
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(21.dp) 
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .background(
                                        color = if (isSelected) Color(0xFF505050) else Color.Transparent,
                                        shape = RoundedCornerShape(3.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = AppIcons.checkSolid(),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Title and period in a Column
                            Column(
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = period,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                
                                if (hasCustomSecondLine) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    // Custom row for Annual plan with price and cancel text side by side
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "$4.58/mo",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                                                                
                                        Text(
                                            text = ". Cancel anytime.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                } else if (secondLine != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    Text(
                                        text = secondLine,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                                
                                if (thirdLine != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    Text(
                                        text = thirdLine,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        TransitionUtils.applyStandardTransitionOnFinish(this)
    }
} 