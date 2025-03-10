package xyz.ecys.vocab.quiz

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Legacy QuizResult class for backwards compatibility with existing code.
 * This is used for in-memory quiz results and passing them between activities.
 * For Firestore storage, use xyz.ecys.vocab.data.QuizResult instead.
 */
@Parcelize
data class QuizResult(
    val word: String,
    val definition: String,
    val userChoice: String,
    val correctChoice: String,
    val isCorrect: Boolean
) : Parcelable 