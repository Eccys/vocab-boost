package xyz.ecys.vocab.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["word"], unique = true)
    ]
)
data class Word(
    @PrimaryKey(autoGenerate = true) 
    val id: Int = 0,
    
    val word: String,
    val definition: String,
    
    // First synonym set
    val synonym1: String,
    val synonym1Definition: String,
    val synonym1ExampleSentence: String,
    
    // Second synonym set
    val synonym2: String,
    val synonym2Definition: String,
    val synonym2ExampleSentence: String,
    
    // Third synonym set
    val synonym3: String,
    val synonym3Definition: String,
    val synonym3ExampleSentence: String,
    
    var isBookmarked: Boolean = false,
    
    // Learning metadata
    var lastReviewed: Long = 0,  // timestamp
    var timesReviewed: Int = 0,
    var timesCorrect: Int = 0,
    
    // Spaced repetition metadata
    var easeFactor: Float = 2.5f,  // SuperMemo-2 ease factor
    var interval: Int = 0,  // in days
    var repetitionCount: Int = 0,
    var nextReviewDate: Long = 0,  // timestamp
    val quality: Int = 0 // Add this field
)

@Entity(tableName = "app_usage")
data class AppUsage(
    @PrimaryKey
    val date: Long,  // Store date as timestamp for easier SQLite storage
    val duration: Long = 0,  // Store duration in milliseconds
    val sessionCount: Int = 0,  // Number of quiz sessions on this date
    val correctAnswers: Int = 0  // Number of correct answers on this date
) 