package com.example.carwash.screen.mainscreen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.carwash.components.CameraCaptureBox
import com.example.carwash.model.AuthViewModel
import com.example.carwash.model.CommissionRateItem
import com.example.carwash.model.ProfileViewModel
import com.example.carwash.model.ServicePackage
import com.example.carwash.model.User
import com.example.carwash.screen.Screen
import com.example.carwash.screen.SubscriptionScreen
import com.example.carwash.ui.theme.CarwashTheme
import com.example.carwash.utils.BugReporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CrudType {
    TEAMS,
    PACKAGES,
    SIZES,
    COMMISSIONS
}

@Composable
fun Profile(
    navController: NavController,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val error by viewModel.error.observeAsState()
    var showSubscriptionPayScreen by remember { mutableStateOf(false) }

    val teamsList by profileViewModel.teams.collectAsState()
    val packagesList by profileViewModel.packages.collectAsState()
    val sizesList by profileViewModel.vehicleSizes.collectAsState()
    val commissionList by profileViewModel.commissionRates.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    ProfileContent(
        user = user,
        error = error,
        isDarkMode = isDarkMode,
        onToggleDarkMode = onToggleDarkMode,
        teamsList = teamsList,
        packagesList = packagesList,
        sizesList = sizesList,
        commissionList = commissionList,
        onAddTeam = profileViewModel::addTeam,
        onEditTeam = profileViewModel::updateTeam,
        onDeleteTeam = profileViewModel::deleteTeam,
        onAddPackage = profileViewModel::addPackage,
        onEditPackage = profileViewModel::updatePackage,
        onDeletePackage = profileViewModel::deletePackage,
        onAddSize = profileViewModel::addVehicleSize,
        onEditSize = profileViewModel::updateVehicleSize,
        onDeleteSize = profileViewModel::deleteVehicleSize,
        onAddCommission = profileViewModel::addCommissionRate,
        onEditCommission = profileViewModel::updateCommissionRate,
        onDeleteCommission = profileViewModel::deleteCommissionRate,
        onChangeAdminPassword = viewModel::updateAdminPassword,
        onUpgradeProClick = { showSubscriptionPayScreen = true },
        onReload = { viewModel.loadUser() },
        onLogout = {
            viewModel.logout()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    )

    if (showSubscriptionPayScreen) {
        SubscriptionScreen(
            user = user,
            onCreateCheckoutSession = viewModel::createCheckoutSession,
            onActivateSubscription = viewModel::activateSubscription,
            onSubscriptionSuccess = {
                viewModel.loadUser()
                showSubscriptionPayScreen = false
            },
            onLogout = {
                showSubscriptionPayScreen = false
                viewModel.logout()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    user: User?,
    error: String?,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    teamsList: List<String> = listOf("Team A", "Team B", "Team C"),
    packagesList: List<ServicePackage> = ServicePackage.defaultPackages,
    sizesList: List<String> = listOf("SMALL", "MEDIUM", "LARGE", "EXTRA_LARGE"),
    commissionList: List<CommissionRateItem> = listOf(
        CommissionRateItem("FORTY", "40% (Default)", 0.40, 0.60)
    ),
    onAddTeam: (String) -> Unit = {},
    onEditTeam: (Int, String) -> Unit = { _, _ -> },
    onDeleteTeam: (Int) -> Unit = {},
    onAddPackage: (ServicePackage) -> Unit = {},
    onEditPackage: (Int, ServicePackage) -> Unit = { _, _ -> },
    onDeletePackage: (Int) -> Unit = {},
    onAddSize: (String) -> Unit = {},
    onEditSize: (Int, String) -> Unit = { _, _ -> },
    onDeleteSize: (Int) -> Unit = {},
    onAddCommission: (CommissionRateItem) -> Unit = {},
    onEditCommission: (Int, CommissionRateItem) -> Unit = { _, _ -> },
    onDeleteCommission: (Int) -> Unit = {},
    onChangeAdminPassword: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onUpgradeProClick: () -> Unit = {},
    onReload: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    // Admin Security States
    var isAdminAuthenticated by remember { mutableStateOf(false) }
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showChangeAdminPasswordDialog by remember { mutableStateOf(false) }
    var showReportBugDialog by remember { mutableStateOf(false) }
    var pendingCrudType by remember { mutableStateOf<CrudType?>(null) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminPasswordError by remember { mutableStateOf<String?>(null) }

    // CRUD Dialog States
    var showTeamsDialog by remember { mutableStateOf(false) }
    var showPackagesDialog by remember { mutableStateOf(false) }
    var showSizesDialog by remember { mutableStateOf(false) }
    var showCommissionDialog by remember { mutableStateOf(false) }

    fun openCrudDialog(type: CrudType) {
        when (type) {
            CrudType.TEAMS -> showTeamsDialog = true
            CrudType.PACKAGES -> showPackagesDialog = true
            CrudType.SIZES -> showSizesDialog = true
            CrudType.COMMISSIONS -> showCommissionDialog = true
        }
    }

    fun onCrudOptionClick(type: CrudType) {
        if (isAdminAuthenticated) {
            openCrudDialog(type)
        } else {
            pendingCrudType = type
            adminPasswordInput = ""
            adminPasswordError = null
            showAdminPasswordDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "My Profile", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(90.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.carwash.R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = user?.name?.ifBlank { "Carwash Operator" } ?: "Carwash Operator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = user?.email?.ifBlank { "operator@carwash.com" } ?: "operator@carwash.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "Admin • Main Branch",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Error banner if any
            error?.let { errorMessage ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onReload) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Subscription Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (user?.isSubscriptionValid == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (user?.isSubscriptionValid == true) "Carwash Sale Tracker Pro Plan" else "Free Trial Plan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (user?.isSubscriptionValid == true) {
                                if ((user?.remainingSubscriptionDays ?: 0) > 0)
                                    "Active Subscription • ${user?.remainingSubscriptionDays} day(s) remaining"
                                else
                                    "Active Subscription • ₱199/mo"
                            } else {
                                "${user?.remainingTrialDays ?: 0} day(s) remaining in free trial"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Button(
                        onClick = onUpgradeProClick,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(if (user?.isSubscriptionValid == true) "Manage Pro" else "Upgrade Pro")
                    }
                }
            }

            // Quick Info Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileInfoCard(
                    modifier = Modifier.weight(1f),
                    title = "Joined Date",
                    value = formatJoinedDate(user?.createdAs ?: System.currentTimeMillis()),
                    icon = Icons.Default.CalendarToday
                )
                ProfileInfoCard(
                    modifier = Modifier.weight(1f),
                    title = "Business",
                    value = "Carwash",
                    icon = Icons.Default.Store
                )
            }

            // App Display & Theme Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isDarkMode) "Dark theme enabled" else "Light theme enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode
                    )
                }
            }

            // Management CRUD Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Management & CRUD Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isAdminAuthenticated) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.clickable { isAdminAuthenticated = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Unlocked (Lock)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Locked",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Management & CRUD Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ProfileOptionItem(
                        icon = Icons.Default.Groups,
                        title = "Assign Teams (${teamsList.size})",
                        subtitle = "Manage Work Teams (${teamsList.joinToString(", ")})",
                        onClick = { onCrudOptionClick(CrudType.TEAMS) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileOptionItem(
                        icon = Icons.Default.LocalCarWash,
                        title = "Service Packages (${packagesList.size})",
                        subtitle = "Manage Packages & Prices",
                        onClick = { onCrudOptionClick(CrudType.PACKAGES) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileOptionItem(
                        icon = Icons.Default.DirectionsCar,
                        title = "Vehicle Sizes (${sizesList.size})",
                        subtitle = "Manage Sizes (${sizesList.joinToString(", ")})",
                        onClick = { onCrudOptionClick(CrudType.SIZES) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileOptionItem(
                        icon = Icons.Default.Percent,
                        title = "Carwash Boy Commission (${commissionList.size})",
                        subtitle = "Manage Worker & Owner Rates",
                        onClick = { onCrudOptionClick(CrudType.COMMISSIONS) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileOptionItem(
                        icon = Icons.Default.Lock,
                        title = "Change Admin Security Password",
                        subtitle = "Update password for CRUD & Editing sales",
                        onClick = { showChangeAdminPasswordDialog = true }
                    )
                }
            }

            // Report Bug / Technical Issue Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                ProfileOptionItem(
                    icon = Icons.Default.BugReport,
                    title = "Report Bug / Technical Issue",
                    subtitle = "Send issue details & screenshot to support",
                    onClick = { showReportBugDialog = true }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout"
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Admin Password Security Dialog
    if (showAdminPasswordDialog) {
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAdminPasswordDialog = false
                pendingCrudType = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Admin Authentication Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter admin password to access CRUD management settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = {
                            adminPasswordInput = it
                            adminPasswordError = null
                        },
                        label = { Text("Admin Password") },
                        singleLine = true,
                        isError = adminPasswordError != null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    adminPasswordError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetAdminPass = user?.effectiveAdminPassword ?: "admin123"
                        if (adminPasswordInput.trim() == targetAdminPass) {
                            isAdminAuthenticated = true
                            showAdminPasswordDialog = false
                            adminPasswordError = null
                            pendingCrudType?.let { openCrudDialog(it) }
                            pendingCrudType = null
                        } else {
                            adminPasswordError = "Incorrect Admin Password"
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdminPasswordDialog = false
                        pendingCrudType = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Change Admin Password Dialog
    if (showChangeAdminPasswordDialog) {
        var currentInput by remember { mutableStateOf("") }
        var newInput by remember { mutableStateOf("") }
        var changeError by remember { mutableStateOf<String?>(null) }
        var isSavingPass by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangeAdminPasswordDialog = false },
            icon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            },
            title = { Text("Change Admin Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter current admin password and your new admin password.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it; changeError = null },
                        label = { Text("Current Admin Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newInput,
                        onValueChange = { newInput = it; changeError = null },
                        label = { Text("New Admin Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    changeError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentTarget = user?.effectiveAdminPassword ?: "admin123"
                        if (currentInput.trim() != currentTarget) {
                            changeError = "Incorrect current admin password"
                        } else if (newInput.trim().isBlank()) {
                            changeError = "New admin password cannot be blank"
                        } else {
                            isSavingPass = true
                            onChangeAdminPassword(newInput.trim()) { success ->
                                isSavingPass = false
                                if (success) {
                                    showChangeAdminPasswordDialog = false
                                } else {
                                    changeError = "Failed to update admin password"
                                }
                            }
                        }
                    },
                    enabled = !isSavingPass
                ) {
                    Text("Save Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeAdminPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Report Bug Dialog
    if (showReportBugDialog) {
        var bugDescription by remember { mutableStateOf("") }
        var bugScreenshotUri by remember { mutableStateOf<Uri?>(null) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showReportBugDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Report Bug / Technical Issue", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Describe what bug or error occurred. You can also capture or attach a screenshot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = bugDescription,
                        onValueChange = { bugDescription = it },
                        label = { Text("Describe Issue / Bug") },
                        placeholder = { Text("e.g. Total sale calculation did not display") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    CameraCaptureBox(
                        title = "Attach Bug Photo / Screenshot",
                        imageUri = bugScreenshotUri,
                        onImageCaptured = { bugScreenshotUri = it },
                        onRemove = { bugScreenshotUri = null }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        BugReporter.sendBugReport(
                            context = context,
                            userDescription = bugDescription,
                            screenshotUri = bugScreenshotUri
                        )
                        showReportBugDialog = false
                    }
                ) {
                    Text("Send Email to Support")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportBugDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 1. Teams CRUD Dialog
    if (showTeamsDialog) {
        ManageTeamsDialog(
            teams = teamsList,
            onDismiss = { showTeamsDialog = false },
            onAddTeam = onAddTeam,
            onEditTeam = onEditTeam,
            onDeleteTeam = onDeleteTeam
        )
    }

    // 2. Packages CRUD Dialog
    if (showPackagesDialog) {
        ManagePackagesDialog(
            packages = packagesList,
            onDismiss = { showPackagesDialog = false },
            onAddPackage = onAddPackage,
            onEditPackage = onEditPackage,
            onDeletePackage = onDeletePackage
        )
    }

    // 3. Vehicle Sizes CRUD Dialog
    if (showSizesDialog) {
        ManageSizesDialog(
            sizes = sizesList,
            onDismiss = { showSizesDialog = false },
            onAddSize = onAddSize,
            onEditSize = onEditSize,
            onDeleteSize = onDeleteSize
        )
    }

    // 4. Commission Rates CRUD Dialog
    if (showCommissionDialog) {
        ManageCommissionDialog(
            rates = commissionList,
            onDismiss = { showCommissionDialog = false },
            onAddRate = onAddCommission,
            onEditRate = onEditCommission,
            onDeleteRate = onDeleteCommission
        )
    }
}

// ----------------------------------------------------
// CRUD Dialogs
// ----------------------------------------------------

// 1. Teams Dialog
@Composable
fun ManageTeamsDialog(
    teams: List<String>,
    onDismiss: () -> Unit,
    onAddTeam: (String) -> Unit,
    onEditTeam: (Int, String) -> Unit,
    onDeleteTeam: (Int) -> Unit
) {
    var newTeamName by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Work Teams") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTeamName,
                        onValueChange = { newTeamName = it },
                        label = { Text(if (editingIndex != null) "Edit Team Name" else "New Team Name") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newTeamName.isNotBlank()) {
                                if (editingIndex != null) {
                                    onEditTeam(editingIndex!!, newTeamName.trim())
                                    editingIndex = null
                                } else {
                                    onAddTeam(newTeamName.trim())
                                }
                                newTeamName = ""
                            }
                        }
                    ) {
                        Text(if (editingIndex != null) "Save" else "Add")
                    }
                }

                HorizontalDivider()

                Column(modifier = Modifier.heightIn(max = 250.dp).verticalScroll(rememberScrollState())) {
                    teams.forEachIndexed { index, team ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = team, fontWeight = FontWeight.Medium)
                            Row {
                                IconButton(onClick = {
                                    editingIndex = index
                                    newTeamName = team
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteTeam(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// 2. Packages Dialog
@Composable
fun ManagePackagesDialog(
    packages: List<ServicePackage>,
    onDismiss: () -> Unit,
    onAddPackage: (ServicePackage) -> Unit,
    onEditPackage: (Int, ServicePackage) -> Unit,
    onDeletePackage: (Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var basePrice by remember { mutableStateOf("") }
    var customAllowed by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Service Packages") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = basePrice,
                    onValueChange = { basePrice = it },
                    label = { Text("Base Price (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = customAllowed, onCheckedChange = { customAllowed = it })
                    Text("Allow Custom Price")
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val priceVal = basePrice.toDoubleOrNull() ?: 0.0
                            val pkg = ServicePackage(
                                id = name.lowercase().replace(" ", "_"),
                                name = name.trim(),
                                description = description.trim(),
                                price = priceVal,
                                prices = emptyMap(),
                                customPriceAllowed = customAllowed
                            )
                            if (editingIndex != null) {
                                onEditPackage(editingIndex!!, pkg)
                                editingIndex = null
                            } else {
                                onAddPackage(pkg)
                            }
                            name = ""
                            description = ""
                            basePrice = ""
                            customAllowed = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingIndex != null) "Save Package" else "Add Package")
                }

                HorizontalDivider()

                packages.forEachIndexed { index, pkg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = pkg.name, fontWeight = FontWeight.Bold)
                            Text(text = pkg.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = {
                                editingIndex = index
                                name = pkg.name
                                description = pkg.description
                                basePrice = pkg.price.toString()
                                customAllowed = pkg.customPriceAllowed
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeletePackage(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// 3. Vehicle Sizes Dialog
@Composable
fun ManageSizesDialog(
    sizes: List<String>,
    onDismiss: () -> Unit,
    onAddSize: (String) -> Unit,
    onEditSize: (Int, String) -> Unit,
    onDeleteSize: (Int) -> Unit
) {
    var sizeName by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Vehicle Sizes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sizeName,
                        onValueChange = { sizeName = it },
                        label = { Text("Size Label") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (sizeName.isNotBlank()) {
                                if (editingIndex != null) {
                                    onEditSize(editingIndex!!, sizeName.trim().uppercase())
                                    editingIndex = null
                                } else {
                                    onAddSize(sizeName.trim().uppercase())
                                }
                                sizeName = ""
                            }
                        }
                    ) {
                        Text(if (editingIndex != null) "Save" else "Add")
                    }
                }

                HorizontalDivider()

                Column(modifier = Modifier.heightIn(max = 250.dp).verticalScroll(rememberScrollState())) {
                    sizes.forEachIndexed { index, size ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = size, fontWeight = FontWeight.Medium)
                            Row {
                                IconButton(onClick = {
                                    editingIndex = index
                                    sizeName = size
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteSize(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// 4. Commission Rates Dialog
@Composable
fun ManageCommissionDialog(
    rates: List<CommissionRateItem>,
    onDismiss: () -> Unit,
    onAddRate: (CommissionRateItem) -> Unit,
    onEditRate: (Int, CommissionRateItem) -> Unit,
    onDeleteRate: (Int) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var workerPercentText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Carwash Boy Commission Tiers") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Rate Title (e.g. 40%)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = workerPercentText,
                    onValueChange = { workerPercentText = it },
                    label = { Text("Carwash Boy % (e.g. 40)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val pct = workerPercentText.toDoubleOrNull() ?: 0.0
                        val workerPct = pct / 100.0
                        val ownerPct = (100.0 - pct).coerceAtLeast(0.0) / 100.0
                        if (label.isNotBlank()) {
                            val item = CommissionRateItem(
                                id = label.lowercase().replace(" ", "_"),
                                displayName = label.trim(),
                                workerPercent = workerPct,
                                ownerPercent = ownerPct
                            )
                            if (editingIndex != null) {
                                onEditRate(editingIndex!!, item)
                                editingIndex = null
                            } else {
                                onAddRate(item)
                            }
                            label = ""
                            workerPercentText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingIndex != null) "Save Tier" else "Add Tier")
                }

                HorizontalDivider()

                rates.forEachIndexed { index, rate ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = rate.displayName, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Worker: ${(rate.workerPercent * 100).toInt()}% • Owner: ${(rate.ownerPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                editingIndex = index
                                label = rate.displayName
                                workerPercentText = (rate.workerPercent * 100).toInt().toString()
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteRate(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ProfileInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatJoinedDate(timeMillis: Long): String {
    return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timeMillis))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfilePreview() {
    CarwashTheme {
        ProfileContent(
            user = User(
                uid = "123",
                name = "John Manager",
                email = "admin@custoworks.com",
                createdAs = System.currentTimeMillis()
            ),
            error = null
        )
    }
}
