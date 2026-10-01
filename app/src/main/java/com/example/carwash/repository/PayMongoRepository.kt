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

    // Optional PayMongo API Secret Key
    var payMongoSecretKey: String = "sk_test_a1b2c3d4e5f6g7h8i9j0"

    suspend fun createCheckoutSession(
        amountPhp: Double = 199.00,
        description: String = "Carwash Sale Tracker Pro Subscription (₱199/mo)"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (defaultPayMongoLink.isNotBlank()) {
                return@withContext Result.success(defaultPayMongoLink)
            }

            val amountInCents = (amountPhp * 100).toInt()
            val jsonPayload = JSONObject().apply {
                val dataObj = JSONObject().apply {
                    val attrObj = JSONObject().apply {
                        put("send_email_receipt", true)
                        put("show_description", true)
                        put("show_line_items", true)
                        put("description", description)

                        val lineItem = JSONObject().apply {
                            put("currency", "PHP")
                            put("amount", amountInCents)
                            put("description", description)
                            put("name", "Custoworks Subscription")
                            put("quantity", 1)
                        }
                        put("line_items", JSONArray().apply { put(lineItem) })
                        put("payment_method_types", JSONArray().apply {
                            put("gcash")
                            put("paymaya")
                            put("card")
                            put("qrph")
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
                Result.success(checkoutUrl)
            } else {
                Result.success(defaultPayMongoLink)
            }
        } catch (e: Exception) {
            Log.e("PayMongoRepo", "Error creating checkout session: ${e.message}", e)
            Result.success(defaultPayMongoLink)
        }
    }

    suspend fun activateSubscription(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))
            val now = System.currentTimeMillis()
            val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L

            firestore.collection("users")
                .document(user.uid)
                .update(
                    mapOf(
                        "isSubscriptionActive" to true,
                        "subscriptionActive" to true,
                        "subscriptionPaidAt" to now,
                        "subscriptionExpiresAt" to (now + thirtyDaysMillis)
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
