package com.example.carwash.screen

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val route: String) {
    @Serializable
    object SplashScreen : Screen("splash_screen")
    @Serializable
    object Login : Screen("login")
    @Serializable
    object Register : Screen("register")
    @Serializable
    object Home : Screen("home")
    @Serializable
    object Add : Screen("add")
    @Serializable
    object History : Screen("history")
    @Serializable
    object Profile : Screen("profile")
}
