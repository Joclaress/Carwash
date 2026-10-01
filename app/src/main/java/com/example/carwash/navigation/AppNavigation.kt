package com.example.carwash.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.carwash.screen.Login
import com.example.carwash.screen.Register
import com.example.carwash.screen.Screen
import com.example.carwash.screen.SplashScreen
import com.example.carwash.screen.mainscreen.MainScreen

@Composable
fun AppNavigation(
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route
    ) {
        composable(Screen.SplashScreen.route) {
            SplashScreen(navController)
        }

        composable(Screen.Login.route) {
            Login(navController)
        }

        composable(Screen.Register.route) {
            Register(navController)
        }

        composable(Screen.Home.route) {
            MainScreen(
                rootNavController = navController,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode
            )
        }
    }
}
