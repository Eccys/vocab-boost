package xyz.ecys.vocab.utils

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import xyz.ecys.vocab.data.SupabaseClient
import xyz.ecys.vocab.data.SupabaseUser
import xyz.ecys.vocab.data.SupabaseWord
import xyz.ecys.vocab.data.SupabaseAppUsage
import xyz.ecys.vocab.data.SupabaseQuizResult
import xyz.ecys.vocab.data.SupabaseQuizQuestion
import xyz.ecys.vocab.data.SupabaseHistoryItem
import xyz.ecys.vocab.data.SupabaseTransaction
import java.util.Date
import xyz.ecys.vocab.data.SupabaseAuthRepository

/**
 * Utility class for migrating data from Firebase to Supabase
 */
object MigrationUtils {
    private const val TAG = "MigrationUtils"
    
    /**
     * Migrates user data from Firebase to Supabase
     */
    suspend fun migrateUser(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val firebaseAuth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                val supabase = SupabaseClient.getInstance(context)
                
                // Ensure the user is logged in to both Firebase and Supabase
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    Log.e(TAG, "No Firebase user logged in")
                    return@withContext false
                }
                
                val supabaseUser = supabase.currentUser
                if (supabaseUser == null) {
                    Log.e(TAG, "No Supabase user logged in")
                    return@withContext false
                }
                
                // Get Firebase user data
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                
                if (!userDoc.exists()) {
                    Log.e(TAG, "No Firebase user document found")
                    return@withContext false
                }
                
                // Create Supabase user data
                val userData = SupabaseUser(
                    id = supabaseUser.id,
                    email = supabaseUser.email ?: "",
                    createdAt = Date().toIsoString(),
                    lastSyncTimestamp = userDoc.getLong("lastSyncTimestamp"),
                    updatedAt = Date().toIsoString(),
                    isPremium = userDoc.getBoolean("isPremium") ?: false,
                    subscriptionType = userDoc.getString("subscriptionType"),
                    subscriptionExpires = userDoc.getDate("subscriptionExpires")?.toIsoString(),
                    transactionId = userDoc.getString("transactionId"),
                    permanentPremium = userDoc.getBoolean("permanentPremium") ?: false,
                    lastPasswordResetRequestTime = userDoc.getLong("lastPasswordResetRequestTime")
                )
                
                // Save user data to Supabase
                supabase.db.from("users")
                    .upsert(userData)
                    .execute()
                
                // Migration successful
                Log.d(TAG, "User data migration successful")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating user data", e)
                false
            }
        }
    }
    
    /**
     * Migrates words from Firebase to Supabase
     */
    suspend fun migrateWords(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val firebaseAuth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                val supabase = SupabaseClient.getInstance(context)
                
                // Ensure the user is logged in to both Firebase and Supabase
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    Log.e(TAG, "No Firebase user logged in")
                    return@withContext false
                }
                
                val supabaseUser = supabase.currentUser
                if (supabaseUser == null) {
                    Log.e(TAG, "No Supabase user logged in")
                    return@withContext false
                }
                
                // Get all words from Firebase
                val wordsCollection = firestore.collection("users").document(firebaseUser.uid)
                    .collection("words")
                
                val wordsDocs = wordsCollection.get().await()
                
                // Convert Firebase documents to Supabase models
                val words = wordsDocs.documents.mapNotNull { doc ->
                    try {
                        SupabaseWord(
                            userId = supabaseUser.id,
                            word = doc.getString("word") ?: return@mapNotNull null,
                            definition = doc.getString("definition") ?: return@mapNotNull null,
                            exampleSentence = doc.getString("exampleSentence"),
                            createdAt = doc.getDate("createdAt")?.toIsoString(),
                            updatedAt = doc.getDate("updatedAt")?.toIsoString(),
                            lastReviewed = doc.getDate("lastReviewed")?.toIsoString(),
                            nextReview = doc.getDate("nextReview")?.toIsoString(),
                            timesReviewed = doc.getLong("timesReviewed")?.toInt() ?: 0,
                            timesCorrect = doc.getLong("timesCorrect")?.toInt() ?: 0,
                            reviewStage = doc.getLong("reviewStage")?.toInt() ?: 0,
                            isFavorite = doc.getBoolean("isFavorite") ?: false,
                            notes = doc.getString("notes")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting word document", e)
                        null
                    }
                }
                
                // Save words to Supabase (in batches of 100)
                words.chunked(100).forEach { batch ->
                    supabase.db.from("words")
                        .upsert(batch)
                        .execute()
                    
                    Log.d(TAG, "Migrated ${batch.size} words")
                }
                
                // Migration successful
                Log.d(TAG, "Words migration successful: ${words.size} words migrated")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating words", e)
                false
            }
        }
    }
    
    /**
     * Migrates app usage data from Firebase to Supabase
     */
    suspend fun migrateAppUsage(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val firebaseAuth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                val supabase = SupabaseClient.getInstance(context)
                
                // Ensure the user is logged in to both Firebase and Supabase
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    Log.e(TAG, "No Firebase user logged in")
                    return@withContext false
                }
                
                val supabaseUser = supabase.currentUser
                if (supabaseUser == null) {
                    Log.e(TAG, "No Supabase user logged in")
                    return@withContext false
                }
                
                // Get all app usage from Firebase
                val usageCollection = firestore.collection("users").document(firebaseUser.uid)
                    .collection("app_usage")
                
                val usageDocs = usageCollection.get().await()
                
                // Convert Firebase documents to Supabase models
                val usageItems = usageDocs.documents.mapNotNull { doc ->
                    try {
                        val featuresUsed = doc.get("featuresUsed") as? Map<String, Any> ?: emptyMap()
                        val featuresUsedMap = featuresUsed.mapValues { (it.value as Number).toInt() }
                        
                        SupabaseAppUsage(
                            userId = supabaseUser.id,
                            dateId = doc.id,
                            appOpens = doc.getLong("appOpens")?.toInt() ?: 0,
                            timeSpentSeconds = doc.getLong("timeSpentSeconds")?.toInt() ?: 0,
                            featuresUsed = featuresUsedMap,
                            updatedAt = doc.getDate("updatedAt")?.toIsoString()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting app usage document", e)
                        null
                    }
                }
                
                // Save app usage to Supabase (in batches of 100)
                usageItems.chunked(100).forEach { batch ->
                    supabase.db.from("app_usage")
                        .upsert(batch)
                        .execute()
                    
                    Log.d(TAG, "Migrated ${batch.size} app usage items")
                }
                
                // Migration successful
                Log.d(TAG, "App usage migration successful: ${usageItems.size} items migrated")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating app usage", e)
                false
            }
        }
    }
    
    /**
     * Migrates quiz results from Firebase to Supabase
     */
    suspend fun migrateQuizResults(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val firebaseAuth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                val supabase = SupabaseClient.getInstance(context)
                
                // Ensure the user is logged in to both Firebase and Supabase
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    Log.e(TAG, "No Firebase user logged in")
                    return@withContext false
                }
                
                val supabaseUser = supabase.currentUser
                if (supabaseUser == null) {
                    Log.e(TAG, "No Supabase user logged in")
                    return@withContext false
                }
                
                // Get all quiz results from Firebase
                val resultsCollection = firestore.collection("quiz_results")
                    .whereEqualTo("userId", firebaseUser.uid)
                
                val resultDocs = resultsCollection.get().await()
                
                // Convert Firebase documents to Supabase models
                val quizResults = resultDocs.documents.mapNotNull { doc ->
                    try {
                        // Convert questions array
                        val questionsData = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()
                        val questions = questionsData.map { questionData ->
                            SupabaseQuizQuestion(
                                word = questionData["word"] as? String ?: "",
                                correctDefinition = questionData["correctDefinition"] as? String ?: "",
                                userAnswer = questionData["userAnswer"] as? String ?: "",
                                isCorrect = questionData["isCorrect"] as? Boolean ?: false
                            )
                        }
                        
                        SupabaseQuizResult(
                            id = doc.id,
                            userId = supabaseUser.id,
                            timestamp = doc.getDate("timestamp")?.toIsoString(),
                            correctAnswers = doc.getLong("correctAnswers")?.toInt() ?: 0,
                            totalQuestions = doc.getLong("totalQuestions")?.toInt() ?: 0,
                            questions = questions,
                            score = doc.getDouble("score")?.toFloat() ?: 0f,
                            durationInSeconds = doc.getLong("durationInSeconds") ?: 0
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting quiz result document", e)
                        null
                    }
                }
                
                // Save quiz results to Supabase (in batches of 50)
                quizResults.chunked(50).forEach { batch ->
                    supabase.db.from("quiz_results")
                        .upsert(batch)
                        .execute()
                    
                    Log.d(TAG, "Migrated ${batch.size} quiz results")
                }
                
                // Migration successful
                Log.d(TAG, "Quiz results migration successful: ${quizResults.size} results migrated")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating quiz results", e)
                false
            }
        }
    }
    
    /**
     * Migrates quiz history from Firebase to Supabase
     */
    suspend fun migrateQuizHistory(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val firebaseAuth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                val supabase = SupabaseClient.getInstance(context)
                
                // Ensure the user is logged in to both Firebase and Supabase
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    Log.e(TAG, "No Firebase user logged in")
                    return@withContext false
                }
                
                val supabaseUser = supabase.currentUser
                if (supabaseUser == null) {
                    Log.e(TAG, "No Supabase user logged in")
                    return@withContext false
                }
                
                // Get all history items from Firebase
                val historyCollection = firestore.collection("history")
                    .whereEqualTo("userId", firebaseUser.uid)
                
                val historyDocs = historyCollection.get().await()
                
                // Convert Firebase documents to Supabase models
                val historyItems = historyDocs.documents.mapNotNull { doc ->
                    try {
                        SupabaseHistoryItem(
                            id = doc.id,
                            userId = supabaseUser.id,
                            timestamp = doc.getDate("timestamp")?.toIsoString(),
                            word = doc.getString("word") ?: "",
                            correctDefinition = doc.getString("correctDefinition") ?: "",
                            userAnswer = doc.getString("userAnswer") ?: "",
                            isCorrect = doc.getBoolean("isCorrect") ?: false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting history document", e)
                        null
                    }
                }
                
                // Save history items to Supabase (in batches of 100)
                historyItems.chunked(100).forEach { batch ->
                    supabase.db.from("history")
                        .upsert(batch)
                        .execute()
                    
                    Log.d(TAG, "Migrated ${batch.size} history items")
                }
                
                // Migration successful
                Log.d(TAG, "History migration successful: ${historyItems.size} items migrated")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating history", e)
                false
            }
        }
    }
    
    /**
     * Migrates transactions from Firebase to Supabase
     */
    suspend fun migrateTransactions(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val firebaseAuth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                val supabase = SupabaseClient.getInstance(context)
                
                // Ensure the user is logged in to both Firebase and Supabase
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    Log.e(TAG, "No Firebase user logged in")
                    return@withContext false
                }
                
                val supabaseUser = supabase.currentUser
                if (supabaseUser == null) {
                    Log.e(TAG, "No Supabase user logged in")
                    return@withContext false
                }
                
                // Get all transactions from Firebase
                val transactionsCollection = firestore.collection("transactions")
                    .whereEqualTo("userId", firebaseUser.uid)
                
                val transactionDocs = transactionsCollection.get().await()
                
                // Convert Firebase documents to Supabase models
                val transactions = transactionDocs.documents.mapNotNull { doc ->
                    try {
                        // Check if it's a verified transaction
                        val status = doc.getString("status") ?: "verified"
                        
                        // Calculate purchase date and expiration date
                        val verifiedAt = doc.getTimestamp("verifiedAt")?.toDate() ?: Date()
                        val subscriptionType = doc.getString("subscriptionType") ?: "MONTHLY"
                        
                        val expirationDate = Date(verifiedAt.time).apply {
                            when (subscriptionType) {
                                "YEARLY" -> {
                                    time = time + (365L * 24 * 60 * 60 * 1000)
                                }
                                else -> {
                                    time = time + (30L * 24 * 60 * 60 * 1000)
                                }
                            }
                        }
                        
                        SupabaseTransaction(
                            id = doc.id,
                            userId = supabaseUser.id,
                            subscriptionType = subscriptionType,
                            purchaseDate = verifiedAt.toIsoString(),
                            expirationDate = expirationDate.toIsoString(),
                            status = status,
                            verificationMethod = "migration-from-firebase",
                            deviceInfo = android.os.Build.MODEL,
                            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting transaction document", e)
                        null
                    }
                }
                
                // Save transactions to Supabase
                transactions.forEach { transaction ->
                    supabase.db.from("transactions")
                        .upsert(transaction)
                        .execute()
                    
                    Log.d(TAG, "Migrated transaction: ${transaction.id}")
                }
                
                // Migration successful
                Log.d(TAG, "Transactions migration successful: ${transactions.size} transactions migrated")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating transactions", e)
                false
            }
        }
    }
    
    /**
     * Migrates all data from Firebase to Supabase
     */
    suspend fun migrateAll(context: Context): MigrationResult {
        return withContext(Dispatchers.IO) {
            val result = MigrationResult()
            
            // 1. Check if both Firebase and Supabase users are logged in
            val firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            
            val supabase = SupabaseClient.getInstance(context)
            val supabaseUser = supabase.currentUser
            
            if (firebaseUser == null) {
                result.message = "No Firebase user logged in"
                return@withContext result
            }
            
            if (supabaseUser == null) {
                result.message = "No Supabase user logged in"
                return@withContext result
            }
            
            // 2. Migrate user data
            result.userMigrated = migrateUser(context)
            if (!result.userMigrated) {
                result.message = "Failed to migrate user data"
                return@withContext result
            }
            
            // 3. Migrate words
            result.wordsMigrated = migrateWords(context)
            
            // 4. Migrate app usage
            result.appUsageMigrated = migrateAppUsage(context)
            
            // 5. Migrate quiz results
            result.quizResultsMigrated = migrateQuizResults(context)
            
            // 6. Migrate quiz history
            result.quizHistoryMigrated = migrateQuizHistory(context)
            
            // 7. Migrate transactions
            result.transactionsMigrated = migrateTransactions(context)
            
            // Set success message
            result.success = true
            result.message = "Migration completed successfully"
            
            return@withContext result
        }
    }
    
    /**
     * Result of a migration operation
     */
    data class MigrationResult(
        var success: Boolean = false,
        var message: String = "",
        var userMigrated: Boolean = false,
        var wordsMigrated: Boolean = false,
        var appUsageMigrated: Boolean = false,
        var quizResultsMigrated: Boolean = false,
        var quizHistoryMigrated: Boolean = false,
        var transactionsMigrated: Boolean = false
    )
    
    /**
     * Extension function to convert Date to ISO-8601 string
     */
    private fun Date.toIsoString(): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(this)
    }
} 