package com.example.carwash.add

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.carwash.components.CameraCaptureBox
import com.example.carwash.components.OfflineStatusBanner
import com.example.carwash.model.AddSaleViewModel
import com.example.carwash.model.CommissionRateItem
import com.example.carwash.model.PaymentMethod
import com.example.carwash.model.ServicePackage
import com.example.carwash.repository.OfflineSaleSyncManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleScreen(
    onSaleSaved: () -> Unit = {},
    viewModel: AddSaleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val vehicleSizes by viewModel.vehicleSizes.collectAsState()
    val commissionRates by viewModel.commissionRates.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        com.example.carwash.utils.InterstitialAdHelper.loadAd(context)
    }

    LaunchedEffect(uiState.saveSaleId) {
        uiState.saveSaleId?.let { saleId ->
            val draft = uiState.draft
            val isOffline = saleId.startsWith("offline_")

            if (isOffline) {
                android.widget.Toast.makeText(
                    context,
                    "No internet connection. Your item has been saved and will upload when the internet is restored.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                com.example.carwash.utils.NotificationHelper.showOfflineSavedNotification(
                    context = context,
                    plateNumber = draft.plateNumber,
                    packageName = draft.selectedPackage?.name ?: "Carwash",
                    amount = draft.amount
                )
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Sale saved successfully!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                com.example.carwash.utils.NotificationHelper.showSaleAddedNotification(
                    context = context,
                    plateNumber = draft.plateNumber,
                    packageName = draft.selectedPackage?.name ?: "Carwash",
                    amount = draft.amount,
                    paymentMethod = draft.paymentMethod.name
                )
            }

            viewModel.resetForm()
            val activity = with(com.example.carwash.utils.InterstitialAdHelper) { context.findActivity() }
            com.example.carwash.utils.InterstitialAdHelper.showAdIfAvailable(activity) {
                onSaleSaved()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add New Sale", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isOnline = remember(uiState) { OfflineSaleSyncManager.isInternetAvailable(context) }
            val pendingCount = remember(uiState) { OfflineSaleSyncManager.getPendingSalesCount(context) }

            if (!isOnline || pendingCount > 0) {
                OfflineStatusBanner(
                    isOnline = isOnline,
                    pendingCount = pendingCount,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            StepIndicator(
                currentStep = uiState.currentStepNumber,
                totalSteps = uiState.totalSteps
            )

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.currentStep) {
                    AddSaleStep.VEHICLE_IMAGE -> {
                        val localContext = androidx.compose.ui.platform.LocalContext.current
                        VehicleStep(
                            vehicleImageUri = uiState.draft.vehicleImageUri,
                            plateNumber = uiState.draft.plateNumber,
                            isRepeatCustomer = uiState.isRepeatCustomer,
                            selectedTeam = uiState.draft.assignedTeamName,
                            teams = teams,
                            onVehicleImageSelected = { uri ->
                                viewModel.setVehicleImage(uri)
                                uri?.let { viewModel.scanPlateNumberOcr(localContext, it) }
                            },
                            onRemoveVehicle = viewModel::removeVehicle,
                            onPlateNumberChanged = viewModel::updatePlateNumber,
                            onTeamSelected = viewModel::selectTeam
                        )
                    }
                    AddSaleStep.SERVICE -> {
                        ServiceStep(
                            selectedPackage = uiState.draft.selectedPackage,
                            selectedVehicleSize = uiState.draft.selectedVehicleSizeLabel,
                            customPrice = uiState.draft.customPrice,
                            notes = uiState.draft.notes,
                            packages = packages,
                            vehicleSizes = vehicleSizes,
                            amount = uiState.draft.amount,
                            commissionRate = uiState.draft.selectedCommissionRate,
                            commissionRates = commissionRates,
                            workerCommission = uiState.draft.workerCommission,
                            ownerShare = uiState.draft.ownerShare,
                            onPackageSelected = viewModel::selectPackage,
                            onVehicleSizeSelected = viewModel::selectVehicleSize,
                            onCustomPriceChanged = viewModel::updateCustomPrice,
                            onNotesChanged = viewModel::updateNotes,
                            onCommissionSelected = viewModel::selectCommissionRate
                        )
                    }
                    AddSaleStep.PAYMENT -> {
                        PaymentStep(
                            paymentMethod = uiState.draft.paymentMethod,
                            paymentMethods = viewModel.paymentMethods,
                            amount = uiState.draft.amount,
                            cashReceived = uiState.draft.cashReceived,
                            changeAmount = uiState.draft.changeAmount,
                            referenceNumber = uiState.draft.referenceNumber,
                            paymentImageUri = uiState.draft.paymentImageUri,
                            onPaymentMethodSelected = viewModel::selectPaymentMethod,
                            onCashReceivedChanged = viewModel::updateCashReceived,
                            onReferenceNumberChanged = viewModel::updateReferenceNumber,
                            onPaymentImageSelected = viewModel::setPaymentImage,
                            onRemovePaymentImage = viewModel::removePaymentImage
                        )
                    }
                    AddSaleStep.REVIEW -> {
                        ReviewStep(uiState = uiState)
                    }
                }
            }

            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                BottomButtons(
                    canGoBack = uiState.canGoBack,
                    isReview = uiState.currentStep == AddSaleStep.REVIEW,
                    isSaving = uiState.isSaving,
                    onBack = viewModel::previousStep,
                    onNext = viewModel::nextStep
                )
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    val stepTitles = listOf("Vehicle", "Service", "Payment", "Review")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val stepNumber = index + 1
            val isActive = stepNumber == currentStep
            val isCompleted = stepNumber < currentStep

            val containerColor = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val contentColor = when {
                isCompleted || isActive -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = containerColor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = contentColor
                            )
                        } else {
                            Text(
                                text = stepNumber.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stepTitles.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            if (index < totalSteps - 1) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 16.dp),
                    color = if (stepNumber < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    thickness = 2.dp
                )
            }
        }
    }
}

@Composable
fun VehicleStep(
    vehicleImageUri: Uri?,
    plateNumber: String,
    isRepeatCustomer: Boolean,
    selectedTeam: String,
    teams: List<String>,
    onVehicleImageSelected: (Uri?) -> Unit,
    onRemoveVehicle: () -> Unit,
    onPlateNumberChanged: (String) -> Unit,
    onTeamSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Step 1: Vehicle Information", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        CameraCaptureBox(
            title = "Vehicle Photo",
            imageUri = vehicleImageUri,
            onImageCaptured = { onVehicleImageSelected(it) },
            onRemove = onRemoveVehicle
        )

        OutlinedTextField(
            value = plateNumber,
            onValueChange = onPlateNumberChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Plate Number *") },
            placeholder = { Text("e.g. ABC-1234") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(14.dp)
        )

        if (isRepeatCustomer) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Welcome Back! (Repeat Customer)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "This plate number has previously availed services.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Assign Work Team *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            TeamDropdown(selectedTeam = selectedTeam, teams = teams, onTeamSelected = onTeamSelected)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDropdown(
    selectedTeam: String,
    teams: List<String>,
    onTeamSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedTeam.ifBlank { "Select Team" },
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text("Assigned Team") },
            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            teams.forEach { teamName ->
                DropdownMenuItem(
                    text = { Text(teamName, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        onTeamSelected(teamName)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServiceStep(
    selectedPackage: ServicePackage?,
    selectedVehicleSize: String,
    customPrice: String,
    notes: String,
    packages: List<ServicePackage>,
    vehicleSizes: List<String>,
    amount: Double,
    commissionRate: CommissionRateItem,
    commissionRates: List<CommissionRateItem>,
    workerCommission: Double,
    ownerShare: Double,
    onPackageSelected: (ServicePackage) -> Unit,
    onVehicleSizeSelected: (String) -> Unit,
    onCustomPriceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onCommissionSelected: (CommissionRateItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Step 2: Service & Package", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Select Service Package *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            PackageDropdown(selectedPackage = selectedPackage, packages = packages, onPackageSelected = onPackageSelected)
        }

        if (selectedPackage != null && !selectedPackage.customPriceAllowed) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Vehicle Size *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vehicleSizes.forEach { sizeLabel ->
                        val price = selectedPackage.getPriceForLabel(sizeLabel)
                        FilterChip(
                            selected = selectedVehicleSize == sizeLabel,
                            onClick = { onVehicleSizeSelected(sizeLabel) },
                            label = { Text("$sizeLabel - ₱${price.toInt()}", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }
        }

        if (selectedPackage?.customPriceAllowed == true) {
            OutlinedTextField(
                value = customPrice,
                onValueChange = onCustomPriceChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Custom Amount (₱) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp)
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Price Amount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(if (amount > 0) "₱%,.2f".format(amount) else "₱0.00", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            label = { Text(if (selectedPackage?.customPriceAllowed == true) "Service Notes *" else "Notes (Optional)") },
            placeholder = { Text("e.g. Include engine wash & tire shine") },
            shape = RoundedCornerShape(14.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Carwash Boy Commission Tier", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            CommissionDropdown(
                selectedRate = commissionRate,
                rates = commissionRates,
                onRateSelected = onCommissionSelected
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Commission Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Carwash Boy (${(commissionRate.workerPercent * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                    Text("₱%,.2f".format(workerCommission), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Owner Share (${(commissionRate.ownerPercent * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                    Text("₱%,.2f".format(ownerShare), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageDropdown(
    selectedPackage: ServicePackage?,
    packages: List<ServicePackage>,
    onPackageSelected: (ServicePackage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedPackage?.name.orEmpty().ifBlank { "Select Package" },
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text("Service Package") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            packages.forEach { service ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(service.name, fontWeight = FontWeight.Bold)
                            if (service.description.isNotBlank()) {
                                Text(service.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    },
                    onClick = {
                        onPackageSelected(service)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommissionDropdown(
    selectedRate: CommissionRateItem,
    rates: List<CommissionRateItem>,
    onRateSelected: (CommissionRateItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedRate.displayName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Commission Rate") },
            leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rates.forEach { rate ->
                DropdownMenuItem(
                    text = { Text(rate.displayName, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        onRateSelected(rate)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentStep(
    paymentMethod: PaymentMethod,
    paymentMethods: List<PaymentMethod>,
    amount: Double,
    cashReceived: Double,
    changeAmount: Double,
    referenceNumber: String,
    paymentImageUri: Uri?,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    onCashReceivedChanged: (String) -> Unit,
    onReferenceNumberChanged: (String) -> Unit,
    onPaymentImageSelected: (Uri?) -> Unit,
    onRemovePaymentImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Step 3: Payment Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Payment Method *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { onPaymentMethodSelected(method) },
                        label = { Text(method.name, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Amount Due", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("₱%,.2f".format(amount), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }

        when (paymentMethod) {
            PaymentMethod.CASH -> {
                OutlinedTextField(
                    value = if (cashReceived > 0) cashReceived.toString() else "",
                    onValueChange = onCashReceivedChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cash Received (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )

                if (cashReceived > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Change Due:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("₱%,.2f".format(changeAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                CameraCaptureBox(
                    title = "Photo of Cash Received (Optional)",
                    imageUri = paymentImageUri,
                    onImageCaptured = { onPaymentImageSelected(it) },
                    onRemove = onRemovePaymentImage
                )
            }
            else -> {
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = onReferenceNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reference / Transaction Number *") },
                    placeholder = { Text("e.g. 123456789") },
                    shape = RoundedCornerShape(14.dp)
                )

                CameraCaptureBox(
                    title = "Payment Proof / Receipt Photo",
                    imageUri = paymentImageUri,
                    onImageCaptured = { onPaymentImageSelected(it) },
                    onRemove = onRemovePaymentImage
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ReviewStep(uiState: AddSaleUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Step 4: Review Sale Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Photo Previews Card
        if (uiState.draft.vehicleImageUri != null || uiState.draft.paymentImageUri != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.draft.vehicleImageUri?.let { uri ->
                    Card(
                        modifier = Modifier.weight(1f).height(120.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Vehicle Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                uiState.draft.paymentImageUri?.let { uri ->
                    Card(
                        modifier = Modifier.weight(1f).height(120.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Payment Proof",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ReviewRow("Plate Number", uiState.draft.plateNumber)
                if (uiState.isRepeatCustomer) {
                    ReviewRow("Customer Status", "Repeat Customer (Welcome Back!)")
                }
                ReviewRow("Assigned Team", uiState.draft.assignedTeamName.ifBlank { "-" })
                HorizontalDivider()
                ReviewRow("Service Package", uiState.draft.selectedPackage?.name ?: "-")
                ReviewRow("Vehicle Size", uiState.draft.selectedVehicleSizeLabel.ifBlank { "-" })
                ReviewRow("Total Amount", "₱%,.2f".format(uiState.draft.amount))
                HorizontalDivider()
                ReviewRow("Carwash Boy (${(uiState.draft.selectedCommissionRate.workerPercent * 100).toInt()}%)", "₱%,.2f".format(uiState.draft.workerCommission))
                ReviewRow("Owner Share (${(uiState.draft.selectedCommissionRate.ownerPercent * 100).toInt()}%)", "₱%,.2f".format(uiState.draft.ownerShare))
                HorizontalDivider()
                ReviewRow("Payment Method", uiState.draft.paymentMethod.name)
                if (uiState.draft.referenceNumber.isNotBlank()) {
                    ReviewRow("Ref #", uiState.draft.referenceNumber)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomButtons(
    canGoBack: Boolean,
    isReview: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (canGoBack) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier.weight(1f).height(52.dp),
            enabled = !isSaving,
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
            } else {
                Text(
                    text = if (isReview) "Save Sale" else "Next Step",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
