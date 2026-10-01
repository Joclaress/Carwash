package com.example.carwash.add

enum class AddSaleStep(  val title: String) {
    VEHICLE_IMAGE("Vehicle Image"),
    SERVICE("Service Package"),
    PAYMENT("Payment"),
    REVIEW("Review");

    fun next(): AddSaleStep {
        return entries[(ordinal + 1 ).coerceAtMost(entries.lastIndex)]
    }
    fun previous(): AddSaleStep {
        return entries[(ordinal - 1 ).coerceAtLeast(0)]
    }

}