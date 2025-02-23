package xyz.ecys.vocab.quiz

import xyz.ecys.vocab.data.Word

fun generateOptions(words: List<Word>, currentWord: Word): Pair<List<String>, Int> {
    // Choose which synonym (1-3) to use for the correct answer
    val correctSynonymNumber = (1..3).random()
    
    // Get the correct answer based on the chosen synonym number
    val correctAnswer = when (correctSynonymNumber) {
        1 -> currentWord.synonym1
        2 -> currentWord.synonym2
        else -> currentWord.synonym3
    }
    
    // Create list of wrong answers by getting random synonyms from other words
    val wrongAnswers = words
        .filter { it.id != currentWord.id }
        .map { word ->
            val randomSynonymNumber = (1..3).random()
            when (randomSynonymNumber) {
                1 -> word.synonym1
                2 -> word.synonym2
                else -> word.synonym3
            }
        }
    
    return Pair((wrongAnswers + correctAnswer).shuffled(), correctSynonymNumber)
} 