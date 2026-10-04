package com.example.carwash.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

object InAppUpdateManager {

    private const val REQUEST_CODE_FORCE_UPDATE = 8888

    fun checkForAppUpdate(activity: Activity) {
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
}
