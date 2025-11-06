package com.misa.ai.domain.model

import java.time.LocalDateTime

/**
 * Task domain model
 * Represents a task with hierarchical structure, dependencies, and intelligent features
 */
data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus,
    val priority: TaskPriority,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dueDate: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
    val estimatedDuration: kotlin.time.Duration? = null,
    val actualDuration: kotlin.time.Duration? = null,
    val assigneeId: String? = null,
    val projectId: String? = null,
    val tags: List<String> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val dependencies: List<String> = emptyList(), // Task IDs this task depends on
    val blockers: List<String> = emptyList(),   // Task IDs blocking this task
    val checklist: List<ChecklistItem> = emptyList(),
    val attachments: List<TaskAttachment> = emptyList(),
    val comments: List<TaskComment> = emptyList(),
    val timeEntries: List<TimeEntry> = emptyList(),
    val labels: List<TaskLabel> = emptyList(),
    val recurrence: TaskRecurrence? = null,
    val reminders: List<TaskReminder> = emptyList(),
    val estimatedEffort: EffortEstimate? = null,
    val actualEffort: EffortEstimate? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Check if task is completed
     */
    fun isCompleted(): Boolean = status == TaskStatus.COMPLETED

    /**
     * Check if task is overdue
     */
    fun isOverdue(): Boolean {
        val now = LocalDateTime.now()
        return dueDate?.let { it.isBefore(now) && !isCompleted() } ?: false
    }

    /**
     * Check if task is due soon (within 24 hours)
     */
    fun isDueSoon(): Boolean {
        val now = LocalDateTime.now()
        return dueDate?.let {
            val hoursUntilDue = kotlin.time.Duration.between(now, it).toHours()
            hoursUntilDue in 0..24 && !isCompleted()
        } ?: false
    }

    /**
     * Check if task has subtasks
     */
    fun hasSubtasks(): Boolean = subtasks.isNotEmpty()

    /**
     * Get subtask completion percentage
     */
    fun getSubtaskCompletionPercentage(): Double {
        if (subtasks.isEmpty()) return 0.0
        val completed = subtasks.count { it.isCompleted }
        return (completed.toDouble() / subtasks.size) * 100
    }

    /**
     * Check if task has active timer
     */
    fun hasActiveTimer(): Boolean {
        return timeEntries.any { it.endTime == null }
    }

    /**
     * Get total time tracked
     */
    fun getTotalTimeTracked(): kotlin.time.Duration {
        return timeEntries.map { it.getDuration() }.fold(kotlin.time.Duration.ZERO) { acc, duration -> acc + duration }
    }

    /**
     * Check if task can be started (all dependencies completed)
     */
    fun canStart(dependencyResolver: (List<String>) -> Map<String, Boolean>): Boolean {
        if (dependencies.isEmpty()) return true
        val dependencyStatus = dependencyResolver(dependencies)
        return dependencyStatus.values.all { it }
    }

    /**
     * Get progress based on status, subtasks, and time
     */
    fun getProgress(): TaskProgress {
        return when (status) {
            TaskStatus.COMPLETED -> TaskProgress(100.0, "Completed")
            TaskStatus.CANCELLED -> TaskProgress(0.0, "Cancelled")
            TaskStatus.BLOCKED -> TaskProgress(0.0, "Blocked by dependencies")
            else -> {
                val subtaskProgress = getSubtaskCompletionPercentage()
                val timeProgress = getTimeProgress()
                val overallProgress = maxOf(subtaskProgress, timeProgress)
                TaskProgress(overallProgress, getStatusDescription(overallProgress))
            }
        }
    }

    /**
     * Check if task has high priority
     */
    fun isHighPriority(): Boolean = priority == TaskPriority.HIGH || priority == TaskPriority.URGENT

    /**
     * Check if task is actionable (not blocked or completed)
     */
    fun isActionable(dependencyResolver: (List<String>) -> Map<String, Boolean>): Boolean {
        return status != TaskStatus.COMPLETED &&
               status != TaskStatus.CANCELLED &&
               canStart(dependencyResolver)
    }

    private fun getTimeProgress(): Double {
        if (estimatedDuration == null || getTotalTimeTracked() == kotlin.time.Duration.ZERO) return 0.0

        val tracked = getTotalTimeTracked()
        return if (estimatedDuration!!.inSeconds > 0) {
            (tracked.inSeconds.toDouble() / estimatedDuration.inSeconds.toDouble()) * 100
        } else {
            0.0
        }
    }

    private fun getStatusDescription(progress: Double): String {
        return when {
            progress == 0.0 -> "Not started"
            progress < 25.0 -> "Just started"
            progress < 50.0 -> "In progress"
            progress < 75.0 -> "Making progress"
            progress < 100.0 -> "Nearly done"
            else -> "Almost complete"
        }
    }
}

/**
 * Subtask model
 */
data class Subtask(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val assigneeId: String? = null,
    val estimatedDuration: kotlin.time.Duration? = null,
    val actualDuration: kotlin.time.Duration? = null,
    val position: Int = 0
) {
    fun isCompleted(): Boolean = status == TaskStatus.COMPLETED
    fun getProgress(): Double = if (isCompleted()) 100.0 else 0.0
}

/**
 * Checklist item model
 */
data class ChecklistItem(
    val id: String,
    val text: String,
    val isCompleted: Boolean = false,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val position: Int = 0
)

/**
 * Task attachment model
 */
data class TaskAttachment(
    val id: String,
    val name: String,
    val type: AttachmentType,
    val size: Long,
    val localPath: String? = null,
    val cloudUrl: String? = null,
    val mimeType: String,
    val metadata: Map<String, Any> = emptyMap()
) {
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
}

/**
 * Task comment model
 */
data class TaskComment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null,
    val mentions: List<String> = emptyList(),
    val attachments: List<TaskAttachment> = emptyList()
)

/**
 * Time entry model for time tracking
 */
data class TimeEntry(
    val id: String,
    val taskId: String,
    val userId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val isBillable: Boolean = false
) {
    fun getDuration(): kotlin.time.Duration {
        val end = endTime ?: LocalDateTime.now()
        return kotlin.time.Duration.between(startTime, end)
    }

    fun isActive(): Boolean = endTime == null
}

/**
 * Task label model
 */
data class TaskLabel(
    val id: String,
    val name: String,
    val color: String,
    val projectId: String? = null,
    val description: String? = null
)

/**
 * Task recurrence model
 */
data class TaskRecurrence(
    val type: RecurrenceType,
    val interval: Int = 1,
    val endDate: LocalDateTime? = null,
    val occurrences: Int? = null,
    val daysOfWeek: List<java.time.DayOfWeek> = emptyList(),
    val dayOfMonth: Int? = null,
    val exceptions: List<LocalDateTime> = emptyList()
)

/**
 * Task reminder model
 */
data class TaskReminder(
    val id: String,
    val type: ReminderType,
    val triggerTime: LocalDateTime,
    val isEnabled: Boolean = true,
    val message: String? = null
)

/**
 * Effort estimate model
 */
data class EffortEstimate(
    val hours: Double,
    val confidence: Double, // 0.0 to 1.0
    val methodology: EstimationMethod,
    val notes: String? = null
)

/**
 * Task progress model
 */
data class TaskProgress(
    val percentage: Double,
    val description: String
)

/**
 * Task status enum
 */
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    BLOCKED,
    IN_REVIEW,
    TESTING,
    COMPLETED,
    CANCELLED,
    ON_HOLD,
    ARCHIVED
}

/**
 * Task priority enum
 */
enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Attachment type enum
 */
enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    PDF,
    SPREADSHEET,
    PRESENTATION,
    CODE,
    OTHER
}

/**
 * Recurrence type enum
 */
enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

/**
 * Reminder type enum
 */
enum class ReminderType {
    NOTIFICATION,
    EMAIL,
    SMS,
    CUSTOM
}

/**
 * Estimation method enum
 */
enum class EstimationMethod {
    EXPERT_JUDGMENT,
    HISTORICAL_DATA,
    PERT_ANALYSIS,
    STORY_POINTS,
    TIME_BOXING,
    AI_ESTIMATED
}

/**
 * Task filter for searching and querying
 */
data class TaskFilter(
    val status: List<TaskStatus> = emptyList(),
    val priority: List<TaskPriority> = emptyList(),
    val assigneeId: String? = null,
    val projectId: String? = null,
    val tags: List<String> = emptyList(),
    val dueDateRange: DateRange? = null,
    val createdDateRange: DateRange? = null,
    val hasSubtasks: Boolean? = null,
    val isOverdue: Boolean? = null,
    val isPinned: Boolean? = null,
    val searchQuery: String? = null
)

/**
 * Date range for filtering
 */
data class DateRange(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

/**
 * Task statistics
 */
data class TaskStatistics(
    val totalTasks: Int,
    val completedTasks: Int,
    val inProgressTasks: Int,
    val overdueTasks: Int,
    val tasksByStatus: Map<TaskStatus, Int>,
    val tasksByPriority: Map<TaskPriority, Int>,
    val averageCompletionTime: kotlin.time.Duration,
    val averageOverdueTime: kotlin.time.Duration,
    val productivityScore: Double,
    val burnoutRisk: BurnoutRisk
)

/**
 * Burnout risk assessment
 */
enum class BurnoutRisk {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Task template for quick task creation
 */
data class TaskTemplate(
    val id: String,
    val name: String,
    val description: String,
    val defaultPriority: TaskPriority,
    val defaultEstimatedDuration: kotlin.time.Duration?,
    val defaultTags: List<String>,
    val defaultChecklist: List<ChecklistItem>,
    val metadata: Map<String, Any>
)

/**
 * Workflow template for task automation
 */
data class WorkflowTemplate(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<WorkflowStep>,
    val triggers: List<WorkflowTrigger>,
    val conditions: List<WorkflowCondition>,
    val metadata: Map<String, Any>
)

/**
 * Workflow step
 */
data class WorkflowStep(
    val id: String,
    val name: String,
    val action: WorkflowAction,
    val parameters: Map<String, Any>,
    val position: Int,
    val isOptional: Boolean = false
)

/**
 * Workflow action
 */
enum class WorkflowAction {
    CREATE_TASK,
    UPDATE_TASK_STATUS,
    SEND_NOTIFICATION,
    SEND_EMAIL,
    CREATE_NOTE,
    ADD_TAG,
    SET_REMINDER,
    START_TIMER,
    STOP_TIMER,
    CUSTOM
}

/**
 * Workflow trigger
 */
enum class WorkflowTrigger {
    TASK_CREATED,
    TASK_COMPLETED,
    TASK_OVERDUE,
    TASK_STATUS_CHANGED,
    TIME_BASED,
    MANUAL,
    WEBHOOK
}

/**
 * Workflow condition
 */
data class WorkflowCondition(
    val field: String,
    val operator: ConditionOperator,
    val value: Any
)

/**
 * Condition operator for workflow conditions
 */
enum class ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    IS_EMPTY,
    IS_NOT_EMPTY
}