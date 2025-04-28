package xyz.ecys.vocab.quiz.components

// Global hint state that will be accessible to all components
object HintManager {
    var hintWasUsed = false
    
    // Track hint usage by word ID
    private val wordHints = mutableMapOf<Int, Boolean>()
    
    // Reset hint state for a specific word
    fun resetHintForWord(wordId: Int) {
        wordHints[wordId] = false
        // Also reset the global hint state when a word's hint is reset
        hintWasUsed = false
    }
    
    // Check if hint was used for a specific word
    fun wasHintUsedForWord(wordId: Int): Boolean {
        return wordHints[wordId] ?: false
    }
    
    // Mark hint as used for a specific word
    fun markHintUsedForWord(wordId: Int) {
        wordHints[wordId] = true
        hintWasUsed = true
    }
} 