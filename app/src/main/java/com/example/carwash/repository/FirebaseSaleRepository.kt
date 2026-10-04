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
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
                        val ref = createImageReference(userId = currentUser.uid, saleId = salesId, folder = "vehicle")
                        vehicleRef = ref
                        uploadImage(uri = compressedVehicleUri, reference = ref)
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
                        val ref = createImageReference(userId = currentUser.uid, saleId = salesId, folder = "payment")
                        paymentRef = ref
                        uploadImage(uri = compressedPaymentUri, reference = ref)
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
            if (exception is StorageException) {
                Log.e("FirebaseSaleRepository", "Storage Error Code: ${exception.errorCode}")
                Log.e("FirebaseSaleRepository", "Storage Error Message: ${exception.message}")
            }
            deleteQuietly(vehicleRef)
            deleteQuietly(paymentRef)
            Result.failure(exception)
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
            saleCollection.document(saleId).delete().await()
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

    private fun createImageReference(
        userId: String,
        saleId: String,
        folder: String
    ): StorageReference {
        val imageName = "${UUID.randomUUID()}.jpg"
        return firebaseStorage.reference
            .child("users")
            .child(userId)
            .child("sales")
            .child(saleId)
            .child(folder)
            .child(imageName)
    }

    private suspend fun uploadImage(uri: Uri, reference: StorageReference): String {
        return try {
            Log.d("FirebaseSaleRepository", "Uploading image to: ${reference.path}")
            reference.putFile(uri).await()
            val downloadUrl = reference.downloadUrl.await().toString()
            Log.d("FirebaseSaleRepository", "Upload successful. Download URL: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e("FirebaseSaleRepository", "Failed to upload image to ${reference.path}: ${e.message}")
            ""
        }
    }

    private suspend fun deleteQuietly(reference: StorageReference?) {
        try {
            reference?.delete()?.await()
        } catch (_: Exception) {
        }
    }
}
