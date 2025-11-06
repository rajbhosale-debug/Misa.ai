package com.misa.ai.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Background service for checking and downloading updates
 */
@AndroidEntryPoint
class UpdateService : Service() {

    @Inject
    lateinit var updateManager: UpdateManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateCheckJob: Job? = null

    companion object {
        const val CHANNEL_ID = "update_channel"
        const val CHANNEL_NAME = "App Updates"
        const val NOTIFICATION_ID = 1001
        const val ACTION_CHECK_UPDATES = "com.misa.ai.action.CHECK_UPDATES"
        const val ACTION_DOWNLOAD_UPDATE = "com.misa.ai.action.DOWNLOAD_UPDATE"
        const val EXTRA_FORCE_CHECK = "force_check"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Checking for updates..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CHECK_UPDATES -> {
                val forceCheck = intent.getBooleanExtra(EXTRA_FORCE_CHECK, false)
                startUpdateCheck(forceCheck)
            }
            ACTION_DOWNLOAD_UPDATE -> {
                downloadUpdate()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        updateManager.cleanup()
    }

    /**
     * Start update check
     */
    private fun startUpdateCheck(force: Boolean) {
        updateCheckJob?.cancel()

        updateCheckJob = serviceScope.launch {
            updateNotification("Checking for updates...")

            try {
                val result = updateManager.checkForUpdates(force)

                when (result) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        updateNotification("Update available!")
                        showUpdateNotification(result)
                    }
                    is UpdateCheckResult.MandatoryUpdate -> {
                        updateNotification("Critical update available!")
                        showMandatoryUpdateNotification(result)
                    }
                    UpdateCheckResult.NoUpdate -> {
                        updateNotification("App is up to date")
                        stopSelf()
                    }
                    is UpdateCheckResult.Error -> {
                        updateNotification("Update check failed: ${result.exception.message}")
                        stopSelf()
                    }
                }
            } catch (e: Exception) {
                updateNotification("Update check failed: ${e.message}")
                stopSelf()
            }
        }
    }

    /**
     * Download update
     */
    private fun downloadUpdate() {
        updateManager.downloadAndInstallUpdate()

        // Monitor download progress
        serviceScope.launch {
            updateManager.updateState.collect { state ->
                state.downloadProgress?.let { progress ->
                    updateNotification(
                        "Downloading update: ${progress.percentage}%",
                        progress.percentage
                    )

                    if (progress.status == DownloadStatus.COMPLETED) {
                        updateNotification("Update downloaded. Installing...")
                        stopSelf()
                    } else if (progress.status == DownloadStatus.FAILED) {
                        updateNotification("Download failed")
                        stopSelf()
                    }
                }
            }
        }
    }

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for app updates"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create service notification
     */
    private fun createNotification(text: String, progress: Int = -1): Notification {
        val pendingIntent = createPendingIntent()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MISA.AI Update")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    /**
     * Update service notification
     */
    private fun updateNotification(text: String, progress: Int = -1) {
        val notification = createNotification(text, progress)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Create pending intent for notification
     */
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Show update available notification
     */
    private fun showUpdateNotification(result: UpdateCheckResult.UpdateAvailable) {
        val updateInfo = updateManager.updateState.value.updateInfo ?: return

        val downloadIntent = Intent(this, UpdateService::class.java).apply {
            action = ACTION_DOWNLOAD_UPDATE
        }

        val downloadPendingIntent = PendingIntent.getService(
            this,
            0,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MISA.AI Update Available")
            .setContentText("Version ${updateInfo.version} is ready to download")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Version ${updateInfo.version}\n${updateInfo.changelog}"))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .addAction(
                android.R.drawable.stat_sys_download,
                "Download",
                downloadPendingIntent
            )
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Show mandatory update notification
     */
    private fun showMandatoryUpdateNotification(result: UpdateCheckResult.MandatoryUpdate) {
        val updateInfo = updateManager.updateState.value.updateInfo ?: return

        val downloadIntent = Intent(this, UpdateService::class.java).apply {
            action = ACTION_DOWNLOAD_UPDATE
        }

        val downloadPendingIntent = PendingIntent.getService(
            this,
            0,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ Critical Update Required")
            .setContentText("Please update to version ${updateInfo.version} immediately")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Version ${updateInfo.version}\n${updateInfo.changelog}\n\nThis update contains important security fixes."))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.stat_sys_download,
                "Update Now",
                downloadPendingIntent
            )
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
}