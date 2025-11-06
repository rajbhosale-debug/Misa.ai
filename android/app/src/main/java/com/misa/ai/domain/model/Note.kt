package com.misa.ai.domain.model

import java.time.LocalDateTime

/**
 * Note domain model
 * Represents a rich text note with various media attachments and metadata
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime,
    val tags: List<String> = emptyList(),
    val attachments: List<NoteAttachment> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isEncrypted: Boolean = false,
    val color: NoteColor? = null,
    val notebookId: String? = null,
    val reminderTime: LocalDateTime? = null,
    val linkedEvents: List<String> = emptyList(), // Calendar event IDs
    val linkedTasks: List<String> = emptyList(),  // Task IDs
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Check if note is encrypted
     */
    fun requiresDecryption(): Boolean = isEncrypted && content.startsWith("ENCRYPTED:")

    /**
     * Get content length
     */
    fun getContentLength(): Int = content.length

    /**
     * Check if note has attachments
     */
    fun hasAttachments(): Boolean = attachments.isNotEmpty()

    /**
     * Check if note has audio attachment
     */
    fun hasAudio(): Boolean = attachments.any { it.type == AttachmentType.AUDIO }

    /**
     * Check if note has image attachment
     */
    fun hasImages(): Boolean = attachments.any { it.type == AttachmentType.IMAGE }

    /**
     * Get word count
     */
    fun getWordCount(): Int {
        return content.split("\\s+".toRegex()).size
    }

    /**
     * Check if note is recent (created in last 24 hours)
     */
    fun isRecent(): Boolean {
        val now = LocalDateTime.now()
        return createdAt.isAfter(now.minusHours(24))
    }

    /**
     * Check if note is empty
     */
    fun isEmpty(): Boolean = content.isBlank() && attachments.isEmpty()

    /**
     * Get content preview (first 100 characters)
     */
    fun getContentPreview(maxLength: Int = 100): String {
        return if (content.length <= maxLength) {
            content
        } else {
            content.take(maxLength) + "..."
        }
    }

    /**
     * Check if note matches search query
     */
    fun matchesQuery(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return title.lowercase().contains(lowerQuery) ||
               content.lowercase().contains(lowerQuery) ||
               tags.any { it.lowercase().contains(lowerQuery) }
    }

    /**
     * Get formatted creation time
     */
    fun getFormattedCreationTime(): String {
        val now = LocalDateTime.now()
        return when {
            createdAt.isAfter(now.minusHours(1)) -> "Just now"
            createdAt.isAfter(now.minusDays(1)) -> "Today"
            createdAt.isAfter(now.minusDays(7)) -> createdAt.dayOfWeek.toString().take(3)
            else -> createdAt.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))
        }
    }
}

/**
 * Note attachment model
 */
data class NoteAttachment(
    val id: String,
    val name: String,
    val type: AttachmentType,
    val size: Long,
    val localPath: String? = null,
    val cloudUrl: String? = null,
    val thumbnailPath: String? = null,
    val mimeType: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Get formatted file size
     */
    fun getFormattedSize(): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> "%.1f GB".format(gb)
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.0f KB".format(kb)
            else -> "$size B"
        }
    }

    /**
     * Check if attachment is an image
     */
    fun isImage(): Boolean = type == AttachmentType.IMAGE

    /**
     * Check if attachment is audio
     */
    fun isAudio(): Boolean = type == AttachmentType.AUDIO

    /**
     * Check if attachment is video
     */
    fun isVideo(): Boolean = type == AttachmentType.VIDEO

    /**
     * Check if attachment is document
     */
    fun isDocument(): Boolean = type == AttachmentType.DOCUMENT

    /**
     * Get file extension
     */
    fun getFileExtension(): String {
        return name.substringAfterLast(".", "")
    }
}

/**
 * Notebook model for organizing notes
 */
data class Notebook(
    val id: String,
    val name: String,
    val description: String? = null,
    val color: NotebookColor,
    val icon: String? = null,
    val isDefault: Boolean = false,
    val createdAt: LocalDateTime,
    val noteCount: Int = 0,
    val tags: Set<String> = emptySet(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Attachment types
 */
enum class AttachmentType {
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    PDF,
    OTHER
}

/**
 * Note color themes
 */
enum class NoteColor(val colorCode: String, val colorName: String) {
    DEFAULT("#FFFFFF", "Default"),
    YELLOW("#FEF9C7", "Yellow"),
    ORANGE("#FFE0B2", "Orange"),
    RED("#FFCDD2", "Red"),
    PINK("#FCE4EC", "Pink"),
    PURPLE("#E1BEE7", "Purple"),
    DEEP_PURPLE("#D1C4E9", "Deep Purple"),
    INDIGO("#C5CAE9", "Indigo"),
    BLUE("#BBDEFB", "Blue"),
    LIGHT_BLUE("#B3E5FC", "Light Blue"),
    CYAN("#B2EBF2", "Cyan"),
    TEAL("#B2DFDB", "Teal"),
    GREEN("#C8E6C9", "Green"),
    LIGHT_GREEN("#DCEDC8", "Light Green"),
    LIME("#F0F4C3", "Lime"),
    AMBER("#FFE0B2", "Amber"),
    BROWN("#D7CCC8", "Brown"),
    GREY("#F5F5F5", "Grey"),
    BLUE_GREY("#ECEFF1", "Blue Grey")
}

/**
 * Notebook color themes
 */
enum class NotebookColor(val colorCode: String, val colorName: String) {
    DEFAULT("#2196F3", "Blue"),
    RED("#F44336", "Red"),
    GREEN("#4CAF50", "Green"),
    ORANGE("#FF9800", "Orange"),
    PURPLE("#9C27B0", "Purple"),
    TEAL("#009688", "Teal"),
    BROWN("#795548", "Brown"),
    GREY("#607D8B", "Grey")
}

/**
 * Note search filters
 */
data class NoteSearchFilter(
    val query: String = "",
    val tags: List<String> = emptyList(),
    val notebookId: String? = null,
    val hasAttachments: Boolean? = null,
    val isPinned: Boolean? = null,
    val dateRange: DateRange? = null,
    val attachmentType: AttachmentType? = null
)

/**
 * Date range for filtering
 */
data class DateRange(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

/**
 * Note export formats
 */
enum class NoteExportFormat {
    MARKDOWN,
    TEXT,
    PDF,
    HTML,
    JSON
}

/**
 * Note import formats
 */
enum class NoteImportFormat {
    MARKDOWN,
    TEXT,
    EVERNOTE_ENEX,
    JSON
}

/**
 * Note sort options
 */
enum class NoteSortField {
    MODIFIED_DATE,
    CREATED_DATE,
    TITLE,
    SIZE
}

/**
 * Note sort order
 */
enum class NoteSortOrder {
    ASCENDING,
    DESCENDING
}

/**
 * Note statistics
 */
data class NoteStatistics(
    val totalNotes: Int,
    val totalWords: Int,
    val totalCharacters: Int,
    val totalAttachments: Int,
    val totalSize: Long,
    val notesByNotebook: Map<String, Int>,
    val notesByTag: Map<String, Int>,
    val notesByDate: Map<LocalDateTime, Int>,
    val mostUsedTags: List<TagUsage>,
    val largestNotes: List<NoteSize>
)

/**
 * Tag usage statistics
 */
data class TagUsage(
    val tag: String,
    val count: Int,
    val lastUsed: LocalDateTime
)

/**
 * Note size statistics
 */
data class NoteSize(
    val noteId: String,
    val title: String,
    val size: Long,
    val wordCount: Int
)

/**
 * Note sharing options
 */
data class NoteShareOptions(
    val isPublic: Boolean = false,
    val shareLink: String? = null,
    val allowedUsers: List<String> = emptyList(),
    val permissions: SharePermissions = SharePermissions.READ,
    val expiresAt: LocalDateTime? = null
)

/**
 * Share permissions
 */
enum class SharePermissions {
    READ,
    COMMENT,
    EDIT
}