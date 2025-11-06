package com.misa.ai.domain.repository

import com.misa.ai.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Note repository interface
 * Defines the contract for note data operations
 */
interface NoteRepository {

    /**
     * Get all notes with optional filtering
     */
    fun getNotes(filter: NoteSearchFilter = NoteSearchFilter()): Flow<Result<List<Note>>>

    /**
     * Get a specific note by ID
     */
    suspend fun getNoteById(noteId: String): Result<Note?>

    /**
     * Create a new note
     */
    suspend fun createNote(note: Note): Result<Note>

    /**
     * Update an existing note
     */
    suspend fun updateNote(note: Note): Result<Note>

    /**
     * Delete a note
     */
    suspend fun deleteNote(noteId: String): Result<Unit>

    /**
     * Get notebooks
     */
    fun getNotebooks(): Flow<Result<List<Notebook>>>

    /**
     * Create a new notebook
     */
    suspend fun createNotebook(notebook: Notebook): Result<Notebook>

    /**
     * Update a notebook
     */
    suspend fun updateNotebook(notebook: Notebook): Result<Notebook>

    /**
     * Delete a notebook
     */
    suspend fun deleteNotebook(notebookId: String): Result<Unit>

    /**
     * Search notes
     */
    suspend fun searchNotes(query: String, filter: NoteSearchFilter = NoteSearchFilter()): Result<List<Note>>

    /**
     * Get notes by tag
     */
    fun getNotesByTag(tag: String): Flow<Result<List<Note>>>

    /**
     * Get notes by notebook
     */
    fun getNotesByNotebook(notebookId: String): Flow<Result<List<Note>>>

    /**
     * Get pinned notes
     */
    fun getPinnedNotes(): Flow<Result<List<Note>>>

    /**
     * Get recent notes (last 24 hours)
     */
    fun getRecentNotes(limit: Int = 10): Flow<Result<List<Note>>)

    /**
     * Get archived notes
     */
    fun getArchivedNotes(): Flow<Result<List<Note>>>

    /**
     * Pin/unpin note
     */
    suspend fun pinNote(noteId: String): Result<Unit>
    suspend fun unpinNote(noteId: String): Result<Unit>

    /**
     * Archive/unarchive note
     */
    suspend fun archiveNote(noteId: String): Result<Unit>
    suspend fun unarchiveNote(noteId: String): Result<Unit>

    /**
     * Add tag to note
     */
    suspend fun addTagToNote(noteId: String, tag: String): Result<Unit>

    /**
     * Remove tag from note
     */
    suspend fun removeTagFromNote(noteId: String, tag: String): Result<Unit>

    /**
     * Get all tags
     */
    fun getAllTags(): Flow<Result<List<String>>>

    /**
     * Add attachment to note
     */
    suspend fun addAttachment(noteId: String, attachment: NoteAttachment): Result<NoteAttachment>

    /**
     * Remove attachment from note
     */
    suspend fun removeAttachment(noteId: String, attachmentId: String): Result<Unit>

    /**
     * Get attachment by ID
     */
    suspend fun getAttachmentById(attachmentId: String): Result<NoteAttachment?>

    /**
     * Sync notes with cloud storage
     */
    suspend fun syncNotes(): Result<SyncResult>

    /**
     * Import notes from file
     */
    suspend fun importNotes(
        filePath: String,
        format: NoteImportFormat,
        notebookId: String? = null
    ): Result<ImportResult>

    /**
     * Export notes to file
     */
    suspend fun exportNotes(
        notes: List<Note>,
        filePath: String,
        format: NoteExportFormat
    ): Result<Unit>

    /**
     * Get note statistics
     */
    suspend fun getNoteStatistics(): Result<NoteStatistics>

    /**
     * Auto-summarize note content
     */
    suspend fun summarizeNote(noteId: String): Result<String>

    /**
     * Extract keywords/tags from note content
     */
    suspend fun extractKeywords(noteId: String): Result<List<String>>

    /**
     * Find related notes based on content similarity
     */
    suspend fun findRelatedNotes(noteId: String, limit: Int = 5): Result<List<Note>>

    /**
     * Voice-to-text transcription
     */
    suspend fun transcribeAudio(noteId: String, audioPath: String): Result<String>

    /**
     * Batch operations
     */
    suspend fun batchCreateNotes(notes: List<Note>): Result<List<Note>>
    suspend fun batchUpdateNotes(notes: List<Note>): Result<List<Note>>
    suspend fun batchDeleteNotes(noteIds: List<String>): Result<Unit>

    /**
     * Share note
     */
    suspend fun shareNote(noteId: String, options: NoteShareOptions): Result<String>

    /**
     * Get shared notes
     */
    fun getSharedNotes(): Flow<Result<List<Note>>>

    /**
     * Subscribe to real-time updates
     */
    fun subscribeToNoteUpdates(): Flow<NoteUpdate>
}

/**
 * Sync result
 */
data class SyncResult(
    val success: Boolean,
    val notesCreated: Int,
    val notesUpdated: Int,
    val notesDeleted: Int,
    val attachmentsCreated: Int,
    val attachmentsUpdated: Int,
    val errors: List<String>,
    val lastSyncTime: LocalDateTime
)

/**
 * Import result
 */
data class ImportResult(
    val success: Boolean,
    val notesImported: Int,
    val notebooksCreated: Int,
    val attachmentsImported: Int,
    val errors: List<ImportError>
)

data class ImportError(
    val line: Int,
    val message: String,
    val rawData: String? = null
)

/**
 * Note update event
 */
sealed class NoteUpdate {
    data class NoteCreated(val note: Note) : NoteUpdate()
    data class NoteUpdated(val note: Note) : NoteUpdate()
    data class NoteDeleted(val noteId: String) : NoteUpdate()
    data class NotePinned(val noteId: String) : NoteUpdate()
    data class NoteUnpinned(val noteId: String) : NoteUpdate()
    data class NoteArchived(val noteId: String) : NoteUpdate()
    data class NoteUnarchived(val noteId: String) : NoteUpdate()
    data class NotebookCreated(val notebook: Notebook) : NoteUpdate()
    data class NotebookUpdated(val notebook: Notebook) : NoteUpdate()
    data class NotebookDeleted(val notebookId: String) : NoteUpdate()
}

/**
 * Note search suggestion
 */
data class NoteSearchSuggestion(
    val query: String,
    val type: SuggestionType,
    val relevanceScore: Double
)

enum class SuggestionType {
    RECENT_SEARCH,
    TAG,
    NOTEBOOK,
    CONTENT
}