package xyz.ecys.vocab.data

data class WordItem(
    val word: String,
    val correctAnswer: String,
    val options: List<String>
) 