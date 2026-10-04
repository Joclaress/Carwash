package com.example.carwash.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.carwash.add.SaleDraft
import com.example.carwash.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object OfflineSaleSyncManager {

    private const val PREFS_NAME = "carwash_offline_sales_prefs"
    private const val KEY_OFFLINE_SALES = "pending_sales_list"
    private var isSyncing = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun saveOfflineSale(context: Context, draft: SaleDraft): String = withContext(Dispatchers.IO) {
        val pendingId = "offline_${UUID.randomUUID()}"
        val prefs = getPrefs(context)
        val existingJsonStr = prefs.getString(KEY_OFFLINE_SALES, "[]") ?: "[]"
        val jsonArray = JSONArray(existingJsonStr)

        val saleObj = JSONObject().apply {
            put("pendingId", pendingId)
            put("plateNumber", draft.plateNumber)
            put("assignedTeamName", draft.assignedTeamName)
            put("packageName", draft.selectedPackage?.name ?: "Carwash")
            put("packageId", draft.selectedPackage?.id ?: "")
            put("selectedVehicleSizeLabel", draft.selectedVehicleSizeLabel)
            put("customPrice", draft.customPrice)
            put("amount", draft.amount)
            put("notes", draft.notes)
            put("paymentMethod", draft.paymentMethod.name)
            put("referenceNumber", draft.referenceNumber)
            put("cashReceived", draft.cashReceivedAmount)
            put("workerPercent", draft.selectedCommissionRate.workerPercent)
            put("ownerPercent", draft.selectedCommissionRate.ownerPercent)
            put("createdAtTime", System.currentTimeMillis())

            draft.vehicleImageUri?.let { put("vehicleImageUriStr", it.toString()) }
            draft.paymentImageUri?.let { put("paymentImageUriStr", it.toString()) }
        }

        jsonArray.put(saleObj)
        prefs.edit().putString(KEY_OFFLINE_SALES, jsonArray.toString()).apply()

        Log.d("OfflineSaleSyncManager", "Saved offline sale $pendingId locally. Total pending: ${jsonArray.length()}")
        pendingId
    }

    suspend fun syncPendingSales(context: Context, saleRepository: SaleRepository) = withContext(Dispatchers.IO) {
        if (isSyncing) return@withContext
        if (!isInternetAvailable(context)) return@withContext

        val prefs = getPrefs(context)
        val existingJsonStr = prefs.getString(KEY_OFFLINE_SALES, "[]") ?: "[]"
        val jsonArray = JSONArray(existingJsonStr)
        if (jsonArray.length() == 0) return@withContext

        isSyncing = true
        Log.d("OfflineSaleSyncManager", "Starting sync of ${jsonArray.length()} offline sales...")

        val remainingArray = JSONArray()

        for (i in 0 until jsonArray.length()) {
            val saleObj = jsonArray.getJSONObject(i)
            val pendingId = saleObj.optString("pendingId", "")
            val plateNumber = saleObj.optString("plateNumber", "")
            val packageName = saleObj.optString("packageName", "")
            val amount = saleObj.optDouble("amount", 0.0)
            val paymentMethodStr = saleObj.optString("paymentMethod", "CASH")

            try {
                val draft = SaleDraft(
                    plateNumber = plateNumber,
                    assignedTeamName = saleObj.optString("assignedTeamName", ""),
                    selectedVehicleSizeLabel = saleObj.optString("selectedVehicleSizeLabel", ""),
                    customPrice = saleObj.optString("customPrice", ""),
                    notes = saleObj.optString("notes", ""),
                    referenceNumber = saleObj.optString("referenceNumber", ""),
                    cashReceived = saleObj.optDouble("cashReceived", 0.0),
                    vehicleImageUri = saleObj.optString("vehicleImageUriStr", "").takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) },
                    paymentImageUri = saleObj.optString("paymentImageUriStr", "").takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) }
                )

                val result = saleRepository.saveSale(draft)
                if (result.isSuccess) {
                    Log.d("OfflineSaleSyncManager", "Successfully synced offline sale $pendingId to Firebase!")
                    NotificationHelper.showSaleAddedNotification(
                        context = context,
                        plateNumber = "$plateNumber (Synced)",
                        packageName = packageName,
                        amount = amount,
                        paymentMethod = paymentMethodStr
                    )
                } else {
                    Log.e("OfflineSaleSyncManager", "Failed syncing sale $pendingId, keeping in queue.")
                    remainingArray.put(saleObj)
                }
            } catch (e: Exception) {
                Log.e("OfflineSaleSyncManager", "Error syncing sale $pendingId: ${e.message}")
                remainingArray.put(saleObj)
            }
        }

        prefs.edit().putString(KEY_OFFLINE_SALES, remainingArray.toString()).apply()
        isSyncing = false
    }

    fun startAutoSyncOnConnectivity(context: Context, saleRepository: SaleRepository) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.d("OfflineSaleSyncManager", "Internet connection restored! Triggering auto-sync...")
                    CoroutineScope(Dispatchers.IO).launch {
                        syncPendingSales(context, saleRepository)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("OfflineSaleSyncManager", "Error registering network callback: ${e.message}")
        }
    }
}
