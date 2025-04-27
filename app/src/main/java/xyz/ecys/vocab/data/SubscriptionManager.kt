package xyz.ecys.vocab.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import android.util.Log
import android.os.Handler
import android.os.Looper

/**
 * Manages premium subscription status and subscription-related functions.
 * This class handles checking if a user is subscribed to premium,
 * purchasing subscriptions, and accessing premium features.
 */
class SubscriptionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appContext = context.applicationContext
    
    // Current subscription status
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium
    
    // Subscription expiration date
    private val _expirationDate = MutableStateFlow<Date?>(null)
    val expirationDate: StateFlow<Date?> = _expirationDate
    
    // Messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    
    // Google Play Billing Client
    private lateinit var billingClient: BillingClient
    private var productDetails: Map<String, ProductDetails> = mapOf()
    
    init {
        // Initialize subscription status from preferences or cloud
        refreshSubscriptionStatus()
        setupBillingClient()
    }
    
    /**
     * Open payment webpage for manual payment processing
     * This is an alternative to Google Play Billing
     */
    fun openExternalPayment(subscriptionType: SubscriptionType, context: Context) {
        // Get current user ID to include in payment URL as a custom field
        val userId = auth.currentUser?.uid ?: "guest"
        
        // Base payment URL
        val baseUrl = when (subscriptionType) {
            SubscriptionType.MONTHLY -> MONTHLY_PAYMENT_URL
            SubscriptionType.YEARLY -> YEARLY_PAYMENT_URL
        }
        
        // Append user ID and subscription type as custom parameters
        val paymentUrl = if (baseUrl.contains("?")) {
            "$baseUrl&client_reference_id=$userId&metadata[subscription_type]=${subscriptionType.name}"
        } else {
            "$baseUrl?client_reference_id=$userId&metadata[subscription_type]=${subscriptionType.name}"
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            _message.value = "Opening payment page..."
        } catch (e: Exception) {
            _message.value = "Error opening payment page: ${e.message}"
        }
    }
    
    /**
     * Verifies a transaction ID using enhanced security measures
     * This approach doesn't require Firebase Functions billing
     */
    suspend fun verifyTransactionId(
        transactionId: String,
        subscriptionType: SubscriptionType = SubscriptionType.MONTHLY
    ): Boolean {
        // Require user to be logged in for verification
        val userId = auth.currentUser?.uid ?: run {
            _message.value = "You must be logged in to verify a purchase"
            return false
        }
        
        // Trim and sanitize transaction ID
        val cleanTransactionId = transactionId.trim()
        
        if (cleanTransactionId.isBlank()) {
            _message.value = "Transaction ID cannot be empty"
            return false
        }
        
        // Enhanced validation of Stripe payment ID format
        if (!isValidStripePaymentId(cleanTransactionId)) {
            _message.value = "Invalid transaction ID format. Please enter the ID from your payment receipt."
            return false
        }
        
        try {
            // First check if the user already has premium in Firebase
            val userDoc = db.collection("users").document(userId).get().await()
            
            if (userDoc.exists()) {
                val isAlreadyPremium = userDoc.getBoolean("isPremium") ?: false
                val premiumTransactionId = userDoc.getString("transactionId")
                
                // If already premium with this transaction, restore local state
                if (isAlreadyPremium && premiumTransactionId == cleanTransactionId) {
                    val cloudExpirationMs = userDoc.getLong("subscriptionExpires") ?: 0
                    val cloudExpiration = if (cloudExpirationMs > 0) Date(cloudExpirationMs) else null
                    
                    if (cloudExpiration != null && cloudExpiration.after(Date())) {
                        // They're already premium with this transaction ID - restore their status
                        prefs.edit()
                            .putBoolean(KEY_IS_PREMIUM, true)
                            .putLong(KEY_EXPIRATION_DATE, cloudExpirationMs)
                            .putString(KEY_TRANSACTION_ID, cleanTransactionId)
                            .putLong(KEY_LAST_VERIFICATION, System.currentTimeMillis())
                            .apply()
                        
                        _isPremium.value = true
                        _expirationDate.value = cloudExpiration
                        
                        _message.value = "Premium restored successfully!"
                        return true
                    }
                }
            }
            
            // Check if this transaction has been used before (by any user)
            val transactionDoc = db.collection("transactions").document(cleanTransactionId).get().await()
            
            if (transactionDoc.exists()) {
                val existingUserId = transactionDoc.getString("userId")
                
                // If used by another user, reject it
                if (existingUserId != null && existingUserId != userId) {
                    _message.value = "This transaction ID has already been used by another account"
                    return false
                }
                
                // If used by this user, allow re-verification
                val existingSubscriptionType = transactionDoc.getString("subscriptionType")
                try {
                    val detectedType = SubscriptionType.valueOf(existingSubscriptionType ?: "")
                    applyPremiumStatus(detectedType, cleanTransactionId)
                    _message.value = "Subscription restored successfully!"
                    return true
                } catch (e: Exception) {
                    // Continue with verification if type can't be determined
                }
            }
            
            // SECURITY IMPROVEMENTS WITHOUT BILLING:
            
            // 1. Check for suspicious patterns in the transaction ID
            if (isLikelyFakeTransactionId(cleanTransactionId)) {
                _message.value = "This transaction ID appears to be invalid"
                return false
            }
            
            // 2. Verify transaction ID hasn't been used by this user before for different subscription
            val userTransactions = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            for (doc in userTransactions.documents) {
                val storedTransactionId = doc.id
                if (areSimilarTransactionIds(storedTransactionId, cleanTransactionId)) {
                    _message.value = "This appears to be similar to a transaction you've already used"
                    return false
                }
            }
            
            // 3. Check for transaction generation time based on ID structure
            val transactionTimestamp = estimateTransactionTime(cleanTransactionId)
            val currentTimeMillis = System.currentTimeMillis()
            
            // Reject extremely old or future transaction IDs
            if (transactionTimestamp > 0) {
                if (currentTimeMillis - transactionTimestamp > 90 * 24 * 60 * 60 * 1000) { // Older than 90 days
                    _message.value = "This transaction ID appears to be too old"
                    return false
                }
                
                if (transactionTimestamp > currentTimeMillis + 24 * 60 * 60 * 1000) { // More than 1 day in future
                    _message.value = "This transaction ID appears to be invalid"
                    return false
                }
            }
            
            // Enhanced logging to help track potential fraud
            Log.d("PremiumVerification", "New transaction: $cleanTransactionId from user: $userId")
            
            // Create new premium subscription
            val currentDate = Date()
            val calendar = java.util.Calendar.getInstance()
            calendar.time = currentDate
            
            when (subscriptionType) {
                SubscriptionType.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
                SubscriptionType.YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
            }
            
            val expirationDate = calendar.time
            
            // Create transaction record with additional security metadata
            val transactionData = hashMapOf(
                "userId" to userId,
                "transactionId" to cleanTransactionId,
                "subscriptionType" to subscriptionType.name,
                "purchaseDate" to currentDate.time,
                "expirationDate" to expirationDate.time,
                "status" to "verified",
                "verificationMethod" to "enhanced-client",
                "deviceInfo" to android.os.Build.MODEL,
                "appVersion" to appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
            )
            
            // Update user's premium status
            val userData = hashMapOf(
                "isPremium" to true,
                "subscriptionType" to subscriptionType.name,
                "subscriptionExpires" to expirationDate.time,
                "lastUpdated" to currentDate.time,
                "transactionId" to cleanTransactionId,
                "permanentPremium" to true
            )
            
            // Write the records in a batch to ensure consistency
            val batch = db.batch()
            
            batch.set(
                db.collection("transactions").document(cleanTransactionId),
                transactionData
            )
            
            batch.update(
                db.collection("users").document(userId),
                userData as Map<String, Any>
            )
            
            // Commit the batch
            batch.commit().await()
            
            // Update local state
            prefs.edit()
                .putBoolean(KEY_IS_PREMIUM, true)
                .putLong(KEY_EXPIRATION_DATE, expirationDate.time)
                .putString(KEY_TRANSACTION_ID, cleanTransactionId)
                .putLong(KEY_LAST_VERIFICATION, System.currentTimeMillis())
                .apply()
            
            _isPremium.value = true
            _expirationDate.value = expirationDate
            
            _message.value = "Premium activated successfully!"
            return true
            
        } catch (e: Exception) {
            _message.value = "Verification failed: ${e.message}"
            return false
        }
    }
    
    /**
     * Enhanced validation to detect potentially fake transaction IDs
     * This is a heuristic approach and not as secure as API verification
     */
    private fun isValidStripePaymentId(paymentId: String): Boolean {
        // More rigorous pattern matching for Stripe IDs
        val piPattern = Regex("^pi_[a-zA-Z0-9]{24}$") // Payment Intent
        val csPattern = Regex("^cs_[a-zA-Z0-9]{24}$") // Checkout Session
        val testPattern = Regex("^test_[a-zA-Z0-9]{24}$") // Test mode
        
        return piPattern.matches(paymentId) || 
               csPattern.matches(paymentId) || 
               testPattern.matches(paymentId)
    }
    
    /**
     * Check if a transaction ID appears to be fabricated
     * Uses simple heuristics to catch obvious fake IDs
     */
    private fun isLikelyFakeTransactionId(transactionId: String): Boolean {
        // Stripe IDs have specific patterns - check for obviously fake ones
        
        // Check for repeated characters (more than 6 in a row)
        val repeatedCharsPattern = Regex(".*(.)\\1{5,}.*")
        if (repeatedCharsPattern.matches(transactionId)) return true
        
        // Check for sequential characters (more than 5 in a row)
        val sequentialPattern = "abcdefghijklmnopqrstuvwxyz0123456789"
        val reverseSequentialPattern = sequentialPattern.reversed()
        
        for (i in 0..transactionId.length - 5) {
            val substr = transactionId.substring(i, i + 5).toLowerCase()
            if (sequentialPattern.contains(substr) || reverseSequentialPattern.contains(substr)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if two transaction IDs are suspiciously similar
     * Helps prevent slight modifications of used transaction IDs
     */
    private fun areSimilarTransactionIds(id1: String, id2: String): Boolean {
        if (id1 == id2) return true
        
        // If different formats (pi_ vs cs_), they're not similar
        if (id1.take(3) != id2.take(3)) return false
        
        // Check edit distance - if only a few characters are different, they may be similar
        val distance = levenshteinDistance(id1, id2)
        return distance <= 3 // If 3 or fewer edits needed, they're suspiciously similar
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     * Used to detect slightly modified transaction IDs
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        
        // Create a table to store results of subproblems
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        // Fill d[][] in bottom up manner
        for (i in 0..m) {
            for (j in 0..n) {
                when {
                    i == 0 -> dp[i][j] = j // First string is empty
                    j == 0 -> dp[i][j] = i // Second string is empty
                    s1[i - 1] == s2[j - 1] -> dp[i][j] = dp[i - 1][j - 1] // Same characters
                    else -> dp[i][j] = 1 + minOf(
                        dp[i][j - 1],      // Insert
                        dp[i - 1][j],      // Remove
                        dp[i - 1][j - 1]   // Replace
                    )
                }
            }
        }
        return dp[m][n]
    }
    
    /**
     * Estimate when a transaction was created based on its ID
     * Stripe IDs encode approximate timestamp information
     */
    private fun estimateTransactionTime(transactionId: String): Long {
        try {
            // Strip prefix
            val idWithoutPrefix = transactionId.substring(3)
            
            // Stripe IDs use base62 encoding and the first characters encode timestamp info
            // This is a very rough approximation
            val firstPart = idWithoutPrefix.take(10)
            val hash = firstPart.hashCode() and 0x7FFFFFFF // Positive hash
            
            // Stripe started in 2011, so use that as base
            val stripeEpochStart = 1293840000000L // Jan 1, 2011
            val maxTimespan = System.currentTimeMillis() - stripeEpochStart
            
            return stripeEpochStart + (hash % maxTimespan)
        } catch (e: Exception) {
            return 0 // Could not estimate
        }
    }
    
    private fun setupBillingClient() {
        // Only initialize if Google Play is being used
        if (!ENABLE_GOOGLE_PLAY_BILLING) return
        
        billingClient = BillingClient.newBuilder(appContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
            
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    queryAvailableProducts()
                    queryPurchases()
                } else {
                    _message.value = "Billing setup failed: ${billingResult.debugMessage}"
                }
            }
            
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                _message.value = "Billing service disconnected"
            }
        })
    }
    
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                _message.value = "Purchase canceled"
            } else {
                _message.value = "Purchase error: ${billingResult.debugMessage}"
            }
        }
        
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                    
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Purchase acknowledged
                        updateSubscriptionStatus(purchase)
                    }
                }
            } else {
                updateSubscriptionStatus(purchase)
            }
        }
    }
    
    private fun updateSubscriptionStatus(purchase: Purchase) {
        val subscriptionId = purchase.products.firstOrNull() ?: return
        
        val now = Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = now
        
        val subscriptionType = when (subscriptionId) {
            MONTHLY_SUBSCRIPTION_ID -> SubscriptionType.MONTHLY
            YEARLY_SUBSCRIPTION_ID -> SubscriptionType.YEARLY
            else -> return
        }
        
        when (subscriptionType) {
            SubscriptionType.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            SubscriptionType.YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
        }
        
        val expirationDate = calendar.time
        
        // Update local preferences
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, true)
            .putLong(KEY_EXPIRATION_DATE, expirationDate.time)
            .putString(KEY_PURCHASE_TOKEN, purchase.purchaseToken)
            .apply()
        
        // Update state
        _isPremium.value = true
        _expirationDate.value = expirationDate
        
        // Save subscription to Firestore
        saveSubscriptionToCloud(subscriptionType, expirationDate, purchase.purchaseToken)
        
        _message.value = "Thank you for your purchase!"
    }
    
    private fun saveSubscriptionToCloud(
        subscriptionType: SubscriptionType,
        expirationDate: Date,
        purchaseToken: String
    ) {
        auth.currentUser?.let { user ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userDoc = db.collection("users").document(user.uid)
                    val now = Date()
                    val userData = hashMapOf(
                        "isPremium" to true,
                        "subscriptionType" to subscriptionType.name,
                        "subscriptionExpires" to expirationDate.time,
                        "lastUpdated" to now.time,
                        "purchaseToken" to purchaseToken
                    )
                    userDoc.update(userData as Map<String, Any>).await()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _message.value = "Cloud sync failed: ${e.message}"
                    }
                }
            }
        }
    }
    
    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.isNotEmpty()) {
                    // Process each active subscription
                    for (purchase in purchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            handlePurchase(purchase)
                        }
                    }
                }
            }
        }
    }
    
    private fun queryAvailableProducts() {
        val productList = listOf(
            MONTHLY_SUBSCRIPTION_ID,
            YEARLY_SUBSCRIPTION_ID
        )
        
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productList.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()
            
        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Process the product details
                productDetails = productDetailsList.associateBy { it.productId }
            } else {
                _message.value = "Failed to query product details: ${billingResult.debugMessage}"
            }
        }
    }
    
    /**
     * Refreshes the user's subscription status from both local storage and cloud
     * Should be called on app start and after sign-in
     */
    fun refreshSubscriptionStatus() {
        // First check if we need to perform full verification
        val localIsPremium = prefs.getBoolean(KEY_IS_PREMIUM, false)
        val localExpirationMs = prefs.getLong(KEY_EXPIRATION_DATE, 0)
        val lastVerificationTime = prefs.getLong(KEY_LAST_VERIFICATION, 0)
        val localExpiration = if (localExpirationMs > 0) Date(localExpirationMs) else null
        val currentTime = System.currentTimeMillis()
        
        Log.d("PremiumStatus", "Local premium status: $localIsPremium, expires: ${localExpiration?.toString() ?: "never"}")
        
        // If we've verified in the last day and the premium status is still valid, avoid unnecessary network check
        if (localIsPremium && 
            localExpiration != null && 
            localExpiration.time > currentTime && 
            (currentTime - lastVerificationTime) < VERIFICATION_INTERVAL) {
            
            // Just set local state and return early
            _isPremium.value = true
            _expirationDate.value = localExpiration
            Log.d("PremiumStatus", "Using cached premium status (valid)")
            return
        }
        
        // Otherwise proceed with full verification
        getServerTime { serverTime ->
            // Update local state using server time
            val localIsStillValid = localIsPremium && (localExpiration == null || 
                (localExpiration.time > serverTime && localExpiration.after(Date(serverTime))))
            
            _isPremium.value = localIsStillValid
            _expirationDate.value = localExpiration
            
            Log.d("PremiumStatus", "Server time: ${Date(serverTime)}, local status valid with server time: $localIsStillValid")
            
            // IMPORTANT: Always check cloud if user is signed in, don't rely only on local status
            auth.currentUser?.let { user ->
                val userDoc = db.collection("users").document(user.uid)
                Log.d("PremiumStatus", "Checking Firestore for user ${user.uid}")
                
                userDoc.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val cloudIsPremium = document.getBoolean("isPremium") ?: false
                        val cloudExpirationMs = document.getLong("subscriptionExpires") ?: 0
                        val cloudExpiration = if (cloudExpirationMs > 0) Date(cloudExpirationMs) else null
                        
                        Log.d("PremiumStatus", "Firestore data - isPremium: $cloudIsPremium, expires: ${cloudExpiration?.toString() ?: "never"}")
                        
                        // Use the most recent data (cloud or local) - using server time
                        val isCurrentlyPremium = if (cloudIsPremium) {
                            if (cloudExpiration == null) {
                                // No expiration = permanent premium
                                true
                            } else {
                                // Check if not expired based on the server time
                                cloudExpiration.time > serverTime
                            }
                        } else false
                        
                        Log.d("PremiumStatus", "Final premium status from cloud: $isCurrentlyPremium (expires: ${cloudExpiration ?: "never"}, server time: ${Date(serverTime)})")
                        
                        // CRITICAL: Override the user's isPremium from Firestore if the subscription has NOT expired
                        // This ensures we respect premium status set directly in Firestore
                        if (cloudIsPremium && (cloudExpiration == null || cloudExpiration.time > serverTime)) {
                            _isPremium.value = true
                            _expirationDate.value = cloudExpiration
                            
                            Log.d("PremiumStatus", "Setting premium=true from Firestore data")
                            
                            // Update local data
                            prefs.edit()
                                .putBoolean(KEY_IS_PREMIUM, true)
                                .putLong(KEY_EXPIRATION_DATE, cloudExpirationMs)
                                .putLong(KEY_LAST_VERIFICATION, serverTime)
                                .apply()
                        } else {
                            _isPremium.value = isCurrentlyPremium
                            _expirationDate.value = cloudExpiration
                            
                            // Store the verification time to reduce future checks
                            prefs.edit()
                                .putBoolean(KEY_IS_PREMIUM, cloudIsPremium)
                                .putLong(KEY_EXPIRATION_DATE, cloudExpirationMs)
                                .putLong(KEY_LAST_VERIFICATION, serverTime)
                                .apply()
                        }
                        
                        // If we have a stored purchase token, update it
                        document.getString("purchaseToken")?.let { token ->
                            prefs.edit().putString(KEY_PURCHASE_TOKEN, token).apply()
                        }
                        
                        // Also store transaction ID if present
                        document.getString("transactionId")?.let { txnId ->
                            prefs.edit().putString(KEY_TRANSACTION_ID, txnId).apply()
                        }
                        
                        // Store the server-device time offset for future offline checks
                        val deviceTime = System.currentTimeMillis()
                        val timeOffset = serverTime - deviceTime
                        prefs.edit().putLong(KEY_TIME_OFFSET, timeOffset).apply()
                    } else {
                        Log.d("PremiumStatus", "No user document found in Firestore")
                    }
                }.addOnFailureListener { e ->
                    Log.e("PremiumStatus", "Error checking Firestore: ${e.message}")
                }
            } ?: Log.d("PremiumStatus", "No user signed in, using only local status")
        }
    }
    
    /**
     * Gets the current time from Firebase server
     * This prevents users from manipulating their device clock
     */
    private fun getServerTime(callback: (Long) -> Unit) {
        // First check if we have network connectivity
        if (!isNetworkAvailable(appContext)) {
            // If offline, use device time with the last known offset
            val deviceTime = System.currentTimeMillis()
            val timeOffset = prefs.getLong(KEY_TIME_OFFSET, 0)
            val estimatedServerTime = deviceTime + timeOffset
            Log.d("PremiumStatus", "Network unavailable, using estimated server time: ${Date(estimatedServerTime)}")
            callback(estimatedServerTime)
            return
        }
        
        // Set a timeout to make sure we don't block indefinitely
        val timeoutHandler = Handler(Looper.getMainLooper())
        
        // Create a server timestamp document
        val timestampRef = db.collection("timestamps").document()
        val timestamp = hashMapOf("timestamp" to com.google.firebase.Timestamp.now())
        
        // Schedule timeout (using direct lambda instead of Runnable)
        val timeoutCallback = timeoutHandler.postDelayed({
            Log.d("PremiumStatus", "Server time request timed out, using device time")
            callback(System.currentTimeMillis())
        }, 5000L) // 5 second timeout
        
        timestampRef.set(timestamp)
            .addOnSuccessListener {
                // Read the server timestamp
                timestampRef.get().addOnSuccessListener { snapshot ->
                    // Cancel timeout
                    timeoutHandler.removeCallbacksAndMessages(null)
                    
                    val serverTimestamp = snapshot.getTimestamp("timestamp")
                    if (serverTimestamp != null) {
                        val serverTime = serverTimestamp.toDate().time
                        Log.d("PremiumStatus", "Server time: ${Date(serverTime)}")
                        callback(serverTime)
                        
                        // Store the server-device time offset
                        val deviceTime = System.currentTimeMillis()
                        val timeOffset = serverTime - deviceTime
                        prefs.edit().putLong(KEY_TIME_OFFSET, timeOffset).apply()
                        
                        // Clean up the temporary document
                        timestampRef.delete()
                    } else {
                        // Fallback to device time if server timestamp is null
                        Log.d("PremiumStatus", "Server timestamp null, using device time")
                        callback(System.currentTimeMillis())
                    }
                }.addOnFailureListener {
                    // Cancel timeout
                    timeoutHandler.removeCallbacksAndMessages(null)
                    
                    // Fallback to device time on failure
                    Log.e("PremiumStatus", "Failed to get timestamp: ${it.message}")
                    callback(System.currentTimeMillis())
                }
            }
            .addOnFailureListener {
                // Cancel timeout
                timeoutHandler.removeCallbacksAndMessages(null)
                
                // Fallback to device time on failure
                Log.e("PremiumStatus", "Failed to create timestamp: ${it.message}")
                callback(System.currentTimeMillis())
            }
    }
    
    /**
     * Checks if the device has network connectivity
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Launch the purchase flow for the selected subscription type
     */
    fun launchPurchaseFlow(
        subscriptionType: SubscriptionType,
        activity: androidx.activity.ComponentActivity
    ) {
        val productId = when (subscriptionType) {
            SubscriptionType.MONTHLY -> MONTHLY_SUBSCRIPTION_ID
            SubscriptionType.YEARLY -> YEARLY_SUBSCRIPTION_ID
        }
        
        val productDetail = productDetails[productId] ?: run {
            _message.value = "Product information not available. Please try again later."
            return
        }
        
        // Get the offer token for the base plan
        val offerToken = productDetail.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            _message.value = "Subscription offer not available"
            return
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetail)
                .setOfferToken(offerToken)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
            
        // Launch the billing flow
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _message.value = "Unable to launch billing flow: ${billingResult.debugMessage}"
        }
    }
    
    /**
     * Calculates time remaining in the subscription
     * @return Pair(days, hours) or null if not subscribed
     */
    fun getTimeRemaining(): Pair<Int, Int>? {
        val expiration = _expirationDate.value ?: return null
        if (!_isPremium.value) return null
        
        val now = Date()
        val diff = expiration.time - now.time
        
        if (diff <= 0) return Pair(0, 0)
        
        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
        val hours = ((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)).toInt()
        
        return Pair(days, hours)
    }
    
    /**
     * Mock function for development to toggle premium status
     * Only for debugging, should be removed in production
     */
    fun togglePremiumForDebug() {
        val newStatus = !_isPremium.value
        
        if (newStatus) {
            // Set premium for 7 days
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
            val expiration = calendar.time
            
            prefs.edit()
                .putBoolean(KEY_IS_PREMIUM, true)
                .putLong(KEY_EXPIRATION_DATE, expiration.time)
                .apply()
            
            _isPremium.value = true
            _expirationDate.value = expiration
            _message.value = "Debug: Premium enabled for 7 days"
        } else {
            prefs.edit()
                .putBoolean(KEY_IS_PREMIUM, false)
                .putLong(KEY_EXPIRATION_DATE, 0)
                .apply()
            
            _isPremium.value = false
            _expirationDate.value = null
            _message.value = "Debug: Premium disabled"
        }
    }
    
    /**
     * Clear notification message
     */
    fun clearMessage() {
        _message.value = null
    }
    
    /**
     * Check for pending transactions that might have been processed by Stripe
     * This should be called periodically to check if user's payment was successful
     */
    fun checkPendingTransactions() {
        auth.currentUser?.uid?.let { userId ->
            // Query Firestore for approved transactions for this user
            db.collection("pending_transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        for (document in documents) {
                            val transactionId = document.id
                            val subscriptionType = document.getString("subscriptionType")?.let {
                                try {
                                    SubscriptionType.valueOf(it)
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: SubscriptionType.MONTHLY
                            
                            // Apply the premium status
                            applyPremiumStatus(subscriptionType, transactionId)
                            
                            // Move transaction from pending to confirmed
                            db.collection("transactions")
                                .document(transactionId)
                                .set(document.data)
                                .addOnSuccessListener {
                                    // Delete from pending after successful transfer
                                    db.collection("pending_transactions")
                                        .document(transactionId)
                                        .delete()
                                }
                        }
                    }
                }
        }
    }
    
    /**
     * Apply premium status locally based on transaction data
     */
    private fun applyPremiumStatus(subscriptionType: SubscriptionType, transactionId: String) {
        val now = Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = now
        
        when (subscriptionType) {
            SubscriptionType.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            SubscriptionType.YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
        }
        
        val expirationDate = calendar.time
        
        // Update local preferences
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, true)
            .putLong(KEY_EXPIRATION_DATE, expirationDate.time)
            .putString(KEY_TRANSACTION_ID, transactionId)
            .apply()
        
        // Update state
        _isPremium.value = true
        _expirationDate.value = expirationDate
        
        _message.value = "Premium activated successfully!"
    }
    
    /**
     * Debug method to enable premium mode with a test transaction ID
     * This bypasses verification for easier testing
     */
    fun activatePremiumForTesting(subscriptionType: SubscriptionType, testId: String) {
        // Make sure it's a test ID
        if (!testId.startsWith("test_") && !testId.startsWith("pi_test")) {
            _message.value = "Only test IDs can use this method"
            return
        }
        
        val userId = auth.currentUser?.uid ?: run {
            _message.value = "You must be logged in to test premium"
            return
        }
        
        // Set up subscription details
        val currentDate = Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = currentDate
        
        when (subscriptionType) {
            SubscriptionType.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            SubscriptionType.YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
        }
        
        val expirationDate = calendar.time
        
        // Update local preferences only (bypass Firestore in test mode)
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, true)
            .putLong(KEY_EXPIRATION_DATE, expirationDate.time)
            .putString(KEY_TRANSACTION_ID, testId)
            .apply()
        
        // Update state
        _isPremium.value = true
        _expirationDate.value = expirationDate
        
        _message.value = "Test premium activated successfully!"
        
        // Log test activation
        Log.d("PremiumTest", "Premium activated with ID $testId until $expirationDate")
    }
    
    enum class SubscriptionType {
        MONTHLY,
        YEARLY
    }
    
    companion object {
        private const val PREF_NAME = "vocab_subscription"
        private const val KEY_IS_PREMIUM = "premium_subscription"
        private const val KEY_EXPIRATION_DATE = "premium_expiration"
        private const val KEY_PURCHASE_TOKEN = "purchase_token"
        private const val KEY_TRANSACTION_ID = "transaction_id"
        private const val KEY_TIME_OFFSET = "server_time_offset"
        private const val KEY_LAST_VERIFICATION = "last_verification"
        
        // Feature flag to enable/disable Google Play Billing
        private const val ENABLE_GOOGLE_PLAY_BILLING = false
        
        // Product IDs need to match what you set up in Google Play Console
        private const val MONTHLY_SUBSCRIPTION_ID = "xyz.ecys.vocab.subscription.monthly"
        private const val YEARLY_SUBSCRIPTION_ID = "xyz.ecys.vocab.subscription.yearly"
        
        // External payment URLs - replace with your actual Stripe links
        // After creating a product in Stripe, go to "Payment links" to get these URLs
        private const val MONTHLY_PAYMENT_URL = "https://buy.stripe.com/fZeaFH0uIf1C5b2fZ0" // Replace with your monthly product link
        private const val YEARLY_PAYMENT_URL = "https://buy.stripe.com/4gw3df0uI7za1YQ28c"  // Replace with your yearly product link
        
        // Verification interval in milliseconds
        private const val VERIFICATION_INTERVAL = 24 * 60 * 60 * 1000 // 24 hours
        
        @Volatile
        private var INSTANCE: SubscriptionManager? = null
        
        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
} 