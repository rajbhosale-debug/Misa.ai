package com.misa.ai.domain.repository

import com.misa.ai.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Task repository interface
 * Defines the contract for task data operations
 */
interface TaskRepository {

    /**
     * Get all tasks with optional filtering
     */
    fun getTasks(filter: TaskFilter = TaskFilter()): Flow<Result<List<Task>>>

    /**
     * Get a specific task by ID
     */
    suspend fun getTaskById(taskId: String): Result<Task?>

    /**
     * Create a new task
     */
    suspend fun createTask(task: Task): Result<Task>

    /**
     * Update an existing task
     */
    suspend fun updateTask(task: Task): Result<Task>

    /**
     * Delete a task
     */
    suspend fun deleteTask(taskId: String): Result<Unit>

    /**
     * Get tasks by status
     */
    fun getTasksByStatus(status: TaskStatus): Flow<Result<List<Task>>>

    /**
     * Get tasks by priority
     */
    fun getTasksByPriority(priority: TaskPriority): Flow<Result<List<Task>>>

    /**
     * Get tasks by assignee
     */
    fun getTasksByAssignee(assigneeId: String): Flow<Result<List<Task>>>

    /**
     * Get tasks by project
     */
    fun getTasksByProject(projectId: String): Flow<Result<List<Task>>>

    /**
     * Get tasks by tag
     */
    fun getTasksByTag(tag: String): Flow<Result<List<Task>>>

    /**
     * Get subtasks for a parent task
     */
    fun getSubtasks(parentTaskId: String): Flow<Result<List<Subtask>>>

    /**
     * Create subtask
     */
    suspend fun createSubtask(subtask: Subtask): Result<Subtask>

    /**
     * Update subtask
     */
    suspend fun updateSubtask(subtask: Subtask): Result<Subtask>

    /**
     * Delete subtask
     */
    suspend fun deleteSubtask(subtaskId: String): Result<Unit>

    /**
     * Get tasks due today
     */
    fun getTasksDueToday(): Flow<Result<List<Task>>>

    /**
     * Get overdue tasks
     */
    fun getOverdueTasks(): Flow<Result<List<Task>>>

    /**
     * Get upcoming tasks (next 7 days)
     */
    fun getUpcomingTasks(): Flow<Result<List<Task>>>

    /**
     * Get completed tasks
     */
    fun getCompletedTasks(limit: Int = 50): Flow<Result<List<Task>>>

    /**
     * Search tasks
     */
    suspend fun searchTasks(query: String, filter: TaskFilter = TaskFilter()): Result<List<Task>>

    /**
     * Get task dependencies
     */
    suspend fun getTaskDependencies(taskId: String): Result<List<Task>>

    /**
     * Get tasks that depend on this task (blockers)
     */
    suspend fun getTaskBlockers(taskId: String): Result<List<Task>>

    /**
     * Add task dependency
     */
    suspend fun addTaskDependency(taskId: String, dependsOnTaskId: String): Result<Unit>

    /**
     * Remove task dependency
     */
    suspend fun removeTaskDependency(taskId: String, dependsOnTaskId: String): Result<Unit>

    /**
     * Get task comments
     */
    fun getTaskComments(taskId: String): Flow<Result<List<TaskComment>>>

    /**
     * Add comment to task
     */
    suspend fun addComment(comment: TaskComment): Result<TaskComment>

    /**
     * Update comment
     */
    suspend fun updateComment(comment: TaskComment): Result<TaskComment>

    /**
     * Delete comment
     */
    suspend fun deleteComment(commentId: String): Result<Unit>

    /**
     * Get task attachments
     */
    fun getTaskAttachments(taskId: String): Flow<Result<List<TaskAttachment>>>

    /**
     * Add attachment to task
     */
    suspend fun addAttachment(attachment: TaskAttachment): Result<TaskAttachment>

    /**
     * Remove attachment from task
     */
    suspend fun removeAttachment(attachmentId: String): Result<Unit>

    /**
     * Get time entries for task
     */
    fun getTimeEntries(taskId: String): Flow<Result<List<TimeEntry>>>

    /**
     * Start time tracking for task
     */
    suspend fun startTimeTracking(taskId: String, userId: String, description: String? = null): Result<TimeEntry>

    /**
     * Stop time tracking for task
     */
    suspend fun stopTimeTracking(timeEntryId: String): Result<TimeEntry>

    /**
     * Add manual time entry
     */
    suspend fun addTimeEntry(timeEntry: TimeEntry): Result<TimeEntry>

    /**
     * Update time entry
     */
    suspend fun updateTimeEntry(timeEntry: TimeEntry): Result<TimeEntry>

    /**
     * Delete time entry
     */
    suspend fun deleteTimeEntry(timeEntryId: String): Result<Unit>

    /**
     * Get task labels
     */
    fun getTaskLabels(): Flow<Result<List<TaskLabel>>>

    /**
     * Create task label
     */
    suspend fun createTaskLabel(label: TaskLabel): Result<TaskLabel>

    /**
     * Update task label
     */
    suspend fun updateTaskLabel(label: TaskLabel): Result<TaskLabel>

    /**
     * Delete task label
     */
    suspend fun deleteTaskLabel(labelId: String): Result<Unit>

    /**
     * Add label to task
     */
    suspend fun addLabelToTask(taskId: String, labelId: String): Result<Unit>

    /**
     * Remove label from task
     */
    suspend fun removeLabelFromTask(taskId: String, labelId: String): Result<Unit>

    /**
     * Get task reminders
     */
    fun getTaskReminders(taskId: String): Flow<Result<List<TaskReminder>>>

    /**
     * Add reminder to task
     */
    suspend fun addReminder(reminder: TaskReminder): Result<TaskReminder>

    /**
     * Update reminder
     */
    suspend fun updateReminder(reminder: TaskReminder): Result<TaskReminder>

    /**
     * Delete reminder
     */
    suspend fun deleteReminder(reminderId: String): Result<Unit>

    /**
     * Get workflow templates
     */
    fun getWorkflowTemplates(): Flow<Result<List<WorkflowTemplate>>>

    /**
     * Create workflow template
     */
    suspend fun createWorkflowTemplate(template: WorkflowTemplate): Result<WorkflowTemplate>

    /**
     * Update workflow template
     */
    suspend fun updateWorkflowTemplate(template: WorkflowTemplate): Result<WorkflowTemplate>

    /**
     * Delete workflow template
     */
    suspend fun deleteWorkflowTemplate(templateId: String): Result<Unit>

    /**
     * Apply workflow template to task
     */
    suspend fun applyWorkflowTemplate(taskId: String, templateId: String): Result<Unit>

    /**
     * Get task statistics
     */
    suspend fun getTaskStatistics(filter: TaskFilter = TaskFilter()): Result<TaskStatistics>

    /**
     * Get productivity metrics
     */
    suspend fun getProductivityMetrics(userId: String? = null, dateRange: DateRange? = null): Result<ProductivityMetrics>

    /**
     * Get task templates
     */
    fun getTaskTemplates(): Flow<Result<List<TaskTemplate>>>

    /**
     * Create task template
     */
    suspend fun createTaskTemplate(template: TaskTemplate): Result<TaskTemplate>

    /**
     * Update task template
     */
    suspend fun updateTaskTemplate(template: TaskTemplate): Result<TaskTemplate>

    /**
     * Delete task template
     */
    suspend fun deleteTaskTemplate(templateId: String): Result<Unit>

    /**
     * Create task from template
     */
    suspend fun createTaskFromTemplate(templateId: String, modifications: Map<String, Any>): Result<Task>

    /**
     * AI-powered task decomposition
     */
    suspend fun decomposeTask(taskId: String, complexity: TaskComplexity = TaskComplexity.MEDIUM): Result<List<Subtask>>

    /**
     * AI-powered task scheduling suggestions
     */
    suspend fun getSchedulingSuggestions(taskIds: List<String>, constraints: SchedulingConstraints): Result<List<SchedulingSuggestion>>

    /**
     * AI-powered task prioritization
     */
    suspend fun prioritizeTasks(taskIds: List<String>, criteria: PrioritizationCriteria): Result<List<TaskPriority>>

    /**
     * AI-powered task estimation
     */
    suspend fun estimateTaskEffort(taskId: String, method: EstimationMethod = EstimationMethod.AI_ESTIMATED): Result<EffortEstimate>

    /**
     * AI-powered task recommendations
     */
    suspend fun getTaskRecommendations(userId: String, context: TaskRecommendationContext): Result<List<TaskRecommendation>>

    /**
     * Batch operations
     */
    suspend fun batchCreateTasks(tasks: List<Task>): Result<List<Task>>
    suspend fun batchUpdateTasks(tasks: List<Task>): Result<List<Task>>
    suspend fun batchDeleteTasks(taskIds: List<String>): Result<Unit>
    suspend fun batchUpdateTaskStatus(taskIds: List<String>, status: TaskStatus): Result<Unit>

    /**
     * Task automation
     */
    suspend fun createTaskAutomation(automation: TaskAutomation): Result<TaskAutomation>
    suspend fun updateTaskAutomation(automation: TaskAutomation): Result<TaskAutomation>
    suspend fun deleteTaskAutomation(automationId: String): Result<Unit>
    suspend fun getTaskAutomations(): Flow<Result<List<TaskAutomation>>>

    /**
     * Sync tasks with cloud storage
     */
    suspend fun syncTasks(): Result<SyncResult>

    /**
     * Export tasks
     */
    suspend fun exportTasks(taskIds: List<String>, format: TaskExportFormat): Result<String>

    /**
     * Import tasks
     */
    suspend fun importTasks(filePath: String, format: TaskImportFormat, projectId: String? = null): Result<ImportResult>

    /**
     * Subscribe to real-time task updates
     */
    fun subscribeToTaskUpdates(): Flow<TaskUpdate>

    /**
     * Get task history/audit log
     */
    suspend fun getTaskHistory(taskId: String): Result<List<TaskHistoryEntry>>
}

/**
 * Task complexity for decomposition
 */
enum class TaskComplexity {
    SIMPLE,
    MEDIUM,
    COMPLEX,
    VERY_COMPLEX
}

/**
 * Scheduling constraints
 */
data class SchedulingConstraints(
    val workingHours: TimeRange,
    val maxTasksPerDay: Int = 5,
    val breakDuration: kotlin.time.Duration = kotlin.time.Duration.parse("PT15M"),
    val priorityWeights: Map<TaskPriority, Double> = mapOf(
        TaskPriority.URGENT to 4.0,
        TaskPriority.HIGH to 3.0,
        TaskPriority.NORMAL to 2.0,
        TaskPriority.LOW to 1.0
    ),
    val dependencies: Map<String, List<String>> = emptyMap(),
    val timeRestrictions: List<TimeRestriction> = emptyList()
)

/**
 * Time range
 */
data class TimeRange(
    val startTime: String, // HH:MM format
    val endTime: String    // HH:MM format
)

/**
 * Time restriction for scheduling
 */
data class TimeRestriction(
    val type: RestrictionType,
    val startTime: String,
    val endTime: String,
    val daysOfWeek: List<java.time.DayOfWeek> = emptyList()
)

enum class RestrictionType {
    UNAVAILABLE,
    PREFERRED,
    DEADLINE
}

/**
 * Scheduling suggestion
 */
data class SchedulingSuggestion(
    val taskId: String,
    val suggestedStartTime: LocalDateTime,
    val suggestedEndTime: LocalDateTime,
    val confidence: Double,
    val reasoning: String,
    val conflicts: List<SchedulingConflict>
)

/**
 * Scheduling conflict
 */
data class SchedulingConflict(
    val type: ConflictType,
    val conflictingTaskId: String,
    val description: String,
    val severity: ConflictSeverity
)

enum class ConflictType {
    TIME_OVERLAP,
    DEPENDENCY_VIOLATION,
    RESOURCE_OVERLOAD,
    DEADLINE_MISS
}

enum class ConflictSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Prioritization criteria
 */
data class PrioritizationCriteria(
    val factors: List<PrioritizationFactor>,
    val weights: Map<String, Double> = emptyMap()
)

/**
 * Prioritization factor
 */
data class PrioritizationFactor(
    val type: FactorType,
    val weight: Double = 1.0,
    val parameters: Map<String, Any> = emptyMap()
)

enum class FactorType {
    DEADLINE_PROXIMITY,
    PRIORITY,
    ESTIMATED_EFFORT,
    DEPENDENCY_COUNT,
    BUSINESS_VALUE,
    STAKEHOLDER_IMPORTANCE,
    SKILL_MATCH,
    RESOURCE_AVAILABILITY
}

/**
 * Task recommendation context
 */
data class TaskRecommendationContext(
    val currentProject: String? = null,
    val availableTime: kotlin.time.Duration,
    val energyLevel: EnergyLevel,
    val focusArea: String? = null,
    val recentTasks: List<String> = emptyList(),
    val goals: List<String> = emptyList()
)

/**
 * Energy level for recommendations
 */
enum class EnergyLevel {
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW
}

/**
 * Task recommendation
 */
data class TaskRecommendation(
    val task: Task,
    val reason: RecommendationReason,
    val confidence: Double,
    val suggestedAction: SuggestedAction
)

/**
 * Recommendation reason
 */
enum class RecommendationReason {
    DEADLINE_APPROACHING,
    HIGH_PRIORITY,
    DEPENDENCY_READY,
    AVAILABLE_TIME_MATCH,
    SKILL_MATCH,
    GOAL_ALIGNMENT,
    MOMENTUM_BUILDING,
    BREAK_SUGGESTION
}

/**
 * Suggested action
 */
enum class SuggestedAction {
    START_NOW,
    SCHEDULE_LATER,
    DECOMPOSE_FURTHER,
    DELEGATE,
    POSTPONE,
    CANCEL
}

/**
 * Productivity metrics
 */
data class ProductivityMetrics(
    val tasksCompleted: Int,
    val tasksCompletedOnTime: Int,
    val averageTaskDuration: kotlin.time.Duration,
    val focusTimePercentage: Double,
    val contextSwitchCount: Int,
    val productivityScore: Double,
    val burnoutRisk: BurnoutRisk,
    val trends: Map<String, List<ProductivityDataPoint>>,
    val insights: List<ProductivityInsight>
)

/**
 * Productivity data point
 */
data class ProductivityDataPoint(
    val date: LocalDateTime,
    val value: Double,
    val context: Map<String, Any> = emptyMap()
)

/**
 * Productivity insight
 */
data class ProductivityInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val impact: ImpactLevel,
    val suggestions: List<String>
)

enum class InsightType {
    PRODUCTIVITY_PATTERN,
    PEAK_HOURS,
    TASK_COMPLEXITY_CORRELATION,
    BREAK_EFFECTIVENESS,
    DISTRACTION_IMPACT,
    ENERGY_MANAGEMENT
}

enum class ImpactLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Task automation
 */
data class TaskAutomation(
    val id: String,
    val name: String,
    val description: String,
    val trigger: AutomationTrigger,
    val conditions: List<AutomationCondition>,
    val actions: List<AutomationAction>,
    val isEnabled: Boolean = true,
    val executionCount: Int = 0,
    val lastExecuted: LocalDateTime? = null
)

/**
 * Automation trigger
 */
data class AutomationTrigger(
    val type: TriggerType,
    val parameters: Map<String, Any> = emptyMap()
)

enum class TriggerType {
    TASK_CREATED,
    TASK_STATUS_CHANGED,
    TASK_DUE_SOON,
    TASK_OVERDUE,
    TASK_COMPLETED,
    TIME_BASED,
    MANUAL,
    WEBHOOK
}

/**
 * Automation condition
 */
data class AutomationCondition(
    val field: String,
    val operator: ConditionOperator,
    val value: Any
)

/**
 * Automation action
 */
data class AutomationAction(
    val type: ActionType,
    val parameters: Map<String, Any> = emptyMap()
)

enum class ActionType {
    SET_STATUS,
    SET_PRIORITY,
    ASSIGN_TASK,
    ADD_TAG,
    SET_DUE_DATE,
    SEND_NOTIFICATION,
    CREATE_TASK,
    START_TIMER,
    STOP_TIMER,
    CUSTOM
}

/**
 * Task export format
 */
enum class TaskExportFormat {
    CSV,
    JSON,
    PDF,
    EXCEL,
    MARKDOWN
}

/**
 * Task import format
 */
enum class TaskImportFormat {
    CSV,
    JSON,
    EXCEL,
    TRELLO,
    ASANA,
    JIRA
}

/**
 * Task update event
 */
sealed class TaskUpdate {
    data class TaskCreated(val task: Task) : TaskUpdate()
    data class TaskUpdated(val task: Task) : TaskUpdate()
    data class TaskDeleted(val taskId: String) : TaskUpdate()
    data class TaskStatusChanged(val taskId: String, val oldStatus: TaskStatus, val newStatus: TaskStatus) : TaskUpdate()
    data class TaskAssigned(val taskId: String, val assigneeId: String) : TaskUpdate()
    data class TaskDueDateChanged(val taskId: String, val oldDueDate: LocalDateTime?, val newDueDate: LocalDateTime?) : TaskUpdate()
    data class TaskCommentAdded(val taskId: String, val comment: TaskComment) : TaskUpdate()
    data class TaskTimeEntryAdded(val taskId: String, val timeEntry: TimeEntry) : TaskUpdate()
    data class TaskDependencyAdded(val taskId: String, val dependsOnTaskId: String) : TaskUpdate()
    data class TaskDependencyRemoved(val taskId: String, val dependsOnTaskId: String) : TaskUpdate()
}

/**
 * Task history entry
 */
data class TaskHistoryEntry(
    val id: String,
    val taskId: String,
    val action: HistoryAction,
    val oldValue: Any? = null,
    val newValue: Any? = null,
    val userId: String? = null,
    val timestamp: LocalDateTime,
    val metadata: Map<String, Any> = emptyMap()
)

enum class HistoryAction {
    CREATED,
    UPDATED,
    DELETED,
    STATUS_CHANGED,
    PRIORITY_CHANGED,
    ASSIGNED,
    UNASSIGNED,
    DUE_DATE_CHANGED,
    DEPENDENCY_ADDED,
    DEPENDENCY_REMOVED,
    COMMENT_ADDED,
    COMMENT_UPDATED,
    COMMENT_DELETED,
    ATTACHMENT_ADDED,
    ATTACHMENT_REMOVED,
    TIME_ENTRY_ADDED,
    TIME_ENTRY_UPDATED,
    TIME_ENTRY_DELETED,
    LABEL_ADDED,
    LABEL_REMOVED
}