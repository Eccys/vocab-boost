package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Manages premium subscription status and subscription-related functions.
 * This class handles checking if a user is subscribed to premium,
 * purchasing subscriptions, and accessing premium features.
 */
class SubscriptionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Current subscription status
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium
    
    // Subscription expiration date
    private val _expirationDate = MutableStateFlow<Date?>(null)
    val expirationDate: StateFlow<Date?> = _expirationDate
    
    // Messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    
    init {
        // Initialize subscription status from preferences or cloud
        refreshSubscriptionStatus()
    }
    
    /**
     * Refreshes the user's subscription status from both local storage and cloud
     * Should be called on app start and after sign-in
     */
    fun refreshSubscriptionStatus() {
        // First check local preferences
        val localIsPremium = prefs.getBoolean(KEY_IS_PREMIUM, false)
        val localExpirationMs = prefs.getLong(KEY_EXPIRATION_DATE, 0)
        val localExpiration = if (localExpirationMs > 0) Date(localExpirationMs) else null
        
        // Update local state
        _isPremium.value = localIsPremium && (localExpiration == null || localExpiration.after(Date()))
        _expirationDate.value = localExpiration
        
        // Then check cloud if user is signed in
        auth.currentUser?.let { user ->
            val userDoc = db.collection("users").document(user.uid)
            userDoc.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val cloudIsPremium = document.getBoolean("isPremium") ?: false
                    val cloudExpirationMs = document.getLong("subscriptionExpires") ?: 0
                    val cloudExpiration = if (cloudExpirationMs > 0) Date(cloudExpirationMs) else null
                    
                    // Use the most recent data (cloud or local)
                    val isCurrentlyPremium = cloudIsPremium && (cloudExpiration == null || cloudExpiration.after(Date()))
                    
                    // Update local preferences and state
                    _isPremium.value = isCurrentlyPremium
                    _expirationDate.value = cloudExpiration
                    
                    prefs.edit()
                        .putBoolean(KEY_IS_PREMIUM, cloudIsPremium)
                        .putLong(KEY_EXPIRATION_DATE, cloudExpirationMs)
                        .apply()
                }
            }
        }
    }
    
    /**
     * Mock function to simulate purchasing a subscription
     * In a real app, this would integrate with Google Play Billing
     */
    suspend fun purchaseSubscription(subscriptionType: SubscriptionType): Boolean {
        // Calculate expiration based on subscription type
        val now = Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = now
        
        when (subscriptionType) {
            SubscriptionType.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            SubscriptionType.YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
            SubscriptionType.LIFETIME -> calendar.add(java.util.Calendar.YEAR, 100) // Effectively lifetime
        }
        
        val expirationDate = calendar.time
        
        // Update local preferences
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, true)
            .putLong(KEY_EXPIRATION_DATE, expirationDate.time)
            .apply()
        
        // Update state
        _isPremium.value = true
        _expirationDate.value = expirationDate
        
        // If user is logged in, update subscription in cloud
        auth.currentUser?.let { user ->
            try {
                val userDoc = db.collection("users").document(user.uid)
                val userData = hashMapOf(
                    "isPremium" to true,
                    "subscriptionType" to subscriptionType.name,
                    "subscriptionExpires" to expirationDate.time,
                    "lastUpdated" to now.time
                )
                userDoc.update(userData as Map<String, Any>).await()
                return true
            } catch (e: Exception) {
                _message.value = "Cloud sync failed: ${e.message}"
                return false
            }
        }
        
        return true
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
    
    enum class SubscriptionType {
        MONTHLY,
        YEARLY,
        LIFETIME
    }
    
    companion object {
        private const val PREF_NAME = "vocab_subscription"
        private const val KEY_IS_PREMIUM = "premium_subscription"
        private const val KEY_EXPIRATION_DATE = "premium_expiration"
        
        @Volatile
        private var INSTANCE: SubscriptionManager? = null
        
        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
} 