package com.example.carwash.model

data class DashboardUI (
    val todaySales: Double = 0.0,
    val yesterdaySales: Double = 0.0,
    val thisMonthSales: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
