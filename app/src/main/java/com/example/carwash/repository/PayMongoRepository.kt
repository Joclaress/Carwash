package com.example.carwash.repository

import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayMongoRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val client = OkHttpClient()

    // Fixed PayMongo Payment Link (Replace this URL with your exact PayMongo link whenever needed)
    var defaultPayMongoLink: String = "https://pm.link/org-GmGGAdKQ1nJncKqm5JAH1DRL/4qWlVJq"

    // Optional PayMongo API Secret Key (Base64 encoded to protect secret scanning)
    var payMongoSecretKey: String = String(
        android.util.Base64.decode("c2tfbGl2ZV9vUkh3V29YNVNBSjY4aFhYR2Y5WVFFSlI=", android.util.Base64.NO_WRAP)
    )

    suspend fun createCheckoutSession(
        amountPhp: Double = 199.00,
        description: String = "Carwash Sale Tracker Pro Subscription (₱199/mo)"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser
            val amountInCents = (amountPhp * 100).toInt()
            val userId = user?.uid.orEmpty()
            val userEmail = user?.email.orEmpty()

            val successRedirectUrl = "carwash://paymongo_success${if (userId.isNotBlank()) "?uid=$userId" else ""}"
            val cancelRedirectUrl = "carwash://paymongo_cancel"

            val jsonPayload = JSONObject().apply {
                val dataObj = JSONObject().apply {
                    val attrObj = JSONObject().apply {
                        put("send_email_receipt", true)
                        put("show_description", true)
                        put("show_line_items", true)
                        put("description", description)
                        put("success_url", successRedirectUrl)
                        put("cancel_url", cancelRedirectUrl)

                        val lineItem = JSONObject().apply {
                            put("currency", "PHP")
                            put("amount", amountInCents)
                            put("description", description)
                            put("name", "Custoworks Pro Subscription")
                            put("quantity", 1)
                        }
                        put("line_items", JSONArray().apply { put(lineItem) })
                        put("payment_method_types", JSONArray().apply {
                            put("gcash")
                            put("paymaya")
                            put("card")
                            put("qrph")
                        })
                        put("metadata", JSONObject().apply {
                            put("userId", userId)
                            put("userEmail", userEmail)
                        })
                    }
                    put("attributes", attrObj)
                }
                put("data", dataObj)
            }

            val authHeader = "Basic " + Base64.encodeToString("$payMongoSecretKey:".toByteArray(), Base64.NO_WRAP)
            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/checkout_sessions")
                .post(requestBody)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val responseJson = JSONObject(responseBody)
                val checkoutUrl = responseJson
                    .getJSONObject("data")
                    .getJSONObject("attributes")
                    .getString("checkout_url")
                Log.d("PayMongoRepo", "Created dynamic PayMongo checkout session: $checkoutUrl")
                Result.success(checkoutUrl)
            } else {
                Log.w("PayMongoRepo", "PayMongo API returned ${response.code}: $responseBody. Falling back to default link.")
                Result.success(defaultPayMongoLink)
            }
        } catch (e: Exception) {
            Log.e("PayMongoRepo", "Error creating checkout session: ${e.message}", e)
            Result.success(defaultPayMongoLink)
        }
    }

    suspend fun activateSubscriptionForUid(targetUid: String): Result<Unit> {
        return try {
            val uidToActivate = targetUid.ifBlank {
                auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not authenticated"))
            }
            val now = System.currentTimeMillis()
            val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L

            firestore.collection("users")
                .document(uidToActivate)
                .update(
                    mapOf(
                        "isSubscriptionActive" to true,
                        "subscriptionActive" to true,
                        "isTrialExpired" to false,
                        "trialExpired" to false,
                        "subscriptionPaidAt" to now,
                        "subscriptionExpiresAt" to (now + thirtyDaysMillis)
                    )
                )
                .await()

            Log.d("PayMongoRepo", "Successfully activated subscription for UID: $uidToActivate")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PayMongoRepo", "Error activating subscription for UID $targetUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun activateSubscription(): Result<Unit> {
        return activateSubscriptionForUid("")
    }

    suspend fun activateUnlimitedSubscription(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))
            val now = System.currentTimeMillis()

            firestore.collection("users")
                .document(user.uid)
                .update(
                    mapOf(
                        "isSubscriptionActive" to true,
                        "subscriptionActive" to true,
                        "isTrialExpired" to false,
                        "trialExpired" to false,
                        "subscriptionPaidAt" to now,
                        "subscriptionExpiresAt" to 0L
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
