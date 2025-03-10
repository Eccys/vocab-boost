package xyz.ecys.vocab.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Parcelize
data class QuizResult(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    @ServerTimestamp
    val timestamp: Date = Date(),
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val questions: List<QuizQuestion> = emptyList(),
    val score: Float = 0f,
    val durationInSeconds: Long = 0
) : Parcelable {
    // Required for Firestore deserialization
    constructor() : this("", "", Date(), 0, 0, emptyList(), 0f, 0)
}

@Parcelize
data class QuizQuestion(
    val word: String = "",
    val correctDefinition: String = "",
    val userAnswer: String = "",
    val isCorrect: Boolean = false
) : Parcelable {
    // Required for Firestore deserialization
    constructor() : this("", "", "", false)
} 