package com.example.carwash.repository

import android.util.Log
import com.example.carwash.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    fun currentUser() = auth.currentUser

    suspend fun login(
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(
                email,
                password
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        email: String,
        password: String,
        name: String,
        adminPassword: String = "admin123"
    ): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(
                email,
                password
            ).await()
            val uid = result.user!!.uid

            val now = System.currentTimeMillis()
            val user = User(
                uid = uid,
                name = name,
                email = email,
                adminPassword = adminPassword.ifBlank { "admin123" },
                createdAs = now,
                trialStartDate = now,
                isSubscriptionActive = false
            )
            firestore
                .collection("users")
                .document(uid)
                .set(user)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAdminPassword(newAdminPassword: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            firestore
                .collection("users")
                .document(uid)
                .update("adminPassword", newAdminPassword)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore
            .collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AuthRepository", "Error observing user: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    trySend(user)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun getUser(): Result<User?> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(null)
            val document = firestore
                .collection("users")
                .document(uid)
                .get()
                .await()
            Result.success(document.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
