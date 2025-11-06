package com.misa.ai.domain.repository

import com.misa.ai.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * File repository interface
 * Provides comprehensive file management operations
 */
interface FileRepository {

    // === Basic File Operations ===

    /**
     * Get file node by ID
     */
    suspend fun getFileNode(fileId: String): Result<FileNode?>

    /**
     * Get file node by path
     */
    suspend fun getFileNodeByPath(path: String): Result<FileNode?>

    /**
     * List files in directory
     */
    fun listFiles(
        parentId: String? = null,
        filter: FileSearchFilter = FileSearchFilter(),
        sort: FileSortConfig = FileSortConfig(FileSortOption.NAME, SortDirection.ASCENDING)
    ): Flow<Result<List<FileNode>>>

    /**
     * Create new folder
     */
    suspend fun createFolder(
        name: String,
        parentId: String? = null,
        metadata: FileMetadata = FileMetadata()
    ): Result<FileNode>

    /**
     * Upload file
     */
    suspend fun uploadFile(
        localPath: String,
        parentId: String? = null,
        metadata: FileMetadata = FileMetadata(),
        onProgress: (FileTransferProgress) -> Unit = {}
    ): Flow<Result<FileTransferProgress>>

    /**
     * Download file
     */
    suspend fun downloadFile(
        fileId: String,
        localPath: String,
        onProgress: (FileTransferProgress) -> Unit = {}
    ): Flow<Result<FileTransferProgress>>

    /**
     * Delete file or folder
     */
    suspend fun deleteFile(fileId: String, permanent: Boolean = false): Result<Unit>

    /**
     * Move file to different location
     */
    suspend fun moveFile(fileId: String, newParentId: String): Result<FileNode>

    /**
     * Copy file to different location
     */
    suspend fun copyFile(fileId: String, newParentId: String, newName: String? = null): Result<FileNode>

    /**
     * Rename file or folder
     */
    suspend fun renameFile(fileId: String, newName: String): Result<FileNode>

    // === Search and Filter ===

    /**
     * Search files
     */
    suspend fun searchFiles(filter: FileSearchFilter): Result<List<FileNode>>

    /**
     * Get recently modified files
     */
    fun getRecentlyModifiedFiles(limit: Int = 50): Flow<Result<List<FileNode>>>

    /**
     * Get recently accessed files
     */
    fun getRecentlyAccessedFiles(limit: Int = 50): Flow<Result<List<FileNode>>>

    /**
     * Get favorite files
     */
    fun getFavoriteFiles(): Flow<Result<List<FileNode>>>

    /**
     * Get shared files
     */
    fun getSharedFiles(): Flow<Result<List<FileNode>>>

    /**
     * Get files by type
     */
    fun getFilesByType(fileType: FileType): Flow<Result<List<FileNode>>>

    /**
     * Get files by category
     */
    fun getFilesByCategory(category: FileCategory): Flow<Result<List<FileNode>>>

    /**
     * Get large files
     */
    suspend fun getLargeFiles(minSize: Long): Result<List<FileNode>>

    /**
     * Get duplicate files
     */
    suspend fun getDuplicateFiles(): Result<Map<String, List<FileNode>>>

    /**
     * Get orphaned files (files in trash with no parent)
     */
    suspend fun getOrphanedFiles(): Result<List<FileNode>>

    // === Favorites and Organization ===

    /**
     * Add file to favorites
     */
    suspend fun addToFavorites(fileId: String): Result<Unit>

    /**
     * Remove file from favorites
     */
    suspend fun removeFromFavorites(fileId: String): Result<Unit>

    /**
     * Set file tags
     */
    suspend fun setFileTags(fileId: String, tags: List<String>): Result<Unit>

    /**
     * Add file tag
     */
    suspend fun addFileTag(fileId: String, tag: String): Result<Unit>

    /**
     * Remove file tag
     */
    suspend fun removeFileTag(fileId: String, tag: String): Result<Unit>

    /**
     * Get all available tags
     */
    suspend fun getAllTags(): Result<List<String>>

    /**
     * Categorize file
     */
    suspend fun categorizeFile(fileId: String, category: FileCategory): Result<Unit>

    /**
     * Auto-categorize files
     */
    suspend fun autoCategorizeFiles(parentId: String? = null): Result<Int> // Returns number of categorized files

    // === File Metadata ===

    /**
     * Update file metadata
     */
    suspend fun updateFileMetadata(fileId: String, metadata: FileMetadata): Result<FileNode>

    /**
     * Get file metadata
     */
    suspend fun getFileMetadata(fileId: String): Result<FileMetadata>

    /**
     * Extract file metadata (EXIF, document properties, etc.)
     */
    suspend fun extractFileMetadata(fileId: String): Result<FileMetadata>

    /**
     * Update file description
     */
    suspend fun updateFileDescription(fileId: String, description: String): Result<FileNode>

    /**
     * Update custom properties
     */
    suspend fun updateCustomProperties(fileId: String, properties: Map<String, String>): Result<FileNode>

    // === Thumbnails and Previews ===

    /**
     * Generate thumbnail for file
     */
    suspend fun generateThumbnail(fileId: String): Result<ThumbnailInfo>

    /**
     * Get file thumbnail
     */
    suspend fun getThumbnail(fileId: String): Result<ThumbnailInfo?>

    /**
     * Generate preview for file
     */
    suspend fun generatePreview(fileId: String): Result<String> // Returns preview URL

    /**
     * Get file preview
     */
    suspend fun getPreview(fileId: String): Result<String?> // Returns preview URL

    // === Sharing and Collaboration ===

    /**
     * Create share link
     */
    suspend fun createShareLink(
        fileId: String,
        shareType: ShareType,
        permissions: SharePermissions,
        expiresAt: LocalDateTime? = null,
        password: String? = null
    ): Result<ShareInfo>

    /**
     * Share file with users/groups
     */
    suspend fun shareFile(
        fileId: String,
        recipients: List<ShareRecipient>,
        permissions: SharePermissions,
        message: String? = null
    ): Result<List<ShareInfo>>

    /**
     * Get file shares
     */
    suspend fun getFileShares(fileId: String): Result<List<ShareInfo>>

    /**
     * Update share permissions
     */
    suspend fun updateSharePermissions(shareId: String, permissions: SharePermissions): Result<ShareInfo>

    /**
     * Revoke share
     */
    suspend fun revokeShare(shareId: String): Result<Unit>

    /**
     * Get files shared with user
     */
    fun getFilesSharedWithMe(): Flow<Result<List<FileNode>>>

    /**
     * Get files shared by user
     */
    fun getFilesSharedByMe(): Flow<Result<List<FileNode>>>

    // === Version Management ===

    /**
     * Create file version
     */
    suspend fun createFileVersion(fileId: String, changes: String? = null): Result<FileVersion>

    /**
     * Get file versions
     */
    suspend fun getFileVersions(fileId: String): Result<List<FileVersion>>

    /**
     * Restore file to specific version
     */
    suspend fun restoreFileVersion(fileId: String, version: String): Result<FileNode>

    /**
     * Delete file version
     */
    suspend fun deleteFileVersion(fileId: String, version: String): Result<Unit>

    // === Sync Operations ===

    /**
     * Sync files with cloud storage
     */
    suspend fun syncFiles(): Result<SyncResult>

    /**
     * Sync specific folder
     */
    suspend fun syncFolder(folderId: String): Result<SyncResult>

    /**
     * Get sync status
     */
    suspend fun getSyncStatus(): Result<SyncStatus>

    /**
     * Get files with sync errors
     */
    suspend fun getFilesWithSyncErrors(): Result<List<FileNode>>

    /**
     * Retry sync for specific file
     */
    suspend fun retryFileSync(fileId: String): Result<Unit>

    /**
     * Resolve sync conflict
     */
    suspend fun resolveSyncConflict(fileId: String, resolution: ConflictResolution): Result<FileNode>

    // === Trash and Recovery ===

    /**
     * Move file to trash
     */
    suspend fun moveToTrash(fileId: String): Result<Unit>

    /**
     * Restore file from trash
     */
    suspend fun restoreFromTrash(fileId: String): Result<FileNode>

    /**
     * Empty trash
     */
    suspend fun emptyTrash(): Result<Unit>

    /**
     * Get trashed files
     */
    fun getTrashedFiles(): Flow<Result<List<FileNode>>>

    /**
     * Permanently delete trashed file
     */
    suspend fun permanentlyDeleteFile(fileId: String): Result<Unit>

    // === Security and Permissions ===

    /**
     * Update file permissions
     */
    suspend fun updateFilePermissions(fileId: String, permissions: FilePermissions): Result<FileNode>

    /**
     * Get file permissions
     */
    suspend fun getFilePermissions(fileId: String): Result<FilePermissions>

    /**
     * Encrypt file
     */
    suspend fun encryptFile(fileId: String, password: String): Result<FileNode>

    /**
     * Decrypt file
     */
    suspend fun decryptFile(fileId: String, password: String): Result<FileNode>

    /**
     * Check if file is password protected
     */
    suspend fun isPasswordProtected(fileId: String): Result<Boolean>

    // === Analytics and Insights ===

    /**
     * Get storage usage statistics
     */
    suspend fun getStorageUsage(): Result<StorageUsage>

    /**
     * Get file type distribution
     */
    suspend fun getFileTypeDistribution(): Result<Map<FileType, Int>>

    /**
     * Get storage growth over time
     */
    suspend fun getStorageGrowth(period: DateRange): Result<List<StorageDataPoint>>

    /**
     * Get access patterns
     */
    suspend fun getAccessPatterns(period: DateRange): Result<List<AccessPattern>>

    /**
     * Get sharing analytics
     */
    suspend fun getSharingAnalytics(period: DateRange): Result<SharingAnalytics>

    // === Batch Operations ===

    /**
     * Batch upload files
     */
    suspend fun batchUploadFiles(
        files: List<BatchUploadItem>,
        parentId: String? = null,
        onProgress: (BatchOperationProgress) -> Unit = {}
    ): Flow<Result<BatchOperationProgress>>

    /**
     * Batch delete files
     */
    suspend fun batchDeleteFiles(fileIds: List<String>, permanent: Boolean = false): Result<BatchOperationResult>

    /**
     * Batch move files
     */
    suspend fun batchMoveFiles(fileIds: List<String>, newParentId: String): Result<BatchOperationResult>

    /**
     * Batch set tags
     */
    suspend fun batchSetTags(fileIds: List<String>, tags: List<String>): Result<BatchOperationResult>

    /**
     * Batch categorize files
     */
    suspend fun batchCategorizeFiles(fileIds: List<String>, category: FileCategory): Result<BatchOperationResult>

    // === Advanced Operations ===

    /**
     * Compress files/folders
     */
    suspend fun compressFiles(
        fileIds: List<String>,
        archiveName: String,
        format: ArchiveFormat,
        onProgress: (FileTransferProgress) -> Unit = {}
    ): Flow<Result<FileTransferProgress>>

    /**
     * Extract archive
     */
    suspend fun extractArchive(
        archiveId: String,
        destinationParentId: String,
        onProgress: (FileTransferProgress) -> Unit = {}
    ): Flow<Result<FileTransferProgress>>

    /**
     * Merge folders
     */
    suspend fun mergeFolders(
        sourceFolderId: String,
        targetFolderId: String,
        conflictResolution: ConflictResolution
    ): Result<FileNode>

    /**
     * Find similar files (content-based)
     */
    suspend fun findSimilarFiles(fileId: String, threshold: Float = 0.8): Result<List<FileNode>>

    /**
     * Detect duplicate content
     */
    suspend fun detectDuplicateContent(): Result<Map<String, List<FileNode>>>

    // === Cloud Storage Integration ===

    /**
     * Connect cloud storage provider
     */
    suspend fun connectCloudProvider(
        provider: CloudProvider,
        credentials: CloudCredentials
    ): Result<CloudConnection>

    /**
     * Disconnect cloud storage provider
     */
    suspend fun disconnectCloudProvider(provider: CloudProvider): Result<Unit>

    /**
     * Get connected cloud providers
     */
    suspend fun getConnectedCloudProviders(): Result<List<CloudConnection>>

    /**
     * Sync with specific cloud provider
     */
    suspend fun syncWithCloudProvider(provider: CloudProvider): Result<SyncResult>

    // === Real-time Updates ===

    /**
     * Subscribe to file changes
     */
    fun subscribeToFileChanges(): Flow<FileChangeEvent>

    /**
     * Subscribe to sync events
     */
    fun subscribeToSyncEvents(): Flow<SyncEvent>

    /**
     * Subscribe to sharing events
     */
    fun subscribeToSharingEvents(): Flow<SharingEvent>
}

// === Supporting Data Classes ===

/**
 * Sync result
 */
data class SyncResult(
    val success: Boolean,
    val syncedFiles: Int,
    val failedFiles: Int,
    val conflicts: List<SyncConflict>,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val bytesTransferred: Long
)

/**
 * Sync conflict
 */
data class SyncConflict(
    val fileId: String,
    val conflictType: ConflictType,
    val localVersion: FileVersion,
    val remoteVersion: FileVersion,
    val description: String
)

/**
 * Sync conflict types
 */
enum class ConflictType {
    MODIFICATION_CONFLICT,
    DELETION_CONFLICT,
    NAMING_CONFLICT,
    PERMISSION_CONFLICT
}

/**
 * Storage usage information
 */
data class StorageUsage(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val fileCount: Int,
    val folderCount: Int,
    val usageByType: Map<FileType, Long>,
    val usageByCategory: Map<FileCategory, Long>
)

/**
 * Storage data point for analytics
 */
data class StorageDataPoint(
    val date: LocalDateTime,
    val totalSize: Long,
    val fileCount: Int
)

/**
 * Access pattern
 */
data class AccessPattern(
    val fileId: String,
    val fileName: String,
    val accessCount: Int,
    val lastAccessed: LocalDateTime,
    val accessFrequency: AccessFrequency,
    val trendingScore: Float
)

/**
 * Access frequency
 */
enum class AccessFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    RARELY,
    NEVER
}

/**
 * Sharing analytics
 */
data class SharingAnalytics(
    val totalShares: Int,
    val publicShares: Int,
    val privateShares: Int,
    val downloadsCount: Int,
    val viewsCount: Int,
    val mostSharedFiles: List<FileShareStats>,
    val sharesByType: Map<ShareType, Int>
)

/**
 * File share statistics
 */
data class FileShareStats(
    val fileId: String,
    val fileName: String,
    val shareCount: Int,
    val downloadCount: Int,
    val viewCount: Int
)

/**
 * Batch upload item
 */
data class BatchUploadItem(
    val localPath: String,
    val name: String,
    val metadata: FileMetadata = FileMetadata()
)

/**
 * Batch operation progress
 */
data class BatchOperationProgress(
    val totalItems: Int,
    val completedItems: Int,
    val currentItem: String,
    val progress: Float,
    val errors: List<String>
)

/**
 * Batch operation result
 */
data class BatchOperationResult(
    val totalItems: Int,
    val successfulItems: Int,
    val failedItems: Int,
    val errors: Map<String, String> // itemId -> error message
)

/**
 * Archive format
 */
enum class ArchiveFormat {
    ZIP,
    RAR,
    TAR,
    GZIP,
    SEVEN_Z
}

/**
 * Cloud credentials
 */
data class CloudCredentials(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: LocalDateTime? = null,
    val additionalData: Map<String, String> = emptyMap()
)

/**
 * Cloud connection information
 */
data class CloudConnection(
    val provider: CloudProvider,
    val accountId: String,
    val accountName: String,
    val isConnected: Boolean,
    val lastSync: LocalDateTime? = null,
    val quota: CloudQuota? = null
)

/**
 * Cloud storage quota
 */
data class CloudQuota(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long
)

/**
 * File change event
 */
sealed class FileChangeEvent {
    data class FileCreated(val fileNode: FileNode) : FileChangeEvent()
    data class FileUpdated(val fileNode: FileNode, val changes: Map<String, Any>) : FileChangeEvent()
    data class FileDeleted(val fileId: String, val fileName: String) : FileChangeEvent()
    data class FileMoved(val fileId: String, val oldPath: String, val newPath: String) : FileChangeEvent()
    data class FileShared(val fileId: String, val shareInfo: ShareInfo) : FileChangeEvent()
}

/**
 * Sync event
 */
sealed class SyncEvent {
    data class SyncStarted(val fileIds: List<String>) : SyncEvent()
    data class SyncProgress(val progress: Float, val currentFile: String) : SyncEvent()
    data class SyncCompleted(val result: SyncResult) : SyncEvent()
    data class SyncError(val fileId: String, val error: String) : SyncEvent()
    data class SyncConflict(val conflict: SyncConflict) : SyncEvent()
}

/**
 * Sharing event
 */
sealed class SharingEvent {
    data class FileShared(val fileId: String, val shareInfo: ShareInfo) : SharingEvent()
    data class FileUnshared(val fileId: String, val shareId: String) : SharingEvent()
    data class ShareAccessed(val shareId: String, val accessor: String) : SharingEvent()
    data class ShareExpired(val shareId: String) : SharingEvent()
}