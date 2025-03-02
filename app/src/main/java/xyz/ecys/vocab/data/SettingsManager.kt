package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "vocab_settings", Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_MULTIPLE_CHOICE_OPTIONS = "multiple_choice_options"
        private const val DEFAULT_OPTIONS_COUNT = 4
        
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
    
    fun getMultipleChoiceOptionsCount(): Int {
        return sharedPreferences.getInt(KEY_MULTIPLE_CHOICE_OPTIONS, DEFAULT_OPTIONS_COUNT)
    }
    
    fun setMultipleChoiceOptionsCount(count: Int) {
        sharedPreferences.edit().putInt(KEY_MULTIPLE_CHOICE_OPTIONS, count).apply()
    }
} 