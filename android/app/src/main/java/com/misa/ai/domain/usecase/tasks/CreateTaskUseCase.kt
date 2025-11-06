package com.misa.ai.domain.usecase.tasks

import com.misa.ai.domain.model.*
import com.misa.ai.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for creating tasks
 * Handles validation, AI-powered suggestions, and intelligent task creation
 */
class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val aiTaskProcessor: AITaskProcessor
) {

    /**
     * Create a new task with validation and AI enhancements
     */
    suspend fun createTask(
        title: String,
        description: String? = null,
        priority: TaskPriority = TaskPriority.NORMAL,
        projectId: String? = null,
        assigneeId: String? = null,
        dueDate: LocalDateTime? = null,
        estimatedDuration: kotlin.time.Duration? = null,
        tags: List<String> = emptyList(),
        enableAISuggestions: Boolean = true
    ): Result<Task> {
        try {
            // Validate task data
            val validationResult = validateTaskData(title, description, dueDate)
            if (!validationResult.isValid) {
                return Result.failure(Exception(validationResult.errorMessage))
            }

            // Generate task ID
            val taskId = generateTaskId()
            val now = LocalDateTime.now()

            // Apply AI suggestions if enabled
            var enhancedTitle = title
            var enhancedDescription = description
            var enhancedPriority = priority
            var enhancedDueDate = dueDate
            var enhancedEstimatedDuration = estimatedDuration
            var enhancedTags = tags.toMutableList()

            if (enableAISuggestions) {
                // AI-powered title enhancement
                val titleSuggestions = aiTaskProcessor.enhanceTaskTitle(title, description ?: "")
                if (titleSuggestions.isNotEmpty()) {
                    enhancedTitle = titleSuggestions.first()
                }

                // AI-powered description expansion
                if (description != null && description.isNotBlank()) {
                    val descriptionEnhancements = aiTaskProcessor.expandTaskDescription(description)
                    if (descriptionEnhancements.isNotEmpty()) {
                        enhancedDescription = description + "\n\nAdditional considerations:\n" +
                            descriptionEnhancements.joinToString("\n")
                    }
                }

                // AI-powered priority suggestion
                val suggestedPriority = aiTaskProcessor.suggestTaskPriority(title, description ?: "", dueDate)
                if (suggestedPriority != null) {
                    enhancedPriority = suggestedPriority
                }

                // AI-powered duration estimation
                if (estimatedDuration == null) {
                    val estimatedEffort = aiTaskProcessor.estimateTaskEffort(title, description ?: "")
                    enhancedEstimatedDuration = estimatedEffort?.let {
                        kotlin.time.Duration.parse("PT${it.hours}H")
                    }
                }

                // AI-powered tag suggestions
                val suggestedTags = aiTaskProcessor.extractTaskTags(title, description ?: "")
                enhancedTags.addAll(suggestedTags.filter { it !in enhancedTags })

                // AI-powered due date suggestion
                if (dueDate == null) {
                    val suggestedDate = aiTaskProcessor.suggestDueDate(title, enhancedPriority, enhancedEstimatedDuration)
                    enhancedDueDate = suggestedDate
                }
            }

            // Create task object
            val task = Task(
                id = taskId,
                title = enhancedTitle,
                description = enhancedDescription,
                status = TaskStatus.TODO,
                priority = enhancedPriority,
                createdAt = now,
                updatedAt = now,
                dueDate = enhancedDueDate,
                estimatedDuration = enhancedEstimatedDuration,
                assigneeId = assigneeId,
                projectId = projectId,
                tags = enhancedTags.distinct(),
                metadata = mapOf(
                    "created_with_ai" to enableAISuggestions,
                    "original_title" to title,
                    "original_priority" to priority.name,
                    "ai_enhanced" to enableAISuggestions
                )
            )

            // Save task
            val result = taskRepository.createTask(task)

            // Post-creation AI processing (background)
            if (enableAISuggestions) {
                try {
                    // Suggest task decomposition if complex
                    aiTaskProcessor.shouldDecomposeTask(task).onSuccess { shouldDecompose ->
                        if (shouldDecompose) {
                            val subtasks = aiTaskProcessor.decomposeTask(task)
                            subtasks.onSuccess { suggestedSubtasks ->
                                // Store decomposition suggestions as metadata
                                // In a real implementation, you might ask the user to confirm
                            }
                        }
                    }

                    // Find related tasks
                    val relatedTasks = taskRepository.searchTasks(title, TaskFilter())
                    relatedTasks.onSuccess { related ->
                        // Store related task IDs as metadata for potential linking
                    }
                } catch (e: Exception) {
                    // AI processing failures shouldn't break task creation
                }
            }

            return result
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create task from natural language input
     */
    suspend fun createTaskFromNaturalLanguage(
        input: String,
        projectId: String? = null,
        enableAISuggestions: Boolean = true
    ): Result<Task> {
        try {
            // Parse natural language input with AI
            val parsedTask = if (enableAISuggestions) {
                aiTaskProcessor.parseNaturalLanguageTask(input).getOrThrow()
            } else {
                ParsedTask(
                    title = input,
                    description = null,
                    priority = TaskPriority.NORMAL,
                    dueDate = null,
                    estimatedDuration = null,
                    tags = emptyList(),
                    assigneeId = null
                )
            }

            return createTask(
                title = parsedTask.title,
                description = parsedTask.description,
                priority = parsedTask.priority,
                projectId = projectId,
                assigneeId = parsedTask.assigneeId,
                dueDate = parsedTask.dueDate,
                estimatedDuration = parsedTask.estimatedDuration,
                tags = parsedTask.tags,
                enableAISuggestions = enableAISuggestions
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create task from email/message
     */
    suspend fun createTaskFromMessage(
        messageContent: String,
        sender: String? = null,
        subject: String? = null,
        projectId: String? = null,
        enableAISuggestions: Boolean = true
    ): Result<Task> {
        try {
            // Process message content with AI
            val processedMessage = if (enableAISuggestions) {
                aiTaskProcessor.processMessageToTask(messageContent, sender, subject).getOrThrow()
            } else {
                ProcessedMessageTask(
                    title = subject ?: "Task from message",
                    description = messageContent,
                    priority = TaskPriority.NORMAL,
                    dueDate = null,
                    tags = emptyList(),
                    assigneeId = null
                )
            }

            val tags = mutableListOf<String>()
            if (sender != null) {
                tags.add("from:$sender")
            }
            tags.addAll(processedMessage.tags)

            return createTask(
                title = processedMessage.title,
                description = processedMessage.description,
                priority = processedMessage.priority,
                projectId = projectId,
                assigneeId = processedMessage.assigneeId,
                dueDate = processedMessage.dueDate,
                tags = tags,
                enableAISuggestions = enableAISuggestions
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create recurring task
     */
    suspend fun createRecurringTask(
        title: String,
        description: String? = null,
        priority: TaskPriority = TaskPriority.NORMAL,
        projectId: String? = null,
        assigneeId: String? = null,
        recurrence: TaskRecurrence,
        estimatedDuration: kotlin.time.Duration? = null,
        tags: List<String> = emptyList(),
        enableAISuggestions: Boolean = true
    ): Result<Task> {
        val nextOccurrence = calculateNextOccurrence(recurrence)

        return createTask(
            title = title,
            description = description,
            priority = priority,
            projectId = projectId,
            assigneeId = assigneeId,
            dueDate = nextOccurrence,
            estimatedDuration = estimatedDuration,
            tags = tags,
            enableAISuggestions = enableAISuggestions
        ).onSuccess { task ->
            // Store recurrence information
            // In a real implementation, you'd create a separate recurrence rule
        }
    }

    /**
     * Create task from template
     */
    suspend fun createTaskFromTemplate(
        templateId: String,
        modifications: Map<String, Any> = emptyMap(),
        enableAISuggestions: Boolean = true
    ): Result<Task> {
        try {
            val task = taskRepository.createTaskFromTemplate(templateId, modifications).getOrThrow()

            // Apply AI enhancements to the created task if enabled
            return if (enableAISuggestions) {
                createTask(
                    title = modifications["title"] as? String ?: task.title,
                    description = modifications["description"] as? String ?: task.description,
                    priority = modifications["priority"] as? TaskPriority ?: task.priority,
                    projectId = modifications["projectId"] as? String ?: task.projectId,
                    assigneeId = modifications["assigneeId"] as? String ?: task.assigneeId,
                    dueDate = modifications["dueDate"] as? LocalDateTime ?: task.dueDate,
                    estimatedDuration = modifications["estimatedDuration"] as? kotlin.time.Duration ?: task.estimatedDuration,
                    tags = modifications["tags"] as? List<String> ?: task.tags,
                    enableAISuggestions = enableAISuggestions
                )
            } else {
                Result.success(task)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create task with dependencies
     */
    suspend fun createTaskWithDependencies(
        title: String,
        description: String? = null,
        priority: TaskPriority = TaskPriority.NORMAL,
        projectId: String? = null,
        assigneeId: String? = null,
        dueDate: LocalDateTime? = null,
        estimatedDuration: kotlin.time.Duration? = null,
        dependencies: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        enableAISuggestions: Boolean = true
    ): Result<Task> {
        return createTask(
            title = title,
            description = description,
            priority = priority,
            projectId = projectId,
            assigneeId = assigneeId,
            dueDate = dueDate,
            estimatedDuration = estimatedDuration,
            tags = tags,
            enableAISuggestions = enableAISuggestions
        ).onSuccess { task ->
            // Add dependencies
            dependencies.forEach { dependencyId ->
                try {
                    taskRepository.addTaskDependency(task.id, dependencyId)
                } catch (e: Exception) {
                    // Log error but don't fail the entire operation
                }
            }
        }
    }

    /**
     * Validate task data
     */
    private fun validateTaskData(
        title: String,
        description: String?,
        dueDate: LocalDateTime?
    ): ValidationResult {
        if (title.isBlank()) {
            return ValidationResult(false, "Task title is required")
        }

        if (title.length > 200) {
            return ValidationResult(false, "Task title is too long (max 200 characters)")
        }

        if (description != null && description.length > 10000) {
            return ValidationResult(false, "Task description is too long (max 10,000 characters)")
        }

        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            return ValidationResult(false, "Due date cannot be in the past")
        }

        return ValidationResult(true)
    }

    /**
     * Calculate next occurrence for recurring task
     */
    private fun calculateNextOccurrence(recurrence: TaskRecurrence): LocalDateTime {
        val now = LocalDateTime.now()

        return when (recurrence.type) {
            RecurrenceType.DAILY -> {
                now.plusDays(recurrence.interval.toLong())
            }
            RecurrenceType.WEEKLY -> {
                if (recurrence.daysOfWeek.isNotEmpty()) {
                    // Find next occurrence in the specified days
                    val nextDay = recurrence.daysOfWeek
                        .sortedBy { it.value }
                        .firstOrNull { it.value > now.dayOfWeek.value }
                        ?: recurrence.daysOfWeek.minByOrNull { it.value }!!

                    var nextDate = now.with(nextDay)
                    if (nextDate.isBefore(now)) {
                        nextDate = nextDate.plusWeeks(1)
                    }
                    nextDate
                } else {
                    now.plusWeeks(recurrence.interval.toLong())
                }
            }
            RecurrenceType.MONTHLY -> {
                if (recurrence.dayOfMonth != null) {
                    now.plusMonths(recurrence.interval.toLong()).withDayOfMonth(recurrence.dayOfMonth)
                } else {
                    now.plusMonths(recurrence.interval.toLong())
                }
            }
            RecurrenceType.YEARLY -> {
                now.plusYears(recurrence.interval.toLong())
            }
            RecurrenceType.CUSTOM -> {
                // For custom recurrence, default to next week
                now.plusWeeks(1)
            }
        }
    }

    /**
     * Generate unique task ID
     */
    private fun generateTaskId(): String {
        return "task_${System.currentTimeMillis()}_${(1000..9999).random()}"
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
 * AI Task Processor interface for AI-powered task operations
 */
interface AITaskProcessor {
    suspend fun enhanceTaskTitle(title: String, description: String): List<String>
    suspend fun expandTaskDescription(description: String): List<String>
    suspend fun suggestTaskPriority(title: String, description: String, dueDate: LocalDateTime?): TaskPriority?
    suspend fun estimateTaskEffort(title: String, description: String): EffortEstimate?
    suspend fun extractTaskTags(title: String, description: String): List<String>
    suspend fun suggestDueDate(title: String, priority: TaskPriority, estimatedDuration: kotlin.time.Duration?): LocalDateTime?
    suspend fun shouldDecomposeTask(task: Task): Result<Boolean>
    suspend fun decomposeTask(task: Task): Result<List<Subtask>>
    suspend fun parseNaturalLanguageTask(input: String): Result<ParsedTask>
    suspend fun processMessageToTask(content: String, sender: String?, subject: String?): Result<ProcessedMessageTask>
}

/**
 * Parsed task from natural language
 */
data class ParsedTask(
    val title: String,
    val description: String?,
    val priority: TaskPriority,
    val dueDate: LocalDateTime?,
    val estimatedDuration: kotlin.time.Duration?,
    val tags: List<String>,
    val assigneeId: String?
)

/**
 * Processed message task
 */
data class ProcessedMessageTask(
    val title: String,
    val description: String?,
    val priority: TaskPriority,
    val dueDate: LocalDateTime?,
    val tags: List<String>,
    val assigneeId: String?
)