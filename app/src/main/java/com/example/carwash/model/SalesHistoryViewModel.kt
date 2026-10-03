package com.example.carwash.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carwash.add.Sale
import com.example.carwash.add.cleanPlateNumber
import com.example.carwash.history.SalesFilter
import com.example.carwash.history.SalesHistoryUiState
import com.example.carwash.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    private val repository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesHistoryUiState())
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
                        errorMessage = exception.message ?: "Failed to load sales."
                    )
                }
                .collect { sales ->
                    _uiState.value = _uiState.value.copy(
                        sales = sales,
                        isLoading = false
                    )
                    applyFilters()
                }
        }
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

    fun selectFilter(filter: SalesFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyFilters()
    }

    fun updateSearch(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
        applyFilters()
    }

    private fun applyFilters() {
        val current = _uiState.value

        val repeatPlates = current.sales
            .filter { it.plateNumber.isNotBlank() }
            .groupBy { it.cleanPlateNumber }
            .filter { it.value.size > 1 }
            .keys

        var result = current.sales.filter {
            matchesDate(sale = it, filter = current.selectedFilter)
        }

        val query = current.searchQuery.trim()

        if (query.isNotBlank()) {
            result = result.filter { sale ->
                sale.plateNumber.contains(query, ignoreCase = true) ||
                        sale.packageName.contains(query, ignoreCase = true) ||
                        sale.vehicleSize.contains(query, ignoreCase = true) ||
                        sale.assignedTeam.contains(query, ignoreCase = true) ||
                        sale.paymentMethod.contains(query, ignoreCase = true) ||
                        sale.notes.contains(query, ignoreCase = true)
            }
        }

        val total = result.sumOf { it.amount }

        val cashTotal = result
            .filter { it.paymentMethod.isBlank() || it.paymentMethod.equals("CASH", ignoreCase = true) }
            .sumOf { it.amount }

        val gcashTotal = result
            .filter { it.paymentMethod.contains("GCASH", ignoreCase = true) }
            .sumOf { it.amount }

        val mayaTotal = result
            .filter { it.paymentMethod.contains("MAYA", ignoreCase = true) }
            .sumOf { it.amount }

        val worker = result.sumOf { sale ->
            if (sale.workerCommission > 0.0) {
                sale.workerCommission
            } else {
                sale.amount * sale.workerPercent
            }
        }

        val owner = result.sumOf { sale ->
            if (sale.ownerShare > 0.0) {
                sale.ownerShare
            } else {
                sale.amount * sale.ownerPercent
            }
        }

        val average = if (result.isEmpty()) 0.0 else total / result.size

        _uiState.value = current.copy(
            filteredSales = result,
            totalSales = total,
            cashSales = cashTotal,
            gcashSales = gcashTotal,
            mayaSales = mayaTotal,
            workCommission = worker,
            ownerShare = owner,
            averageSale = average,
            repeatPlates = repeatPlates
        )
    }

    private fun matchesDate(
        sale: Sale,
        filter: SalesFilter
    ): Boolean {
        val date = sale.CreatedAt?.toDate() ?: return filter == SalesFilter.ALL

        val saleCalendar = Calendar.getInstance().apply {
            time = date
        }

        val today = Calendar.getInstance()

        return when (filter) {
            SalesFilter.ALL -> true
            SalesFilter.TODAY -> sameDay(saleCalendar, today)
            SalesFilter.YESTERDAY -> {
                val yesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                sameDay(saleCalendar, yesterday)
            }
            SalesFilter.THIS_WEEK -> {
                saleCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        saleCalendar.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)
            }
            SalesFilter.THIS_MONTH -> {
                saleCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        saleCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
            }
        }
    }

    private fun sameDay(
        first: Calendar,
        second: Calendar
    ): Boolean {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}
