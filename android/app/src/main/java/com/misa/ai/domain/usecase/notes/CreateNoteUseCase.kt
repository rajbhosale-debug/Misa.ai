package com.misa.ai.domain.usecase.notes

import com.misa.ai.domain.model.*
import com.misa.ai.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for creating notes
 * Handles validation, AI-powered suggestions, and intelligent note creation
 */
class CreateNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val aiNoteProcessor: AINoteProcessor
) {

    /**
     * Create a new note with validation and AI enhancements
     */
    suspend fun createNote(
        title: String,
        content: String,
        notebookId: String? = null,
        tags: List<String> = emptyList(),
        attachments: List<NoteAttachment> = emptyList(),
        enableAISuggestions: Boolean = true
    ): Result<Note> {
        try {
            // Validate note data
            val validationResult = validateNoteData(title, content)
            if (!validationResult.isValid) {
                return Result.failure(Exception(validationResult.errorMessage))
            }

            // Generate note ID
            val noteId = generateNoteId()
            val now = LocalDateTime.now()

            // Apply AI suggestions if enabled
            var enhancedTitle = title
            var enhancedContent = content
            var enhancedTags = tags.toMutableList()

            if (enableAISuggestions) {
                // AI-powered title suggestion if title is empty
                if (title.isBlank()) {
                    enhancedTitle = aiNoteProcessor.generateTitleFromContent(content).getOrDefault("Untitled Note")
                }

                // AI-powered content enhancement
                val contentSuggestions = aiNoteProcessor.enhanceContent(content)
                if (contentSuggestions.isNotEmpty()) {
                    enhancedContent = content + "\n\n" + contentSuggestions.joinToString("\n")
                }

                // AI-powered tag suggestions
                val suggestedTags = aiNoteProcessor.extractTags(content).getOrDefault(emptyList())
                enhancedTags.addAll(suggestedTags.filter { it !in enhancedTags })
            }

            // Create note object
            val note = Note(
                id = noteId,
                title = enhancedTitle,
                content = enhancedContent,
                createdAt = now,
                modifiedAt = now,
                tags = enhancedTags.distinct(),
                attachments = attachments,
                notebookId = notebookId,
                metadata = mapOf(
                    "created_with_ai" to enableAISuggestions,
                    "original_title" to title,
                    "original_content_length" to content.length
                )
            )

            // Save note
            val result = noteRepository.createNote(note)

            // Post-creation AI processing (background)
            if (enableAISuggestions) {
                try {
                    // Generate summary in background
                    val summary = aiNoteProcessor.summarizeContent(enhancedContent)
                    summary.onSuccess { summaryText ->
                        // Note: In a real implementation, you might update the note with the summary
                        // or store it as metadata
                    }

                    // Find related notes
                    val relatedNotes = noteRepository.findRelatedNotes(noteId, 3)
                    relatedNotes.onSuccess { related ->
                        // Store related note IDs as metadata
                    }
                } catch (e: Exception) {
                    // AI processing failures shouldn't break note creation
                }
            }

            return result
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create note from voice input
     */
    suspend fun createNoteFromVoice(
        transcribedText: String,
        notebookId: String? = null,
        enableAISuggestions: Boolean = true
    ): Result<Note> {
        try {
            // Process voice transcription with AI
            val processedContent = if (enableAISuggestions) {
                aiNoteProcessor.processVoiceTranscription(transcribedText).getOrDefault(transcribedText)
            } else {
                transcribedText
            }

            // Extract title from first sentence or use AI to generate
            val title = extractTitleFromContent(processedContent)
            val content = processedContent

            return createNote(title, content, notebookId, emptyList(), emptyList(), enableAISuggestions)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create note from image (OCR + AI analysis)
     */
    suspend fun createNoteFromImage(
        imagePath: String,
        notebookId: String? = null,
        enableAISuggestions: Boolean = true
    ): Result<Note> {
        try {
            // Extract text from image using OCR
            val extractedText = aiNoteProcessor.extractTextFromImage(imagePath).getOrThrow()

            // Analyze image content with AI
            val analysis = if (enableAISuggestions) {
                aiNoteProcessor.analyzeImageContent(imagePath, extractedText).getOrThrow()
            } else {
                ImageAnalysis(extractedText, emptyList(), "Unknown image")
            }

            // Generate title from analysis
            val title = analysis.title.ifBlank {
                generateTitleFromImageAnalysis(analysis)
            }

            // Combine extracted text with AI analysis
            val content = buildString {
                appendLine(extractedText)
                if (analysis.description.isNotBlank()) {
                    appendLine()
                    appendLine("AI Analysis:")
                    appendLine(analysis.description)
                }
                if (analysis.detectedObjects.isNotEmpty()) {
                    appendLine()
                    appendLine("Detected Objects:")
                    analysis.detectedObjects.forEach { obj ->
                        appendLine("• $obj")
                    }
                }
                if (analysis.suggestedTags.isNotEmpty()) {
                    appendLine()
                    appendLine("Suggested Tags:")
                    analysis.suggestedTags.forEach { tag ->
                        appendLine("• $tag")
                    }
                }
            }

            // Create attachments
            val attachment = NoteAttachment(
                id = generateAttachmentId(),
                name = "Image_${LocalDateTime.now().toString().replace(":", "-")}",
                type = AttachmentType.IMAGE,
                size = java.io.File(imagePath).length(),
                localPath = imagePath,
                mimeType = "image/jpeg"
            )

            val tags = if (enableAISuggestions) {
                analysis.suggestedTags
            } else {
                emptyList()
            }

            return createNote(title, content, notebookId, tags, listOf(attachment), enableAISuggestions)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create quick note (minimal input, maximum AI assistance)
     */
    suspend fun createQuickNote(
        input: String,
        notebookId: String? = null
    ): Result<Note> {
        try {
            // Let AI determine if this is title, content, or both
            val processedInput = aiNoteProcessor.processQuickInput(input).getOrThrow()

            return createNote(
                title = processedInput.title,
                content = processedInput.content,
                notebookId = notebookId,
                tags = processedInput.suggestedTags,
                enableAISuggestions = true
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create note with reminder
     */
    suspend fun createNoteWithReminder(
        title: String,
        content: String,
        reminderTime: LocalDateTime,
        notebookId: String? = null,
        tags: List<String> = emptyList()
    ): Result<Note> {
        val note = Note(
            id = generateNoteId(),
            title = title,
            content = content,
            createdAt = LocalDateTime.now(),
            modifiedAt = LocalDateTime.now(),
            tags = tags,
            notebookId = notebookId,
            reminderTime = reminderTime,
            metadata = mapOf("has_reminder" to true)
        )

        return noteRepository.createNote(note)
    }

    /**
     * Validate note data
     */
    private fun validateNoteData(title: String, content: String): ValidationResult {
        if (title.isBlank() && content.isBlank()) {
            return ValidationResult(false, "Note must have either a title or content")
        }

        if (title.length > 200) {
            return ValidationResult(false, "Title is too long (max 200 characters)")
        }

        if (content.length > 100000) {
            return ValidationResult(false, "Content is too long (max 100,000 characters)")
        }

        return ValidationResult(true)
    }

    /**
     * Extract title from content
     */
    private fun extractTitleFromContent(content: String): String {
        val lines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return if (lines.isNotEmpty()) {
            val firstLine = lines[0]
            if (firstLine.length <= 100) {
                firstLine
            } else {
                firstLine.take(97) + "..."
            }
        } else {
            "Untitled Note"
        }
    }

    /**
     * Generate title from image analysis
     */
    private fun generateTitleFromImageAnalysis(analysis: ImageAnalysis): String {
        return when {
            analysis.title.isNotBlank() -> analysis.title
            analysis.detectedObjects.isNotEmpty() -> "Photo: ${analysis.detectedObjects.first()}"
            analysis.description.isNotBlank() -> "Note from image"
            else -> "Image Note"
        }
    }

    /**
     * Generate unique note ID
     */
    private fun generateNoteId(): String {
        return "note_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * Generate unique attachment ID
     */
    private fun generateAttachmentId(): String {
        return "attachment_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)

/**
 * AI Note Processor interface for AI-powered note operations
 */
interface AINoteProcessor {
    suspend fun generateTitleFromContent(content: String): Result<String>
    suspend fun enhanceContent(content: String): List<String>
    suspend fun extractTags(content: String): Result<List<String>>
    suspend fun processVoiceTranscription(transcription: String): Result<String>
    suspend fun extractTextFromImage(imagePath: String): Result<String>
    suspend fun analyzeImageContent(imagePath: String, extractedText: String): Result<ImageAnalysis>
    suspend fun processQuickInput(input: String): Result<QuickNoteProcessResult>
    suspend fun summarizeContent(content: String): Result<String>
}

/**
 * Image analysis result
 */
data class ImageAnalysis(
    val title: String,
    val description: String,
    val detectedObjects: List<String>,
    val suggestedTags: List<String>,
    val confidence: Double
)

/**
 * Quick note processing result
 */
data class QuickNoteProcessResult(
    val title: String,
    val content: String,
    val suggestedTags: List<String>,
    val detectedIntent: NoteIntent
)

/**
 * Detected intent from quick input
 */
enum class NoteIntent {
    TITLE_ONLY,
    CONTENT_ONLY,
    TITLE_AND_CONTENT,
    REMINDER,
    TASK,
    IDEA,
    REFERENCE
}