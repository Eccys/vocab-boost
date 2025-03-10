package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class QuizStateManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "quiz_state", Context.MODE_PRIVATE
    )
    
    companion object {
        private const val TAG = "QuizStateManager"
        private const val KEY_LIVES = "lives_remaining"
        private const val KEY_HINTS = "hints_remaining"
        private const val DEFAULT_LIVES = 3
        private const val DEFAULT_HINTS = 3
        
        @Volatile
        private var INSTANCE: QuizStateManager? = null
        
        fun getInstance(context: Context): QuizStateManager {
            return INSTANCE ?: synchronized(this) {
                QuizStateManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    fun saveLivesCount(lives: Int) {
        Log.d(TAG, "Saving lives count: $lives")
        sharedPreferences.edit().putInt(KEY_LIVES, lives).commit()
    }
    
    fun getLivesCount(): Int {
        val lives = sharedPreferences.getInt(KEY_LIVES, DEFAULT_LIVES)
        Log.d(TAG, "Getting lives count: $lives")
        return lives
    }
    
    fun saveHintsCount(hints: Int) {
        Log.d(TAG, "Saving hints count: $hints")
        sharedPreferences.edit().putInt(KEY_HINTS, hints).commit()
    }
    
    fun getHintsCount(): Int {
        val hints = sharedPreferences.getInt(KEY_HINTS, DEFAULT_HINTS)
        Log.d(TAG, "Getting hints count: $hints")
        return hints
    }
    
    fun resetState() {
        Log.d(TAG, "Resetting state to defaults")
        sharedPreferences.edit()
            .putInt(KEY_LIVES, DEFAULT_LIVES)
            .putInt(KEY_HINTS, DEFAULT_HINTS)
            .commit()
    }
} 