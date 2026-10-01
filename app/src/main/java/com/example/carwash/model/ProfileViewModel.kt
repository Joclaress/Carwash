package com.example.carwash.model

import androidx.lifecycle.ViewModel
import com.example.carwash.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val teams: StateFlow<List<String>> = settingsRepository.teams
    val packages: StateFlow<List<ServicePackage>> = settingsRepository.packages
    val vehicleSizes: StateFlow<List<String>> = settingsRepository.vehicleSizes
    val commissionRates: StateFlow<List<CommissionRateItem>> = settingsRepository.commissionRates

    fun addTeam(name: String) = settingsRepository.addTeam(name)
    fun updateTeam(index: Int, name: String) = settingsRepository.updateTeam(index, name)
    fun deleteTeam(index: Int) = settingsRepository.deleteTeam(index)

    fun addPackage(pkg: ServicePackage) = settingsRepository.addPackage(pkg)
    fun updatePackage(index: Int, pkg: ServicePackage) = settingsRepository.updatePackage(index, pkg)
    fun deletePackage(index: Int) = settingsRepository.deletePackage(index)

    fun addVehicleSize(size: String) = settingsRepository.addVehicleSize(size)
    fun updateVehicleSize(index: Int, size: String) = settingsRepository.updateVehicleSize(index, size)
    fun deleteVehicleSize(index: Int) = settingsRepository.deleteVehicleSize(index)

    fun addCommissionRate(rate: CommissionRateItem) = settingsRepository.addCommissionRate(rate)
    fun updateCommissionRate(index: Int, rate: CommissionRateItem) = settingsRepository.updateCommissionRate(index, rate)
    fun deleteCommissionRate(index: Int) = settingsRepository.deleteCommissionRate(index)
}
