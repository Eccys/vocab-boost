package xyz.ecys.vocab.quiz

import xyz.ecys.vocab.data.Word
import android.content.Context
import android.util.Log

private const val TAG = "QuizDebug"

fun generateOptions(words: List<Word>, currentWord: Word, context: Context? = null): Pair<List<String>, Int> {
    // Get the preferred number of wrong options from preferences (default to 3)
    val preferredWrongOptionsCount = context?.let {
        val prefs = it.getSharedPreferences("vocab_settings", Context.MODE_PRIVATE)
        prefs.getInt("quiz_wrong_options_count", 3)
    } ?: (words.size - 1) // Fall back to original behavior if context is null
    
    // Log the categories of all words in the batch
    // Log.w(TAG, "Generating options for word: ${currentWord.word} (${currentWord.category})")
    // Log.w(TAG, "Batch contains ${words.size} words:")
    words.forEach { 
        // Log.w(TAG, "  - ${it.word} (${it.category})")
    }
    
    // Choose which synonym (1-3) to use for the correct answer
    val correctSynonymNumber = (1..3).random()
    
    // Get the correct answer based on the chosen synonym number
    val correctAnswer = when (correctSynonymNumber) {
        1 -> currentWord.synonym1
        2 -> currentWord.synonym2
        else -> currentWord.synonym3
    }
    
    // Log.w(TAG, "Selected correct answer: $correctAnswer (from synonym$correctSynonymNumber)")
    
    // Get all synonyms of the current word to avoid using them as wrong answers
    val currentWordSynonyms = setOf(
        currentWord.word,
        currentWord.synonym1,
        currentWord.synonym2,
        currentWord.synonym3
    )
    
    // Create a set to track used synonyms (to avoid duplicates)
    val usedSynonyms = mutableSetOf<String>()
    usedSynonyms.add(correctAnswer) // Add the correct answer to avoid duplicates
    
    // Create list of wrong answers
    val wrongAnswers = mutableListOf<String>()
    
    // Get words of the same category as potential sources for wrong answers
    val sameCategoryWords = words.filter { it.id != currentWord.id && it.category == currentWord.category }
    
    if (sameCategoryWords.isEmpty()) {
        Log.e(TAG, "ERROR: No words of the same category (${currentWord.category}) found in batch!")
    } else {
        // Log.w(TAG, "Found ${sameCategoryWords.size} words of category ${currentWord.category}:")
        sameCategoryWords.forEach {
            // Log.w(TAG, "  - ${it.word} (${it.category})")
        }
    }
    
    // Shuffle the same-category words to randomize selection
    val shuffledWords = sameCategoryWords.shuffled()
    
    // Try to get synonyms from same-category words first
    for (word in shuffledWords) {
        if (wrongAnswers.size >= preferredWrongOptionsCount) break
        
        // Try each synonym in random order
        val synonymNumbers = (1..3).shuffled()
        for (num in synonymNumbers) {
            val synonym = when (num) {
                1 -> word.synonym1
                2 -> word.synonym2
                else -> word.synonym3
            }
            
            // Check if this synonym is valid (not a duplicate and not from the current word)
            if (synonym !in usedSynonyms && synonym !in currentWordSynonyms) {
                wrongAnswers.add(synonym)
                usedSynonyms.add(synonym)
                // Log.w(TAG, "Added wrong answer: $synonym (from word: ${word.word}, category: ${word.category}, synonym$num)")
                break // Move to next word after finding a valid synonym
            }
        }
    }
    
    // If we still don't have enough options, use words from other categories as a last resort
    if (wrongAnswers.size < preferredWrongOptionsCount) {
        Log.e(TAG, "WARNING: Not enough same-category words (${wrongAnswers.size}/$preferredWrongOptionsCount), using other categories")
        
        val otherWords = words.filter { it.id != currentWord.id && it !in sameCategoryWords }.shuffled()
        
        for (word in otherWords) {
            if (wrongAnswers.size >= preferredWrongOptionsCount) break
            
            val synonymNumbers = (1..3).shuffled()
            for (num in synonymNumbers) {
                val synonym = when (num) {
                    1 -> word.synonym1
                    2 -> word.synonym2
                    else -> word.synonym3
                }
                
                if (synonym !in usedSynonyms && synonym !in currentWordSynonyms) {
                    wrongAnswers.add(synonym)
                    usedSynonyms.add(synonym)
                    Log.e(TAG, "Added fallback answer: $synonym (from word: ${word.word}, category: ${word.category}, synonym$num)")
                    break
                }
            }
        }
    }
    
    // Final check: Log all selected options
    // Log.w(TAG, "Final options selected:")
    (wrongAnswers + correctAnswer).forEach { option ->
        val sourceWord = words.find { word ->
            word.synonym1 == option || word.synonym2 == option || word.synonym3 == option
        }
        if (sourceWord != null) {
            // Log.w(TAG, "  - $option (from word: ${sourceWord.word}, category: ${sourceWord.category})")
        }
    }
    
    // If we still don't have enough options, this is a serious issue
    if (wrongAnswers.size < preferredWrongOptionsCount) {
        Log.e(TAG, "CRITICAL: Could not generate enough valid options!")
    }
    
    // Combine and shuffle the options
    return Pair((wrongAnswers + correctAnswer).shuffled(), correctSynonymNumber)
} 