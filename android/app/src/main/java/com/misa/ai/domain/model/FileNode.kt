package com.misa.ai.domain.model

import java.time.LocalDateTime

/**
 * File Node - represents a file or folder in the FileHub system
 * Supports hierarchical structure with comprehensive metadata
 */
data class FileNode(
    val id: String,
    val name: String,
    val path: String,
    val type: FileType,
    val size: Long,
    val mimeType: String? = null,
    val parentId: String? = null,
    val children: List<FileNode> = emptyList(),
    val metadata: FileMetadata,
    val permissions: FilePermissions,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val isShared: Boolean = false,
    val shareInfo: ShareInfo? = null,
    val tags: List<String> = emptyList(),
    val thumbnail: ThumbnailInfo? = null,
    val content: ContentInfo? = null,
    val version: FileVersion? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val accessedAt: LocalDateTime? = null
) {

    /**
     * Check if this is a folder
     */
    fun isFolder(): Boolean = type == FileType.FOLDER

    /**
     * Check if this is a file
     */
    fun isFile(): Boolean = type != FileType.FOLDER

    /**
     * Check if file is currently syncing
     */
    fun isSyncing(): Boolean = syncStatus == SyncStatus.SYNCING

    /**
     * Check if file has sync errors
     */
    fun hasSyncError(): Boolean = syncStatus == SyncStatus.ERROR

    /**
     * Get file extension
     */
    fun getFileExtension(): String? {
        return if (isFile()) {
            name.substringAfterLast('.', null)
        } else null
    }

    /**
     * Get formatted file size
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
            else -> "${size / (1024 * 1024 * 1024)}GB"
        }
    }

    /**
     * Check if file can be previewed
     */
    fun canPreview(): Boolean {
        return when (type) {
            FileType.IMAGE -> true
            FileType.VIDEO -> true
            FileType.AUDIO -> true
            FileType.TEXT -> true
            FileType.PDF -> true
            FileType.DOCUMENT -> true
            else -> false
        }
    }

    /**
     * Get display path (excluding root)
     */
    fun getDisplayPath(): String {
        return path.substringAfter("/").ifEmpty { "/" }
    }

    /**
     * Check if file is recently modified
     */
    fun isRecentlyModified(): Boolean {
        val now = LocalDateTime.now()
        return updatedAt.isAfter(now.minusDays(1))
    }

    /**
     * Check if file is recently accessed
     */
    fun isRecentlyAccessed(): Boolean {
        val now = LocalDateTime.now()
        return accessedAt?.isAfter(now.minusDays(1)) == true
    }
}

/**
 * File types supported by FileHub
 */
enum class FileType {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    TEXT,
    PDF,
    DOCUMENT,
    SPREADSHEET,
    PRESENTATION,
    ARCHIVE,
    CODE,
    DATABASE,
    CONFIG,
    EXECUTABLE,
    UNKNOWN
}

/**
 * Comprehensive file metadata
 */
data class FileMetadata(
    val checksum: String? = null,
    val hash: String? = null,
    val originalName: String? = null,
    val description: String? = null,
    val category: FileCategory? = null,
    val attributes: Map<String, Any> = emptyMap(),
    val exifData: EXIFData? = null,
    val documentProperties: DocumentProperties? = null,
    val mediaInfo: MediaInfo? = null,
    val customProperties: Map<String, String> = emptyMap()
)

/**
 * File categories for organization
 */
enum class FileCategory {
    DOCUMENTS,
    IMAGES,
    VIDEOS,
    AUDIO,
    CODE,
    DATA,
    ARCHIVES,
    SYSTEM,
    DOWNLOADS,
    WORK,
    PERSONAL,
    PROJECTS,
    TEMP,
    UNKNOWN
}

/**
 * File permissions
 */
data class FilePermissions(
    val owner: Permission,
    val group: Permission,
    val others: Permission,
    val special: SpecialPermissions = SpecialPermissions()
)

/**
 * Individual permission set
 */
data class Permission(
    val read: Boolean,
    val write: Boolean,
    val execute: Boolean
) {
    fun toOctal(): Int {
        var result = 0
        if (read) result += 4
        if (write) result += 2
        if (execute) result += 1
        return result
    }

    fun toSymbolic(): String {
        return (if (read) "r" else "-") +
               (if (write) "w" else "-") +
               (if (execute) "x" else "-")
    }
}

/**
 * Special permissions (setuid, setgid, sticky bit)
 */
data class SpecialPermissions(
    val setUid: Boolean = false,
    val setGid: Boolean = false,
    val stickyBit: Boolean = false
)

/**
 * Sync status for files
 */
enum class SyncStatus {
    SYNCED,
    SYNCING,
    PENDING_UPLOAD,
    PENDING_DOWNLOAD,
    ERROR,
    OFFLINE,
    CONFLICT
}

/**
 * File sharing information
 */
data class ShareInfo(
    val shareId: String,
    val shareType: ShareType,
    val sharedBy: String,
    val sharedWith: List<ShareRecipient>,
    val permissions: SharePermissions,
    val expiresAt: LocalDateTime? = null,
    val passwordProtected: Boolean = false,
    val downloadLimit: Int? = null,
    val createdAt: LocalDateTime,
    val accessCount: Int = 0
)

/**
 * Share types
 */
enum class ShareType {
    PUBLIC_LINK,
    PRIVATE_LINK,
    DIRECT_SHARE,
    TEAM_SHARE,
    TEMPORARY
}

/**
 * Share recipient
 */
data class ShareRecipient(
    val id: String,
    val type: RecipientType,
    val name: String,
    val email: String? = null,
    val accessLevel: AccessLevel
)

/**
 * Recipient types
 */
enum class RecipientType {
    USER,
    GROUP,
    TEAM,
    EMAIL,
    PUBLIC
}

/**
 * Access levels for sharing
 */
enum class AccessLevel {
    VIEWER,
    COMMENTER,
    EDITOR,
    OWNER
}

/**
 * Share permissions
 */
data class SharePermissions(
    val canView: Boolean,
    val canComment: Boolean = false,
    val canEdit: Boolean = false,
    val canDownload: Boolean = true,
    val canShare: Boolean = false,
    val canDelete: Boolean = false
)

/**
 * Thumbnail information
 */
data class ThumbnailInfo(
    val url: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val format: String,
    val generatedAt: LocalDateTime
)

/**
 * Content information
 */
data class ContentInfo(
    val lineCount: Int? = null,
    val wordCount: Int? = null,
    val characterCount: Int? = null,
    val language: String? = null,
    val encoding: String? = null,
    val compressionRatio: Float? = null,
    val isEncrypted: Boolean = false,
    val passwordProtected: Boolean = false,
    val isCompressed: Boolean = false,
    val containsViruses: Boolean = false,
    val virusScanResult: String? = null
)

/**
 * File version information
 */
data class FileVersion(
    val version: String,
    val size: Long,
    val checksum: String,
    val createdAt: LocalDateTime,
    val createdBy: String,
    val changes: String? = null,
    val isCurrent: Boolean = false
)

/**
 * EXIF data for images
 */
data class EXIFData(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val dateTime: LocalDateTime? = null,
    val gpsLocation: GPSLocation? = null,
    val dimensions: ImageDimensions? = null,
    val flashUsed: Boolean? = null,
    val focalLength: Float? = null,
    val aperture: Float? = null,
    val iso: Int? = null,
    val exposureTime: Float? = null
)

/**
 * GPS location
 */
data class GPSLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val precision: Float? = null
)

/**
 * Image dimensions
 */
data class ImageDimensions(
    val width: Int,
    val height: Int
)

/**
 * Document properties
 */
data class DocumentProperties(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: List<String> = emptyList(),
    val pageCount: Int? = null,
    val wordCount: Int? = null,
    val characterCount: Int? = null,
    val creationDate: LocalDateTime? = null,
    val lastModified: LocalDateTime? = null,
    val template: String? = null,
    val security: DocumentSecurity? = null
)

/**
 * Document security information
 */
data class DocumentSecurity(
    val isEncrypted: Boolean,
    val hasPassword: Boolean,
    val hasDigitalSignature: Boolean,
    val hasWatermark: Boolean,
    val restrictions: List<String> = emptyList()
)

/**
 * Media information
 */
data class MediaInfo(
    val duration: kotlin.time.Duration? = null,
    val bitRate: Int? = null,
    val frameRate: Float? = null,
    val resolution: ImageDimensions? = null,
    val audioCodec: String? = null,
    val videoCodec: String? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val language: String? = null,
    val subtitles: List<SubtitleInfo> = emptyList()
)

/**
 * Subtitle information
 */
data class SubtitleInfo(
    val language: String,
    val format: String,
    val encoding: String? = null
)

/**
 * File operation result
 */
sealed class FileOperationResult {
    data class Success(val fileNode: FileNode, val message: String? = null) : FileOperationResult()
    data class Error(val error: String, val errorCode: String? = null) : FileOperationResult()
    data class Progress(val progress: Float, val message: String? = null) : FileOperationResult()
}

/**
 * File search filters
 */
data class FileSearchFilter(
    val query: String? = null,
    val fileType: FileType? = null,
    val mimeType: String? = null,
    val category: FileCategory? = null,
    val tags: List<String> = emptyList(),
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val dateRange: DateRange? = null,
    val modifiedBy: String? = null,
    val isFavorite: Boolean? = null,
    val isShared: Boolean? = null,
    val syncStatus: SyncStatus? = null,
    val parentId: String? = null,
    val recursive: Boolean = true,
    val includeTrashed: Boolean = false
)

/**
 * Date range for filtering
 */
data class DateRange(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

/**
 * File sort options
 */
enum class FileSortOption {
    NAME,
    SIZE,
    TYPE,
    MODIFIED_DATE,
    CREATED_DATE,
    ACCESSED_DATE
}

/**
 * Sort direction
 */
enum class SortDirection {
    ASCENDING,
    DESCENDING
}

/**
 * File sort configuration
 */
data class FileSortConfig(
    val sortBy: FileSortOption,
    val direction: SortDirection,
    val foldersFirst: Boolean = true
)

/**
 * Cloud storage provider
 */
enum class CloudProvider {
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE,
    ICLOUD,
    BOX,
    AWS_S3,
    AZURE_BLOB,
    LOCAL
}

/**
 * Sync configuration
 */
data class SyncConfiguration(
    val autoSync: Boolean = true,
    val syncInterval: kotlin.time.Duration = kotlin.time.Duration.parse("PT5M"),
    val wifiOnly: Boolean = false,
    val excludePatterns: List<String> = emptyList(),
    val includePatterns: List<String> = emptyList(),
    val maxFileSize: Long = 100L * 1024 * 1024, // 100MB
    val cloudProvider: CloudProvider,
    val syncPath: String,
    val conflictResolution: ConflictResolution = ConflictResolution.KEEP_BOTH
)

/**
 * Conflict resolution strategies
 */
enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_REMOTE,
    KEEP_BOTH,
    KEEP_NEWER,
    MANUAL_RESOLVE
}

/**
 * File transfer progress
 */
data class FileTransferProgress(
    val transferId: String,
    val fileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speed: Long, // bytes per second
    val estimatedTimeRemaining: kotlin.time.Duration,
    val status: TransferStatus,
    val startTime: LocalDateTime,
    val estimatedEndTime: LocalDateTime
)

/**
 * File transfer status
 */
enum class TransferStatus {
    PENDING,
    PREPARING,
    TRANSFERRING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}