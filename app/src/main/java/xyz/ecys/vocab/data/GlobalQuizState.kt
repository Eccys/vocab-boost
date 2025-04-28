package xyz.ecys.vocab.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * A legacy global singleton for quiz state.
 * NOTE: Lives system has been removed but this class is kept for backward compatibility.
 */
object GlobalQuizState {
    private const val TAG = "GlobalQuizState"
    private const val PREF_NAME = "global_quiz_state"
    private const val KEY_LIVES = "global_lives_remaining"
    private const val KEY_HINTS = "global_hints_remaining"
    private const val DEFAULT_LIVES = 100 // Now set to very high value since lives are unlimited
    private const val DEFAULT_HINTS = 100 // Now set to very high value since hints are unlimited
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appContext: Context
    
    // In-memory cache of the current state
    private var _lives = DEFAULT_LIVES
    private var _hints = DEFAULT_HINTS
    
    /**
     * Initialize the GlobalQuizState. Must be called before using any other methods.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = appContext.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE
            )
            
            // Set default high values since quiz is now endless
            _lives = DEFAULT_LIVES
            _hints = DEFAULT_HINTS
            
            // Persist these high values to make sure they're available everywhere
            setLives(DEFAULT_LIVES)
            setHints(DEFAULT_HINTS)
        }
    }
    
    /**
     * Get the current number of lives (always returns high value now)
     */
    fun getLives(): Int {
        return DEFAULT_LIVES
    }
    
    /**
     * Set the number of lives (no-op now that lives system is removed)
     */
    fun setLives(lives: Int) {
        sharedPreferences.edit().putInt(KEY_LIVES, DEFAULT_LIVES).apply()
    }
    
    /**
     * Decrement lives by 1 (no-op now that lives system is removed)
     */
    fun decrementLives(): Int {
        return DEFAULT_LIVES
    }
    
    /**
     * Get the current number of hints (always returns high value now)
     */
    fun getHints(): Int {
        return DEFAULT_HINTS
    }
    
    /**
     * Set the number of hints (no-op now that hint limits are removed)
     */
    fun setHints(hints: Int) {
        sharedPreferences.edit().putInt(KEY_HINTS, DEFAULT_HINTS).apply()
    }
    
    /**
     * Decrement hints by 1 (no-op now that hint limits are removed)
     */
    fun decrementHints(): Int {
        return DEFAULT_HINTS
    }
    
    /**
     * Reset the state to defaults (no-op since there are no lives or hint limits now)
     */
    fun resetState() {
        if (::sharedPreferences.isInitialized) {
            // Clear preferences and reset in-memory state
            sharedPreferences.edit().clear().apply()
            _lives = DEFAULT_LIVES
            _hints = DEFAULT_HINTS
            
            // Re-set default values
            setLives(DEFAULT_LIVES)
            setHints(DEFAULT_HINTS)
            
            Log.d(TAG, "Quiz state has been reset")
        }
    }
    
    /**
     * For debug purposes: Read current values directly from SharedPreferences
     */
    fun checkPersistedValues(): Pair<Int, Int> {
        return Pair(DEFAULT_LIVES, DEFAULT_HINTS)
    }
} 