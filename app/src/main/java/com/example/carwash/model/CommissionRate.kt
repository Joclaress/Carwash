package com.example.carwash.model

enum class CommissionRate (
    val displayName: String,
    val workerPercent: Double,
    val ownerPercent: Double
){
    FORTY(
        displayName = "40% (Default)",
        workerPercent = 0.40,
        ownerPercent = 0.60
    ),
    THIRTY(
        displayName = "30%",
        workerPercent = 0.30,
        ownerPercent = 0.70
    ),
    FULL_OWNER(
    displayName = "100% Owner",
        workerPercent = 0.00,
        ownerPercent = 1.00
    )
}