package com.example.carwash

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.carwash.navigation.AppNavigation
import com.example.carwash.repository.SaleRepository
import com.example.carwash.ui.theme.CarwashTheme
import com.example.carwash.utils.InAppUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var saleRepository: SaleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    override fun onResume() {
        super.onResume()
        InAppUpdateManager.checkForAppUpdate(this)
    }
}
