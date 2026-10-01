package com.example.carwash.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.carwash.add.Sale
import com.example.carwash.add.cleanPlateNumber
import com.example.carwash.components.SaleDetailsDialog
import com.example.carwash.model.AuthViewModel
import com.example.carwash.model.SalesHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: SalesHistoryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by authViewModel.user.collectAsState()
    var selectedSaleForDetails by remember { mutableStateOf<Sale?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sales History",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "View all sales",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Something went wrong."
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        HistoryFilters(
                            selected = uiState.selectedFilter,
                            onSelected = viewModel::selectFilter
                        )
                    }

                    item {
                        HistorySummary(
                            total = uiState.totalSales,
                            transactions = uiState.filteredSales.size,
                            worker = uiState.workCommission,
                            owner = uiState.ownerShare,
                            average = uiState.averageSale
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::updateSearch,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            placeholder = {
                                Text("Search package, size, team or payment")
                            }
                        )
                    }

                    if (uiState.filteredSales.isEmpty()) {
                        item {
                            EmptyHistory()
                        }
                    } else {
                        items(
                            items = uiState.filteredSales,
                            key = { it.id }
                        ) { sale ->
                            val isRepeat = sale.plateNumber.isNotBlank() &&
                                    sale.cleanPlateNumber in uiState.repeatPlates
                            HistorySaleCard(
                                sale = sale,
                                isRepeatCustomer = isRepeat,
                                onClick = { selectedSaleForDetails = sale }
                            )
                        }
                    }

                    item {
                        com.example.carwash.components.AdBanner()
                    }

                    item {
                        Spacer(Modifier.height(50.dp))
                    }
                }
            }
        }

        selectedSaleForDetails?.let { sale ->
            val isRepeat = sale.plateNumber.isNotBlank() &&
                    sale.cleanPlateNumber in uiState.repeatPlates
            SaleDetailsDialog(
                sale = sale,
                isRepeatCustomer = isRepeat,
                adminPassword = user?.effectiveAdminPassword ?: "admin123",
                onDismiss = { selectedSaleForDetails = null },
                onDeleteSale = { target ->
                    viewModel.deleteSale(target.id)
                },
                onUpdateSale = { target ->
                    viewModel.updateSale(target)
                }
            )
        }
    }
}

@Composable
private fun HistoryFilters(
    selected: SalesFilter,
    onSelected: (SalesFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SalesFilterChip(
                title = "All",
                filter = SalesFilter.ALL,
                selected = selected,
                onSelected = onSelected
            )
        }

        item {
            SalesFilterChip(
                title = "Today",
                filter = SalesFilter.TODAY,
                selected = selected,
                onSelected = onSelected
            )
        }

        item {
            SalesFilterChip(
                title = "Yesterday",
                filter = SalesFilter.YESTERDAY,
                selected = selected,
                onSelected = onSelected
            )
        }

        item {
            SalesFilterChip(
                title = "This Week",
                filter = SalesFilter.THIS_WEEK,
                selected = selected,
                onSelected = onSelected
            )
        }

        item {
            SalesFilterChip(
                title = "This Month",
                filter = SalesFilter.THIS_MONTH,
                selected = selected,
                onSelected = onSelected
            )
        }
    }
}

@Composable
private fun SalesFilterChip(
    title: String,
    filter: SalesFilter,
    selected: SalesFilter,
    onSelected: (SalesFilter) -> Unit
) {
    FilterChip(
        selected = selected == filter,
        onClick = { onSelected(filter) },
        label = { Text(title) }
    )
}

@Composable
private fun HistorySummary(
    total: Double,
    transactions: Int,
    worker: Double,
    owner: Double,
    average: Double
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistorySummaryCard(
            title = "Total Sales",
            amount = total,
            subtitle = "$transactions Transactions"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistorySummaryCard(
                modifier = Modifier.weight(1f),
                title = "Carwash Boy",
                amount = worker,
                subtitle = "Commission"
            )

            HistorySummaryCard(
                modifier = Modifier.weight(1f),
                title = "Owner",
                amount = owner,
                subtitle = "Income"
            )
        }

        HistorySummaryCard(
            title = "Average Sale",
            amount = average,
            subtitle = "Average per transaction"
        )
    }
}

@Composable
private fun HistorySummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun HistorySaleCard(
    sale: Sale,
    isRepeatCustomer: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .size(width = 90.dp, height = 70.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = sale.vehicleImageUrl,
                    contentDescription = "Vehicle",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.packageName.ifBlank { "Carwash" },
                        fontWeight = FontWeight.Bold
                    )
                    if (isRepeatCustomer) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = "Repeat",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = buildString {
                        if (sale.plateNumber.isNotBlank()) {
                            append(sale.plateNumber)
                            append(" • ")
                        }
                        append(sale.vehicleSize.ifBlank { "-" })
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = formatTeam(sale.assignedTeam),
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = formatDateTime(sale),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(sale.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                HistoryCommissionBadge(
                    percentage = sale.workerPercent
                )

                Spacer(Modifier.height(6.dp))

                PaymentBadge(
                    paymentMethod = sale.paymentMethod
                )
            }
        }
    }
}

@Composable
private fun HistoryCommissionBadge(
    percentage: Double
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(100.dp)
    ) {
        Text(
            text = "${(percentage * 100).toInt()}%",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentBadge(
    paymentMethod: String
) {
    Card(
        shape = RoundedCornerShape(100.dp)
    ) {
        Text(
            text = formatPayment(paymentMethod),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyHistory() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No sales found",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Try another filter or search."
            )
        }
    }
}

private fun formatCurrency(
    amount: Double
): String {
    return "₱%,.2f".format(amount)
}

private fun formatDateTime(
    sale: Sale
): String {
    val date = sale.CreatedAt?.toDate() ?: return "-"

    return SimpleDateFormat(
        "MMM d, yyyy • hh:mm a",
        Locale.getDefault()
    ).format(date)
}

private fun formatTeam(
    team: String
): String {
    return when (team) {
        "TEAM_A" -> "Team A"
        "TEAM_B" -> "Team B"
        else -> team.ifBlank { "-" }
    }
}

private fun formatPayment(
    payment: String
): String {
    return when (payment) {
        "CASH" -> "Cash"
        "GCASH" -> "GCash"
        "BANK_TRANSFER" -> "Bank Transfer"
        else -> payment.ifBlank { "-" }
    }
}
