package com.example.carwash.repository

import android.util.Log
import com.example.carwash.model.CommissionRateItem
import com.example.carwash.model.ServicePackage
import com.example.carwash.model.VehicleSize
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSettingsRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SettingsRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    private var listenerRegistration: ListenerRegistration? = null
    private var activeUserId: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                if (activeUserId != user.uid) {
                    activeUserId = user.uid
                    listenToUserSettings(user.uid)
                }
            } else {
                activeUserId = null
                listenerRegistration?.remove()
                listenerRegistration = null
                resetToDefaults()
            }
        }
    }

    private fun listenToUserSettings(userId: String) {
        listenerRegistration?.remove()

        val docRef = firestore
            .collection("users")
            .document(userId)
            .collection("settings")
            .document("config")

        listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseSettingsRepo", "Error listening to settings: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val teamsList = (snapshot.get("teams") as? List<*>)?.mapNotNull { it?.toString() }
                if (!teamsList.isNullOrEmpty()) {
                    _teams.value = teamsList
                }

                val sizesList = (snapshot.get("vehicleSizes") as? List<*>)?.mapNotNull { it?.toString() }
                if (!sizesList.isNullOrEmpty()) {
                    _vehicleSizes.value = sizesList
                }

                val pkgsRaw = snapshot.get("packages") as? List<Map<String, Any>>
                if (pkgsRaw != null) {
                    val parsedPkgs = pkgsRaw.mapNotNull { map ->
                        try {
                            val rawPrices = map["prices"] as? Map<String, Any> ?: emptyMap()
                            val parsedPrices = mutableMapOf<VehicleSize, Double>()
                            rawPrices.forEach { (k, v) ->
                                val sizeEnum = VehicleSize.entries.find { it.name.equals(k, ignoreCase = true) }
                                val priceVal = (v as? Number)?.toDouble()
                                if (sizeEnum != null && priceVal != null) {
                                    parsedPrices[sizeEnum] = priceVal
                                }
                            }
                            ServicePackage(
                                id = map["id"] as? String ?: "",
                                name = map["name"] as? String ?: "",
                                description = map["description"] as? String ?: "",
                                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                                prices = parsedPrices,
                                customPriceAllowed = map["customPriceAllowed"] as? Boolean ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (parsedPkgs.isNotEmpty()) {
                        _packages.value = parsedPkgs
                    }
                }

                val commsRaw = snapshot.get("commissionRates") as? List<Map<String, Any>>
                if (commsRaw != null) {
                    val parsedComms = commsRaw.mapNotNull { map ->
                        try {
                            CommissionRateItem(
                                id = map["id"] as? String ?: "",
                                displayName = map["displayName"] as? String ?: "",
                                workerPercent = (map["workerPercent"] as? Number)?.toDouble() ?: 0.40,
                                ownerPercent = (map["ownerPercent"] as? Number)?.toDouble() ?: 0.60
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (parsedComms.isNotEmpty()) {
                        _commissionRates.value = parsedComms
                    }
                }
            } else {
                saveSettingsToFirestore(userId)
            }
        }
    }

    private fun resetToDefaults() {
        _teams.value = listOf("Team A", "Team B", "Team C")
        _packages.value = ServicePackage.defaultPackages
        _vehicleSizes.value = VehicleSize.entries.map { it.name }
        _commissionRates.value = listOf(
            CommissionRateItem("FORTY", "40% (Default)", 0.40, 0.60),
            CommissionRateItem("THIRTY", "30%", 0.30, 0.70),
            CommissionRateItem("FULL_OWNER", "100% Owner", 0.00, 1.00)
        )
    }

    private fun saveSettingsToFirestore(userId: String? = auth.currentUser?.uid) {
        if (userId.isNullOrBlank()) return
        repositoryScope.launch {
            try {
                val pkgsFormatted = _packages.value.map { pkg ->
                    mapOf(
                        "id" to pkg.id,
                        "name" to pkg.name,
                        "description" to pkg.description,
                        "price" to pkg.price,
                        "prices" to pkg.prices.mapKeys { it.key.name },
                        "customPriceAllowed" to pkg.customPriceAllowed
                    )
                }
                val commsFormatted = _commissionRates.value.map { comm ->
                    mapOf(
                        "id" to comm.id,
                        "displayName" to comm.displayName,
                        "workerPercent" to comm.workerPercent,
                        "ownerPercent" to comm.ownerPercent
                    )
                }
                val data = hashMapOf(
                    "teams" to _teams.value,
                    "packages" to pkgsFormatted,
                    "vehicleSizes" to _vehicleSizes.value,
                    "commissionRates" to commsFormatted
                )
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("settings")
                    .document("config")
                    .set(data)
            } catch (e: Exception) {
                Log.e("FirebaseSettingsRepo", "Failed to save settings: ${e.message}")
            }
        }
    }

    override fun addTeam(name: String) {
        _teams.update { it + name }
        saveSettingsToFirestore()
    }

    override fun updateTeam(index: Int, name: String) {
        _teams.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = name
            }
            mutable
        }
        saveSettingsToFirestore()
    }

    override fun deleteTeam(index: Int) {
        _teams.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            mutable
        }
        saveSettingsToFirestore()
    }

    override fun addPackage(pkg: ServicePackage) {
        _packages.update { it + pkg }
        saveSettingsToFirestore()
    }

    override fun updatePackage(index: Int, pkg: ServicePackage) {
        _packages.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = pkg
            }
            mutable
        }
        saveSettingsToFirestore()
    }

    override fun deletePackage(index: Int) {
        _packages.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            mutable
        }
        saveSettingsToFirestore()
    }

    override fun addVehicleSize(size: String) {
        _vehicleSizes.update { it + size }
        saveSettingsToFirestore()
    }

    override fun updateVehicleSize(index: Int, size: String) {
        _vehicleSizes.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = size
            }
            mutable
        }
        saveSettingsToFirestore()
    }

    override fun deleteVehicleSize(index: Int) {
        _vehicleSizes.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            mutable
        }
        saveSettingsToFirestore()
    }

    override fun addCommissionRate(rate: CommissionRateItem) {
        _commissionRates.update { it + rate }
        saveSettingsToFirestore()
    }

    override fun updateCommissionRate(index: Int, rate: CommissionRateItem) {
        _commissionRates.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = rate
            }
            mutable
        }
        saveSettingsToFirestore()
    }

    override fun deleteCommissionRate(index: Int) {
        _commissionRates.update { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            mutable
        }
        saveSettingsToFirestore()
    }
}
