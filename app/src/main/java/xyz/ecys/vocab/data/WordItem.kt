package xyz.ecys.vocab.data

data class WordItem(
    val id: Int,
    val word: String,
    val correctAnswer: String,
    val options: List<String>,
    val category: String
) 