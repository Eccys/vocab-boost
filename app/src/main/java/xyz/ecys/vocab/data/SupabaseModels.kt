package xyz.ecys.vocab.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.util.Date
import java.util.UUID

/**
 * Data models for Supabase tables
 * These classes are used for serialization/deserialization when interacting with Supabase
 */

@Serializable
data class SupabaseUser(
    val id: String,
    val email: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("last_sync_timestamp")
    val lastSyncTimestamp: Long? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("is_premium")
    val isPremium: Boolean = false,
    @SerialName("subscription_type")
    val subscriptionType: String? = null,
    @SerialName("subscription_expires")
    val subscriptionExpires: String? = null,
    @SerialName("transaction_id")
    val transactionId: String? = null,
    @SerialName("permanent_premium")
    val permanentPremium: Boolean = false,
    @SerialName("last_password_reset_request_time")
    val lastPasswordResetRequestTime: Long? = null
)

@Serializable
data class SupabaseWord(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    val word: String,
    val definition: String,
    @SerialName("example_sentence")
    val exampleSentence: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("last_reviewed")
    val lastReviewed: String? = null,
    @SerialName("next_review")
    val nextReview: String? = null,
    @SerialName("times_reviewed")
    val timesReviewed: Int = 0,
    @SerialName("times_correct")
    val timesCorrect: Int = 0,
    @SerialName("review_stage")
    val reviewStage: Int = 0,
    @SerialName("is_favorite")
    val isFavorite: Boolean = false,
    val notes: String? = null
)

@Serializable
data class SupabaseAppUsage(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    @SerialName("date_id")
    val dateId: String,
    @SerialName("app_opens")
    val appOpens: Int = 0,
    @SerialName("time_spent_seconds")
    val timeSpentSeconds: Int = 0,
    @SerialName("features_used")
    val featuresUsed: Map<String, Int> = emptyMap(),
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class SupabaseQuizQuestion(
    val word: String,
    @SerialName("correct_definition")
    val correctDefinition: String,
    @SerialName("user_answer")
    val userAnswer: String,
    @SerialName("is_correct")
    val isCorrect: Boolean
)

@Serializable
data class SupabaseQuizResult(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    val timestamp: String? = null,
    @SerialName("correct_answers")
    val correctAnswers: Int,
    @SerialName("total_questions")
    val totalQuestions: Int,
    val questions: List<SupabaseQuizQuestion>,
    val score: Float,
    @SerialName("duration_in_seconds")
    val durationInSeconds: Long
)

@Serializable
data class SupabaseHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    val timestamp: String? = null,
    val word: String,
    @SerialName("correct_definition")
    val correctDefinition: String,
    @SerialName("user_answer")
    val userAnswer: String,
    @SerialName("is_correct")
    val isCorrect: Boolean
)

@Serializable
data class SupabaseTransaction(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("subscription_type")
    val subscriptionType: String,
    @SerialName("purchase_date")
    val purchaseDate: String,
    @SerialName("expiration_date")
    val expirationDate: String,
    val status: String,
    @SerialName("verification_method")
    val verificationMethod: String? = null,
    @SerialName("device_info")
    val deviceInfo: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null
)

@Serializable
data class SupabasePendingTransaction(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("subscription_type")
    val subscriptionType: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class SupabaseTimestamp(
    val id: String,
    val timestamp: Long,
    @SerialName("created_at")
    val createdAt: String? = null
) 