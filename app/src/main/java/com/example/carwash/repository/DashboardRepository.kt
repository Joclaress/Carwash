package com.example.carwash.repository

import com.example.carwash.add.Sale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun getSales(): Result<List<Sale>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.success(emptyList())
            
            val snapshot = firestore
                .collection("sales")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val sales = snapshot.documents.mapNotNull {
                it.toObject(Sale::class.java)
            }
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
