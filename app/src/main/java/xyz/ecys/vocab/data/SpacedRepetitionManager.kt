package xyz.ecys.vocab.data

import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

class SpacedRepetitionManager {
    companion object {
        private const val MIN_EASE_FACTOR = 1.3f
        private const val DEFAULT_EASE_FACTOR = 2.5f
        private const val MILLISECONDS_IN_DAY = 86400000L // 24 * 60 * 60 * 1000

        fun calculateNextReview(word: Word, quality: Int, responseTime: Long) {
            // Update times reviewed
            word.timesReviewed++
            
            if (quality >= 3) {
                // Correct answer
                word.timesCorrect++
                word.repetitionCount++
                
                // Update ease factor for correct answers (quality 3-5)
                if (quality >= 3) {
                    val newEaseFactor = word.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
                    word.easeFactor = max(newEaseFactor.toFloat(), MIN_EASE_FACTOR.toFloat())
                }
                
                // Calculate new interval
                word.interval = when (word.repetitionCount) {
                    1 -> 1  // First correct answer: 1 day
                    2 -> 2  // Second correct answer: 2 days
                    else -> (word.interval * word.easeFactor).roundToInt()  // Subsequent: interval * ease factor
                }
            } else {
                // Incorrect answer
                word.repetitionCount = 0
                word.interval = 0
                
                // Adjust ease factor for incorrect answers
                word.easeFactor = max(word.easeFactor - 0.2f, MIN_EASE_FACTOR)
            }
            
            // Update review timestamps
            word.lastReviewed = System.currentTimeMillis()
            word.nextReviewDate = word.lastReviewed + (word.interval * MILLISECONDS_IN_DAY)
        }

        fun getNextWord(words: List<Word>): Word? {
            val now = System.currentTimeMillis()
            
            // First, check for overdue words
            val overdueWords = words.filter { it.nextReviewDate > 0 && it.nextReviewDate <= now }
            if (overdueWords.isNotEmpty()) {
                // Return the hardest overdue word (lowest ease factor)
                return overdueWords.minByOrNull { it.easeFactor }
            }
            
            // If no overdue words, return a random word that hasn't been reviewed yet
            val unreviewed = words.filter { it.nextReviewDate == 0L }
            if (unreviewed.isNotEmpty()) {
                return unreviewed.random()
            }
            
            // If all words have been reviewed, return a random word
            return words.randomOrNull()
        }

        fun calculateQuality(responseTime: Long, isCorrect: Boolean, hasSeenBefore: Boolean): Int {
            if (!isCorrect) {
                return when {
                    !hasSeenBefore -> 1  // Never seen before
                    hasSeenBefore -> 2   // Seen before, answered correctly in the past
                    else -> 0            // Seen before, never answered correctly
                }
            }

            // For correct answers, determine quality based on response time
            return when {
                responseTime < 3000 -> 5  // Fast: < 3 seconds
                responseTime < 5000 -> 4  // Medium: 3-5 seconds
                else -> 3                 // Slow: > 5 seconds
            }
        }
    }
} 