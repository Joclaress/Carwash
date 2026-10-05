package com.example.carwash

import android.app.Application
import android.util.Log
import com.example.carwash.repository.OfflineSaleSyncManager
import com.example.carwash.repository.SaleRepository
import com.example.carwash.utils.BugReporter
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CarwashApplication : Application() {

    @Inject
    lateinit var saleRepository: SaleRepository

    override fun onCreate() {
        super.onCreate()

        // Set uncaught crash log handler to capture bug reports and handle GMS broker security exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val msg = throwable.message.orEmpty()
            val causeMsg = throwable.cause?.message.orEmpty()
            val isGmsSecurityException = (
                ((throwable is SecurityException) && (msg.contains("com.google.android.gms") || msg.contains("Unknown calling package name"))) ||
                ((throwable.cause is SecurityException) && (causeMsg.contains("com.google.android.gms") || causeMsg.contains("Unknown calling package name")))
            )

            if (isGmsSecurityException) {
                Log.e("CarwashApps", "Handled background GMS broker SecurityException in thread ${thread.name}: $msg")
            } else {
                Log.e("CarwashApps", "Uncaught exception in thread ${thread.name}", throwable)
                BugReporter.saveCrashLog(this, throwable)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        Log.d("CarwashApps", "Application onCreate started")

        // 1. Check Google Play Services Availability FIRST before initializing GMS dependent features
        var isGmsAvailable = false
        try {
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(this)

            if (resultCode == ConnectionResult.SUCCESS) {
                isGmsAvailable = true
                Log.d("CarwashApps", "Google Play Services is available and verified.")
            } else {
                Log.w("CarwashApps", "Google Play Services issue: ${availability.getErrorString(resultCode)} (code: $resultCode)")
            }
        } catch (e: SecurityException) {
            Log.e("CarwashApps", "SecurityException checking Google Play Services: ${e.message}")
        } catch (e: Exception) {
            Log.e("CarwashApps", "Error checking Google Play Services availability: ${e.message}")
        }

        // 2. Initialize Firebase safely
        try {
            val apps = FirebaseApp.getApps(this)
            if (apps.isEmpty()) {
                Log.w("CarwashApps", "Firebase not initialized automatically, initializing manually...")
                FirebaseApp.initializeApp(this)
            } else {
                Log.d("CarwashApps", "Firebase initialized successfully with ${apps.size} apps.")
            }
        } catch (e: SecurityException) {
            Log.e("CarwashApps", "SecurityException initializing Firebase: ${e.message}")
        } catch (e: Exception) {
            Log.e("CarwashApps", "Error initializing Firebase: ${e.message}")
        }

        // 3. Initialize Google AdMob SDK ONLY if Google Play Services is available
        if (isGmsAvailable) {
            try {
                val requestConfiguration = RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR))
                    .build()
                MobileAds.setRequestConfiguration(requestConfiguration)

                MobileAds.initialize(this) { status ->
                    Log.d("CarwashApps", "AdMob MobileAds initialized successfully: $status")
                }
            } catch (e: SecurityException) {
                Log.e("CarwashApps", "SecurityException during MobileAds init: ${e.message}")
            } catch (e: Exception) {
                Log.e("CarwashApps", "Error initializing MobileAds: ${e.message}")
            }
        } else {
            Log.w("CarwashApps", "Skipping MobileAds initialization because Google Play Services is not ready/available on this device/emulator.")
        }

        // 4. Start offline sale auto-sync connectivity listener
        try {
            OfflineSaleSyncManager.startAutoSyncOnConnectivity(this, saleRepository)
        } catch (e: Exception) {
            Log.e("CarwashApps", "Error starting offline sale sync listener: ${e.message}")
        }
    }
}
