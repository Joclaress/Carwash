package com.example.carwash.history

import com.example.carwash.add.Sale

enum class SalesFilter {
    ALL,
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH
}

data class SalesHistoryUiState(
    val sales: List<Sale> = emptyList(),
    val filteredSales: List<Sale> = emptyList(),
    val selectedFilter: SalesFilter = SalesFilter.ALL,
    val searchQuery: String = "",
    val totalSales: Double = 0.0,
    val workCommission: Double = 0.0,
    val ownerShare: Double = 0.0,
    val averageSale: Double = 0.0,
    val repeatPlates: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
