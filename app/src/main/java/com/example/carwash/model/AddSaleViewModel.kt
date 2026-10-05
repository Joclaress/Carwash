package com.example.carwash.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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
import com.example.carwash.utils.PlateNumberOcr
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
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val packages: StateFlow<List<ServicePackage>> = settingsRepository.packages
    val vehicleSizes: StateFlow<List<String>> = settingsRepository.vehicleSizes
    val paymentMethods: List<PaymentMethod> = PaymentMethod.entries
    val teams: StateFlow<List<String>> = settingsRepository.teams
    val commissionRates: StateFlow<List<CommissionRateItem>> = settingsRepository.commissionRates

    private val _uiState = MutableStateFlow(restoreInitialState())
    val uiState: StateFlow<AddSaleUiState> = _uiState.asStateFlow()

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

        viewModelScope.launch {
            packages.collect { packageList ->
                val savedPackageId = savedStateHandle.get<String>(KEY_SELECTED_PACKAGE_ID)
                if (savedPackageId != null && _uiState.value.draft.selectedPackage == null) {
                    val foundPackage = packageList.find { it.id == savedPackageId }
                    if (foundPackage != null) {
                        updateDraft { draft -> draft.copy(selectedPackage = foundPackage) }
                    }
                }
            }
        }

        viewModelScope.launch {
            commissionRates.collect { rateList ->
                val savedCommId = savedStateHandle.get<String>(KEY_COMMISSION_RATE_ID)
                if (savedCommId != null) {
                    val foundRate = rateList.find { it.id == savedCommId }
                    if (foundRate != null) {
                        updateDraft { draft -> draft.copy(selectedCommissionRate = foundRate) }
                    }
                }
            }
        }
    }

    private fun restoreInitialState(): AddSaleUiState {
        val stepName = savedStateHandle.get<String>(KEY_CURRENT_STEP)
        val restoredStep = stepName?.let { name ->
            try { AddSaleStep.valueOf(name) } catch (_: Exception) { null }
        } ?: AddSaleStep.VEHICLE_IMAGE

        val savedVehicleUri = savedStateHandle.get<String>(KEY_VEHICLE_IMAGE_URI)?.let { Uri.parse(it) }
        val savedPlate = savedStateHandle.get<String>(KEY_PLATE_NUMBER).orEmpty()
        val savedTeam = savedStateHandle.get<String>(KEY_ASSIGNED_TEAM_NAME).orEmpty()
        val savedPackageId = savedStateHandle.get<String>(KEY_SELECTED_PACKAGE_ID)
        val savedSize = savedStateHandle.get<String>(KEY_SELECTED_VEHICLE_SIZE).orEmpty()
        val savedCustomPrice = savedStateHandle.get<String>(KEY_CUSTOM_PRICE).orEmpty()
        val savedCommId = savedStateHandle.get<String>(KEY_COMMISSION_RATE_ID)
        val savedNotes = savedStateHandle.get<String>(KEY_NOTES).orEmpty()
        val savedPaymentMethodName = savedStateHandle.get<String>(KEY_PAYMENT_METHOD)
        val savedPaymentUri = savedStateHandle.get<String>(KEY_PAYMENT_IMAGE_URI)?.let { Uri.parse(it) }
        val savedRefNum = savedStateHandle.get<String>(KEY_REFERENCE_NUMBER).orEmpty()
        val savedCash = savedStateHandle.get<Double>(KEY_CASH_RECEIVED) ?: 0.0

        val restoredPackage = savedPackageId?.let { id ->
            packages.value.find { it.id == id }
        }
        val restoredCommRate = savedCommId?.let { id ->
            commissionRates.value.find { it.id == id }
        } ?: CommissionRateItem("FORTY", "40% (Default)", 0.40, 0.60)

        val restoredPaymentMethod = savedPaymentMethodName?.let { name ->
            try { PaymentMethod.valueOf(name) } catch (_: Exception) { null }
        } ?: PaymentMethod.CASH

        val restoredDraft = SaleDraft(
            vehicleImageUri = savedVehicleUri,
            plateNumber = savedPlate,
            assignedTeamName = savedTeam,
            selectedPackage = restoredPackage,
            selectedVehicleSizeLabel = savedSize,
            customPrice = savedCustomPrice,
            selectedCommissionRate = restoredCommRate,
            notes = savedNotes,
            paymentMethod = restoredPaymentMethod,
            paymentImageUri = savedPaymentUri,
            referenceNumber = savedRefNum,
            cashReceived = savedCash
        )

        return AddSaleUiState(
            currentStep = restoredStep,
            draft = restoredDraft
        )
    }

    private fun saveStateToHandle(state: AddSaleUiState) {
        savedStateHandle[KEY_CURRENT_STEP] = state.currentStep.name
        savedStateHandle[KEY_VEHICLE_IMAGE_URI] = state.draft.vehicleImageUri?.toString()
        savedStateHandle[KEY_PLATE_NUMBER] = state.draft.plateNumber
        savedStateHandle[KEY_ASSIGNED_TEAM_NAME] = state.draft.assignedTeamName
        savedStateHandle[KEY_SELECTED_PACKAGE_ID] = state.draft.selectedPackage?.id
        savedStateHandle[KEY_SELECTED_VEHICLE_SIZE] = state.draft.selectedVehicleSizeLabel
        savedStateHandle[KEY_CUSTOM_PRICE] = state.draft.customPrice
        savedStateHandle[KEY_COMMISSION_RATE_ID] = state.draft.selectedCommissionRate.id
        savedStateHandle[KEY_NOTES] = state.draft.notes
        savedStateHandle[KEY_PAYMENT_METHOD] = state.draft.paymentMethod.name
        savedStateHandle[KEY_PAYMENT_IMAGE_URI] = state.draft.paymentImageUri?.toString()
        savedStateHandle[KEY_REFERENCE_NUMBER] = state.draft.referenceNumber
        savedStateHandle[KEY_CASH_RECEIVED] = state.draft.cashReceived
    }

    fun setVehicleImage(uri: Uri?) {
        updateDraft { draft ->
            draft.copy(vehicleImageUri = uri)
        }
    }

    fun scanPlateNumberOcr(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            val scannedPlate = PlateNumberOcr.recognizePlateNumber(context, imageUri)
            if (!scannedPlate.isNullOrBlank()) {
                updatePlateNumber(scannedPlate)
            }
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
        _uiState.update { state ->
            val newState = state.copy(isRepeatCustomer = isRepeat)
            saveStateToHandle(newState)
            newState
        }
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
            val newState = state.copy(
                currentStep = currentState.currentStep.next(),
                errorMessage = null,
                successMessage = null
            )
            saveStateToHandle(newState)
            newState
        }
    }

    fun previousStep() {
        val currentState = _uiState.value
        if (!currentState.canGoBack) return

        _uiState.update { state ->
            val newState = state.copy(
                currentStep = currentState.currentStep.previous(),
                errorMessage = null,
                successMessage = null
            )
            saveStateToHandle(newState)
            newState
        }
    }

    fun goToStep(step: AddSaleStep) {
        _uiState.update { state ->
            val newState = state.copy(currentStep = step, errorMessage = null, successMessage = null)
            saveStateToHandle(newState)
            newState
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
                val newState = state.copy(
                    isSaving = true,
                    successMessage = null,
                    errorMessage = null,
                    saveSaleId = null
                )
                saveStateToHandle(newState)
                newState
            }

            val result = saleRepository.saveSale(currentState.draft)

            result.onSuccess { saleId ->
                val isOffline = saleId.startsWith("offline_")
                val message = if (isOffline) {
                    "No internet connection. Your item has been saved and will upload when the internet is restored."
                } else {
                    "Sale saved successfully!"
                }
                _uiState.update { state ->
                    val newState = state.copy(
                        isSaving = false,
                        saveSaleId = saleId,
                        successMessage = message,
                        errorMessage = null
                    )
                    saveStateToHandle(newState)
                    newState
                }
            }.onFailure { exception ->
                _uiState.update { state ->
                    val newState = state.copy(
                        isSaving = false,
                        saveSaleId = null,
                        successMessage = null,
                        errorMessage = "Failed to save sale: ${exception.message}"
                    )
                    saveStateToHandle(newState)
                    newState
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { state ->
            val newState = state.copy(errorMessage = null, successMessage = null)
            saveStateToHandle(newState)
            newState
        }
    }

    fun resetForm() {
        savedStateHandle.keys().forEach { key ->
            savedStateHandle.remove<Any>(key)
        }
        _uiState.value = AddSaleUiState()
    }

    private fun updateDraft(transform: (SaleDraft) -> SaleDraft) {
        _uiState.update { state ->
            val newDraft = transform(state.draft)
            val newState = state.copy(
                draft = newDraft,
                errorMessage = null,
                successMessage = null
            )
            saveStateToHandle(newState)
            newState
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
            val newState = state.copy(errorMessage = message, successMessage = null)
            saveStateToHandle(newState)
            newState
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

    fun selectCommissionRate(
        rate: CommissionRateItem
    ) {
        updateDraft {
            it.copy(
                selectedCommissionRate = rate
            )
        }
    }

    private companion object {
        const val KEY_CURRENT_STEP = "key_current_step"
        const val KEY_VEHICLE_IMAGE_URI = "key_vehicle_image_uri"
        const val KEY_PLATE_NUMBER = "key_plate_number"
        const val KEY_ASSIGNED_TEAM_NAME = "key_assigned_team_name"
        const val KEY_SELECTED_PACKAGE_ID = "key_selected_package_id"
        const val KEY_SELECTED_VEHICLE_SIZE = "key_selected_vehicle_size"
        const val KEY_CUSTOM_PRICE = "key_custom_price"
        const val KEY_COMMISSION_RATE_ID = "key_commission_rate_id"
        const val KEY_NOTES = "key_notes"
        const val KEY_PAYMENT_METHOD = "key_payment_method"
        const val KEY_PAYMENT_IMAGE_URI = "key_payment_image_uri"
        const val KEY_REFERENCE_NUMBER = "key_reference_number"
        const val KEY_CASH_RECEIVED = "key_cash_received"

        const val MAX_NOTES_LENGTH = 500
        const val MAX_REFERENCE_NUMBER_LENGTH = 50
        const val MAX_AMOUNT_LENGTH = 9
    }
}
