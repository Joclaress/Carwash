package com.example.carwash.screen.mainscreen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.carwash.add.AddSaleScreen
import com.example.carwash.history.SalesHistoryScreen
import com.example.carwash.home.HomeScreen.HomeScreen
import com.example.carwash.model.AuthViewModel
import com.example.carwash.screen.Screen
import com.example.carwash.screen.SubscriptionScreen

@Composable
fun MainScreen(
    rootNavController: NavController,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()

    // Block access if trial/subscription is expired and subscription is not valid
    if (user != null && !user!!.isSubscriptionValid && user!!.computedTrialExpired) {
        SubscriptionScreen(
            user = user,
            onCreateCheckoutSession = authViewModel::createCheckoutSession,
            onActivateSubscription = authViewModel::activateSubscription,
            onSubscriptionSuccess = { authViewModel.loadUser() },
            onLogout = {
                authViewModel.logout()
                rootNavController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            BottomBar(navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddSaleClick = {
                        navController.navigate(Screen.Add.route) {
                            launchSingleTop = true
                        }
                    },
                    onHistoryClick = {
                        navController.navigate(Screen.History.route) {
                            launchSingleTop = true
                        }
                    },
                    onTeamsClick = {},
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Add.route) {
                AddSaleScreen(
                    onSaleSaved = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.History.route) {
                SalesHistoryScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Profile.route) {
                Profile(
                    navController = rootNavController,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }
    }
}

@Composable
fun BottomBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = {
                if (currentRoute != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Add.route,
            onClick = {
                if (currentRoute != Screen.Add.route) {
                    navController.navigate(Screen.Add.route) {
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.AddCircle, contentDescription = "Add") },
            label = { Text("Add") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.History.route,
            onClick = {
                if (currentRoute != Screen.History.route) {
                    navController.navigate(Screen.History.route) {
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = {
                if (currentRoute != Screen.Profile.route) {
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}
