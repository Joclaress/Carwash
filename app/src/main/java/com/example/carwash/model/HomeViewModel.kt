package com.example.carwash.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carwash.add.Sale
import com.example.carwash.add.cleanPlateNumber
import com.example.carwash.home.HomeUiState
import com.example.carwash.home.TeamSalesSummary
import com.example.carwash.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SaleRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState = _uiState.asStateFlow()

    init {
        observeSales()
    }

    private fun observeSales() {
        viewModelScope.launch {
            repository.observeRecentSale(limit = 500)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Unable to load sales"
                    )
                }
                .collect { sales ->
                    calculateDashboard(sales)
                }
        }
    }

    fun selectTeamFilter(teamName: String?) {
        _uiState.value = _uiState.value.copy(
            selectedTeamFilter = if (_uiState.value.selectedTeamFilter == teamName) null else teamName
        )
    }

    fun deleteSale(saleId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteSale(saleId)
            onResult(result.isSuccess)
        }
    }

    fun updateSale(sale: Sale, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.updateSale(sale)
            onResult(result.isSuccess)
        }
    }

    private fun calculateDashboard(
        sales: List<Sale>
    ) {
        val today = Calendar.getInstance()

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val todayList = sales.filter {
            isSameDay(
                sale = it,
                target = today
            )
        }
        val yesterDayList = sales.filter {
            isSameDay(
                sale = it,
                target = yesterday
            )
        }
        val monthList = sales.filter {
            isSameMonth(
                sale = it,
                target = today
            )
        }

        val todaySales = todayList.sumOf { it.amount }
        val yesterdaySales = yesterDayList.sumOf { it.amount }
        val monthSales = monthList.sumOf { it.amount }

        val workerCommision = todayList.sumOf { sale ->
            if (sale.workerCommission > 0.0) {
                sale.workerCommission
            } else {
                sale.amount * sale.workerPercent
            }
        }
        val ownerShare = todayList.sumOf { sale ->
            if (sale.ownerShare > 0.0) {
                sale.ownerShare
            } else {
                sale.amount * sale.ownerPercent
            }
        }

        val defaultTeams = listOf("Team A", "Team B")
        val teamsInSales = sales.map { normalizeTeam(it.assignedTeam) }
            .filter { it.isNotBlank() && it != "Unassigned" }
        val allTeamNames = (defaultTeams + teamsInSales).distinct()

        val teamSummaries = allTeamNames.map { teamName ->
            val teamTodaySales = todayList.filter { normalizeTeam(it.assignedTeam) == teamName }
            val teamAllSales = sales.filter { normalizeTeam(it.assignedTeam) == teamName }

            TeamSalesSummary(
                teamName = teamName,
                todaySales = teamTodaySales.sumOf { it.amount },
                todayTransactionCount = teamTodaySales.size,
                todayWorkerCommission = teamTodaySales.sumOf {
                    if (it.workerCommission > 0.0) it.workerCommission else it.amount * it.workerPercent
                },
                todayOwnerShare = teamTodaySales.sumOf {
                    if (it.ownerShare > 0.0) it.ownerShare else it.amount * it.ownerPercent
                },
                totalSales = teamAllSales.sumOf { it.amount },
                totalTransactionCount = teamAllSales.size
            )
        }

        val repeatPlates = sales
            .filter { it.plateNumber.isNotBlank() }
            .groupBy { it.cleanPlateNumber }
            .filter { it.value.size > 1 }
            .keys

        _uiState.value = HomeUiState(
            todaySales = todaySales,
            yesterdaySales = yesterdaySales,
            monthSales = monthSales,
            todayTransactionCount = todayList.size,
            yesterdayTransactionCount = yesterDayList.size,
            monthTransctionCount = monthList.size,
            todayWorkerCommision = workerCommision,
            todayOwnerShare = ownerShare,
            teamSalesSummaries = teamSummaries,
            selectedTeamFilter = _uiState.value.selectedTeamFilter,
            recentSales = sales,
            repeatPlates = repeatPlates,
            isLoading = false
        )
    }

    private fun normalizeTeam(team: String): String {
        val trimmed = team.trim()
        if (trimmed.isBlank()) return "Unassigned"
        return when {
            trimmed.equals("TEAM_A", ignoreCase = true) || trimmed.equals("Team A", ignoreCase = true) -> "Team A"
            trimmed.equals("TEAM_B", ignoreCase = true) || trimmed.equals("Team B", ignoreCase = true) -> "Team B"
            trimmed.equals("TEAM_C", ignoreCase = true) || trimmed.equals("Team C", ignoreCase = true) -> "Team C"
            else -> trimmed
        }
    }

    private fun isSameDay(
        sale: Sale,
        target: Calendar
    ): Boolean {
        val date = sale.CreatedAt?.toDate()
            ?: return false
        val calendar = Calendar.getInstance().apply {
            time = date
        }
        return calendar.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameMonth(
        sale: Sale,
        target: Calendar
    ): Boolean {
        val date = sale.CreatedAt?.toDate()
            ?: return false

        val calendar = Calendar.getInstance().apply {
            time = date
        }
        return calendar.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == target.get(Calendar.MONTH)
    }
}
