package com.example.carwash

import android.app.Application
import android.util.Log
import com.example.carwash.utils.BugReporter
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CarwashApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Set uncaught crash log handler to capture bug reports
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CarwashApps", "Uncaught exception in thread ${thread.name}", throwable)
            BugReporter.saveCrashLog(this, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

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

            // Initialize Google AdMob SDK safely & configure test device rules
            val requestConfiguration = com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

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
