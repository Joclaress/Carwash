package com.example.carwash.add

import android.net.Uri
import com.example.carwash.model.CommissionRate
import com.example.carwash.model.CommissionRateItem
import com.example.carwash.model.PaymentMethod
import com.example.carwash.model.ServicePackage
import com.example.carwash.model.VehicleSize
import com.example.carwash.model.WorkTeam

data class SaleDraft(
    val vehicleImageUri: Uri? = null,
    val plateNumber: String = "",
    val assignedTeamName: String = "",
    val selectedPackage: ServicePackage? = null,
    val selectedVehicleSizeLabel: String = "",
    val customPrice: String = "",
    val selectedCommissionRate: CommissionRateItem = CommissionRateItem("FORTY", "40% (Default)", 0.40, 0.60),

    val notes: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentImageUri: Uri? = null,
    val referenceNumber: String = "",
    val cashReceived: Double = 0.0
) {
    val assignedTeam: WorkTeam?
        get() = WorkTeam.entries.find { it.displayName.equals(assignedTeamName, ignoreCase = true) || it.name.equals(assignedTeamName, ignoreCase = true) }

    val selectedVehicleSize: VehicleSize?
        get() = VehicleSize.entries.find { it.name.equals(selectedVehicleSizeLabel, ignoreCase = true) }

    val commissionRate: CommissionRate
        get() = CommissionRate.entries.find { (it.workerPercent * 100).toInt() == (selectedCommissionRate.workerPercent * 100).toInt() } ?: CommissionRate.FORTY

    val useCustomPrice: Boolean
        get() = selectedPackage?.customPriceAllowed == true

    val amount: Double
        get() {
            val servicePackage = selectedPackage ?: return 0.0
            return if (servicePackage.customPriceAllowed) {
                customPrice.toDoubleOrNull() ?: 0.0
            } else {
                servicePackage.getPriceForLabel(selectedVehicleSizeLabel)
            }
        }

    val cashReceivedAmount: Double
        get() = cashReceived

    val changeAmount: Double
        get() {
            return if (paymentMethod == PaymentMethod.CASH) {
                (cashReceivedAmount - amount).coerceAtLeast(0.0)
            } else {
                0.0
            }
        }

    fun validateTeam(): String? {
        return if (assignedTeamName.isBlank()) "Team is required" else null
    }

    fun validatedVehicleImage(): String? {
        return when {
            vehicleImageUri == null -> "Please capture a vehicle image."
            assignedTeamName.isBlank() -> "Please select a team."
            else -> null
        }
    }

    fun validatePlateNumber(): String? {
        return when {
            plateNumber.isEmpty() -> "Plate Number is required"
            plateNumber.trim().length < 3 -> "Invalid Plate Number"
            else -> null
        }
    }

    fun validateVehicle(): String? {
        return when {
            vehicleImageUri == null -> "Please capture a vehicle image."
            assignedTeamName.isBlank() -> "Please select a team."
            else -> null
        }
    }

    fun validatePackage(): String? {
        return when {
            selectedPackage == null -> "Please select a service package."
            !useCustomPrice && selectedVehicleSizeLabel.isBlank() -> "Please select a vehicle size."
            useCustomPrice && customPrice.isBlank() -> "Please enter a custom price."
            useCustomPrice && amount <= 0 -> "Please enter a valid custom price."
            useCustomPrice && notes.isBlank() -> "Please enter notes for Others."
            amount <= 0.0 -> "Invalid amount. Please check your package price."
            else -> null
        }
    }

    fun validatePayment(): String? {
        return when (paymentMethod) {
            PaymentMethod.CASH -> {
                when {
                    cashReceived <= 0.0 -> "Please enter the cash received."
                    cashReceivedAmount < amount -> "Cash received is less than the amount."
                    paymentImageUri == null -> "Please add a photo of the cash received."
                    else -> null
                }
            }
            PaymentMethod.Gcash -> {
                when {
                    referenceNumber.isBlank() -> "Please enter a Gcash reference number."
                    paymentImageUri == null -> "Please add a photo of the payment."
                    else -> null
                }
            }
            PaymentMethod.BANK_TRANSFER -> {
                when {
                    referenceNumber.isBlank() -> "Please enter a bank transfer reference number."
                    paymentImageUri == null -> "Please add a photo of the payment."
                    else -> null
                }
            }
        }
    }

    val workerCommission: Double get() = amount * selectedCommissionRate.workerPercent
    val ownerShare: Double get() = amount * selectedCommissionRate.ownerPercent

    fun validateAll(): String? {
        return validatedVehicleImage()
            ?: validatePlateNumber()
            ?: validateTeam()
            ?: validatePackage()
            ?: validatePayment()
    }
}
