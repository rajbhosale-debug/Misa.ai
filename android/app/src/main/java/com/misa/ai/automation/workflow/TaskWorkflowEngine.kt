package com.misa.ai.automation.workflow

import com.misa.ai.domain.model.*
import com.misa.ai.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task Workflow Engine
 * Handles automated task workflows, triggers, and actions
 */
@Singleton
class TaskWorkflowEngine @Inject constructor(
    private val taskRepository: TaskRepository,
    private val actionExecutor: WorkflowActionExecutor
) {
    private val executionMutex = Mutex()
    private val activeWorkflows = mutableMapOf<String, WorkflowInstance>()

    /**
     * Process a task event and trigger relevant workflows
     */
    suspend fun processTaskEvent(event: TaskEvent): Result<List<WorkflowExecutionResult>> {
        return executionMutex.withLock {
            try {
                val results = mutableListOf<WorkflowExecutionResult>()

                // Get all enabled automations
                val automations = taskRepository.getTaskAutomations().first().getOrDefault(emptyList())

                // Find matching automations for this event
                val matchingAutomations = findMatchingAutomations(event, automations)

                // Execute matching automations
                matchingAutomations.forEach { automation ->
                    val result = executeAutomation(automation, event)
                    results.add(result)
                }

                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Execute a workflow template on a task
     */
    suspend fun executeWorkflowTemplate(
        taskId: String,
        templateId: String
    ): Result<WorkflowExecutionResult> {
        return executionMutex.withLock {
            try {
                val task = taskRepository.getTaskById(taskId).getOrThrow()
                    ?: return Result.failure(Exception("Task not found"))

                val template = getWorkflowTemplate(templateId)
                    ?: return Result.failure(Exception("Workflow template not found"))

                val workflowInstance = WorkflowInstance(
                    id = generateWorkflowInstanceId(),
                    templateId = templateId,
                    taskId = taskId,
                    status = WorkflowStatus.RUNNING,
                    startedAt = LocalDateTime.now()
                )

                activeWorkflows[workflowInstance.id] = workflowInstance

                val results = mutableListOf<ActionExecutionResult>()

                // Execute workflow steps
                template.steps.forEach { step ->
                    if (evaluateWorkflowConditions(step, task, template)) {
                        val actionResult = actionExecutor.executeAction(
                            step.action,
                            step.parameters,
                            task
                        )
                        results.add(actionResult)

                        // Stop execution if action failed and step is not optional
                        if (!actionResult.success && !step.isOptional) {
                            workflowInstance.status = WorkflowStatus.FAILED
                            break
                        }
                    }
                }

                if (workflowInstance.status == WorkflowStatus.RUNNING) {
                    workflowInstance.status = WorkflowStatus.COMPLETED
                }

                workflowInstance.completedAt = LocalDateTime.now()

                Result.success(
                    WorkflowExecutionResult(
                        workflowInstanceId = workflowInstance.id,
                        success = workflowInstance.status == WorkflowStatus.COMPLETED,
                        actionResults = results,
                        startedAt = workflowInstance.startedAt,
                        completedAt = workflowInstance.completedAt
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Schedule time-based workflows
     */
    suspend fun scheduleTimeBasedWorkflows(): Result<Int> {
        return executionMutex.withLock {
            try {
                val scheduledCount = scheduleRecurringWorkflows() +
                                  scheduleDeadlineWorkflows() +
                                  scheduleReminderWorkflows()

                Result.success(scheduledCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get active workflow instances
     */
    suspend fun getActiveWorkflows(): List<WorkflowInstance> {
        return executionMutex.withLock {
            activeWorkflows.values.toList()
        }
    }

    /**
     * Cancel a running workflow
     */
    suspend fun cancelWorkflow(workflowInstanceId: String): Result<Boolean> {
        return executionMutex.withLock {
            try {
                val workflow = activeWorkflows[workflowInstanceId]
                if (workflow != null && workflow.status == WorkflowStatus.RUNNING) {
                    workflow.status = WorkflowStatus.CANCELLED
                    workflow.completedAt = LocalDateTime.now()
                    Result.success(true)
                } else {
                    Result.success(false)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Find automations that match the given event
     */
    private suspend fun findMatchingAutomations(
        event: TaskEvent,
        automations: List<TaskAutomation>
    ): List<TaskAutomation> {
        return automations.filter { automation ->
            // Check if trigger matches event type
            if (automation.trigger.type != event.type.toTriggerType()) {
                return@filter false
            }

            // Check trigger conditions
            evaluateTriggerConditions(automation.trigger, event)
        }
    }

    /**
     * Execute an automation
     */
    private suspend fun executeAutomation(
        automation: TaskAutomation,
        event: TaskEvent
    ): WorkflowExecutionResult {
        val results = mutableListOf<ActionExecutionResult>()
        var success = true

        try {
            // Get task context
            val task = taskRepository.getTaskById(event.taskId).getOrNull()

            // Evaluate conditions
            if (evaluateAutomationConditions(automation.conditions, task, event)) {
                // Execute actions
                automation.actions.forEach { action ->
                    val result = actionExecutor.executeAction(
                        action.type,
                        action.parameters,
                        task,
                        event
                    )
                    results.add(result)

                    if (!result.success) {
                        success = false
                    }
                }

                // Update execution count
                taskRepository.updateTaskAutomation(
                    automation.copy(
                        executionCount = automation.executionCount + 1,
                        lastExecuted = LocalDateTime.now()
                    )
                )
            }
        } catch (e: Exception) {
            success = false
            results.add(
                ActionExecutionResult(
                    actionType = "ERROR",
                    success = false,
                    message = e.message ?: "Unknown error",
                    duration = kotlin.time.Duration.ZERO
                )
            )
        }

        return WorkflowExecutionResult(
            workflowInstanceId = automation.id,
            success = success,
            actionResults = results,
            startedAt = LocalDateTime.now(),
            completedAt = LocalDateTime.now()
        )
    }

    /**
     * Evaluate trigger conditions
     */
    private fun evaluateTriggerConditions(
        trigger: AutomationTrigger,
        event: TaskEvent
    ): Boolean {
        // Check trigger-specific parameters
        return when (trigger.type) {
            TriggerType.TIME_BASED -> {
                val scheduledTime = trigger.parameters["scheduledTime"] as? String
                val currentTime = LocalDateTime.now().toString()
                scheduledTime == currentTime
            }
            TriggerType.TASK_DUE_SOON -> {
                val hoursBefore = trigger.parameters["hoursBefore"] as? Int ?: 24
                val task = event as? TaskDueEvent
                task?.let {
                    val hoursUntilDue = kotlin.time.Duration.between(
                        LocalDateTime.now(),
                        it.dueDate
                    ).inHours()
                    hoursUntilDue <= hoursBefore
                } ?: false
            }
            else -> true // Other triggers are handled by event type matching
        }
    }

    /**
     * Evaluate automation conditions
     */
    private suspend fun evaluateAutomationConditions(
        conditions: List<AutomationCondition>,
        task: Task?,
        event: TaskEvent
    ): Boolean {
        if (conditions.isEmpty()) return true

        return conditions.all { condition ->
            evaluateCondition(condition, task, event)
        }
    }

    /**
     * Evaluate a single condition
     */
    private suspend fun evaluateCondition(
        condition: AutomationCondition,
        task: Task?,
        event: TaskEvent
    ): Boolean {
        val actualValue = getFieldValue(condition.field, task, event)

        return when (condition.operator) {
            ConditionOperator.EQUALS -> actualValue == condition.value
            ConditionOperator.NOT_EQUALS -> actualValue != condition.value
            ConditionOperator.GREATER_THAN -> compareValues(actualValue, condition.value) > 0
            ConditionOperator.LESS_THAN -> compareValues(actualValue, condition.value) < 0
            ConditionOperator.CONTAINS -> actualValue?.toString()?.contains(condition.value.toString()) == true
            ConditionOperator.STARTS_WITH -> actualValue?.toString()?.startsWith(condition.value.toString()) == true
            ConditionOperator.ENDS_WITH -> actualValue?.toString()?.endsWith(condition.value.toString()) == true
            ConditionOperator.IS_EMPTY -> actualValue == null || actualValue.toString().isBlank()
            ConditionOperator.IS_NOT_EMPTY -> actualValue != null && actualValue.toString().isNotBlank()
        }
    }

    /**
     * Get field value from task or event
     */
    private suspend fun getFieldValue(
        field: String,
        task: Task?,
        event: TaskEvent
    ): Any? {
        return when (field) {
            "task.status" -> task?.status
            "task.priority" -> task?.priority
            "task.assigneeId" -> task?.assigneeId
            "task.projectId" -> task?.projectId
            "task.dueDate" -> task?.dueDate
            "task.tags" -> task?.tags
            "task.isCompleted" -> task?.isCompleted()
            "task.isOverdue" -> task?.isOverdue()
            "event.type" -> event.type
            "event.timestamp" -> event.timestamp
            "event.userId" -> (event as? TaskUserEvent)?.userId
            else -> null
        }
    }

    /**
     * Evaluate workflow step conditions
     */
    private suspend fun evaluateWorkflowConditions(
        step: WorkflowStep,
        task: Task,
        template: WorkflowTemplate
    ): Boolean {
        // Combine step-specific conditions with template conditions
        val allConditions = step.conditions + template.conditions

        return allConditions.all { condition ->
            evaluateCondition(condition, task, TaskCreatedEvent(task.id, LocalDateTime.now()))
        }
    }

    /**
     * Schedule recurring workflows
     */
    private suspend fun scheduleRecurringWorkflows(): Int {
        var scheduledCount = 0

        // Get tasks with recurrence
        val tasks = taskRepository.getTasks().first().getOrDefault(emptyList())
        val recurringTasks = tasks.filter { it.recurrence != null }

        recurringTasks.forEach { task ->
            // Check if next occurrence should be created
            if (shouldCreateNextOccurrence(task)) {
                createNextRecurrence(task)
                scheduledCount++
            }
        }

        return scheduledCount
    }

    /**
     * Schedule deadline-based workflows
     */
    private suspend fun scheduleDeadlineWorkflows(): Int {
        var scheduledCount = 0

        // Get tasks due soon
        val upcomingTasks = taskRepository.getUpcomingTasks().first().getOrDefault(emptyList())

        upcomingTasks.forEach { task ->
            if (task.dueDate?.let { isWithinScheduleWindow(it) } == true) {
                // Trigger deadline workflow
                processTaskEvent(TaskDueEvent(task.id, task.dueDate!!))
                scheduledCount++
            }
        }

        return scheduledCount
    }

    /**
     * Schedule reminder workflows
     */
    private suspend fun scheduleReminderWorkflows(): Int {
        var scheduledCount = 0

        // Get pending reminders
        val tasks = taskRepository.getTasks().first().getOrDefault(emptyList())

        tasks.forEach { task ->
            task.reminders.forEach { reminder ->
                if (reminder.isEnabled && shouldTriggerReminder(reminder)) {
                    processTaskEvent(TaskReminderEvent(task.id, reminder.id))
                    scheduledCount++
                }
            }
        }

        return scheduledCount
    }

    /**
     * Helper functions
     */
    private fun generateWorkflowInstanceId(): String {
        return "workflow_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private suspend fun getWorkflowTemplate(templateId: String): WorkflowTemplate? {
        return taskRepository.getWorkflowTemplates().first().getOrNull()
            ?.find { it.id == templateId }
    }

    private fun shouldCreateNextOccurrence(task: Task): Boolean {
        // Implementation depends on recurrence logic
        return false // Placeholder
    }

    private suspend fun createNextRecurrence(task: Task) {
        // Implementation creates next task occurrence
    }

    private fun isWithinScheduleWindow(dueDate: LocalDateTime): Boolean {
        val now = LocalDateTime.now()
        val windowHours = kotlin.time.Duration.between(now, dueDate).inHours()
        return windowHours in 0..24
    }

    private fun shouldTriggerReminder(reminder: TaskReminder): Boolean {
        val now = LocalDateTime.now()
        return !reminder.triggerTime.isAfter(now)
    }

    private fun compareValues(value1: Any?, value2: Any?): Int {
        return when {
            value1 is Comparable<*> && value2 is Comparable<*> -> {
                @Suppress("UNCHECKED_CAST")
                (value1 as Comparable<Any>).compareTo(value2)
            }
            else -> value1.toString().compareTo(value2.toString())
        }
    }
}

/**
 * Workflow execution result
 */
data class WorkflowExecutionResult(
    val workflowInstanceId: String,
    val success: Boolean,
    val actionResults: List<ActionExecutionResult>,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime
)

/**
 * Action execution result
 */
data class ActionExecutionResult(
    val actionType: String,
    val success: Boolean,
    val message: String? = null,
    val affectedEntities: List<String> = emptyList(),
    val duration: kotlin.time.Duration
)

/**
 * Workflow instance
 */
data class WorkflowInstance(
    val id: String,
    val templateId: String,
    val taskId: String,
    val status: WorkflowStatus,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime? = null
)

/**
 * Workflow status
 */
enum class WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Task event types
 */
sealed class TaskEvent {
    abstract val taskId: String
    abstract val timestamp: LocalDateTime
}

data class TaskCreatedEvent(
    override val taskId: String,
    override val timestamp: LocalDateTime
) : TaskEvent()

data class TaskUpdatedEvent(
    override val taskId: String,
    override val timestamp: LocalDateTime,
    val changes: Map<String, Any>
) : TaskEvent()

data class TaskStatusChangedEvent(
    override val taskId: String,
    override val timestamp: LocalDateTime,
    val oldStatus: TaskStatus,
    val newStatus: TaskStatus,
    val userId: String?
) : TaskEvent(), TaskUserEvent

data class TaskDueEvent(
    override val taskId: String,
    val dueDate: LocalDateTime,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : TaskEvent()

data class TaskReminderEvent(
    override val taskId: String,
    val reminderId: String,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : TaskEvent()

interface TaskUserEvent : TaskEvent {
    val userId: String?
}

/**
 * Extension function to convert task event type to trigger type
 */
fun TaskEventType.toTriggerType(): TriggerType {
    return when (this) {
        TaskEventType.CREATED -> TriggerType.TASK_CREATED
        TaskEventType.UPDATED -> TriggerType.TASK_STATUS_CHANGED
        TaskEventType.DUE -> TriggerType.TASK_DUE_SOON
        TaskEventType.OVERDUE -> TriggerType.TASK_OVERDUE
        TaskEventType.COMPLETED -> TriggerType.TASK_COMPLETED
    }
}

enum class TaskEventType {
    CREATED,
    UPDATED,
    DUE,
    OVERDUE,
    COMPLETED
}