package com.example.carwash.add

data class AddSaleUiState(
    val currentStep: AddSaleStep = AddSaleStep.VEHICLE_IMAGE,
    val draft: SaleDraft = SaleDraft(),
    val isSaving: Boolean = false,
    val saveSaleId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isRepeatCustomer: Boolean = false
) {
    val currentStepNumber: Int
        get() = currentStep.ordinal + 1

    val totalSteps: Int
        get() = AddSaleStep.entries.size

    val canGoBack: Boolean
        get() = currentStep != AddSaleStep.VEHICLE_IMAGE
}
