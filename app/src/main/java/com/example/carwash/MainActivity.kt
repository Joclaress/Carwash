package com.example.carwash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.carwash.navigation.AppNavigation
import com.example.carwash.repository.PayMongoRepository
import com.example.carwash.repository.SaleRepository
import com.example.carwash.ui.theme.CarwashTheme
import com.example.carwash.utils.InAppUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var saleRepository: SaleRepository

    @Inject
    lateinit var payMongoRepository: PayMongoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handlePayMongoDeepLink(intent)
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("carwash_app_prefs", Context.MODE_PRIVATE) }
            var isDarkMode by remember {
                mutableStateOf(prefs.getBoolean("dark_mode_enabled", false))
            }

            fun toggleDarkMode(enabled: Boolean) {
                isDarkMode = enabled
                prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
            }

            CarwashTheme(darkTheme = isDarkMode) {
                AppNavigation(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = ::toggleDarkMode
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePayMongoDeepLink(intent)
    }

    private fun handlePayMongoDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "carwash") {
            if (data.host == "paymongo_success") {
                val targetUid = data.getQueryParameter("uid").orEmpty()
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = payMongoRepository.activateSubscriptionForUid(targetUid)
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            Toast.makeText(
                                this@MainActivity,
                                "Payment Successful! Pro Subscription activated for 30 days.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Payment received! Activating account...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else if (data.host == "paymongo_cancel") {
                Toast.makeText(
                    this,
                    "Payment was cancelled. You can try again anytime.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        InAppUpdateManager.checkForAppUpdate(this)
    }
}
