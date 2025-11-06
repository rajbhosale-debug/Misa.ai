package com.misa.ai.installation.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import com.misa.ai.R
import com.misa.ai.installation.InstallationGuideActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.URL

/**
 * Background service for coordinated installation
 * Handles APK downloads, installation workflows, and pairing with desktop coordinators
 */
class AutoInstallerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "AutoInstallerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "misa_installation_channel"

        // Actions
        const val ACTION_DOWNLOAD_APK = "com.misa.ai.DOWNLOAD_APK"
        const val ACTION_INSTALL_APK = "com.misa.ai.INSTALL_APK"
        const val ACTION_START_PAIRING = "com.misa.ai.START_PAIRING"
        const val ACTION_REPORT_STATUS = "com.misa.ai.REPORT_STATUS"

        // Extras
        const val EXTRA_APK_URL = "apk_url"
        const val EXTRA_COORDINATOR_URL = "coordinator_url"
        const val EXTRA_PAIRING_TOKEN = "pairing_token"
        const val EXTRA_INSTALLATION_ID = "installation_id"
        const val EXTRA_DEVICE_ID = "device_id"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("MISA.AI Installation Service", "Ready to coordinate installation"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD_APK -> {
                val apkUrl = intent.getStringExtra(EXTRA_APK_URL)
                val coordinatorUrl = intent.getStringExtra(EXTRA_COORDINATOR_URL)
                val pairingToken = intent.getStringExtra(EXTRA_PAIRING_TOKEN)

                if (apkUrl != null) {
                    downloadApk(apkUrl, coordinatorUrl, pairingToken)
                }
            }
            ACTION_START_PAIRING -> {
                val coordinatorUrl = intent.getStringExtra(EXTRA_COORDINATOR_URL)
                val pairingToken = intent.getStringExtra(EXTRA_PAIRING_TOKEN)

                if (coordinatorUrl != null) {
                    startPairing(coordinatorUrl, pairingToken)
                }
            }
            ACTION_REPORT_STATUS -> {
                // Handle status reporting from other components
                handleStatusReport(intent)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MISA.AI Installation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for MISA.AI installation process"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_misa_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotificationProgress(title: String, content: String, progress: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_misa_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(100, progress, false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun downloadApk(apkUrl: String, coordinatorUrl: String?, pairingToken: String?) {
        serviceScope.launch {
            try {
                Log.i(TAG, "Starting APK download: $apkUrl")
                updateNotification("Downloading MISA.AI", "Preparing download...")

                // Create download directory
                val downloadDir = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "misa")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val apkFile = File(downloadDir, "misa-latest.apk")
                val tempFile = File(downloadDir, "misa-latest.apk.tmp")

                // Download with progress tracking
                val url = URL(apkUrl)
                val connection = url.openConnection()
                val fileSize = connection.contentLength

                url.openStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            // Update progress
                            if (fileSize > 0) {
                                val progress = ((totalBytesRead * 100) / fileSize).toInt()
                                updateNotificationProgress(
                                    "Downloading MISA.AI",
                                    "${totalBytesRead / (1024 * 1024)}MB / ${fileSize / (1024 * 1024)}MB",
                                    progress
                                )
                            }
                        }
                    }
                }

                // Move temp file to final location
                tempFile.renameTo(apkFile)

                Log.i(TAG, "APK download completed: ${apkFile.absolutePath}")
                updateNotification("Download Complete", "APK downloaded successfully")

                // Report download completion to coordinator
                coordinatorUrl?.let { reportStatus(it, "download_completed", mapOf("apk_size" to apkFile.length())) }

                // Start installation process
                delay(1000) // Brief delay before proceeding
                startInstallationProcess(apkFile.absolutePath, coordinatorUrl, pairingToken)

            } catch (e: Exception) {
                Log.e(TAG, "APK download failed", e)
                updateNotification("Download Failed", "Error: ${e.message}")

                // Report download failure to coordinator
                coordinatorUrl?.let { reportStatus(it, "download_failed", mapOf("error" to e.message)) }
            }
        }
    }

    private fun startInstallationProcess(apkPath: String, coordinatorUrl: String?, pairingToken: String?) {
        // This would trigger the installation UI
        // For now, we'll just report that installation is ready to begin

        serviceScope.launch {
            try {
                Log.i(TAG, "Starting installation process")
                updateNotification("Installation Ready", "Tap to begin installation")

                // Launch InstallationGuideActivity if not already running
                val guideIntent = InstallationGuideActivity.createIntent(
                    this@AutoInstallerService,
                    coordinatorUrl,
                    "file://$apkPath",
                    pairingToken
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                startActivity(guideIntent)

                // Report installation start to coordinator
                coordinatorUrl?.let { reportStatus(it, "installation_started", mapOf("apk_path" to apkPath)) }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start installation process", e)
                coordinatorUrl?.let { reportStatus(it, "installation_failed", mapOf("error" to e.message)) }
            }
        }
    }

    private fun startPairing(coordinatorUrl: String, pairingToken: String?) {
        serviceScope.launch {
            try {
                Log.i(TAG, "Starting pairing process")
                updateNotification("Pairing", "Establishing connection with coordinator")

                val deviceId = getDeviceId()

                val pairingData = mapOf(
                    "device_id" to deviceId,
                    "device_name" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                    "device_type" to "android",
                    "pairing_token" to pairingToken,
                    "timestamp" to System.currentTimeMillis()
                )

                // Send pairing request to coordinator
                val pairingResponse = sendPairingRequest(coordinatorUrl, pairingData)

                if (pairingResponse != null) {
                    Log.i(TAG, "Pairing successful")
                    updateNotification("Pairing Complete", "Successfully paired with coordinator")

                    // Save pairing information
                    savePairingInfo(coordinatorUrl, pairingToken, deviceId)

                    // Report successful pairing
                    reportStatus(coordinatorUrl, "pairing_completed", mapOf(
                        "device_id" to deviceId,
                        "pairing_successful" to true
                    ))

                } else {
                    Log.w(TAG, "Pairing failed")
                    updateNotification("Pairing Failed", "Unable to establish connection")

                    reportStatus(coordinatorUrl, "pairing_failed", mapOf(
                        "error" to "No response from coordinator"
                    ))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Pairing process failed", e)
                updateNotification("Pairing Error", "Error: ${e.message}")

                reportStatus(coordinatorUrl, "pairing_failed", mapOf("error" to e.message))
            }
        }
    }

    private suspend fun sendPairingRequest(coordinatorUrl: String, data: Map<String, Any?>): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$coordinatorUrl/misa/pair")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "MISA-Android/1.0")
                connection.doOutput = true

                // Send pairing data
                val jsonData = JSONObject(data)
                connection.outputStream.write(jsonData.toString().toByteArray())

                // Get response
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    JSONObject(response)
                } else {
                    Log.w(TAG, "Pairing request failed with code: $responseCode")
                    null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send pairing request", e)
                null
            }
        }
    }

    private fun reportStatus(coordinatorUrl: String, status: String, data: Map<String, Any?> = emptyMap()) {
        serviceScope.launch {
            try {
                val reportData = data.toMutableMap().apply {
                    put("status", status)
                    put("device_id", getDeviceId())
                    put("timestamp", System.currentTimeMillis())
                }

                val url = URL("$coordinatorUrl/misa/status")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "MISA-Android/1.0")
                connection.doOutput = true

                val jsonData = JSONObject(reportData)
                connection.outputStream.write(jsonData.toString().toByteArray())

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    Log.d(TAG, "Status report sent successfully: $status")
                } else {
                    Log.w(TAG, "Failed to send status report: $responseCode")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to report status", e)
            }
        }
    }

    private fun handleStatusReport(intent: Intent) {
        val status = intent.getStringExtra("status") ?: return
        val data = intent.getBundleExtra("data")?.let { bundle ->
            bundle.keySet().associateWith { key -> bundle.get(key) }
        } ?: emptyMap()

        // Handle different status reports from other components
        when (status) {
            "installation_progress" -> {
                val progress = data["progress"] as? Int ?: 0
                updateNotificationProgress("Installing MISA.AI", "Installation in progress", progress)
            }
            "pairing_progress" -> {
                val step = data["step"] as? String ?: "Unknown"
                updateNotification("Pairing", step)
            }
        }
    }

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    private fun savePairingInfo(coordinatorUrl: String, pairingToken: String?, deviceId: String) {
        val prefs = getSharedPreferences("misa_pairing", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("coordinator_url", coordinatorUrl)
            putString("pairing_token", pairingToken)
            putString("device_id", deviceId)
            putLong("pairing_time", System.currentTimeMillis())
            apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Static methods for coordinating installation from other parts of the app
     */
    companion object {
        fun startInstallation(context: Context, coordinatorUrl: String, apkUrl: String, pairingToken: String?) {
            val intent = Intent(context, AutoInstallerService::class.java).apply {
                action = ACTION_DOWNLOAD_APK
                putExtra(EXTRA_APK_URL, apkUrl)
                putExtra(EXTRA_COORDINATOR_URL, coordinatorUrl)
                putExtra(EXTRA_PAIRING_TOKEN, pairingToken)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startPairing(context: Context, coordinatorUrl: String, pairingToken: String?) {
            val intent = Intent(context, AutoInstallerService::class.java).apply {
                action = ACTION_START_PAIRING
                putExtra(EXTRA_COORDINATOR_URL, coordinatorUrl)
                putExtra(EXTRA_PAIRING_TOKEN, pairingToken)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun reportStatus(context: Context, status: String, data: Map<String, Any?> = emptyMap()) {
            val intent = Intent(context, AutoInstallerService::class.java).apply {
                action = ACTION_REPORT_STATUS
                putExtra("status", status)

                val bundle = android.os.Bundle()
                data.forEach { (key, value) ->
                    when (value) {
                        is String -> bundle.putString(key, value)
                        is Int -> bundle.putInt(key, value)
                        is Long -> bundle.putLong(key, value)
                        is Boolean -> bundle.putBoolean(key, value)
                        else -> bundle.putString(key, value.toString())
                    }
                }
                putExtra("data", bundle)
            }

            context.startService(intent)
        }
    }
}