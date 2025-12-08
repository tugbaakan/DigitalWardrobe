package com.digitalwardrobe

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Main Application class for Digital Wardrobe.
 * Used for app-wide initialization (Firebase, DI, etc.)
 */
class DigitalWardrobeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}

