package com.example.carwash.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carwash.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.firestore.FirebaseFirestore

data class AppUpdateInfoData(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val isForceUpdate: Boolean = false,
    val updateNotes: String = ""
)

object InAppUpdateManager {

    private const val REQUEST_CODE_FORCE_UPDATE = 8888

    fun checkFirestoreUpdate(
        onUpdateAvailable: (latestVersionCode: Int, latestVersionName: String, isForce: Boolean, updateNotes: String) -> Unit
    ) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("app_config")
                .document("version")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                    val latestCode = snapshot.getLong("latestVersionCode")?.toInt() ?: 0
                    val latestName = snapshot.getString("latestVersionName") ?: ""
                    val isForce = snapshot.getBoolean("forceUpdate") ?: false
                    val notes = snapshot.getString("updateNotes") ?: ""

                    val currentCode = BuildConfig.VERSION_CODE

                    if (latestCode > currentCode) {
                        Log.d("InAppUpdateManager", "New version detected in Firestore: v$latestName ($latestCode) > current ($currentCode)")
                        onUpdateAvailable(latestCode, latestName, isForce, notes)
                    }
                }
        } catch (e: Exception) {
            Log.e("InAppUpdateManager", "Error checking Firestore version config: ${e.message}", e)
        }
    }

    fun checkForAppUpdate(activity: Activity) {
        if (BuildConfig.DEBUG) {
            Log.d("InAppUpdateManager", "Skipping Play Store in-app update check in debug build.")
            return
        }

        try {
            val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    Log.d("InAppUpdateManager", "Immediate force update available on Google Play!")
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            REQUEST_CODE_FORCE_UPDATE
                        )
                    } catch (e: Exception) {
                        Log.e("InAppUpdateManager", "Error starting update flow: ${e.message}", e)
                    }
                } else {
                    Log.d("InAppUpdateManager", "App is up to date.")
                }
            }.addOnFailureListener { e ->
                Log.e("InAppUpdateManager", "Failed checking for update: ${e.message}")
            }
        } catch (e: SecurityException) {
            Log.e("InAppUpdateManager", "SecurityException during in-app update check (GMS broker): ${e.message}", e)
        } catch (e: Exception) {
            Log.e("InAppUpdateManager", "Error checking in-app update: ${e.message}", e)
        }
    }

    fun openPlayStorePage(context: Context) {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    @Composable
    fun AppUpdateDialog(
        latestVersionName: String,
        updateNotes: String,
        isForce: Boolean,
        onDismiss: () -> Unit,
        onUpdateClick: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { if (!isForce) onDismiss() },
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "New Update Available! (v$latestVersionName)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A new version of Carwash Sale Tracker is available. Please update to enjoy the latest features and fixes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (updateNotes.isNotBlank()) {
                        Text(
                            text = "What's New:\n$updateNotes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onUpdateClick,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Update Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isForce) {
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }
                }
            }
        )
    }
}
