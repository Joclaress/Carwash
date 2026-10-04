package com.example.carwash.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.carwash.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Safe AdMob Banner Composable designed to follow Google AdMob Policies and prevent account bans.
 *
 * NOTE FOR PUBLISHING:
 * - Always use Google's official Test Ad Unit ID during development and testing to prevent policy violations/bans.
 * - Test Banner Unit ID: ca-app-pub-3940256099942544/6300978111
 * - Replace `adUnitId` with your Live AdMob Unit ID when building your final Production release.
 */
const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-4394473472808077/1559107131"

fun getBannerAdUnitId(): String {
    return if (BuildConfig.DEBUG) {
        TEST_BANNER_AD_UNIT_ID
    } else {
        PROD_BANNER_AD_UNIT_ID
    }
}

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = getBannerAdUnitId()
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        setAdUnitId(adUnitId)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("AdBanner", "AdMob Banner loaded successfully.")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.e("AdBanner", "AdMob Banner failed to load: ${error.message} (Code: ${error.code})")
                            }
                        }
                        try {
                            loadAd(AdRequest.Builder().build())
                        } catch (e: SecurityException) {
                            Log.e("AdBanner", "SecurityException loading banner ad: ${e.message}", e)
                        } catch (e: Exception) {
                            Log.e("AdBanner", "Exception loading banner ad: ${e.message}", e)
                        }
                    }
                }
            )
        }
    }
}
