package com.example.carwash.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdHelper {

    // Google Official Test Interstitial Ad Unit ID (Prevents AdMob Account Bans during testing)
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4394473472808077/2861630268"

    private var mInterstitialAd: InterstitialAd? = null

    fun loadAd(context: Context, adUnitId: String = TEST_INTERSTITIAL_AD_UNIT_ID) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("InterstitialAdHelper", "Interstitial Ad Loaded successfully.")
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("InterstitialAdHelper", "Interstitial Ad failed to load: ${loadAdError.message}")
                    mInterstitialAd = null
                }
            }
        )
    }

    fun showAdIfAvailable(activity: Activity?, onAdDismissed: () -> Unit) {
        val ad = mInterstitialAd
        if (activity != null && ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("InterstitialAdHelper", "Interstitial Ad dismissed by user.")
                    mInterstitialAd = null
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e("InterstitialAdHelper", "Interstitial Ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            Log.d("InterstitialAdHelper", "No Interstitial Ad available, proceeding directly.")
            if (activity != null) {
                loadAd(activity)
            }
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
