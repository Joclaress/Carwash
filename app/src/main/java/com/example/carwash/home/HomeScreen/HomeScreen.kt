package com.example.carwash.home.HomeScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.carwash.add.Sale
import com.example.carwash.add.cleanPlateNumber
import com.example.carwash.components.SaleDetailsDialog
import com.example.carwash.home.HomeUiState
import com.example.carwash.home.TeamSalesSummary
import com.example.carwash.model.AuthViewModel
import com.example.carwash.model.HomeViewModel
import com.example.carwash.model.User
import com.example.carwash.screen.SubscriptionScreen
import com.example.carwash.ui.theme.CarwashTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    showBottomBar: Boolean = false,
    onAddSaleClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onTeamsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by authViewModel.user.collectAsState()
    var showSubscriptionPayScreen by remember { mutableStateOf(false) }

    HomeScreenContent(
        uiState = uiState,
        user = user,
        showBottomBar = showBottomBar,
        onAddSaleClick = onAddSaleClick,
        onHistoryClick = onHistoryClick,
        onTeamsClick = {
            val teams = uiState.teamSalesSummaries.map { it.teamName }
            if (teams.isNotEmpty()) {
                val current = uiState.selectedTeamFilter
                val next = if (current == null) {
                    teams.firstOrNull()
                } else {
                    val currentIndex = teams.indexOf(current)
                    if (currentIndex >= 0 && currentIndex < teams.size - 1) {
                        teams[currentIndex + 1]
                    } else {
                        null
                    }
                }
                viewModel.selectTeamFilter(next)
            }
            onTeamsClick()
        },
        onProfileClick = onProfileClick,
        onUpgradeClick = { showSubscriptionPayScreen = true },
        onSelectTeamFilter = viewModel::selectTeamFilter,
        onDeleteSale = viewModel::deleteSale,
        onUpdateSale = viewModel::updateSale
    )

    if (showSubscriptionPayScreen) {
        SubscriptionScreen(
            user = user,
            onCreateCheckoutSession = authViewModel::createCheckoutSession,
            onActivateSubscription = authViewModel::activateSubscription,
            onSubscriptionSuccess = {
                authViewModel.loadUser()
                showSubscriptionPayScreen = false
            },
            onLogout = {
                showSubscriptionPayScreen = false
                authViewModel.logout()
            }
        )
    }
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    user: User? = null,
    showBottomBar: Boolean = false,
    onAddSaleClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onTeamsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
    onSelectTeamFilter: (String?) -> Unit = {},
    onDeleteSale: (String) -> Unit = {},
    onUpdateSale: (Sale) -> Unit = {}
) {
    var selectedSaleForDetails by remember { mutableStateOf<Sale?>(null) }

    val filteredSales = remember(uiState.recentSales, uiState.selectedTeamFilter) {
        if (uiState.selectedTeamFilter.isNull_or_blank()) {
            uiState.recentSales.take(5)
        } else {
            uiState.recentSales.filter { normalizeTeam(it.assignedTeam) == uiState.selectedTeamFilter }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HomeBottomNavigation(
                    onAddSaleClick = onAddSaleClick,
                    onHistoryClick = onHistoryClick,
                    onProfileClick = onProfileClick
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HomeHeader()
                    }
                    if (user != null && user.isTrialActive) {
                        item {
                            TrialBannerCard(user = user, onUpgradeClick = onUpgradeClick)
                        }
                    }
                    item {
                        SalesSummarySection(uiState = uiState)
                    }
                    item {
                        QuickActions(
                            onAddSaleClick = onAddSaleClick,
                            onHistoryClick = onHistoryClick,
                            onTeamsClick = onTeamsClick
                        )
                    }
                    if (uiState.teamSalesSummaries.isNotEmpty()) {
                        item {
                            TeamSalesSection(
                                teamSummaries = uiState.teamSalesSummaries,
                                selectedTeamFilter = uiState.selectedTeamFilter,
                                onSelectTeamFilter = onSelectTeamFilter
                            )
                        }
                    }
                    item {
                        CommissionSection(
                            worker = uiState.todayWorkerCommision,
                            owner = uiState.todayOwnerShare
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (uiState.selectedTeamFilter != null) "${uiState.selectedTeamFilter} Sales" else "Recent Sales",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.selectedTeamFilter != null) {
                                    val summary = uiState.teamSalesSummaries.find { it.teamName == uiState.selectedTeamFilter }
                                    val totalSales = summary?.todaySales ?: 0.0
                                    Text(
                                        text = "Showing only ${uiState.selectedTeamFilter} • Today: ${formatCurrency(totalSales)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Button(onClick = onHistoryClick) {
                                Text("View All")
                            }
                        }
                    }
                    if (filteredSales.isEmpty()) {
                        item {
                            EmptyRecentSales(teamFilter = uiState.selectedTeamFilter)
                        }
                    } else {
                        items(
                            items = filteredSales,
                            key = { it.id }
                        ) { sale ->
                            val isRepeat = sale.plateNumber.isNotBlank() &&
                                    sale.cleanPlateNumber in uiState.repeatPlates
                            RecentSaleCard(
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
                        Spacer(Modifier.height(20.dp))
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
                    onDeleteSale(target.id)
                },
                onUpdateSale = { target ->
                    onUpdateSale(target)
                }
            )
        }
    }
}

@Composable
private fun TrialBannerCard(
    user: User,
    onUpgradeClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "7-Day Free Trial Active",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${user.remainingTrialDays} day(s) remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Button(
                onClick = onUpgradeClick,
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Upgrade Pro", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.isBlank()
}

@Composable
private fun HomeHeader() {
    val date = SimpleDateFormat(
        "MMM d, yyyy",
        Locale.getDefault()
    ).format(Date())

    Column {
        Text(
            text = "CARWASH SALE TRACKER",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sales • Commissions • Teams",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = greeting(),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

@Composable
private fun SalesSummarySection(uiState: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SalesSummaryCard(
            modifier = Modifier.weight(1f),
            title = "Today's Sale",
            amount = uiState.todaySales,
            transactions = uiState.todayTransactionCount
        )
        SalesSummaryCard(
            modifier = Modifier.weight(1f),
            title = "Yesterday",
            amount = uiState.yesterdaySales,
            transactions = uiState.yesterdayTransactionCount
        )
        SalesSummaryCard(
            modifier = Modifier.weight(1f),
            title = "This Month",
            amount = uiState.monthSales,
            transactions = uiState.monthTransctionCount
        )
    }
}

@Composable
private fun SalesSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    transactions: Int
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$transactions Transactions",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun TeamSalesSection(
    teamSummaries: List<TeamSalesSummary>,
    selectedTeamFilter: String?,
    onSelectTeamFilter: (String?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Team Sales",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (selectedTeamFilter != null) {
                TextButton(onClick = { onSelectTeamFilter(null) }) {
                    Text("Show All")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            teamSummaries.forEach { summary ->
                val isSelected = selectedTeamFilter == summary.teamName
                TeamSalesCard(
                    modifier = Modifier.width(200.dp),
                    summary = summary,
                    isSelected = isSelected,
                    onClick = {
                        onSelectTeamFilter(if (isSelected) null else summary.teamName)
                    }
                )
            }
        }
    }
}

@Composable
private fun TeamSalesCard(
    modifier: Modifier = Modifier,
    summary: TeamSalesSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.teamName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${summary.todayTransactionCount} jobs",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Today's Sale",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
            )
            Text(
                text = formatCurrency(summary.todaySales),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Worker",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatCurrency(summary.todayWorkerCommission),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Owner",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatCurrency(summary.todayOwnerShare),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    onAddSaleClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTeamsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionCard(
            modifier = Modifier.weight(1f),
            title = "Add Sale",
            icon = {
                Icon(Icons.Default.Add, contentDescription = null)
            },
            onClick = onAddSaleClick
        )
        QuickActionCard(
            modifier = Modifier.weight(1f),
            title = "History",
            icon = {
                Icon(Icons.Default.History, contentDescription = null)
            },
            onClick = onHistoryClick
        )
        QuickActionCard(
            modifier = Modifier.weight(1f),
            title = "Teams",
            icon = {
                Icon(Icons.Default.Groups, contentDescription = null)
            },
            onClick = onTeamsClick
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CommissionSection(
    worker: Double,
    owner: Double
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Today's Commission",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CommisionCard(
                modifier = Modifier.weight(1f),
                title = "Carwash Boy",
                amount = worker
            )
            CommisionCard(
                modifier = Modifier.weight(1f),
                title = "Owner",
                amount = owner
            )
        }
    }
}

@Composable
private fun CommisionCard(
    modifier: Modifier,
    title: String,
    amount: Double
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp)
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
                color = Color(0xFFFFA500),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Total for today",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecentSaleCard(
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
                    contentDescription = "Vehicle Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
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
                        if (sale.assignedTeam.isNotBlank()) {
                            append(" • ")
                            append(normalizeTeam(sale.assignedTeam))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatSaleTime(sale),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(sale.amount),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                CommissionBadge(percent = sale.workerPercent)
            }
        }
    }
}

@Composable
private fun CommissionBadge(percent: Double) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(100.dp)
    ) {
        Text(
            text = "${(percent * 100).toInt()}%",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyRecentSales(teamFilter: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (teamFilter != null) "No sales for $teamFilter yet" else "No sales yet",
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (teamFilter != null) "Sales assigned to $teamFilter will appear here." else "Saved sales will appear here."
            )
        }
    }
}

@Composable
private fun HomeBottomNavigation(
    onAddSaleClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null
                )
            },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onAddSaleClick,
            icon = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null
                )
            },
            label = { Text("Add Sale") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onHistoryClick,
            icon = {
                Icon(
                    Icons.Default.History,
                    contentDescription = null
                )
            },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onProfileClick,
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null
                )
            },
            label = { Text("Profile") }
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return "₱%,.2f".format(amount)
}

private fun formatSaleTime(sale: Sale): String {
    val date = sale.CreatedAt?.toDate() ?: return "-"
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    CarwashTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                todaySales = 1250.0,
                yesterdaySales = 900.0,
                monthSales = 15400.0,
                todayTransactionCount = 5,
                yesterdayTransactionCount = 4,
                monthTransctionCount = 62,
                todayWorkerCommision = 500.0,
                todayOwnerShare = 750.0,
                teamSalesSummaries = listOf(
                    TeamSalesSummary("Team A", todaySales = 750.0, todayTransactionCount = 3, todayWorkerCommission = 300.0, todayOwnerShare = 450.0),
                    TeamSalesSummary("Team B", todaySales = 500.0, todayTransactionCount = 2, todayWorkerCommission = 200.0, todayOwnerShare = 300.0)
                ),
                recentSales = listOf(
                    Sale(
                        id = "1",
                        plateNumber = "ABC-1234",
                        packageName = "Wash & Wax",
                        vehicleSize = "SUV",
                        assignedTeam = "TEAM_A",
                        amount = 350.0,
                        workerPercent = 0.4
                    ),
                    Sale(
                        id = "2",
                        plateNumber = "XYZ-8888",
                        packageName = "Body Wash",
                        vehicleSize = "Sedan",
                        assignedTeam = "TEAM_B",
                        amount = 200.0,
                        workerPercent = 0.4
                    )
                ),
                repeatPlates = setOf("ABC-1234"),
                isLoading = false
            ),
            showBottomBar = true
        )
    }
}
