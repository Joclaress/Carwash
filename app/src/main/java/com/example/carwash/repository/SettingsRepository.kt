package com.example.carwash.repository

import com.example.carwash.model.CommissionRateItem
import com.example.carwash.model.ServicePackage
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val teams: StateFlow<List<String>>
    val packages: StateFlow<List<ServicePackage>>
    val vehicleSizes: StateFlow<List<String>>
    val commissionRates: StateFlow<List<CommissionRateItem>>

    fun addTeam(name: String)
    fun updateTeam(index: Int, name: String)
    fun deleteTeam(index: Int)

    fun addPackage(pkg: ServicePackage)
    fun updatePackage(index: Int, pkg: ServicePackage)
    fun deletePackage(index: Int)

    fun addVehicleSize(size: String)
    fun updateVehicleSize(index: Int, size: String)
    fun deleteVehicleSize(index: Int)

    fun addCommissionRate(rate: CommissionRateItem)
    fun updateCommissionRate(index: Int, rate: CommissionRateItem)
    fun deleteCommissionRate(index: Int)
}
