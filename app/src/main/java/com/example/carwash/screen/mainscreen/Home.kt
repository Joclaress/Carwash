package com.example.carwash.screen.mainscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.carwash.model.AuthViewModel
import com.example.carwash.model.DashboardViewModel

@Composable
fun Home(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by dashboardViewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { dashboardViewModel.loadDashboardData() }) {
                    Text("Retry")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Custoworks Dashboard",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                SaleCard(
                    title = "Today's Sales",
                    amount = "₱${uiState.todaySales}"
                )
            }

            item {
                SaleCard(
                    title = "Yesterday's Sales",
                    amount = "₱${uiState.yesterdaySales}"
                )
            }

            item {
                SaleCard(
                    title = "This Month's Sales",
                    amount = "₱${uiState.thisMonthSales}"
                )
            }
        }
    }
}

@Composable
fun SaleCard(title: String, amount: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = amount, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
