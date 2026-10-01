package com.example.carwash.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carwash.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUI())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = dashboardRepository.getSales()
            
            result.onSuccess { sales ->
                try {
                    val today = LocalDate.now()
                    val yesterday = today.minusDays(1)

                    val todayTotal = sales
                        .filter { sale ->
                            sale.CreatedAt?.toDate()
                                ?.toInstant()
                                ?.atZone(ZoneId.systemDefault())
                                ?.toLocalDate() == today
                        }
                        .sumOf { it.amount }

                    val yesterdayTotal = sales
                        .filter { sale ->
                            sale.CreatedAt?.toDate()
                                ?.toInstant()
                                ?.atZone(ZoneId.systemDefault())
                                ?.toLocalDate() == yesterday
                        }
                        .sumOf { it.amount }

                    val thisMonthTotal = sales
                        .filter { sale ->
                            val date = sale.CreatedAt?.toDate()
                                ?.toInstant()
                                ?.atZone(ZoneId.systemDefault())
                                ?.toLocalDate()

                            date?.month == today.month && date?.year == today.year
                        }
                        .sumOf { it.amount }

                    _uiState.update { 
                        it.copy(
                            todaySales = todayTotal,
                            yesterdaySales = yesterdayTotal,
                            thisMonthSales = thisMonthTotal,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Data processing error: ${e.message}"
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unknown error occurred"
                    )
                }
            }
        }
    }
}
