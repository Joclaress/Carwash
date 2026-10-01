package com.example.carwash

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CarwashApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d("CarwashApps", "Application onCreate started")
        try {
            // Check if Firebase is initialized
            val apps = FirebaseApp.getApps(this)
            if (apps.isEmpty()) {
                Log.w("CarwashApps", "Firebase not initialized automatically, initializing manually...")
                FirebaseApp.initializeApp(this)
            } else {
                Log.d("CarwashApps", "Firebase initialized successfully with ${apps.size} apps.")
            }

            // Initialize Google AdMob SDK safely
            MobileAds.initialize(this) { status ->
                Log.d("CarwashApps", "AdMob MobileAds initialized successfully: $status")
            }

            // Check for Google Play Services availability
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(this)

            if (resultCode != ConnectionResult.SUCCESS) {
                Log.e("CarwashApps", "Google Play Services issue: ${availability.getErrorString(resultCode)}")
                if (availability.isUserResolvableError(resultCode)) {
                    Log.w("CarwashApps", "This error is user-resolvable. Please update Google Play Services.")
                }
            } else {
                Log.d("CarwashApps", "Google Play Services is available and verified.")
            }
        } catch (e: Exception) {
            Log.e("CarwashApps", "Error during application initialization", e)
        }
    }
}
