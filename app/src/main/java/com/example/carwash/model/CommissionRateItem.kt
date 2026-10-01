package com.example.carwash.model

data class CommissionRateItem(
    val id: String = "",
    val displayName: String = "",
    val workerPercent: Double = 0.40,
    val ownerPercent: Double = 0.60
)
