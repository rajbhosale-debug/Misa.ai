package com.misa.ai.update

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Download manager wrapper for handling APK downloads
 */
@Singleton
class MisaDownloadManager @Inject constructor(
    private val context: Context
) {
    private val systemDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /**
     * Get download status
     */
    suspend fun getDownloadStatus(downloadId: Long): DownloadStatusInfo? = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = systemDownloadManager.query(query)

        return@withContext try {
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))

                DownloadStatusInfo(
                    status = status,
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = totalBytes,
                    localUri = localUri,
                    reason = reason
                )
            } else {
                null
            }
        } finally {
            cursor.close()
        }
    }

    /**
     * Check if download is complete
     */
    suspend fun isDownloadComplete(downloadId: Long): Boolean {
        val status = getDownloadStatus(downloadId)
        return status?.status == DownloadManager.STATUS_SUCCESSFUL
    }

    /**
     * Get download file path
     */
    suspend fun getDownloadFilePath(downloadId: Long): String? {
        val status = getDownloadStatus(downloadId)
        return status?.localUri?.let { uri ->
            if (uri.startsWith("file://")) {
                uri.substring(7)
            } else {
                null
            }
        }
    }

    /**
     * Cancel download
     */
    fun cancelDownload(downloadId: Long): Int {
        return systemDownloadManager.remove(downloadId)
    }

    /**
     * Pause download (not directly supported, remove and re-enqueue)
     */
    fun pauseDownload(downloadId: Long): Boolean {
        return systemDownloadManager.remove(downloadId) > 0
    }

    /**
     * Get all downloads
     */
    suspend fun getAllDownloads(): List<DownloadStatusInfo> = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query()
        val cursor = systemDownloadManager.query(query)

        return@withContext try {
            val downloads = mutableListOf<DownloadStatusInfo>()
            while (cursor.moveToNext()) {
                val downloadId = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))

                downloads.add(
                    DownloadStatusInfo(
                        downloadId = downloadId,
                        status = status,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        localUri = localUri,
                        reason = reason
                    )
                )
            }
            downloads
        } finally {
            cursor.close()
        }
    }

    /**
     * Clean up completed downloads
     */
    fun cleanupCompletedDownloads() {
        // This would be implemented based on your cleanup strategy
        // For now, we don't automatically clean up to allow users to access downloaded files
    }
}

/**
 * Download status information
 */
data class DownloadStatusInfo(
    val downloadId: Long = -1L,
    val status: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val localUri: String?,
    val reason: Int = 0
) {
    /**
     * Get status as enum
     */
    fun getStatusEnum(): DownloadStatus {
        return when (status) {
            DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
            DownloadManager.STATUS_RUNNING -> DownloadStatus.IN_PROGRESS
            DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
            DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
            else -> DownloadStatus.CANCELLED
        }
    }

    /**
     * Get progress percentage
     */
    fun getProgressPercentage(): Int {
        return if (totalBytes > 0) {
            ((bytesDownloaded * 100) / totalBytes).toInt()
        } else 0
    }

    /**
     * Is download complete
     */
    fun isComplete(): Boolean = status == DownloadManager.STATUS_SUCCESSFUL

    /**
     * Is download failed
     */
    fun isFailed(): Boolean = status == DownloadManager.STATUS_FAILED

    /**
     * Get failure reason
     */
    fun getFailureReason(): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device not found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "File error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
            DownloadManager.ERROR_UNKNOWN -> "Unknown error"
            else -> "Unknown error ($reason)"
        }
    }
}