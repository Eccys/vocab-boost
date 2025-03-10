package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "vocab_settings", Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_SPACED_REPETITION = "spaced_repetition"
        private const val KEY_QUIZ_WRONG_OPTIONS = "quiz_wrong_options_count"
        private const val DEFAULT_WRONG_OPTIONS_COUNT = 3
        
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                SettingsManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    fun isSpacedRepetitionEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SPACED_REPETITION, true)
    }
    
    fun getMultipleChoiceOptionsCount(): Int {
        // Return total number of options (wrong options + 1 correct option)
        val wrongOptionsCount = sharedPreferences.getInt(KEY_QUIZ_WRONG_OPTIONS, DEFAULT_WRONG_OPTIONS_COUNT)
        return wrongOptionsCount + 1
    }
} 