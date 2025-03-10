package xyz.ecys.vocab

import android.app.Application
import xyz.ecys.vocab.data.AuthRepository
import xyz.ecys.vocab.data.GlobalQuizState

class VocabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthRepository.initialize(this)
        
        // Initialize the GlobalQuizState
        GlobalQuizState.initialize(this)
    }
} 