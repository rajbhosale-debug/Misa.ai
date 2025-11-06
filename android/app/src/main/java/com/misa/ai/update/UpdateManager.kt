package com.misa.ai.update

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Update information data class
 */
@Serializable
data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val releaseDate: String,
    val changelog: String,
    val downloadUrl: String,
    val checksum: String,
    val size: Long,
    val isMandatory: Boolean = false,
    val minSupportedVersion: String? = null
)

/**
 * Download progress data class
 */
@Serializable
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Int,
    val downloadId: Long = -1L,
    val status: DownloadStatus = DownloadStatus.IN_PROGRESS
)

enum class DownloadStatus {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Update check result
 */
sealed class UpdateCheckResult {
    object NoUpdate : UpdateCheckResult()
    object UpdateAvailable : UpdateCheckResult()
    object MandatoryUpdate : UpdateCheckResult()
    data class Error(val exception: Exception) : UpdateCheckResult()
}

/**
 * Update state for UI
 */
data class UpdateState(
    val isChecking: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val downloadProgress: DownloadProgress? = null,
    val isInstalling: Boolean = false,
    val lastCheckTime: Long = 0L,
    val autoUpdateEnabled: Boolean = true
)

/**
 * Android in-app update manager
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager
) {
    companion object {
        private const val GITHUB_RELEASES_API = "https://api.github.com/repos/misa-ai/misa.ai/releases/latest"
        private const val DOWNLOAD_BASE_URL = "https://download.misa.ai/releases/android/"
        private const val UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
        private const val PREFERENCES_NAME = "update_preferences"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        private const val KEY_SKIP_VERSION = "skip_version"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    // State flows
    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    private var downloadId: Long = -1L
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
            if (id == downloadId) {
                handleDownloadComplete(id)
            }
        }
    }

    init {
        // Register download receiver
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(downloadReceiver, filter)

        // Load saved preferences
        loadPreferences()
    }

    /**
     * Check for updates
     */
    suspend fun checkForUpdates(force: Boolean = false): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                _updateState.value = _updateState.value.copy(isChecking = true)

                // Check if enough time has passed since last check
                val currentTime = System.currentTimeMillis()
                val lastCheckTime = sharedPreferences.getLong(KEY_LAST_CHECK_TIME, 0L)

                if (!force && (currentTime - lastCheckTime) < UPDATE_CHECK_INTERVAL) {
                    _updateState.value = _updateState.value.copy(isChecking = false)
                    return@withContext UpdateCheckResult.NoUpdate
                }

                // Get current app version
                val currentVersion = getCurrentVersionCode()

                // Fetch latest release from GitHub
                val request = Request.Builder()
                    .url(GITHUB_RELEASES_API)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch release info: ${response.code}")
                }

                val responseBody = response.body?.string()
                    ?: throw Exception("Empty response body")

                val releaseData = json.decodeMap<String, Any>(responseBody)

                // Extract version information
                val tagName = releaseData["tag_name"] as? String
                    ?: throw Exception("Missing tag_name in release")
                val version = tagName.removePrefix("v")

                val versionCode = extractVersionCode(releaseData)
                val releaseDate = releaseData["published_at"] as? String ?: ""
                val changelog = extractChangelog(releaseData)

                // Find download URL for this architecture
                val downloadUrl = findDownloadUrl(releaseData)
                val checksum = extractChecksum(releaseData, downloadUrl)
                val size = extractSize(releaseData, downloadUrl)

                val updateInfo = UpdateInfo(
                    version = version,
                    versionCode = versionCode,
                    releaseDate = releaseDate,
                    changelog = changelog,
                    downloadUrl = downloadUrl,
                    checksum = checksum,
                    size = size
                )

                // Determine if update is available
                val result = when {
                    versionCode > currentVersion -> {
                        _updateState.value = _updateState.value.copy(
                            updateInfo = updateInfo,
                            lastCheckTime = currentTime
                        )

                        // Check if update is mandatory
                        val isMandatory = updateInfo.version.contains("critical") ||
                                       updateInfo.version.contains("security")

                        if (isMandatory) UpdateCheckResult.MandatoryUpdate
                        else UpdateCheckResult.UpdateAvailable
                    }
                    else -> UpdateCheckResult.NoUpdate
                }

                // Save last check time
                sharedPreferences.edit()
                    .putLong(KEY_LAST_CHECK_TIME, currentTime)
                    .apply()

                _updateCheckResult.value = result
                result

            } catch (e: Exception) {
                val errorResult = UpdateCheckResult.Error(e)
                _updateCheckResult.value = errorResult
                errorResult
            } finally {
                _updateState.value = _updateState.value.copy(isChecking = false)
            }
        }
    }

    /**
     * Download and install update
     */
    fun downloadAndInstallUpdate() {
        val updateInfo = _updateState.value.updateInfo ?: return

        try {
            // Check if we have storage permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    // Request storage permission
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${context.packageName}")
                    // This should be handled by the activity
                    return
                }
            }

            // Create download request
            val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl)).apply {
                setTitle("MISA.AI Update")
                setDescription("Downloading version ${updateInfo.version}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "misa-update-${updateInfo.version}.apk")
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                setMimeType("application/vnd.android.package-archive")
            }

            // Enqueue download
            downloadId = downloadManager.enqueue(request)

            // Start monitoring progress
            CoroutineScope(Dispatchers.Main).launch {
                monitorDownloadProgress(downloadId)
            }

        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(
                downloadProgress = null,
                isInstalling = false
            )
        }
    }

    /**
     * Monitor download progress
     */
    private suspend fun monitorDownloadProgress(downloadId: Long) {
        while (true) {
            try {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    val downloadStatus = when (status) {
                        DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
                        DownloadManager.STATUS_RUNNING -> DownloadStatus.IN_PROGRESS
                        DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            _updateState.value = _updateState.value.copy(downloadProgress = null)
                            return // Download completed
                        }
                        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                        else -> DownloadStatus.CANCELLED
                    }

                    val progress = if (totalBytes > 0) {
                        ((bytesDownloaded * 100) / totalBytes).toInt()
                    } else 0

                    _updateState.value = _updateState.value.copy(
                        downloadProgress = DownloadProgress(
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            percentage = progress,
                            downloadId = downloadId,
                            status = downloadStatus
                        )
                    )
                }

                cursor.close()
                delay(1000) // Update every second

            } catch (e: Exception) {
                break
            }
        }
    }

    /**
     * Handle download completion
     */
    private fun handleDownloadComplete(downloadId: Long) {
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))

                cursor.close()

                if (status == DownloadManager.STATUS_SUCCESSFUL && localUri != null) {
                    val filePath = Uri.parse(localUri).path
                    if (filePath != null) {
                        installUpdate(File(filePath))
                    }
                } else {
                    _updateState.value = _updateState.value.copy(
                        downloadProgress = null,
                        isInstalling = false
                    )
                }
            } else {
                cursor.close()
            }
        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(
                downloadProgress = null,
                isInstalling = false
            )
        }
    }

    /**
     * Install downloaded update
     */
    private fun installUpdate(apkFile: File) {
        try {
            _updateState.value = _updateState.value.copy(isInstalling = true)

            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)

        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(isInstalling = false)
        }
    }

    /**
     * Cancel current download
     */
    fun cancelDownload() {
        if (downloadId != -1L) {
            downloadManager.remove(downloadId)
            downloadId = -1L
            _updateState.value = _updateState.value.copy(
                downloadProgress = null,
                isInstalling = false
            )
        }
    }

    /**
     * Skip current version
     */
    fun skipCurrentVersion() {
        val currentVersion = _updateState.value.updateInfo?.version ?: return
        sharedPreferences.edit()
            .putString(KEY_SKIP_VERSION, currentVersion)
            .apply()
    }

    /**
     * Toggle auto-updates
     */
    fun toggleAutoUpdates(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_AUTO_UPDATE_ENABLED, enabled)
            .apply()
        _updateState.value = _updateState.value.copy(autoUpdateEnabled = enabled)
    }

    /**
     * Get current app version code
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.longVersionCode.toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Extract version code from release data
     */
    private fun extractVersionCode(releaseData: Map<String, Any>): Int {
        return try {
            val body = releaseData["body"] as? String ?: return 1
            val versionCodeRegex = Regex("versionCode[\\s]*:[\\s]*(\\d+)")
            versionCodeRegex.find(body)?.groupValues?.get(1)?.toInt() ?: 1
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Extract changelog from release data
     */
    private fun extractChangelog(releaseData: Map<String, Any>): String {
        return (releaseData["body"] as? String) ?: "No changelog available"
    }

    /**
     * Find appropriate download URL for device architecture
     */
    private fun findDownloadUrl(releaseData: Map<String, Any>): String {
        val assets = releaseData["assets"] as? List<Map<String, Any>> ?: return ""

        val abi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS[0]
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }

        // Look for architecture-specific APK
        val preferredSuffix = when {
            abi.contains("arm64") -> "arm64-v8a"
            abi.contains("arm") -> "armeabi-v7a"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> "universal"
        }

        // Find matching asset
        for (asset in assets) {
            val name = asset["name"] as? String ?: continue
            if (name.endsWith(".apk") && name.contains(preferredSuffix)) {
                return asset["browser_download_url"] as? String ?: ""
            }
        }

        // Fallback to first APK
        for (asset in assets) {
            val name = asset["name"] as? String ?: continue
            if (name.endsWith(".apk")) {
                return asset["browser_download_url"] as? String ?: ""
            }
        }

        return ""
    }

    /**
     * Extract checksum for download URL
     */
    private fun extractChecksum(releaseData: Map<String, Any>, downloadUrl: String): String {
        // This would need to be implemented based on your release structure
        // For now, return empty
        return ""
    }

    /**
     * Extract size for download URL
     */
    private fun extractSize(releaseData: Map<String, Any>, downloadUrl: String): Long {
        val assets = releaseData["assets"] as? List<Map<String, Any>> ?: return 0L

        for (asset in assets) {
            if (asset["browser_download_url"] == downloadUrl) {
                return (asset["size"] as? Number)?.toLong() ?: 0L
            }
        }

        return 0L
    }

    /**
     * Load saved preferences
     */
    private fun loadPreferences() {
        val lastCheckTime = sharedPreferences.getLong(KEY_LAST_CHECK_TIME, 0L)
        val autoUpdateEnabled = sharedPreferences.getBoolean(KEY_AUTO_UPDATE_ENABLED, true)

        _updateState.value = _updateState.value.copy(
            lastCheckTime = lastCheckTime,
            autoUpdateEnabled = autoUpdateEnabled
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            context.unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            // Receiver may not be registered
        }
    }
}