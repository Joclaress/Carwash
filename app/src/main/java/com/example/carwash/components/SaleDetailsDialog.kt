package com.example.carwash.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.carwash.add.Sale
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SaleDetailsDialog(
    sale: Sale,
    isRepeatCustomer: Boolean = false,
    adminPassword: String = "admin123",
    onDismiss: () -> Unit,
    onDeleteSale: (Sale) -> Unit,
    onUpdateSale: (Sale) -> Unit
) {
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Sale Details", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Vehicle Image Preview
                if (sale.vehicleImageUrl.isNotBlank()) {
                    Text(text = "Vehicle Photo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Card(shape = RoundedCornerShape(12.dp)) {
                        AsyncImage(
                            model = sale.vehicleImageUrl,
                            contentDescription = "Vehicle Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }

                // Money / Payment Proof Photo ("money picture")
                if (sale.paymentImageUrl.isNotBlank()) {
                    Text(text = "Money / Payment Proof Photo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Card(shape = RoundedCornerShape(12.dp)) {
                        AsyncImage(
                            model = sale.paymentImageUrl,
                            contentDescription = "Payment Proof",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Details Rows
                DetailRow("Plate Number", sale.plateNumber.ifBlank { "-" })
                if (isRepeatCustomer) {
                    DetailRow("Status", "Repeat Customer (Welcome Back!)")
                }
                DetailRow("Package", sale.packageName.ifBlank { "Carwash" })
                DetailRow("Vehicle Size", sale.vehicleSize.ifBlank { "-" })
                DetailRow("Assigned Team", sale.assignedTeam.ifBlank { "-" })
                DetailRow("Amount Paid", "₱%,.2f".format(sale.amount))
                val workerAmount = if (sale.workerCommission > 0) sale.workerCommission else sale.amount * sale.workerPercent
                val ownerAmount = if (sale.ownerShare > 0) sale.ownerShare else sale.amount * sale.ownerPercent
                DetailRow("Carwash Boy Commission", "${"₱%,.2f".format(workerAmount)} (${(sale.workerPercent * 100).toInt()}%)")
                DetailRow("Owner Share", "${"₱%,.2f".format(ownerAmount)} (${(sale.ownerPercent * 100).toInt()}%)")
                DetailRow("Payment Method", sale.paymentMethod.ifBlank { "CASH" })
                if (sale.referenceNumber.isNotBlank()) {
                    DetailRow("Ref #", sale.referenceNumber)
                }
                if (sale.cashReceived > 0) {
                    DetailRow("Cash Received", "₱%,.2f".format(sale.cashReceived))
                    DetailRow("Change", "₱%,.2f".format(sale.changeAmount))
                }
                if (sale.notes.isNotBlank()) {
                    DetailRow("Notes", sale.notes)
                }
                val dateStr = sale.CreatedAt?.toDate()?.let {
                    SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(it)
                } ?: "-"
                DetailRow("Date & Time", dateStr)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        pendingAction = "EDIT"
                        showAdminPasswordDialog = true
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }

                Button(
                    onClick = {
                        pendingAction = "DELETE"
                        showAdminPasswordDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Admin Password Dialog for CRUD on Sales
    if (showAdminPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAdminPasswordDialog = false },
            icon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            },
            title = { Text("Admin Authentication Required", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter admin password to ${if (pendingAction == "DELETE") "delete" else "edit"} this sale.")
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("Admin Password") },
                        singleLine = true,
                        isError = passwordError != null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    passwordError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPass = adminPassword.ifBlank { "admin123" }
                        if (passwordInput.trim() == targetPass) {
                            showAdminPasswordDialog = false
                            passwordError = null
                            if (pendingAction == "DELETE") {
                                onDeleteSale(sale)
                                onDismiss()
                            } else if (pendingAction == "EDIT") {
                                showEditDialog = true
                            }
                        } else {
                            passwordError = "Incorrect Admin Password"
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Sale Details Dialog
    if (showEditDialog) {
        var editPlateText by remember { mutableStateOf(sale.plateNumber) }
        var editAmountText by remember { mutableStateOf(sale.amount.toString()) }
        var editCommissionText by remember { mutableStateOf((sale.workerPercent * 100).toInt().toString()) }
        var editTeamText by remember { mutableStateOf(sale.assignedTeam) }
        var editNotesText by remember { mutableStateOf(sale.notes) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Sale Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editPlateText,
                        onValueChange = { editPlateText = it },
                        label = { Text("Plate Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { editAmountText = it },
                        label = { Text("Total Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCommissionText,
                        onValueChange = { editCommissionText = it },
                        label = { Text("Carwash Boy Commission % (e.g. 40)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTeamText,
                        onValueChange = { editTeamText = it },
                        label = { Text("Assigned Team (e.g. Team A)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotesText,
                        onValueChange = { editNotesText = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = editAmountText.toDoubleOrNull() ?: sale.amount
                        val parsedCommPct = editCommissionText.toDoubleOrNull() ?: (sale.workerPercent * 100.0)
                        val newWorkerPct = (parsedCommPct / 100.0).coerceIn(0.0, 1.0)
                        val newOwnerPct = (1.0 - newWorkerPct).coerceAtLeast(0.0)

                        val updatedSale = sale.copy(
                            plateNumber = editPlateText.trim(),
                            amount = newAmount,
                            assignedTeam = editTeamText.trim(),
                            workerPercent = newWorkerPct,
                            ownerPercent = newOwnerPct,
                            workerCommission = newAmount * newWorkerPct,
                            ownerShare = newAmount * newOwnerPct,
                            notes = editNotesText.trim()
                        )
                        onUpdateSale(updatedSale)
                        showEditDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
