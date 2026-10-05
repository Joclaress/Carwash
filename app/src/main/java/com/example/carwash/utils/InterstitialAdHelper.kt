package com.example.carwash.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.carwash.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Safe & AdMob Policy-Compliant Interstitial Ad Helper
 *
 * AdMob Account Protection Safeguards:
 * 1. Automatic Test Ad Unit selection in Debug builds (prevents bans from accidental developer clicks).
 * 2. Strict Frequency Capping (60-second cooldown between ads to strictly follow AdMob "Disruptive Ads" policy).
 * 3. Activity Lifecycle Checks (!isFinishing && !isDestroyed) to prevent overlay crashes and policy violations.
 * 4. Automatic Ad Expiration Handling (discards ads loaded > 50 minutes ago).
 * 5. Non-blocking Async Callback Flow (app navigation proceeds immediately even if ad fails or is skipped).
 */
object InterstitialAdHelper {

    // Official Google Sample/Test Interstitial Ad Unit ID (Prevents 403 & AdMob Policy Violations during testing)
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Production Interstitial Ad Unit ID
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4394473472808077/2861630268"

    // Cooldown interval to prevent showing ads too frequently (AdMob Frequency Capping Policy)
    private const val MIN_SHOW_INTERVAL_MS = 60_000L // 60 seconds

    // Ad expiration threshold (50 minutes)
    private const val AD_EXPIRATION_MS = 50 * 60 * 1000L

    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var lastAdShownTimestamp: Long = 0L
    private var adLoadedTimestamp: Long = 0L

    fun getAdUnitId(): String {
        return if (BuildConfig.DEBUG) {
            TEST_INTERSTITIAL_AD_UNIT_ID
        } else {
            PROD_INTERSTITIAL_AD_UNIT_ID
        }
    }

    fun loadAd(context: Context, adUnitId: String = getAdUnitId()) {
        // Check Google Play Services availability first
        try {
            val availability = GoogleApiAvailability.getInstance()
            if (availability.isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
                Log.w("InterstitialAdHelper", "Google Play Services not available. Skipping interstitial ad load.")
                return
            }
        } catch (e: Exception) {
            Log.e("InterstitialAdHelper", "Error checking GMS availability: ${e.message}")
            return
        }

        val now = System.currentTimeMillis()
        if (mInterstitialAd != null && (now - adLoadedTimestamp < AD_EXPIRATION_MS)) {
            Log.d("InterstitialAdHelper", "Ad already loaded and fresh. Skipping reload request.")
            return
        }

        if (isAdLoading) {
            Log.d("InterstitialAdHelper", "Ad is currently loading. Skipping duplicate request.")
            return
        }

        isAdLoading = true
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d("InterstitialAdHelper", "Interstitial Ad loaded successfully.")
                        mInterstitialAd = interstitialAd
                        adLoadedTimestamp = System.currentTimeMillis()
                        isAdLoading = false
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e("InterstitialAdHelper", "Interstitial Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                        mInterstitialAd = null
                        isAdLoading = false

                        // If production unit failed during debug build, fallback to official Google test unit
                        if (adUnitId != TEST_INTERSTITIAL_AD_UNIT_ID && BuildConfig.DEBUG) {
                            Log.d("InterstitialAdHelper", "Retrying load with Google Official Test Interstitial Unit ID...")
                            loadAd(context, TEST_INTERSTITIAL_AD_UNIT_ID)
                        }
                    }
                }
            )
        } catch (e: SecurityException) {
            Log.e("InterstitialAdHelper", "SecurityException during interstitial ad load: ${e.message}", e)
            mInterstitialAd = null
            isAdLoading = false
        } catch (e: Exception) {
            Log.e("InterstitialAdHelper", "Exception during interstitial ad load: ${e.message}", e)
            mInterstitialAd = null
            isAdLoading = false
        }
    }

    fun showAdIfAvailable(activity: Activity?, onAdDismissed: () -> Unit) {
        val now = System.currentTimeMillis()

        // 1. Verify Activity validity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w("InterstitialAdHelper", "Activity unavailable or finishing. Skipping ad presentation.")
            onAdDismissed()
            return
        }

        // 2. Frequency Capping Check (Prevents AdMob Account Suspension for Disruptive/Excessive Ads)
        val timeSinceLastAd = now - lastAdShownTimestamp
        if (timeSinceLastAd < MIN_SHOW_INTERVAL_MS) {
            val remainingCooldown = (MIN_SHOW_INTERVAL_MS - timeSinceLastAd) / 1000
            Log.d("InterstitialAdHelper", "Ad skipped due to frequency capping cooldown (${remainingCooldown}s remaining).")
            onAdDismissed()
            return
        }

        // 3. Ad Expiration Check
        val ad = mInterstitialAd
        if (ad != null && (now - adLoadedTimestamp > AD_EXPIRATION_MS)) {
            Log.w("InterstitialAdHelper", "Loaded ad expired (>50 min). Discarding and preloading fresh ad...")
            mInterstitialAd = null
            loadAd(activity)
            onAdDismissed()
            return
        }

        // 4. Present Ad if valid and available
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d("InterstitialAdHelper", "Interstitial Ad presented on screen.")
                    lastAdShownTimestamp = System.currentTimeMillis()
                    mInterstitialAd = null
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d("InterstitialAdHelper", "Interstitial Ad dismissed by user.")
                    mInterstitialAd = null
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e("InterstitialAdHelper", "Interstitial Ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdClicked() {
                    Log.d("InterstitialAdHelper", "Interstitial Ad clicked by user.")
                }
            }
            try {
                ad.show(activity)
            } catch (e: SecurityException) {
                Log.e("InterstitialAdHelper", "SecurityException showing interstitial ad (GMS broker issue): ${e.message}", e)
                mInterstitialAd = null
                onAdDismissed()
            } catch (e: Exception) {
                Log.e("InterstitialAdHelper", "Exception showing interstitial ad: ${e.message}", e)
                mInterstitialAd = null
                onAdDismissed()
            }
        } else {
            Log.d("InterstitialAdHelper", "No Interstitial Ad available. Preloading next ad and proceeding.")
            loadAd(activity)
            onAdDismissed()
        }
    }

    fun Context.findActivity(): Activity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}
