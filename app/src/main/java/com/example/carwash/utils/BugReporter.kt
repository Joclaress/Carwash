package com.example.carwash.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast

object BugReporter {

    const val SUPPORT_EMAIL = "custoworks1@gmail.com"

    fun sendBugReport(
        context: Context,
        userDescription: String = "",
        screenshotUri: Uri? = null
    ) {
        val crashLog = readCrashLog(context)

        val deviceInfo = """
            --- APP & DEVICE INFO ---
            App: Custoworks Carwash
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android OS: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Date: ${java.util.Date()}
            
            --- USER ISSUE DESCRIPTION ---
            ${userDescription.ifBlank { "(User did not enter additional details)" }}
            
            --- SYSTEM & CRASH LOGS ---
            ${crashLog.ifBlank { "No recent crash logs recorded." }}
        """.trimIndent()

        val subject = "Bug Report - Custoworks App (${Build.MODEL})"

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, deviceInfo)

            screenshotUri?.let { uri ->
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        try {
            val chooser = Intent.createChooser(emailIntent, "Send Bug Report via Email")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("BugReporter", "Failed to launch email intent: ${e.message}")
            Toast.makeText(context, "Contact support at $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
        }
    }

    fun saveCrashLog(context: Context, throwable: Throwable) {
        try {
            val logFile = java.io.File(context.cacheDir, "crash_log.txt")
            val stackTrace = Log.getStackTraceString(throwable)
            logFile.writeText("Crash Date: ${java.util.Date()}\n\n$stackTrace")
        } catch (e: Exception) {
            Log.e("BugReporter", "Failed to save crash log: ${e.message}")
        }
    }

    private fun readCrashLog(context: Context): String {
        return try {
            val logFile = java.io.File(context.cacheDir, "crash_log.txt")
            if (logFile.exists()) logFile.readText() else ""
        } catch (e: Exception) {
            ""
        }
    }
}
