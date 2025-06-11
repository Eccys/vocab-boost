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
import com.google.android.gms.tasks.TaskCompletionSource
import xyz.ecys.vocab.data.FirestoreAccessMonitor
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.FieldValue.serverTimestamp
import java.util.Date

class AuthRepository private constructor(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val googleSignInClient: GoogleSignInClient
    
    private val _authState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val authState: StateFlow<FirebaseUser?> = _authState

    private var lastSyncTimestamp: Long? = null
    private val TAG = "AuthRepository"

    // Track last password reset request time
    private var lastPasswordResetRequestTime: Long = 0

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

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun canRequestPasswordReset(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // If user is not logged in, use local variable
        if (auth.currentUser == null) {
            // If lastPasswordResetRequestTime is 0, no request has been made yet
            if (lastPasswordResetRequestTime == 0L) return true
            
            val hoursSinceLastRequest = (currentTime - lastPasswordResetRequestTime) / (1000 * 60 * 60)
            return hoursSinceLastRequest >= 24
        }
        
        // For logged in users, check Firestore
        try {
            // Try to get the value synchronously
            val userDoc = firestore.collection("users").document(auth.currentUser!!.uid).get()
            val task = Tasks.await(userDoc)
            
            if (task.exists() && task.contains("lastPasswordResetRequestTime")) {
                val lastResetTime = task.getLong("lastPasswordResetRequestTime") ?: 0L
                // Update local cache
                lastPasswordResetRequestTime = lastResetTime
                
                val hoursSinceLastRequest = (currentTime - lastResetTime) / (1000 * 60 * 60)
                return hoursSinceLastRequest >= 24
            }
            
            // If no record exists, they can request a reset
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking password reset time", e)
            // Fall back to local variable if there's an error
            if (lastPasswordResetRequestTime == 0L) return true
            
            val hoursSinceLastRequest = (currentTime - lastPasswordResetRequestTime) / (1000 * 60 * 60)
            return hoursSinceLastRequest >= 24
        }
    }

    fun getTimeUntilNextPasswordReset(): Long {
        val currentTime = System.currentTimeMillis()
        
        // If user is not logged in, use local variable
        if (auth.currentUser == null) {
            val millisUntilNextReset = (lastPasswordResetRequestTime + (24 * 60 * 60 * 1000)) - currentTime
            return if (millisUntilNextReset > 0) millisUntilNextReset else 0
        }
        
        // For logged in users, check Firestore
        try {
            // Try to get the value synchronously
            val userDoc = firestore.collection("users").document(auth.currentUser!!.uid).get()
            val task = Tasks.await(userDoc)
            
            if (task.exists() && task.contains("lastPasswordResetRequestTime")) {
                val lastResetTime = task.getLong("lastPasswordResetRequestTime") ?: 0L
                // Update local cache
                lastPasswordResetRequestTime = lastResetTime
                
                val millisUntilNextReset = (lastResetTime + (24 * 60 * 60 * 1000)) - currentTime
                return if (millisUntilNextReset > 0) millisUntilNextReset else 0
            }
            
            // If no record exists, they can request a reset immediately
            return 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking time until next password reset", e)
            // Fall back to local variable if there's an error
            val millisUntilNextReset = (lastPasswordResetRequestTime + (24 * 60 * 60 * 1000)) - currentTime
            return if (millisUntilNextReset > 0) millisUntilNextReset else 0
        }
    }

    fun sendPasswordResetEmail(email: String): Task<Void> {
        // Update the last request time locally
        lastPasswordResetRequestTime = System.currentTimeMillis()
        
        // If user is logged in, also update in Firestore
        if (auth.currentUser != null) {
            val userDoc = firestore.collection("users").document(auth.currentUser!!.uid)
            userDoc.update("lastPasswordResetRequestTime", lastPasswordResetRequestTime)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update password reset time in Firestore", e)
                }
        }
        
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
        
        // Validate Firestore write access
        if (!FirestoreAccessMonitor.validateWrite("AuthRepository.syncData", 
            "Sync user data to cloud", true)) {
            Log.w(TAG, "Unauthorized attempt to write to Firestore, sync aborted")
            val taskCompletionSource = TaskCompletionSource<Void>()
            taskCompletionSource.setException(Exception("Unauthorized Firestore write attempt"))
            return taskCompletionSource.task
        }
        
        // Get database instances
        val wordDatabase = WordDatabase.getDatabase(context)
        val wordDao = wordDatabase.wordDao()
        val appUsageDao = wordDatabase.appUsageDao()
        
        // Create a batch operation for Firestore
        val batch = firestore.batch()
        
        // Create a task that will be completed when all operations are done
        val taskCompletionSource = TaskCompletionSource<Void>()
        
        // Launch a coroutine to perform the sync operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the user document reference
                val userDoc = firestore.collection("users").document(userId)
                
                // 1. First check if we have remote data
                val userSnapshot = userDoc.get().await()
                var remoteWordsMap = mapOf<String, Map<String, Any>>()
                
                // Check if the user document contains the words map
                if (userSnapshot.exists() && userSnapshot.get("words") != null) {
                    remoteWordsMap = userSnapshot.get("words") as? Map<String, Map<String, Any>> ?: mapOf()
                    Log.d(TAG, "Found ${remoteWordsMap.size} words in remote user document")
                }
                
                // 2. Sync words data - only for words that have been reviewed at least once
                val allWords = wordDao.getAllWords().filter { it.timesReviewed > 0 }
                val updatedWordsMap = mutableMapOf<String, Map<String, Any>>()
                
                // Start with the existing remote data
                updatedWordsMap.putAll(remoteWordsMap)
                
                // Process local words
                for (word in allWords) {
                    val wordId = word.word.lowercase().trim()
                    
                    // Check if we have a remote version
                    val remoteWord = remoteWordsMap[wordId]
                    
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
                    if (remoteWord != null && (remoteWord["lastUpdated"] as? Long ?: 0) > word.lastReviewed) {
                        // Update local word with remote learning data
                        wordDao.updateWordLearningData(
                            wordId = word.id,
                            isBookmarked = remoteWord["isBookmarked"] as? Boolean ?: word.isBookmarked,
                            timesReviewed = (remoteWord["timesReviewed"] as? Number)?.toInt() ?: word.timesReviewed,
                            timesCorrect = (remoteWord["timesCorrect"] as? Number)?.toInt() ?: word.timesCorrect,
                            lastReviewed = (remoteWord["lastReviewed"] as? Number)?.toLong() ?: word.lastReviewed,
                            easeFactor = (remoteWord["easeFactor"] as? Number)?.toFloat() ?: word.easeFactor,
                            interval = (remoteWord["interval"] as? Number)?.toInt() ?: word.interval,
                            repetitionCount = (remoteWord["repetitionCount"] as? Number)?.toInt() ?: word.repetitionCount,
                            nextReviewDate = (remoteWord["nextReviewDate"] as? Number)?.toLong() ?: word.nextReviewDate,
                            quality = (remoteWord["quality"] as? Number)?.toInt() ?: word.quality
                        )
                    } else {
                        // Add the local word data to the map to be saved
                        updatedWordsMap[wordId] = wordData
                    }
                }
                
                // 3. Add all words data to the user document
                if (updatedWordsMap.isNotEmpty()) {
                    Log.d(TAG, "Updating user document with ${updatedWordsMap.size} words")
                    batch.update(userDoc, "words", updatedWordsMap)
                }
                
                // 4. Sync app usage data (this remains unchanged)
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
                
                // 5. Update last sync timestamp in user document
                lastSyncTimestamp = System.currentTimeMillis()
                
                // Check if the user document exists, if not create it
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
                    batch.update(userDoc, "lastUpdated", serverTimestamp())
                }
                
                // 6. Commit all changes
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
        
        // Validate Firestore read access - this is allowed during initial load
        if (!FirestoreAccessMonitor.validateRead("AuthRepository.downloadDataFromCloud", 
            "Initial data download")) {
            Log.w(TAG, "Unauthorized attempt to read from Firestore, download aborted")
            val taskCompletionSource = TaskCompletionSource<Void>()
            taskCompletionSource.setException(Exception("Unauthorized Firestore read attempt"))
            return taskCompletionSource.task
        }
        
        // Get database instances
        val wordDatabase = WordDatabase.getDatabase(context)
        val wordDao = wordDatabase.wordDao()
        val appUsageDao = wordDatabase.appUsageDao()
        
        // Create a task that will be completed when all operations are done
        val taskCompletionSource = TaskCompletionSource<Void>()
        
        // Launch a coroutine to perform the download operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. First, try to get the user document which should contain the words map
                Log.d(TAG, "Attempting to read words from user document")
                val userDoc = firestore.collection("users").document(userId)
                val userSnapshot = userDoc.get().await()
                
                if (userSnapshot.exists() && userSnapshot.get("words") != null) {
                    // New format: words are stored in the user document
                    val wordsMap = userSnapshot.get("words") as? Map<String, Map<String, Any>> ?: mapOf()
                    
                    Log.d(TAG, "Found ${wordsMap.size} words in user document")
                    
                    // Process each word in the map
                    for ((wordKey, wordData) in wordsMap) {
                        // Check if word already exists in local database
                        val existingWord = wordDao.getWordByText(wordKey)
                        
                        if (existingWord != null) {
                            Log.d(TAG, "Updating word from user document: $wordKey")
                        
                        // Update existing word with remote learning data
                        wordDao.updateWordLearningData(
                            wordId = existingWord.id,
                            isBookmarked = wordData["isBookmarked"] as? Boolean ?: existingWord.isBookmarked,
                            timesReviewed = (wordData["timesReviewed"] as? Number)?.toInt() ?: existingWord.timesReviewed,
                            timesCorrect = (wordData["timesCorrect"] as? Number)?.toInt() ?: existingWord.timesCorrect,
                            lastReviewed = (wordData["lastReviewed"] as? Number)?.toLong() ?: existingWord.lastReviewed,
                            easeFactor = (wordData["easeFactor"] as? Number)?.toFloat() ?: existingWord.easeFactor,
                            interval = (wordData["interval"] as? Number)?.toInt() ?: existingWord.interval,
                            repetitionCount = (wordData["repetitionCount"] as? Number)?.toInt() ?: existingWord.repetitionCount,
                            nextReviewDate = (wordData["nextReviewDate"] as? Number)?.toLong() ?: existingWord.nextReviewDate,
                            quality = (wordData["quality"] as? Number)?.toInt() ?: existingWord.quality
                        )
                        } else {
                            Log.w(TAG, "Found word in cloud that doesn't exist locally: $wordKey")
                        }
                    }
                } else {
                    // Old format or no data: Check if we need to migrate from old format
                    Log.d(TAG, "No words map found in user document, checking for old format...")
                    
                    val wordsCollection = firestore.collection("users").document(userId)
                        .collection("words")
                    
                    val remoteWordsSnapshot = wordsCollection.get().await()
                    
                    if (remoteWordsSnapshot.documents.isNotEmpty()) {
                        Log.d(TAG, "Found ${remoteWordsSnapshot.documents.size} words in old format. Migrating...")
                        
                        // Prepare a map to hold the migrated data
                        val migratedWordsMap = mutableMapOf<String, Any>()
                        
                        // Process each word document from the old format
                        for (doc in remoteWordsSnapshot.documents) {
                            val wordData = doc.data ?: continue
                            
                            // The word ID in old format
                            val wordText = wordData["word"] as? String ?: continue
                            
                            // Get or process the word data
                            val existingWord = wordDao.getWordByText(wordText)
                            
                            if (existingWord != null) {
                                Log.d(TAG, "Migrating word from old format: $wordText")
                                
                                // Update the local database with the remote data
                                wordDao.updateWordLearningData(
                                    wordId = existingWord.id,
                                    isBookmarked = wordData["isBookmarked"] as? Boolean ?: existingWord.isBookmarked,
                                    timesReviewed = (wordData["timesReviewed"] as? Number)?.toInt() ?: existingWord.timesReviewed,
                                    timesCorrect = (wordData["timesCorrect"] as? Number)?.toInt() ?: existingWord.timesCorrect,
                                    lastReviewed = (wordData["lastReviewed"] as? Number)?.toLong() ?: existingWord.lastReviewed,
                                    easeFactor = (wordData["easeFactor"] as? Number)?.toFloat() ?: existingWord.easeFactor,
                                    interval = (wordData["interval"] as? Number)?.toInt() ?: existingWord.interval,
                                    repetitionCount = (wordData["repetitionCount"] as? Number)?.toInt() ?: existingWord.repetitionCount,
                                    nextReviewDate = (wordData["nextReviewDate"] as? Number)?.toLong() ?: existingWord.nextReviewDate,
                                    quality = (wordData["quality"] as? Number)?.toInt() ?: existingWord.quality
                                )
                                
                                // Add the word data to the migrated map
                                migratedWordsMap[wordText] = wordData
                            }
                        }
                        
                        // Save the migrated words map to the user document
                        if (migratedWordsMap.isNotEmpty()) {
                            Log.d(TAG, "Saving ${migratedWordsMap.size} migrated words to user document")
                            
                            val updateData = hashMapOf<String, Any>(
                                "words" to migratedWordsMap,
                                "lastSyncTimestamp" to System.currentTimeMillis(),
                                "lastUpdated" to serverTimestamp(),
                                "migrationCompleted" to true
                            )
                            
                            userDoc.set(updateData, SetOptions.merge()).await()
                            
                            Log.d(TAG, "Migration complete. Words now stored in user document.")
                        }
                    } else {
                        Log.d(TAG, "No words found in either format. User may be new.")
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
                    val quizDuration = usageData["quizDuration"] as? Long ?: 0
                    val sessionCount = usageData["sessionCount"] as? Int ?: 0
                    val correctAnswers = usageData["correctAnswers"] as? Int ?: 0
                    
                    // Check if usage data already exists for this date
                    val existingUsage = appUsageDao.getUsageForDate(date)
                    
                    if (existingUsage != null) {
                        // Merge local and remote usage data
                        appUsageDao.updateUsage(
                            date = date,
                            duration = existingUsage.duration + duration,
                            quizDuration = existingUsage.quizDuration + quizDuration,
                            sessionCount = existingUsage.sessionCount + sessionCount,
                            correctAnswers = existingUsage.correctAnswers + correctAnswers
                        )
                    
                        // Create new usage data
                        val newUsage = AppUsage(
                            date = date,
                            duration = duration,
                            quizDuration = quizDuration,
                            sessionCount = sessionCount,
                            correctAnswers = correctAnswers
                        )
                        appUsageDao.recordUsage(newUsage)
                    }
                }
                
                // 3. Update last sync timestamp
                lastSyncTimestamp = System.currentTimeMillis()
                
                // Update user document if it doesn't exist yet
                if (!userSnapshot.exists()) {
                    val userData = hashMapOf(
                        "email" to auth.currentUser?.email,
                        "lastSyncTimestamp" to lastSyncTimestamp,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    userDoc.set(userData).await()
                } else {
                    // Just update the timestamp
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

    fun verifyPasswordResetCode(code: String): Task<Void> {
        // In a real app, this would verify the code with Firebase
        // For this demo, we'll simulate verification and reset
        return Tasks.call {
            // Simulate network delay
            Thread.sleep(1000)
            
            // Simulate verification (accept any 6-digit code for demo)
            if (code.length == 6 && code.all { it.isDigit() }) {
                // Code is valid, proceed with password reset
                null
            } else {
                throw Exception("Invalid verification code")
            }
        }
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