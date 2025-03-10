package xyz.ecys.vocab.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.util.Log
import android.app.Application
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository.getInstance()
    private val TAG = "AuthViewModel"

    val currentUser: FirebaseUser? get() = authRepository.currentUser
    val authState: StateFlow<FirebaseUser?> = authRepository.authState

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _signInIntent = MutableStateFlow<Intent?>(null)
    val signInIntent: StateFlow<Intent?> = _signInIntent

    // Auto-sync settings
    private val AUTO_SYNC_INTERVAL = 30 * 60 * 1000L // 30 minutes in milliseconds
    private var lastAutoSyncTime = 0L
    private val autoSyncJob = viewModelScope.launch {
        while (isActive) {
            // Check if user is signed in and if it's time to sync
            if (currentUser != null && !currentUser!!.isAnonymous && 
                System.currentTimeMillis() - lastAutoSyncTime > AUTO_SYNC_INTERVAL) {
                
                try {
                    Log.d(TAG, "Starting auto-sync")
                    authRepository.syncData().await()
                    lastAutoSyncTime = System.currentTimeMillis()
                    Log.d(TAG, "Auto-sync completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-sync failed", e)
                }
            }
            
            // Check again after a delay
            delay(5 * 60 * 1000) // Check every 5 minutes
        }
    }

    fun signUp(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepository.registerUser(email, password)
                .fold(
                    onSuccess = { 
                        _authError.value = null
                        // Send verification email
                        sendEmailVerification {}
                        onComplete(true)
                    },
                    onFailure = { e ->
                        _authError.value = e.message
                        onComplete(false)
                    }
                )
        }
    }

    fun signIn(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepository.loginUser(email, password)
                .fold(
                    onSuccess = { 
                        _authError.value = null
                        // Download data from cloud after sign in
                        try {
                            authRepository.downloadDataFromCloud().await()
                        } catch (e: Exception) {
                            // Log error but don't fail the sign-in
                            Log.e("AuthViewModel", "Error downloading data", e)
                        }
                        onComplete(true)
                    },
                    onFailure = { e ->
                        _authError.value = e.message
                        onComplete(false)
                    }
                )
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            try {
                val intent = authRepository.signInWithGoogle()
                _signInIntent.value = intent
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                authRepository.signOut()
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    fun clearError() {
        _authError.value = null
    }

    fun showMessage(msg: String?) {
        _message.value = msg
    }

    fun sendPasswordResetEmail(email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.sendPasswordResetEmail(email).await()
                _authError.value = null
                onComplete(true)
            } catch (e: Exception) {
                _authError.value = e.message
                onComplete(false)
            }
        }
    }

    fun sendEmailVerification(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification().await()
                _authError.value = null
                onComplete(true)
            } catch (e: Exception) {
                _authError.value = e.message
                onComplete(false)
            }
        }
    }

    fun updateEmail(newEmail: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.updateEmail(newEmail, password).await()
                _authError.value = null
                // Send verification email for new email
                sendEmailVerification {}
                onComplete(true)
            } catch (e: Exception) {
                _authError.value = e.message
                onComplete(false)
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.updatePassword(currentPassword, newPassword).await()
                _authError.value = null
                onComplete(true)
            } catch (e: Exception) {
                _authError.value = e.message
                onComplete(false)
            }
        }
    }

    fun syncData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.syncData().await()
                lastAutoSyncTime = System.currentTimeMillis() // Update last auto-sync time
                _message.value = "Data synced successfully"
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _authError.value = e.message ?: "Sync failed"
                onComplete(false)
            }
        }
    }

    fun downloadDataFromCloud(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.downloadDataFromCloud().await()
                _authError.value = null
                _message.value = "Data downloaded successfully"
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Download error", e)
                val errorMsg = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> "Permission denied. Please check your account."
                    e.message?.contains("NOT_FOUND") == true -> "No data found to download."
                    e.message?.contains("NETWORK") == true -> "Network error. Please check your connection."
                    else -> "Error downloading data: ${e.message}"
                }
                _authError.value = errorMsg
                onComplete(false)
            }
        }
    }

    fun verifyPassword(password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.verifyPassword(password).await()
                _authError.value = null
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password verification error", e)
                _authError.value = "Incorrect password. Please try again."
                onComplete(false)
            }
        }
    }

    fun getLastSyncTime(): String {
        val timestamp = authRepository.getLastSyncTime()
        return if (timestamp != null) {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
        } else {
            "Never"
        }
    }

    fun clearSignInIntent() {
        _signInIntent.value = null
    }

    suspend fun handleGoogleSignInResult(idToken: String) {
        try {
            authRepository.handleGoogleSignInResult(idToken)
            _authError.value = null
            
            // Download data from cloud after sign in
            try {
                authRepository.downloadDataFromCloud().await()
            } catch (e: Exception) {
                // Log error but don't fail the sign-in
                Log.e("AuthViewModel", "Error downloading data", e)
            }
        } catch (e: Exception) {
            _authError.value = e.message
        }
    }

    fun setPasswordForGoogleUser(newPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.setPasswordForGoogleUser(newPassword).await()
                _authError.value = null
                _message.value = "Password set successfully"
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error setting password", e)
                _authError.value = "Error setting password: ${e.message}"
                onComplete(false)
            }
        }
    }

    fun hasPassword(): Boolean {
        return authRepository.hasPassword()
    }

    fun verifyPasswordResetCode(code: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.verifyPasswordResetCode(code).await()
                _authError.value = null
                onComplete(true)
            } catch (e: Exception) {
                _authError.value = e.message
                onComplete(false)
            }
        }
    }

    fun getCurrentUserEmail(): String? {
        return authRepository.getCurrentUserEmail()
    }

    fun canRequestPasswordReset(): Boolean {
        return authRepository.canRequestPasswordReset()
    }

    fun getTimeUntilNextPasswordReset(): Long {
        return authRepository.getTimeUntilNextPasswordReset()
    }

    override fun onCleared() {
        super.onCleared()
        autoSyncJob.cancel()
    }
} 