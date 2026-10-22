package com.example.carwash.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import com.example.carwash.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAdHelper {

    private const val TAG = "RewardedAdHelper"

    // Official AdMob Rewarded Ad Unit ID from AdMob Dashboard
    var rewardedAdUnitId: String = "ca-app-pub-4394473472808077/5659594080"
    private const val GOOGLE_TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var mRewardedAd: RewardedAd? = null
    private var isLoading = false

    fun loadAd(context: Context, onLoaded: (() -> Unit)? = null) {
        if (mRewardedAd != null) {
            Log.d(TAG, "Rewarded Ad already loaded.")
            onLoaded?.invoke()
            return
        }
        if (isLoading) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()
        val targetAdUnitId = if (rewardedAdUnitId.isNotBlank()) rewardedAdUnitId else GOOGLE_TEST_REWARDED_AD_UNIT_ID

        RewardedAd.load(
            context,
            targetAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    isLoading = false
                    Log.d(TAG, "Rewarded Ad ($targetAdUnitId) loaded successfully.")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mRewardedAd = null
                    isLoading = false
                    Log.e(TAG, "Rewarded Ad ($targetAdUnitId) failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")

                    // Fallback to Google official test rewarded ad unit ID if live unit fails in debug/test environments
                    if (targetAdUnitId != GOOGLE_TEST_REWARDED_AD_UNIT_ID) {
                        Log.d(TAG, "Retrying with Google Test Rewarded Ad Unit ID...")
                        RewardedAd.load(
                            context,
                            GOOGLE_TEST_REWARDED_AD_UNIT_ID,
                            adRequest,
                            object : RewardedAdLoadCallback() {
                                override fun onAdLoaded(rewardedAd: RewardedAd) {
                                    mRewardedAd = rewardedAd
                                    Log.d(TAG, "Test Rewarded Ad loaded successfully.")
                                    onLoaded?.invoke()
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    mRewardedAd = null
                                    Log.e(TAG, "Test Rewarded Ad failed to load: ${error.message}")
                                }
                            }
                        )
                    }
                }
            }
        )
    }

    fun showRewardedAd(
        context: Context,
        onRewardEarned: () -> Unit
    ) {
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing) {
            Log.w(TAG, "Activity not available to show Rewarded Ad.")
            Toast.makeText(context, "Carwash Boy Unlocked!", Toast.LENGTH_SHORT).show()
            onRewardEarned()
            return
        }

        val ad = mRewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                Toast.makeText(context, "Carwash Boy Commission Unlocked!", Toast.LENGTH_SHORT).show()
                onRewardEarned()
            }
            mRewardedAd = null
            loadAd(context)
        } else {
            Log.d(TAG, "Rewarded Ad not ready yet. Preloading and unlocking...")
            Toast.makeText(context, "Loading video ad... Unlocking commission!", Toast.LENGTH_SHORT).show()
            loadAd(context)
            onRewardEarned()
        }
    }

    fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
