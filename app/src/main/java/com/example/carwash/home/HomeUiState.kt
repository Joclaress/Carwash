package com.example.carwash.home

import com.example.carwash.add.Sale

data class TeamSalesSummary(
    val teamName: String,
    val todaySales: Double = 0.0,
    val todayTransactionCount: Int = 0,
    val todayWorkerCommission: Double = 0.0,
    val todayOwnerShare: Double = 0.0,
    val totalSales: Double = 0.0,
    val totalTransactionCount: Int = 0
)

data class HomeUiState(
    val todaySales: Double = 0.0,
    val yesterdaySales: Double = 0.0,
    val monthSales: Double = 0.0,

    val todayTransactionCount: Int = 0,
    val yesterdayTransactionCount: Int = 0,
    val monthTransctionCount: Int = 0,

    val todayWorkerCommision: Double = 0.0,
    val todayOwnerShare: Double = 0.0,

    val teamSalesSummaries: List<TeamSalesSummary> = emptyList(),
    val selectedTeamFilter: String? = null,

    val recentSales: List<Sale> = emptyList(),
    val repeatPlates: Set<String> = emptySet(),

    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
