package com.example.carwash.model

import com.google.firebase.firestore.PropertyName

data class User (
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String = "",
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("adminPassword") @set:PropertyName("adminPassword") var adminPassword: String = "admin123",
    @get:PropertyName("createdAs") @set:PropertyName("createdAs") var createdAs: Long = 0L,
    @get:PropertyName("trialStartDate") @set:PropertyName("trialStartDate") var trialStartDate: Long = 0L,
    @get:PropertyName("isSubscriptionActive") @set:PropertyName("isSubscriptionActive") @JvmField var isSubscriptionActive: Boolean = false,
    @get:PropertyName("subscriptionActive") @set:PropertyName("subscriptionActive") @JvmField var subscriptionActive: Boolean = false,
    @get:PropertyName("isTrialExpired") @set:PropertyName("isTrialExpired") @JvmField var isTrialExpired: Boolean = false,
    @get:PropertyName("trialExpired") @set:PropertyName("trialExpired") @JvmField var trialExpired: Boolean = false,
    @get:PropertyName("subscriptionPaidAt") @set:PropertyName("subscriptionPaidAt") var subscriptionPaidAt: Long = 0L,
    @get:PropertyName("subscriptionExpiresAt") @set:PropertyName("subscriptionExpiresAt") var subscriptionExpiresAt: Long = 0L
) {
    val effectiveAdminPassword: String
        get() = adminPassword.ifBlank { "admin123" }

    val effectiveSubscriptionActive: Boolean
        get() = isSubscriptionActive || subscriptionActive

    val effectiveTrialExpired: Boolean
        get() = isTrialExpired || trialExpired

    val isSubscriptionValid: Boolean
        get() {
            if (!effectiveSubscriptionActive) return false
            if (subscriptionExpiresAt == 0L) return true
            return System.currentTimeMillis() <= subscriptionExpiresAt
        }

    val isSubscriptionExpired: Boolean
        get() {
            if (!effectiveSubscriptionActive) return false
            if (subscriptionExpiresAt == 0L) return false
            return System.currentTimeMillis() > subscriptionExpiresAt
        }

    val remainingSubscriptionDays: Int
        get() {
            if (!isSubscriptionValid || subscriptionExpiresAt == 0L) return 0
            val remainingMillis = subscriptionExpiresAt - System.currentTimeMillis()
            val days = (remainingMillis / (1000 * 60 * 60 * 24)).toInt()
            return (days + 1).coerceAtLeast(1)
        }

    val trialDurationMillis: Long
        get() = 7 * 24 * 60 * 60 * 1000L

    val trialExpiresAt: Long
        get() {
            val start = when {
                trialStartDate > 0L -> trialStartDate
                createdAs > 0L -> createdAs
                else -> 0L
            }
            if (start == 0L) return 0L
            return start + trialDurationMillis
        }

    val computedTrialExpired: Boolean
        get() {
            if (isSubscriptionValid) return false
            if (effectiveTrialExpired || isSubscriptionExpired) return true
            if (trialExpiresAt == 0L) return false
            return System.currentTimeMillis() > trialExpiresAt
        }

    val isTrialActive: Boolean
        get() {
            if (isSubscriptionValid) return false
            return !computedTrialExpired
        }

    val remainingTrialDays: Int
        get() {
            if (!isTrialActive) return 0
            if (trialExpiresAt == 0L) return 7
            val remainingMillis = trialExpiresAt - System.currentTimeMillis()
            if (remainingMillis <= 0L) return 0
            val days = (remainingMillis / (1000 * 60 * 60 * 24)).toInt()
            return (days + 1).coerceIn(1, 7)
        }
}
