package xyz.ecys.vocab.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.EmailAuthProvider
import com.google.android.gms.tasks.Tasks
import xyz.ecys.vocab.R
import android.content.Intent
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.Dispatchers

class AuthRepository private constructor(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val googleSignInClient: GoogleSignInClient
    
    private val _authState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val authState: StateFlow<FirebaseUser?> = _authState

    private var lastSyncTimestamp: Long? = null
    private val TAG = "AuthRepository"

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _authState.value = firebaseAuth.currentUser
        }

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    suspend fun registerUser(email: String, password: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                val userData = hashMapOf(
                    "email" to email,
                    "createdAt" to System.currentTimeMillis(),
                    "lastSyncTimestamp" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid)
                    .set(userData)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signInWithGoogle(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleGoogleSignInResult(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    suspend fun signOut() {
        googleSignInClient.signOut().await()
        auth.signOut()
    }

    fun sendPasswordResetEmail(email: String): Task<Void> {
        return auth.sendPasswordResetEmail(email)
    }

    fun sendEmailVerification(): Task<Void> {
        return auth.currentUser?.sendEmailVerification() ?: throw Exception("No user logged in")
    }

    fun updateEmail(newEmail: String, password: String): Task<Void> {
        val user = auth.currentUser ?: throw Exception("No user logged in")
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        return user.reauthenticate(credential)
            .continueWithTask { task ->
                if (task.isSuccessful) {
                    user.updateEmail(newEmail)
                } else {
                    throw task.exception ?: Exception("Reauthentication failed")
                }
            }
    }

    fun updatePassword(currentPassword: String, newPassword: String): Task<Void> {
        val user = auth.currentUser ?: throw Exception("No user logged in")
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        return user.reauthenticate(credential)
            .continueWithTask { task ->
                if (task.isSuccessful) {
                    user.updatePassword(newPassword)
                } else {
                    throw task.exception ?: Exception("Reauthentication failed")
                }
            }
    }

    fun verifyPassword(password: String): Task<Void> {
        val user = auth.currentUser ?: throw Exception("No user logged in")
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        return user.reauthenticate(credential)
    }

    fun syncData(): Task<Void> {
        val user = auth.currentUser ?: throw Exception("No user logged in")
        val userId = user.uid
        
        // Get database instances
        val wordDatabase = WordDatabase.getDatabase(context)
        val wordDao = wordDatabase.wordDao()
        val appUsageDao = wordDatabase.appUsageDao()
        
        // Create a batch operation for Firestore
        val batch = firestore.batch()
        
        // Create a task that will be completed when all operations are done
        val taskCompletionSource = com.google.android.gms.tasks.TaskCompletionSource<Void>()
        
        // Launch a coroutine to perform the sync operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Sync words data - only for words that have been reviewed at least once
                val allWords = wordDao.getAllWords().filter { it.timesReviewed > 0 }
                val wordsCollection = firestore.collection("users").document(userId)
                    .collection("words")
                
                // First check if we have remote data that's newer
                val remoteWordsSnapshot = wordsCollection.get().await()
                val remoteWords = mutableMapOf<String, Map<String, Any>>()
                
                for (doc in remoteWordsSnapshot.documents) {
                    val wordData = doc.data
                    if (wordData != null) {
                        remoteWords[doc.id] = wordData
                    }
                }
                
                // Process local words
                for (word in allWords) {
                    val wordId = word.word.lowercase().trim()
                    val wordDoc = wordsCollection.document(wordId)
                    
                    // Check if we have a remote version
                    val remoteWord = remoteWords[wordId]
                    
                    // Prepare word data for Firestore - only include learning progress data, not content data
                    val wordData = hashMapOf(
                        "word" to word.word,  // We need the word text as an identifier
                        "isBookmarked" to word.isBookmarked,
                        "timesReviewed" to word.timesReviewed,
                        "timesCorrect" to word.timesCorrect,
                        "lastReviewed" to word.lastReviewed,
                        "easeFactor" to word.easeFactor,
                        "interval" to word.interval,
                        "repetitionCount" to word.repetitionCount,
                        "nextReviewDate" to word.nextReviewDate,
                        "quality" to word.quality,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    
                    // If remote word exists and has a newer lastUpdated timestamp, 
                    // we'll merge the data favoring the newer values for learning metadata
                    if (remoteWord != null && remoteWord["lastUpdated"] as? Long ?: 0 > word.lastReviewed) {
                        // Update local word with remote learning data
                        wordDao.updateWordLearningData(
                            wordId = word.id,
                            isBookmarked = remoteWord["isBookmarked"] as? Boolean ?: word.isBookmarked,
                            timesReviewed = remoteWord["timesReviewed"] as? Int ?: word.timesReviewed,
                            timesCorrect = remoteWord["timesCorrect"] as? Int ?: word.timesCorrect,
                            lastReviewed = remoteWord["lastReviewed"] as? Long ?: word.lastReviewed,
                            easeFactor = (remoteWord["easeFactor"] as? Double)?.toFloat() ?: word.easeFactor,
                            interval = remoteWord["interval"] as? Int ?: word.interval,
                            repetitionCount = remoteWord["repetitionCount"] as? Int ?: word.repetitionCount,
                            nextReviewDate = remoteWord["nextReviewDate"] as? Long ?: word.nextReviewDate,
                            quality = remoteWord["quality"] as? Int ?: word.quality
                        )
                    } else {
                        // Upload local word data to Firestore
                        batch.set(wordDoc, wordData, SetOptions.merge())
                    }
                }
                
                // 2. Sync app usage data
                val startDate = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000) // Last year
                val usageData = appUsageDao.getUsageBetweenDatesSync(startDate, System.currentTimeMillis())
                
                val usageCollection = firestore.collection("users").document(userId)
                    .collection("app_usage")
                
                for (usage in usageData) {
                    val dateStr = usage.date.toString()
                    val usageDoc = usageCollection.document(dateStr)
                    
                    val usageDataMap = hashMapOf(
                        "date" to usage.date,
                        "duration" to usage.duration,
                        "sessionCount" to usage.sessionCount,
                        "correctAnswers" to usage.correctAnswers
                    )
                    
                    batch.set(usageDoc, usageDataMap, SetOptions.merge())
                }
                
                // 3. Update last sync timestamp in user document
                lastSyncTimestamp = System.currentTimeMillis()
                val userDoc = firestore.collection("users").document(userId)
                
                // Check if the user document exists, if not create it
                val userSnapshot = userDoc.get().await()
                if (!userSnapshot.exists()) {
                    // Create the user document first
                    val userData = hashMapOf(
                        "email" to auth.currentUser?.email,
                        "lastSyncTimestamp" to lastSyncTimestamp,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(userDoc, userData)
                } else {
                    // Update existing user document
                    batch.update(userDoc, "lastSyncTimestamp", lastSyncTimestamp)
                }
                
                // 4. Commit all changes
                batch.commit().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Data sync successful")
                        taskCompletionSource.setResult(null)
                    } else {
                        Log.e(TAG, "Data sync failed", task.exception)
                        taskCompletionSource.setException(task.exception ?: Exception("Unknown error during sync"))
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync", e)
                taskCompletionSource.setException(e)
            }
        }
        
        return taskCompletionSource.task
    }

    fun downloadDataFromCloud(): Task<Void> {
        val user = auth.currentUser ?: throw Exception("No user logged in")
        val userId = user.uid
        
        // Get database instances
        val wordDatabase = WordDatabase.getDatabase(context)
        val wordDao = wordDatabase.wordDao()
        val appUsageDao = wordDatabase.appUsageDao()
        
        // Create a task that will be completed when all operations are done
        val taskCompletionSource = com.google.android.gms.tasks.TaskCompletionSource<Void>()
        
        // Launch a coroutine to perform the download operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Download words data
                val wordsCollection = firestore.collection("users").document(userId)
                    .collection("words")
                
                val remoteWordsSnapshot = wordsCollection.get().await()
                
                for (doc in remoteWordsSnapshot.documents) {
                    val wordData = doc.data ?: continue
                    
                    // Check if word already exists in local database
                    val wordText = wordData["word"] as? String ?: continue
                    val existingWord = wordDao.getWordByText(wordText)
                    
                    if (existingWord != null) {
                        // Update existing word with remote learning data
                        wordDao.updateWordLearningData(
                            wordId = existingWord.id,
                            isBookmarked = wordData["isBookmarked"] as? Boolean ?: existingWord.isBookmarked,
                            timesReviewed = wordData["timesReviewed"] as? Int ?: existingWord.timesReviewed,
                            timesCorrect = wordData["timesCorrect"] as? Int ?: existingWord.timesCorrect,
                            lastReviewed = wordData["lastReviewed"] as? Long ?: existingWord.lastReviewed,
                            easeFactor = (wordData["easeFactor"] as? Double)?.toFloat() ?: existingWord.easeFactor,
                            interval = wordData["interval"] as? Int ?: existingWord.interval,
                            repetitionCount = wordData["repetitionCount"] as? Int ?: existingWord.repetitionCount,
                            nextReviewDate = wordData["nextReviewDate"] as? Long ?: existingWord.nextReviewDate,
                            quality = wordData["quality"] as? Int ?: existingWord.quality
                        )
                    } else {
                        // We can't create a new word from just the learning data
                        // Instead, log that we found a word in the cloud that doesn't exist locally
                        Log.w(TAG, "Found word in cloud that doesn't exist locally: $wordText")
                    }
                }
                
                // 2. Download app usage data
                val usageCollection = firestore.collection("users").document(userId)
                    .collection("app_usage")
                
                val remoteUsageSnapshot = usageCollection.get().await()
                
                for (doc in remoteUsageSnapshot.documents) {
                    val usageData = doc.data ?: continue
                    
                    val date = usageData["date"] as? Long ?: continue
                    val duration = usageData["duration"] as? Long ?: 0
                    val sessionCount = usageData["sessionCount"] as? Int ?: 0
                    val correctAnswers = usageData["correctAnswers"] as? Int ?: 0
                    
                    // Check if usage data already exists for this date
                    val existingUsage = appUsageDao.getUsageForDate(date)
                    
                    if (existingUsage != null) {
                        // Merge local and remote usage data
                        appUsageDao.updateUsage(
                            date = date,
                            duration = existingUsage.duration + duration,
                            sessionCount = existingUsage.sessionCount + sessionCount,
                            correctAnswers = existingUsage.correctAnswers + correctAnswers
                        )
                    } else {
                        // Create new usage data
                        val newUsage = AppUsage(
                            date = date,
                            duration = duration,
                            sessionCount = sessionCount,
                            correctAnswers = correctAnswers
                        )
                        appUsageDao.recordUsage(newUsage)
                    }
                }
                
                // 3. Update last sync timestamp
                lastSyncTimestamp = System.currentTimeMillis()
                val userDoc = firestore.collection("users").document(userId)
                
                // Check if the user document exists, if not create it
                val userSnapshot = userDoc.get().await()
                if (!userSnapshot.exists()) {
                    // Create the user document first
                    val userData = hashMapOf(
                        "email" to auth.currentUser?.email,
                        "lastSyncTimestamp" to lastSyncTimestamp,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    userDoc.set(userData).await()
                } else {
                    // Update existing user document
                    userDoc.update("lastSyncTimestamp", lastSyncTimestamp).await()
                }
                
                Log.d(TAG, "Data download successful")
                taskCompletionSource.setResult(null)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during data download", e)
                taskCompletionSource.setException(e)
            }
        }
        
        return taskCompletionSource.task
    }

    fun getLastSyncTime(): Long? = lastSyncTimestamp

    val currentUser get() = auth.currentUser

    fun setPasswordForGoogleUser(newPassword: String): Task<Void> {
        val user = auth.currentUser ?: throw Exception("No user logged in")
        
        // Check if the user is a Google user
        val isGoogleUser = user.providerData.any { it.providerId == "google.com" }
        if (!isGoogleUser) {
            throw Exception("This method is only for Google users")
        }
        
        // Link the email/password provider to the Google account
        val credential = EmailAuthProvider.getCredential(user.email!!, newPassword)
        return user.linkWithCredential(credential)
            .continueWithTask { task ->
                if (task.isSuccessful) {
                    // Update user document to indicate they have a password
                    val userDoc = firestore.collection("users").document(user.uid)
                    userDoc.update("hasPassword", true)
                } else {
                    throw task.exception ?: Exception("Failed to set password")
                }
            }
    }
    
    fun hasPassword(): Boolean {
        val user = auth.currentUser ?: return false
        
        // Check if the user has the password provider
        return user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    }

    companion object {
        @Volatile
        private var instance: AuthRepository? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AuthRepository(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): AuthRepository {
            return instance ?: throw IllegalStateException(
                "AuthRepository must be initialized first"
            )
        }
    }
} 