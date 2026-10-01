package com.example.carwash.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carwash.add.AddSaleStep
import com.example.carwash.add.AddSaleUiState
import com.example.carwash.add.Sale
import com.example.carwash.add.SaleDraft
import com.example.carwash.add.cleanPlateNumber
import com.example.carwash.add.normalizePlateNumber
import com.example.carwash.repository.SaleRepository
import com.example.carwash.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddSaleViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSaleUiState())
    val uiState = _uiState.asStateFlow()

    val packages: StateFlow<List<ServicePackage>> = settingsRepository.packages
    val vehicleSizes: StateFlow<List<String>> = settingsRepository.vehicleSizes
    val paymentMethods: List<PaymentMethod> = PaymentMethod.entries
    val teams: StateFlow<List<String>> = settingsRepository.teams
    val commissionRates: StateFlow<List<CommissionRateItem>> = settingsRepository.commissionRates

    private var existingSales: List<Sale> = emptyList()

    init {
        viewModelScope.launch {
            saleRepository.observeRecentSale(500)
                .catch { }
                .collect { sales ->
                    existingSales = sales
                    checkRepeatCustomer(_uiState.value.draft.plateNumber)
                }
        }
    }

    fun setVehicleImage(uri: Uri?) {
        updateDraft { draft ->
            draft.copy(vehicleImageUri = uri)
        }
    }

    fun removeVehicle() {
        setVehicleImage(null)
    }

    fun updatePlateNumber(plateNumber: String) {
        updateDraft { draft ->
            draft.copy(plateNumber = plateNumber)
        }
        checkRepeatCustomer(plateNumber)
    }

    private fun checkRepeatCustomer(plateNumber: String) {
        val cleanPlate = plateNumber.normalizePlateNumber()
        val isRepeat = cleanPlate.isNotBlank() && existingSales.any {
            it.cleanPlateNumber == cleanPlate
        }
        _uiState.update { it.copy(isRepeatCustomer = isRepeat) }
    }

    fun selectTeam(teamName: String) {
        updateDraft { draft ->
            draft.copy(assignedTeamName = teamName)
        }
    }

    fun selectPackage(servicePackage: ServicePackage) {
        updateDraft { draft ->
            val packageChanged = draft.selectedPackage?.id != servicePackage.id

            draft.copy(
                selectedPackage = servicePackage,
                selectedVehicleSizeLabel = if (servicePackage.customPriceAllowed) "" else draft.selectedVehicleSizeLabel,
                customPrice = if (servicePackage.customPriceAllowed) draft.customPrice else "",
                notes = if (packageChanged && !servicePackage.customPriceAllowed) "" else draft.notes,
                cashReceived = 0.0,
                referenceNumber = "",
                paymentImageUri = null
            )
        }
    }

    fun selectVehicleSize(vehicleSizeLabel: String) {
        updateDraft { draft ->
            draft.copy(
                selectedVehicleSizeLabel = vehicleSizeLabel,
                cashReceived = 0.0,
                referenceNumber = "",
                paymentImageUri = null
            )
        }
    }

    fun updateCustomPrice(value: String) {
        val sanitizedValue = sanitizeDecimalInput(value)
        updateDraft { draft ->
            draft.copy(
                customPrice = sanitizedValue,
                cashReceived = 0.0,
                referenceNumber = "",
                paymentImageUri = null
            )
        }
    }

    fun updateNotes(value: String) {
        updateDraft { draft ->
            draft.copy(notes = value.take(MAX_NOTES_LENGTH))
        }
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        updateDraft { draft ->
            if (draft.paymentMethod == paymentMethod) {
                draft
            } else {
                draft.copy(
                    paymentMethod = paymentMethod,
                    cashReceived = 0.0,
                    referenceNumber = "",
                    paymentImageUri = null
                )
            }
        }
    }

    fun updateCashReceived(value: String) {
        val sanitizedValue = sanitizeDecimalInput(value)
        val amount = sanitizedValue.toDoubleOrNull() ?: 0.0
        updateDraft { draft ->
            draft.copy(cashReceived = amount)
        }
    }

    fun updateReferenceNumber(value: String) {
        updateDraft { draft ->
            draft.copy(
                referenceNumber = value
                    .trimStart()
                    .take(MAX_REFERENCE_NUMBER_LENGTH)
            )
        }
    }

    fun setPaymentImage(uri: Uri?) {
        updateDraft { draft ->
            draft.copy(paymentImageUri = uri)
        }
    }

    fun removePaymentImage() {
        setPaymentImage(null)
    }

    fun nextStep() {
        val currentState = _uiState.value
        val error = validateCurrentstep(step = currentState.currentStep, draft = currentState.draft)
        
        if (error != null) {
            showError(error)
            return
        }

        if (currentState.currentStep == AddSaleStep.REVIEW) {
            saveSale()
            return
        }

        _uiState.update { state ->
            state.copy(
                currentStep = currentState.currentStep.next(),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun previousStep() {
        val currentState = _uiState.value
        if (!currentState.canGoBack) return

        _uiState.update { state ->
            state.copy(
                currentStep = currentState.currentStep.previous(),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun goToStep(step: AddSaleStep) {
        _uiState.update { state ->
            state.copy(currentStep = step, errorMessage = null, successMessage = null)
        }
    }

    fun saveSale() {
        val currentState = _uiState.value
        if (currentState.isSaving) {
            return
        }
        val validationError = currentState.draft.validateAll()
        if (validationError != null) {
            showError(validationError)
            return
        }
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isSaving = true,
                    successMessage = null,
                    errorMessage = null,
                    saveSaleId = null
                )
            }

            val result = saleRepository.saveSale(currentState.draft)

            result.onSuccess { saleId ->
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveSaleId = saleId,
                        successMessage = "Sale saved successfully!",
                        errorMessage = null
                    )
                }
            }.onFailure { exception ->
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveSaleId = null,
                        successMessage = null,
                        errorMessage = "Failed to save sale: ${exception.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { state ->
            state.copy(errorMessage = null, successMessage = null)
        }
    }

    fun resetForm() {
        _uiState.value = AddSaleUiState()
    }

    private fun updateDraft(transform: (SaleDraft) -> SaleDraft) {
        _uiState.update { state ->
            state.copy(
                draft = transform(state.draft),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun validateCurrentstep(
        step: AddSaleStep,
        draft: SaleDraft
    ): String? {
        return when (step) {
            AddSaleStep.VEHICLE_IMAGE -> {
                draft.validateVehicle() ?: draft.validatePlateNumber()
            }

            AddSaleStep.SERVICE -> {
                draft.validatePackage()
            }

            AddSaleStep.PAYMENT -> {
                draft.validatePayment()
            }

            AddSaleStep.REVIEW -> {
                draft.validateAll()
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update { state ->
            state.copy(errorMessage = message, successMessage = null)
        }
    }

    private fun sanitizeDecimalInput(input: String): String {
        val filtered = input.filter { character ->
            character.isDigit() || character == '.'
        }
        if (filtered.isBlank()) {
            return ""
        }
        val firstDecimalIndex = filtered.indexOf('.')
        if (firstDecimalIndex == -1) {
            return filtered.take(MAX_AMOUNT_LENGTH)
        }
        val wholeNumber = filtered.substring(0, firstDecimalIndex).take(MAX_AMOUNT_LENGTH)
        val decimalNumber = filtered.substring(firstDecimalIndex + 1).replace(".", "").take(2)
        return "$wholeNumber.$decimalNumber"
    }

    private companion object {
        const val MAX_NOTES_LENGTH = 500
        const val MAX_REFERENCE_NUMBER_LENGTH = 50
        const val MAX_AMOUNT_LENGTH = 9
    }

    fun selectCommissionRate(
        rate: CommissionRateItem
    ) {
        updateDraft {
            it.copy(
                selectedCommissionRate = rate
            )
        }
    }
}
