package com.misa.ai.ai.core

import android.content.Context
import com.misa.ai.ai.ocr.OCREngine
import com.misa.ai.ai.usecase.notes.*
import com.misa.ai.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Note Processor implementation
 * Provides AI-powered features for note creation and processing
 */
@Singleton
class AINoteProcessorImpl @Inject constructor(
    private val context: Context,
    private val ocrEngine: OCREngine,
    private val nlpProcessor: NLPProcessor
) : AINoteProcessor {

    override suspend fun generateTitleFromContent(content: String): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val title = nlpProcessor.extractTitleFromContent(content)
                Result.success(title)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun enhanceContent(content: String): List<String> {
        return withContext(Dispatchers.Default) {
            val suggestions = mutableListOf<String>()

            try {
                // Extract action items
                val actionItems = nlpProcessor.extractActionItems(content)
                suggestions.addAll(actionItems.map { "• $it" })

                // Extract questions
                val questions = nlpProcessor.extractQuestions(content)
                suggestions.addAll(questions.map { "Question: $it" })

                // Extract key insights
                val insights = nlpProcessor.extractInsights(content)
                suggestions.addAll(insights.map { "• $it" })

                // Suggest related topics
                val topics = nlpProcessor.extractTopics(content)
                if (topics.isNotEmpty()) {
                    suggestions.add("Related topics: ${topics.joinToString(", ")}")
                }
            } catch (e: Exception) {
                // Return empty list if AI processing fails
            }

            suggestions
        }
    }

    override suspend fun extractTags(content: String): Result<List<String>> {
        return withContext(Dispatchers.Default) {
            try {
                val tags = nlpProcessor.extractTags(content)
                Result.success(tags)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun processVoiceTranscription(transcription: String): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val processed = nlpProcessor.processVoiceTranscription(transcription)
                Result.success(processed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun extractTextFromImage(imagePath: String): Result<String> {
        return ocrEngine.extractTextFromImage(android.net.Uri.parse(imagePath))
    }

    override suspend fun analyzeImageContent(
        imagePath: String,
        extractedText: String
    ): Result<ImageAnalysis> {
        return withContext(Dispatchers.Default) {
            try {
                // Use AI to analyze both the OCR text and image
                val title = nlpProcessor.generateImageTitle(extractedText)
                val description = nlpProcessor.generateImageDescription(extractedText)
                val detectedObjects = nlpProcessor.detectObjectsInText(extractedText)
                val suggestedTags = nlpProcessor.suggestImageTags(extractedText)

                val analysis = ImageAnalysis(
                    title = title,
                    description = description,
                    detectedObjects = detectedObjects,
                    suggestedTags = suggestedTags,
                    confidence = 0.8
                )

                Result.success(analysis)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun processQuickInput(input: String): Result<QuickNoteProcessResult> {
        return withContext(Dispatchers.Default) {
            try {
                val intent = detectIntent(input)
                val processed = when (intent) {
                    NoteIntent.TITLE_ONLY -> {
                        QuickNoteProcessResult(
                            title = input.trim(),
                            content = "",
                            suggestedTags = emptyList(),
                            detectedIntent = intent
                        )
                    }
                    NoteIntent.CONTENT_ONLY -> {
                        QuickNoteProcessResult(
                            title = "",
                            content = input.trim(),
                            suggestedTags = nlpProcessor.extractTags(input),
                            detectedIntent = intent
                        )
                    }
                    NoteIntent.TITLE_AND_CONTENT -> {
                        val parts = splitTitleAndContent(input)
                        QuickNoteProcessResult(
                            title = parts.first,
                            content = parts.second,
                            suggestedTags = nlpProcessor.extractTags(input),
                            detectedIntent = intent
                        )
                    }
                    NoteIntent.REMINDER -> {
                        val parts = splitTitleAndContent(input)
                        QuickNoteProcessResult(
                            title = "Reminder: ${parts.first}",
                            content = parts.second,
                            suggestedTags = listOf("reminder", "important"),
                            detectedIntent = intent
                        )
                    }
                    else -> {
                        QuickNoteProcessResult(
                            title = input.trim(),
                            content = "",
                            suggestedTags = emptyList(),
                            detectedIntent = intent
                        )
                    }
                }

                Result.success(processed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun summarizeContent(content: String): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val summary = nlpProcessor.summarizeText(content)
                Result.success(summary)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun detectIntent(input: String): NoteIntent {
        val lowerInput = input.lowercase()

        return when {
            // Reminder intent patterns
            lowerInput.startsWith("remind") ||
            lowerInput.startsWith("don't forget") ||
            lowerInput.contains("reminder") -> NoteIntent.REMINDER

            // Task intent patterns
            lowerInput.startsWith("todo") ||
            lowerInput.startsWith("task") ||
            lowerInput.contains("need to") -> NoteIntent.TASK

            // Idea intent patterns
            lowerInput.startsWith("idea") ||
            lowerInput.startsWith("brainstorm") ||
            lowerInput.contains("what if") -> NoteIntent.IDEA

            // Reference intent patterns
            lowerInput.startsWith("reference") ||
            lowerInput.startsWith("link") ||
            lowerInput.contains("see also") -> NoteIntent.REFERENCE

            // Title and content patterns (contains colon or dash)
            input.contains(":") ||
            input.contains("-") ||
            (input.length > 50 && input.contains(".")) -> NoteIntent.TITLE_AND_CONTENT

            // Short input likely title only
            input.length < 20 && !input.contains(".") -> NoteIntent.TITLE_ONLY

            // Longer input likely content only
            else -> NoteIntent.CONTENT_ONLY
        }
    }

    private fun splitTitleAndContent(input: String): Pair<String, String> {
        val separators = listOf(":", "-", "|", "->")

        for (separator in separators) {
            if (input.contains(separator)) {
                val parts = input.split(separator, limit = 2)
                if (parts.size == 2) {
                    return Pair(parts[0].trim(), parts[1].trim())
                }
            }
        }

        // If no clear separator, split on first sentence
        val firstSentenceEnd = input.indexOfAny('.', '!', '?')
        return if (firstSentenceEnd > 0 && firstSentenceEnd < input.length - 1) {
            Pair(input.substring(0, firstSentenceEnd + 1), input.substring(firstSentenceEnd + 1).trim())
        } else {
            Pair(input.substring(0, input.length.coerceAtMost(50)), input.substring(input.length.coerceAtMost(50)).trim())
        }
    }
}

/**
 * NLP Processor for natural language processing
 */
interface NLPProcessor {
    suspend fun extractTitleFromContent(content: String): String
    suspend fun extractActionItems(content: String): List<String>
    suspend fun extractQuestions(content: String): List<String>
    suspend fun extractInsights(content: String): List<String>
    suspend fun extractTopics(content: String): List<String>
    suspend fun extractTags(content: String): List<String>
    suspend fun processVoiceTranscription(transcription: String): String
    suspend fun generateImageTitle(extractedText: String): String
    suspend fun generateImageDescription(extractedText: String): String
    suspend fun detectObjectsInText(text: String): List<String>
    suspend fun suggestImageTags(text: String): List<String>
    suspend fun summarizeText(text: String): String
}

/**
 * Simple NLP Processor implementation
 * In a production app, this would use more sophisticated NLP libraries or AI services
 */
class SimpleNLPProcessor : NLPProcessor {

    override suspend fun extractTitleFromContent(content: String): String {
        val lines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return if (lines.isNotEmpty()) {
            val firstLine = lines[0]
            if (firstLine.length <= 100) firstLine else firstLine.take(97) + "..."
        } else {
            "Untitled Note"
        }
    }

    override suspend fun extractActionItems(content: String): List<String> {
        val actionWords = listOf("todo", "need to", "should", "must", "will", "plan to", "going to")
        val lines = content.split("\n")

        return lines.filter { line ->
            actionWords.any { word ->
                line.lowercase().contains(word)
            }
        }.map { it.trim() }
    }

    override suspend fun extractQuestions(content: String): List<String> {
        val lines = content.split("\n")

        return lines.filter { line ->
            line.contains("?") &&
            (line.contains("what") || line.contains("how") || line.contains("why") || line.contains("when"))
        }.map { it.trim() }
    }

    override suspend fun extractInsights(content: String): List<String> {
        val insightWords = listOf("important", "key", "main", "critical", "essential", "crucial")

        return content.split(". ")
            .filter { sentence ->
                insightWords.any { word ->
                    sentence.lowercase().contains(word)
                }
            }
            .map { it.trim() + "." }
    }

    override suspend fun extractTopics(content: String): List<String> {
        val commonTopics = listOf("work", "project", "meeting", "idea", "plan", "goal", "task", "deadline", "review", "summary")

        return commonTopics.filter { topic ->
            content.lowercase().contains(topic)
        }.distinct()
    }

    override suspend fun extractTags(content: String): List<String> {
        val lines = content.lowercase()
        val tags = mutableSetOf<String>()

        // Extract hashtags
        val hashtagPattern = Regex("#\\w+")
        hashtags.findAllIn(lines).forEach { tags.add(it.value.drop(1)) }

        // Extract keywords from common patterns
        val commonTags = listOf(
            "urgent", "important", "meeting", "project", "deadline",
            "idea", "todo", "follow-up", "review", "action-item"
        )

        commonTags.forEach { tag ->
            if (lines.contains(tag)) {
                tags.add(tag)
            }
        }

        return tags.toList()
    }

    override suspend fun processVoiceTranscription(transcription: String): String {
        // Clean up common voice transcription issues
        return transcription
            .replace(Regex("\\b(um|uh|ah)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .split(". ")
            .joinToString(". ") { it.trim() }
            .trim()
            .replaceFirst { it.uppercase() + it.substring(1) }
    }

    override suspend fun generateImageTitle(extractedText: String): String {
        val words = extractedText.split(" ").take(5)
        return if (words.isNotEmpty()) {
            words.joinToString(" ") { it.capitalize() }
        } else {
            "Image Note"
        }
    }

    override suspend fun generateImageDescription(extractedText: String): String {
        return when {
            extractedText.isBlank() -> "No text detected in image"
            extractedText.length < 20 -> extractedText
            else -> extractedText.take(100) + "..."
        }
    }

    override suspend fun detectObjectsInText(text: String): List<String> {
        val commonObjects = listOf("document", "chart", "diagram", "table", "whiteboard", "screenshot", "photo", "calendar", "schedule", "list")

        return commonObjects.filter { obj ->
            text.lowercase().contains(obj)
        }
    }

    override suspend fun suggestImageTags(text: String): List<String> {
        val tags = mutableSetOf<String>()

        // Suggest tags based on content
        if (text.contains("meeting")) tags.add("meeting")
        if (text.contains("project")) tags.add("project")
        if (text.contains("deadline")) tags.add("deadline")
        if (text.contains("todo")) tags.add("todo")
        if (text.contains("list")) tags.add("list")

        return tags.toList()
    }

    override suspend fun summarizeText(text: String): String {
        val sentences = text.split(". ").filter { it.isNotBlank() }

        return when {
            sentences.size <= 3 -> text
            sentences.size <= 5 -> sentences.take(3).joinToString(". ") + "."
            else -> {
                val firstThree = sentences.take(3)
                val summary = firstThree.joinToString(". ") + "."
                summary + "\n\n(Note: This is a partial summary of ${sentences.size} sentences)"
            }
        }
    }
}