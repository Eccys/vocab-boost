package xyz.ecys.vocab.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuizResult(
    val word: String,
    val definition: String,
    val userChoice: String,
    val correctChoice: String,
    val isCorrect: Boolean
) : Parcelable 