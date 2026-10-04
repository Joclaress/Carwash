package com.example.carwash.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import com.example.carwash.add.SaleDraft
import com.example.carwash.model.CommissionRateItem
import com.example.carwash.model.PaymentMethod
import com.example.carwash.model.ServicePackage
import com.example.carwash.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

    fun getPendingSalesCount(context: Context): Int {
        val prefs = getPrefs(context)
        val existingJsonStr = prefs.getString(KEY_OFFLINE_SALES, "[]") ?: "[]"
        return try {
            JSONArray(existingJsonStr).length()
        } catch (_: Exception) {
            0
        }
    }

    private fun copyUriToOfflineCache(context: Context, uri: Uri?, fileNamePrefix: String): String? {
        if (uri == null) return null
        return try {
            val offlineDir = File(context.filesDir, "offline_images").apply { if (!exists()) mkdirs() }
            val cacheFile = File(offlineDir, "${fileNamePrefix}_${UUID.randomUUID()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (cacheFile.exists() && cacheFile.length() > 0) {
                Uri.fromFile(cacheFile).toString()
            } else {
                uri.toString()
            }
        } catch (e: Exception) {
            Log.e("OfflineSaleSyncManager", "Error copying Uri $uri to offline cache: ${e.message}", e)
            uri.toString()
        }
    }

    private fun deleteCachedFile(uriStr: String?) {
        if (uriStr.isNullOrBlank()) return
        try {
            val uri = Uri.parse(uriStr)
            if (uri.scheme == "file" && uri.path != null) {
                val file = File(uri.path!!)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.d("OfflineSaleSyncManager", "Could not delete cached file $uriStr: ${e.message}")
        }
    }

    suspend fun saveOfflineSale(context: Context, draft: SaleDraft): String = withContext(Dispatchers.IO) {
        val pendingId = "offline_${UUID.randomUUID()}"
        val prefs = getPrefs(context)
        val existingJsonStr = prefs.getString(KEY_OFFLINE_SALES, "[]") ?: "[]"
        val jsonArray = JSONArray(existingJsonStr)

        val cachedVehicleUri = copyUriToOfflineCache(context, draft.vehicleImageUri, "vehicle")
        val cachedPaymentUri = copyUriToOfflineCache(context, draft.paymentImageUri, "payment")

        val saleObj = JSONObject().apply {
            put("pendingId", pendingId)
            put("plateNumber", draft.plateNumber)
            put("assignedTeamName", draft.assignedTeamName)
            put("packageId", draft.selectedPackage?.id ?: "")
            put("packageName", draft.selectedPackage?.name ?: "Carwash")
            put("packageDescription", draft.selectedPackage?.description ?: "")
            put("customPriceAllowed", draft.selectedPackage?.customPriceAllowed ?: false)
            put("selectedVehicleSizeLabel", draft.selectedVehicleSizeLabel)
            put("customPrice", draft.customPrice)
            put("amount", draft.amount)
            put("notes", draft.notes)
            put("paymentMethod", draft.paymentMethod.name)
            put("referenceNumber", draft.referenceNumber)
            put("cashReceived", draft.cashReceivedAmount)
            put("commissionRateId", draft.selectedCommissionRate.id)
            put("commissionRateLabel", draft.selectedCommissionRate.displayName)
            put("workerPercent", draft.selectedCommissionRate.workerPercent)
            put("ownerPercent", draft.selectedCommissionRate.ownerPercent)
            put("createdAtTime", System.currentTimeMillis())

            cachedVehicleUri?.let { put("cachedVehicleImageUriStr", it) }
            cachedPaymentUri?.let { put("cachedPaymentImageUriStr", it) }
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
            val packageId = saleObj.optString("packageId", "")
            val packageName = saleObj.optString("packageName", "Carwash")
            val packageDescription = saleObj.optString("packageDescription", "")
            val customPriceAllowed = saleObj.optBoolean("customPriceAllowed", false)
            val amount = saleObj.optDouble("amount", 0.0)

            val matchedPackage = ServicePackage.defaultPackages.find {
                it.id.equals(packageId, ignoreCase = true) || it.name.equals(packageName, ignoreCase = true)
            } ?: ServicePackage(
                id = packageId.ifBlank { "custom" },
                name = packageName,
                description = packageDescription.ifBlank { "Offline Service Package" },
                prices = emptyMap(),
                customPriceAllowed = customPriceAllowed || packageId.equals("others", ignoreCase = true)
            )

            val paymentMethodStr = saleObj.optString("paymentMethod", "CASH")
            val paymentMethod = PaymentMethod.entries.find {
                it.name.equals(paymentMethodStr, ignoreCase = true)
            } ?: PaymentMethod.CASH

            val rateId = saleObj.optString("commissionRateId", "FORTY")
            val rateLabel = saleObj.optString("commissionRateLabel", "40% (Default)")
            val workerPercent = saleObj.optDouble("workerPercent", 0.40)
            val ownerPercent = saleObj.optDouble("ownerPercent", 0.60)
            val selectedCommissionRate = CommissionRateItem(
                id = rateId,
                displayName = rateLabel,
                workerPercent = workerPercent,
                ownerPercent = ownerPercent
            )

            val cachedVehicleUriStr = saleObj.optString("cachedVehicleImageUriStr", "")
            val cachedPaymentUriStr = saleObj.optString("cachedPaymentImageUriStr", "")
            val vehicleUriStr = cachedVehicleUriStr.ifBlank { saleObj.optString("vehicleImageUriStr", "") }
            val paymentUriStr = cachedPaymentUriStr.ifBlank { saleObj.optString("paymentImageUriStr", "") }

            val vehicleImageUri = vehicleUriStr.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            val paymentImageUri = paymentUriStr.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

            try {
                val draft = SaleDraft(
                    vehicleImageUri = vehicleImageUri,
                    plateNumber = plateNumber,
                    assignedTeamName = saleObj.optString("assignedTeamName", ""),
                    selectedPackage = matchedPackage,
                    selectedVehicleSizeLabel = saleObj.optString("selectedVehicleSizeLabel", ""),
                    customPrice = saleObj.optString("customPrice", ""),
                    selectedCommissionRate = selectedCommissionRate,
                    notes = saleObj.optString("notes", ""),
                    paymentMethod = paymentMethod,
                    paymentImageUri = paymentImageUri,
                    referenceNumber = saleObj.optString("referenceNumber", ""),
                    cashReceived = saleObj.optDouble("cashReceived", 0.0)
                )

                val result = saleRepository.saveSale(draft)
                if (result.isSuccess) {
                    Log.d("OfflineSaleSyncManager", "Successfully synced offline sale $pendingId to Firebase!")

                    // Clean up cached image files
                    deleteCachedFile(cachedVehicleUriStr)
                    deleteCachedFile(cachedPaymentUriStr)

                    NotificationHelper.showOfflineSaleSyncedNotification(
                        context = context,
                        plateNumber = plateNumber,
                        packageName = packageName,
                        amount = amount
                    )
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Internet restored! Offline sale for ${plateNumber.ifBlank { "Carwash" }} has been synced.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Log.e("OfflineSaleSyncManager", "Failed syncing sale $pendingId (${result.exceptionOrNull()?.message}), keeping in queue.")
                    remainingArray.put(saleObj)
                }
            } catch (e: Exception) {
                Log.e("OfflineSaleSyncManager", "Error syncing sale $pendingId: ${e.message}", e)
                remainingArray.put(saleObj)
            }
        }

        prefs.edit().putString(KEY_OFFLINE_SALES, remainingArray.toString()).apply()
        isSyncing = false
    }

    fun startAutoSyncOnConnectivity(context: Context, saleRepository: SaleRepository) {
        // Immediately trigger sync if internet is currently available on app launch
        if (isInternetAvailable(context)) {
            CoroutineScope(Dispatchers.IO).launch {
                syncPendingSales(context, saleRepository)
            }
        }

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
