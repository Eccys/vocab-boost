package xyz.ecys.vocab

import android.app.Application
import xyz.ecys.vocab.data.AuthRepository

class VocabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthRepository.initialize(this)
    }
} 