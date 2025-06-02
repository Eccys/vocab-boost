package xyz.ecys.vocab.data

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.flow.Flow
import xyz.ecys.vocab.BuildConfig

/**
 * Singleton class for Supabase client
 * Handles authentication and database operations
 */
class SupabaseClient private constructor(private val context: Context) {
    
    // Supabase client
    private val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
        
        // Enable logging in debug mode
        if (BuildConfig.DEBUG) {
            httpClient {
                install(io.ktor.client.plugins.logging.Logging) {
                    level = LogLevel.ALL
                }
            }
        }
    }
    
    // Shortcut for auth
    val auth get() = client.auth
    
    // Shortcut for database
    val db get() = client.postgrest
    
    // Shortcut for storage
    val storage get() = client.storage
    
    // Get auth session status as a flow
    val sessionStatus: Flow<SessionStatus> get() = auth.sessionStatus
    
    // Get current user
    val currentUser get() = auth.currentUserOrNull()
    
    // Check if user is logged in
    val isLoggedIn get() = auth.currentSessionOrNull() != null
    
    // Google sign-in
    suspend fun signInWithGoogle(idToken: String) {
        auth.signInWith(Google) {
            this.idToken = idToken
        }
    }
    
    // Helper methods
    
    // Sign up with email and password
    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(io.github.jan.supabase.gotrue.providers.Email) {
            this.email = email
            this.password = password
        }
    }
    
    // Sign in with email and password
    suspend fun signIn(email: String, password: String) {
        auth.signInWith(io.github.jan.supabase.gotrue.providers.Email) {
            this.email = email
            this.password = password
        }
    }
    
    // Sign out
    suspend fun signOut() {
        auth.signOut()
    }
    
    // Reset password
    suspend fun resetPassword(email: String) {
        auth.resetPasswordForEmail(email)
    }
    
    // Update password
    suspend fun updatePassword(newPassword: String) {
        auth.updateUser {
            this.password = newPassword
        }
    }
    
    // Update email
    suspend fun updateEmail(newEmail: String) {
        auth.updateUser {
            this.email = newEmail
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SupabaseClient? = null
        
        fun getInstance(context: Context): SupabaseClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseClient(context).also { INSTANCE = it }
            }
        }
    }
} 