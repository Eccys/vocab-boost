package xyz.ecys.vocab.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import xyz.ecys.vocab.data.SupabaseClient
import xyz.ecys.vocab.data.SupabaseAuthRepository
import xyz.ecys.vocab.utils.MigrationUtils

class MigrationViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val supabaseClient = SupabaseClient.getInstance(context)
    private val supabaseAuthRepository = SupabaseAuthRepository.getInstance(context)
    
    private val _isFirebaseSignedIn = MutableStateFlow(false)
    val isFirebaseSignedIn: StateFlow<Boolean> = _isFirebaseSignedIn
    
    private val _isSupabaseSignedIn = MutableStateFlow(false)
    val isSupabaseSignedIn: StateFlow<Boolean> = _isSupabaseSignedIn
    
    private val _migrationState = MutableStateFlow(MigrationUtils.MigrationResult())
    val migrationState: StateFlow<MigrationUtils.MigrationResult> = _migrationState
    
    private val _isMigrating = MutableStateFlow(false)
    val isMigrating: StateFlow<Boolean> = _isMigrating
    
    fun checkAuthStatus() {
        _isFirebaseSignedIn.value = firebaseAuth.currentUser != null
        _isSupabaseSignedIn.value = supabaseClient.isLoggedIn
    }
    
    fun signInToSupabase() {
        viewModelScope.launch {
            try {
                // If already signed in to Firebase, use same credentials
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser != null) {
                    // Get a fresh Firebase ID token
                    val idToken = firebaseUser.getIdToken(true).await().token
                    
                    if (idToken != null) {
                        // Sign in to Supabase with Firebase token (assuming you have OAuth set up)
                        supabaseClient.signInWithGoogle(idToken)
                        _isSupabaseSignedIn.value = supabaseClient.isLoggedIn
                    }
                } else {
                    // Handle case where user isn't signed in to Firebase
                    _migrationState.value = _migrationState.value.copy(
                        message = "Please sign in to Firebase first"
                    )
                }
            } catch (e: Exception) {
                _migrationState.value = _migrationState.value.copy(
                    message = "Error signing in to Supabase: ${e.message}"
                )
            }
        }
    }
    
    suspend fun startMigration() {
        if (!_isFirebaseSignedIn.value || !_isSupabaseSignedIn.value) {
            _migrationState.value = _migrationState.value.copy(
                message = "You must be signed in to both Firebase and Supabase to migrate data"
            )
            return
        }
        
        _isMigrating.value = true
        _migrationState.value = _migrationState.value.copy(message = "Migration in progress...")
        
        try {
            val result = MigrationUtils.migrateAll(context)
            _migrationState.value = result
        } catch (e: Exception) {
            _migrationState.value = _migrationState.value.copy(
                success = false,
                message = "Migration failed: ${e.message}"
            )
        } finally {
            _isMigrating.value = false
        }
    }
    
    // Helper extension function for Firebase Tasks
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return kotlinx.coroutines.tasks.await()
    }
} 