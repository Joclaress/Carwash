package com.example.carwash.repository

import com.example.carwash.model.CommissionRateItem
import com.example.carwash.model.ServicePackage
import com.example.carwash.model.VehicleSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemorySettingsRepository @Inject constructor() : SettingsRepository {

    private val _teams = MutableStateFlow(listOf("Team A", "Team B", "Team C"))
    override val teams: StateFlow<List<String>> = _teams.asStateFlow()

    private val _packages = MutableStateFlow(ServicePackage.defaultPackages)
    override val packages: StateFlow<List<ServicePackage>> = _packages.asStateFlow()

    private val _vehicleSizes = MutableStateFlow(
        VehicleSize.entries.map { it.name }
    )
    override val vehicleSizes: StateFlow<List<String>> = _vehicleSizes.asStateFlow()

    private val _commissionRates = MutableStateFlow(
        listOf(
            CommissionRateItem("FORTY", "40% (Default)", 0.40, 0.60),
            CommissionRateItem("THIRTY", "30%", 0.30, 0.70),
            CommissionRateItem("FULL_OWNER", "100% Owner", 0.00, 1.00)
        )
    )
    override val commissionRates: StateFlow<List<CommissionRateItem>> = _commissionRates.asStateFlow()

    override fun addTeam(name: String) {
        _teams.update { it + name }
    }

    override fun updateTeam(index: Int, name: String) {
        _teams.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = name
            }
            mutable
        }
    }

    override fun deleteTeam(index: Int) {
        _teams.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            mutable
        }
    }

    override fun addPackage(pkg: ServicePackage) {
        _packages.update { it + pkg }
    }

    override fun updatePackage(index: Int, pkg: ServicePackage) {
        _packages.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = pkg
            }
            mutable
        }
    }

    override fun deletePackage(index: Int) {
        _packages.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            mutable
        }
    }

    override fun addVehicleSize(size: String) {
        _vehicleSizes.update { it + size }
    }

    override fun updateVehicleSize(index: Int, size: String) {
        _vehicleSizes.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = size
            }
            mutable
        }
    }

    override fun deleteVehicleSize(index: Int) {
        _vehicleSizes.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            mutable
        }
    }

    override fun addCommissionRate(rate: CommissionRateItem) {
        _commissionRates.update { it + rate }
    }

    override fun updateCommissionRate(index: Int, rate: CommissionRateItem) {
        _commissionRates.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = rate
            }
            mutable
        }
    }

    override fun deleteCommissionRate(index: Int) {
        _commissionRates.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            mutable
        }
    }
}
