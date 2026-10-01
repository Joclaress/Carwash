package com.example.carwash.add

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Sale(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("plateNumber") @set:PropertyName("plateNumber") var plateNumber: String = "",
    @get:PropertyName("packageId") @set:PropertyName("packageId") var packageId: String = "",
    @get:PropertyName("packageName") @set:PropertyName("packageName") var packageName: String = "",
    @get:PropertyName("customPrice") @set:PropertyName("customPrice") var customPrice: Double = 0.0,
    @get:PropertyName("amount") @set:PropertyName("amount") var amount: Double = 0.0,
    @get:PropertyName("vehicleSize") @set:PropertyName("vehicleSize") var vehicleSize: String = "",
    @get:PropertyName("notes") @set:PropertyName("notes") var notes: String = "",
    @get:PropertyName("paymentMethod") @set:PropertyName("paymentMethod") var paymentMethod: String = "",
    @get:PropertyName("referenceNumber") @set:PropertyName("referenceNumber") var referenceNumber: String = "",
    @get:PropertyName("changeAmount") @set:PropertyName("changeAmount") var changeAmount: Double = 0.0,
    @get:PropertyName("cashReceived") @set:PropertyName("cashReceived") var cashReceived: Double = 0.0,
    @get:PropertyName("vehicleImageUrl") @set:PropertyName("vehicleImageUrl") var vehicleImageUrl: String = "",
    @get:PropertyName("paymentImageUrl") @set:PropertyName("paymentImageUrl") var paymentImageUrl: String = "",
    @get:PropertyName("assignedTeam") @set:PropertyName("assignedTeam") var assignedTeam: String = "",
    @get:PropertyName("workerPercent") @set:PropertyName("workerPercent") var workerPercent: Double = 0.40,
    @get:PropertyName("ownerPercent") @set:PropertyName("ownerPercent") var ownerPercent: Double = 0.60,
    @get:PropertyName("workerCommission") @set:PropertyName("workerCommission") var workerCommission: Double = 0.0,
    @get:PropertyName("ownerShare") @set:PropertyName("ownerShare") var ownerShare: Double = 0.0,
    @get:PropertyName("CreatedAt") @set:PropertyName("CreatedAt") var CreatedAt: Timestamp? = null
)

fun String.normalizePlateNumber(): String {
    return this.replace("-", "")
        .replace(" ", "")
        .replace("_", "")
        .uppercase()
        .trim()
}

val Sale.cleanPlateNumber: String
    get() = plateNumber.normalizePlateNumber()
