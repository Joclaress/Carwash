package com.example.carwash.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.carwash.add.Sale
import com.example.carwash.add.SaleDraft
import com.example.carwash.utils.ImageCompressor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSaleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage
) : SaleRepository {

    private val saleCollection = firebaseFirestore.collection("sales")

    override suspend fun saveSale(draft: SaleDraft): Result<String> {
        val validation = draft.validateAll()
        if (validation != null) {
            return Result.failure(IllegalArgumentException(validation))
        }

        if (!OfflineSaleSyncManager.isInternetAvailable(context)) {
            Log.d("FirebaseSaleRepository", "No internet connection. Saving sale to local offline storage.")
            val offlineId = OfflineSaleSyncManager.saveOfflineSale(context, draft)
            return Result.success(offlineId)
        }

        val currentUser = firebaseAuth.currentUser ?: return Result.failure(IllegalArgumentException("User not authenticated"))
        val selectedPackage = requireNotNull(draft.selectedPackage) { "Package is required" }
        val salesDocument = saleCollection.document()
        val salesId = salesDocument.id

        var vehicleRef: StorageReference? = null
        var paymentRef: StorageReference? = null

        return try {
            val vehicleUri = draft.vehicleImageUri

            val (vehicleUrl, paymentUrl) = coroutineScope {
                val vehicleDeferred = async {
                    if (vehicleUri != null) {
                        val compressedVehicleUri = try {
                            ImageCompressor.compress(context, vehicleUri)
                        } catch (e: Exception) {
                            Log.e("FirebaseSaleRepository", "Vehicle image compression failed: ${e.message}")
                            vehicleUri
                        }
                        val refs = createImageReferences(userId = currentUser.uid, saleId = salesId, folder = "vehicle")
                        vehicleRef = refs.firstOrNull()
                        uploadImage(uri = compressedVehicleUri, candidateReferences = refs)
                    } else {
                        ""
                    }
                }

                val paymentDeferred = async {
                    if (draft.paymentImageUri != null) {
                        val compressedPaymentUri = try {
                            ImageCompressor.compress(context, draft.paymentImageUri)
                        } catch (e: Exception) {
                            Log.e("FirebaseSaleRepository", "Payment image compression failed: ${e.message}")
                            draft.paymentImageUri
                        }
                        val refs = createImageReferences(userId = currentUser.uid, saleId = salesId, folder = "payment")
                        paymentRef = refs.firstOrNull()
                        uploadImage(uri = compressedPaymentUri, candidateReferences = refs)
                    } else {
                        ""
                    }
                }

                Pair(vehicleDeferred.await(), paymentDeferred.await())
            }

            val saleData = hashMapOf(
                "id" to salesId,
                "userId" to currentUser.uid,
                "plateNumber" to draft.plateNumber,
                "assignedTeam" to draft.assignedTeamName,
                "packageId" to selectedPackage.id,
                "packageName" to selectedPackage.name,
                "vehicleSize" to draft.selectedVehicleSizeLabel,
                "customPrice" to if (selectedPackage.customPriceAllowed) draft.amount else 0.0,
                "amount" to draft.amount,
                "notes" to draft.notes,
                "paymentMethod" to draft.paymentMethod.name,
                "referenceNumber" to draft.referenceNumber.trim(),
                "cashReceived" to draft.cashReceivedAmount,
                "changeAmount" to draft.changeAmount,
                "vehicleImageUrl" to vehicleUrl,
                "paymentImageUrl" to paymentUrl,
                "workerPercent" to draft.selectedCommissionRate.workerPercent,
                "ownerPercent" to draft.selectedCommissionRate.ownerPercent,
                "workerCommission" to draft.workerCommission,
                "ownerShare" to draft.ownerShare,
                "CreatedAt" to FieldValue.serverTimestamp()
            )

            salesDocument.set(saleData).await()
            Result.success(salesId)
        } catch (exception: Exception) {
            Log.e("FirebaseSaleRepository", "Error saving sale: ${exception.message}", exception)
            deleteQuietly(vehicleRef)
            deleteQuietly(paymentRef)

            Log.d("FirebaseSaleRepository", "Network issue detected. Saving sale locally to offline queue.")
            val offlineId = OfflineSaleSyncManager.saveOfflineSale(context, draft)
            Result.success(offlineId)
        }
    }

    override suspend fun getSale(saleId: String): Result<Sale> {
        return try {
            val saleDocument = saleCollection.document(saleId).get().await()
            val sale = saleDocument.toObject(Sale::class.java)
            if (sale != null) {
                Result.success(sale)
            } else {
                Result.failure(Exception("Sale not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSale(saleId: String): Result<Unit> {
        return try {
            val doc = try { saleCollection.document(saleId).get().await() } catch (_: Exception) { null }
            val vehicleUrl = doc?.getString("vehicleImageUrl").orEmpty()
            val paymentUrl = doc?.getString("paymentImageUrl").orEmpty()

            saleCollection.document(saleId).delete().await()

            if (vehicleUrl.startsWith("http") || vehicleUrl.startsWith("gs://")) {
                deleteUrlQuietly(vehicleUrl)
            }
            if (paymentUrl.startsWith("http") || paymentUrl.startsWith("gs://")) {
                deleteUrlQuietly(paymentUrl)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSaleRepository", "Delete sale failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateSale(sale: Sale): Result<Unit> {
        return try {
            val saleData = hashMapOf(
                "id" to sale.id,
                "userId" to sale.userId,
                "plateNumber" to sale.plateNumber,
                "assignedTeam" to sale.assignedTeam,
                "packageId" to sale.packageId,
                "packageName" to sale.packageName,
                "vehicleSize" to sale.vehicleSize,
                "customPrice" to sale.customPrice,
                "amount" to sale.amount,
                "notes" to sale.notes,
                "paymentMethod" to sale.paymentMethod,
                "referenceNumber" to sale.referenceNumber,
                "cashReceived" to sale.cashReceived,
                "changeAmount" to sale.changeAmount,
                "vehicleImageUrl" to sale.vehicleImageUrl,
                "paymentImageUrl" to sale.paymentImageUrl,
                "workerPercent" to sale.workerPercent,
                "ownerPercent" to sale.ownerPercent,
                "workerCommission" to sale.workerCommission,
                "ownerShare" to sale.ownerShare,
                "CreatedAt" to sale.CreatedAt
            )
            saleCollection.document(sale.id).set(saleData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSaleRepository", "Update sale failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun observeRecentSale(limit: Long): Flow<List<Sale>> = callbackFlow {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.e("FirebaseSaleRepository", "No authenticated user found when observing sales")
            trySend(emptyList())
            close(IllegalArgumentException("User not authenticated"))
            return@callbackFlow
        }

        Log.d("FirebaseSaleRepository", "Observing recent sales for userId: ${user.uid}")

        val listener = saleCollection
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseSaleRepository", "Observe recent sales failed: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sales = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject(Sale::class.java)
                    } catch (e: Exception) {
                        Log.e("FirebaseSaleRepository", "Failed to parse document ${document.id}: ${e.message}", e)
                        null
                    }
                }.orEmpty()

                val sortedSales = sales
                    .sortedByDescending { it.CreatedAt?.seconds ?: 0L }
                    .take(limit.toInt())

                Log.d("FirebaseSaleRepository", "Fetched and parsed ${sortedSales.size} sales")
                trySend(sortedSales)
            }

        awaitClose { listener.remove() }
    }

    private fun createImageReferences(
        userId: String,
        saleId: String,
        folder: String
    ): List<StorageReference> {
        val imageName = "${UUID.randomUUID()}.jpg"
        val rootRef = firebaseStorage.reference
        val refs = mutableListOf<StorageReference>()

        // 1. Primary candidate paths under default root reference
        refs.add(rootRef.child("users").child(userId).child("sales").child(saleId).child(folder).child(imageName))
        refs.add(rootRef.child("sales").child(saleId).child(folder).child(imageName))

        // 2. Alternative bucket host references in case bucket naming differs between firebasestorage.app vs appspot.com
        try {
            val altBucket = if (rootRef.bucket.contains("firebasestorage.app")) {
                "gs://custoworks-carwash.appspot.com"
            } else {
                "gs://custoworks-carwash.firebasestorage.app"
            }
            val altRef = firebaseStorage.getReferenceFromUrl(altBucket)
            refs.add(altRef.child("users").child(userId).child("sales").child(saleId).child(folder).child(imageName))
            refs.add(altRef.child("sales").child(saleId).child(folder).child(imageName))
        } catch (_: Exception) {
            // Ignore if custom bucket URL resolution isn't supported
        }

        // 3. Simple fallback paths
        refs.add(rootRef.child("users").child(userId).child(folder).child(imageName))
        refs.add(rootRef.child("sales").child(folder).child(imageName))

        return refs
    }

    private suspend fun uploadImage(uri: Uri, candidateReferences: List<StorageReference>): String {
        val bytes = readBytesFromUri(context, uri)
            ?: run {
                Log.w("FirebaseSaleRepository", "Could not read bytes for Uri: $uri. Falling back to local Uri.")
                return uri.toString()
            }

        if (bytes.isEmpty()) {
            Log.w("FirebaseSaleRepository", "Byte array is empty for Uri: $uri. Falling back to local Uri.")
            return uri.toString()
        }

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCacheControl("public, max-age=31536000")
            .build()

        var lastException: Exception? = null

        for (reference in candidateReferences) {
            val maxRetries = 2
            var currentAttempt = 0
            var delayMs = 500L

            while (currentAttempt < maxRetries) {
                currentAttempt++
                try {
                    // Force refresh auth token if signed in to ensure a valid JWT bearer token is present
                    try {
                        firebaseAuth.currentUser?.getIdToken(true)?.await()
                    } catch (tokenErr: Exception) {
                        Log.w("FirebaseSaleRepository", "Auth token refresh notice: ${tokenErr.message}")
                    }

                    Log.d(
                        "FirebaseSaleRepository",
                        "Uploading image (attempt $currentAttempt/$maxRetries) to: ${reference.path} [bucket: ${reference.bucket}]"
                    )

                    reference.putBytes(bytes, metadata).await()

                    val downloadUrl = reference.downloadUrl.await().toString()
                    Log.d("FirebaseSaleRepository", "Upload successful. Download URL: $downloadUrl")
                    return downloadUrl
                } catch (e: Exception) {
                    lastException = e
                    if (isStorageUploadOrPermissionError(e)) {
                        Log.w(
                            "FirebaseSaleRepository",
                            "Storage upload/bucket error on path ${reference.path} [bucket: ${reference.bucket}]: ${e.message}"
                        )
                        break // Break retry loop on this candidate path and try next candidate reference
                    }

                    Log.w(
                        "FirebaseSaleRepository",
                        "Upload attempt $currentAttempt failed for ${reference.path}: ${e.message}"
                    )
                    if (currentAttempt < maxRetries) {
                        delay(delayMs)
                        delayMs *= 2
                    }
                }
            }
        }

        // If upload could not succeed on any Storage candidate reference path (e.g. non-existent bucket,
        // terminated upload session, or permission denied), log warning and fall back to local Uri so sale creation is preserved.
        Log.w(
            "FirebaseSaleRepository",
            "Firebase Storage upload failed across candidate paths (${lastException?.message}). Falling back to local Uri so sale creation is preserved: $uri"
        )
        return uri.toString()
    }

    private fun isStorageUploadOrPermissionError(e: Exception): Boolean {
        if (e is StorageException) {
            when (e.errorCode) {
                StorageException.ERROR_NOT_AUTHORIZED,
                StorageException.ERROR_UNKNOWN,
                StorageException.ERROR_OBJECT_NOT_FOUND,
                StorageException.ERROR_BUCKET_NOT_FOUND,
                StorageException.ERROR_PROJECT_NOT_FOUND,
                StorageException.ERROR_QUOTA_EXCEEDED,
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED,
                StorageException.ERROR_CANCELED -> return true
            }
            if (e.httpResultCode in listOf(400, 401, 402, 403, 404, 409, 412, 500, 503)) {
                return true
            }
        }
        val msg = e.message?.lowercase().orEmpty()
        val causeMsg = e.cause?.message?.lowercase().orEmpty()

        return msg.contains("404") || msg.contains("not found") ||
                msg.contains("permission") || msg.contains("not authorized") ||
                msg.contains("does not have permission") ||
                msg.contains("terminated the upload session") ||
                msg.contains("server state") ||
                msg.contains("server has terminated") ||
                msg.contains("403") || msg.contains("bucket") ||
                causeMsg.contains("404") || causeMsg.contains("not found") ||
                causeMsg.contains("permission") || causeMsg.contains("not authorized") ||
                causeMsg.contains("does not have permission") ||
                causeMsg.contains("terminated the upload session") ||
                causeMsg.contains("server state") ||
                causeMsg.contains("server has terminated") ||
                causeMsg.contains("403") || causeMsg.contains("bucket")
    }

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            if (uri.scheme == "file" && !uri.path.isNullOrEmpty()) {
                val file = File(uri.path!!)
                if (file.exists() && file.length() > 0) {
                    file.readBytes()
                } else {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes().takeIf { it.isNotEmpty() }
                    }
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes().takeIf { it.isNotEmpty() }
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSaleRepository", "Error reading bytes from Uri $uri: ${e.message}", e)
            null
        }
    }

    private suspend fun deleteQuietly(reference: StorageReference?) {
        if (reference == null) return
        try {
            reference.delete().await()
        } catch (_: Exception) {
        }
    }

    private suspend fun deleteUrlQuietly(url: String) {
        if (url.isBlank()) return
        try {
            firebaseStorage.getReferenceFromUrl(url).delete().await()
        } catch (e: Exception) {
            Log.d("FirebaseSaleRepository", "deleteUrlQuietly ignored error for $url: ${e.message}")
        }
    }
}
