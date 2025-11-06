package com.misa.ai.scheduling

import com.misa.ai.domain.model.*
import com.misa.ai.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task Scheduler
 * Handles intelligent task scheduling, dependency resolution, and time management
 */
@Singleton
class TaskScheduler @Inject constructor(
    private val taskRepository: TaskRepository
) {

    /**
     * Generate optimal schedule for given tasks
     */
    suspend fun generateSchedule(
        taskIds: List<String>,
        constraints: SchedulingConstraints,
        existingSchedule: Map<String, ScheduledTimeSlot> = emptyMap()
    ): Result<TaskSchedule> {
        try {
            // Get all tasks
            val tasks = taskIds.mapNotNull { taskId ->
                taskRepository.getTaskById(taskId).getOrNull()
            }

            if (tasks.isEmpty()) {
                return Result.failure(Exception("No valid tasks found"))
            }

            // Sort tasks by priority and dependencies
            val sortedTasks = sortTasksByPriorityAndDependencies(tasks)

            // Resolve dependencies
            val dependencyGraph = buildDependencyGraph(sortedTasks)
            val executionOrder = resolveDependencyOrder(dependencyGraph)

            // Generate time slots
            val timeSlots = generateTimeSlots(executionOrder, constraints, existingSchedule)

            // Validate schedule
            val validation = validateSchedule(timeSlots, constraints)
            if (!validation.isValid) {
                return Result.failure(Exception(validation.errorMessage))
            }

            return Result.success(
                TaskSchedule(
                    id = generateScheduleId(),
                    taskSlots = timeSlots,
                    constraints = constraints,
                    generatedAt = LocalDateTime.now(),
                    totalEstimatedDuration = timeSlots.sumOf { it.duration.inMinutes },
                    conflicts = detectConflicts(timeSlots)
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Reschedule tasks after changes
     */
    suspend fun rescheduleTasks(
        scheduleId: String,
        changedTaskIds: List<String>
    ): Result<TaskSchedule> {
        try {
            // Get existing schedule (would be stored in repository)
            // For now, regenerate from scratch
            val currentSchedule = getCurrentSchedule(scheduleId)

            // Get affected tasks
            val affectedTasks = getAffectedTasks(changedTaskIds)

            // Regenerate schedule for affected tasks
            val newSchedule = generateSchedule(
                affectedTasks.map { it.id },
                currentSchedule.constraints,
                currentSchedule.taskSlots.filter { it.taskId !in changedTaskIds }
                .associateBy { it.taskId }
            ).getOrThrow()

            return Result.success(newSchedule)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Get optimal start time for a single task
     */
    suspend fun getOptimalStartTime(
        taskId: String,
        constraints: SchedulingConstraints,
        preferredStartTime: LocalDateTime? = null
    ): Result<LocalDateTime> {
        try {
            val task = taskRepository.getTaskById(taskId).getOrNull()
                ?: return Result.failure(Exception("Task not found"))

            val dependencies = taskRepository.getTaskDependencies(taskId).getOrDefault(emptyList())

            // Find earliest completion time of dependencies
            val dependencyCompletionTime = dependencies.maxOfOrNull { dependency ->
                dependency.completedAt ?: dependency.dueDate ?: LocalDateTime.now()
            } ?: LocalDateTime.now()

            // Consider working hours
            val workingHours = constraints.workingHours
            val nextAvailableSlot = findNextAvailableTimeSlot(
                dependencyCompletionTime,
                task.estimatedDuration ?: kotlin.time.Duration.parse("PT1H"),
                workingHours,
                constraints.timeRestrictions
            )

            // Consider deadline
            val deadline = task.dueDate
            if (deadline != null) {
                val latestStartTime = deadline.minus(
                    task.estimatedDuration ?: kotlin.time.Duration.parse("PT1H")
                )
                if (nextAvailableSlot.isAfter(latestStartTime)) {
                    return Result.failure(Exception("Cannot schedule task before deadline"))
                }
            }

            // Use preferred time if it's available and optimal
            val optimalTime = if (preferredStartTime != null &&
                isTimeSlotAvailable(preferredStartTime, task.estimatedDuration, constraints)) {
                preferredStartTime
            } else {
                nextAvailableSlot
            }

            return Result.success(optimalTime)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Check for scheduling conflicts
     */
    suspend fun detectSchedulingConflicts(
        schedule: TaskSchedule
    ): List<SchedulingConflict> {
        val conflicts = mutableListOf<SchedulingConflict>()

        // Check for overlapping tasks
        val sortedSlots = schedule.taskSlots.sortedBy { it.startTime }
        for (i in 0 until sortedSlots.size - 1) {
            val current = sortedSlots[i]
            val next = sortedSlots[i + 1]

            if (current.endTime.isAfter(next.startTime)) {
                conflicts.add(
                    SchedulingConflict(
                        type = ConflictType.TIME_OVERLAP,
                        conflictingTaskId = next.taskId,
                        description = "Task '${current.taskId}' overlaps with task '${next.taskId}'",
                        severity = ConflictSeverity.HIGH
                    )
                )
            }
        }

        // Check for deadline violations
        schedule.taskSlots.forEach { slot ->
            val task = taskRepository.getTaskById(slot.taskId).getOrNull() ?: return@forEach
            if (task.dueDate != null && slot.endTime.isAfter(task.dueDate)) {
                conflicts.add(
                    SchedulingConflict(
                        type = ConflictType.DEADLINE_MISS,
                        conflictingTaskId = slot.taskId,
                        description = "Task '${task.title}' scheduled after deadline",
                        severity = ConflictSeverity.CRITICAL
                    )
                )
            }
        }

        // Check for dependency violations
        schedule.taskSlots.forEach { slot ->
            val task = taskRepository.getTaskById(slot.taskId).getOrNull() ?: return@forEach
            task.dependencies.forEach { dependencyId ->
                val dependencySlot = schedule.taskSlots.find { it.taskId == dependencyId }
                if (dependencySlot != null && dependencySlot.endTime.isAfter(slot.startTime)) {
                    conflicts.add(
                        SchedulingConflict(
                            type = ConflictType.DEPENDENCY_VIOLATION,
                            conflictingTaskId = slot.taskId,
                            description = "Task '${task.title}' scheduled before dependency completion",
                            severity = ConflictSeverity.HIGH
                        )
                    )
                }
            }
        }

        return conflicts
    }

    /**
     * Get schedule analytics
     */
    suspend fun getScheduleAnalytics(schedule: TaskSchedule): Result<ScheduleAnalytics> {
        try {
            val totalTasks = schedule.taskSlots.size
            val totalDuration = schedule.taskSlots.sumOf { it.duration.inMinutes }
            val averageTaskDuration = if (totalTasks > 0) totalDuration / totalTasks else 0

            // Calculate buffer time between tasks
            val bufferTime = calculateBufferTime(schedule.taskSlots)

            // Analyze task distribution by priority
            val priorityDistribution = schedule.taskSlots
                .groupBy {
                    taskRepository.getTaskById(it.taskId).getOrNull()?.priority ?: TaskPriority.NORMAL
                }
                .mapValues { it.value.size }

            // Analyze time distribution
            val timeDistribution = analyzeTimeDistribution(schedule.taskSlots)

            return Result.success(
                ScheduleAnalytics(
                    totalTasks = totalTasks,
                    totalScheduledMinutes = totalDuration,
                    averageTaskDuration = averageTaskDuration,
                    bufferTimeMinutes = bufferTime,
                    priorityDistribution = priorityDistribution,
                    timeDistribution = timeDistribution,
                    conflicts = schedule.conflicts,
                    utilization = calculateUtilization(schedule)
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Private helper methods
     */
    private suspend fun sortTasksByPriorityAndDependencies(tasks: List<Task>): List<Task> {
        // Sort by priority first
        val prioritySorted = tasks.sortedByDescending { it.priority }

        // Then apply dependency-aware sorting
        return applyDependencyOrdering(prioritySorted)
    }

    private suspend fun applyDependencyOrdering(tasks: List<Task>): List<Task> {
        val ordered = mutableListOf<Task>()
        val processed = mutableSetOf<String>()
        val taskMap = tasks.associateBy { it.id }

        fun processTask(task: Task) {
            if (task.id in processed) return

            // Process dependencies first
            task.dependencies.forEach { depId ->
                taskMap[depId]?.let { dep ->
                    if (dep.id !in processed) {
                        processTask(dep)
                    }
                }
            }

            ordered.add(task)
            processed.add(task.id)
        }

        tasks.forEach { processTask(it) }
        return ordered
    }

    private suspend fun buildDependencyGraph(tasks: List<Task>): Map<String, List<String>> {
        return tasks.associate { task ->
            task.id to task.dependencies.filter { depId ->
                tasks.any { it.id == depId }
            }
        }
    }

    private fun resolveDependencyOrder(dependencyGraph: Map<String, List<String>>): List<String> {
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val result = mutableListOf<String>()

        fun visit(node: String) {
            if (node in visiting) {
                // Circular dependency detected
                return
            }
            if (node in visited) return

            visiting.add(node)
            dependencyGraph[node]?.forEach { dependency ->
                visit(dependency)
            }
            visiting.remove(node)
            visited.add(node)
            result.add(node)
        }

        dependencyGraph.keys.forEach { visit(it) }
        return result
    }

    private suspend fun generateTimeSlots(
        taskIds: List<String>,
        constraints: SchedulingConstraints,
        existingSchedule: Map<String, ScheduledTimeSlot>
    ): List<ScheduledTimeSlot> {
        val timeSlots = mutableListOf<ScheduledTimeSlot>()
        var currentTime = findNextAvailableStartTime(LocalDateTime.now(), constraints)

        taskIds.forEach { taskId ->
            val task = taskRepository.getTaskById(taskId).getOrNull() ?: return@forEach

            // Skip if already scheduled
            if (existingSchedule.containsKey(taskId)) {
                timeSlots.add(existingSchedule[taskId]!!)
                return@forEach
            }

            val duration = task.estimatedDuration ?: kotlin.time.Duration.parse("PT1H")
            val startTime = findNextAvailableTimeSlot(
                currentTime,
                duration,
                constraints.workingHours,
                constraints.timeRestrictions
            )

            timeSlots.add(
                ScheduledTimeSlot(
                    taskId = taskId,
                    startTime = startTime,
                    endTime = startTime.plus(duration),
                    duration = duration,
                    isFlexible = task.dueDate == null
                )
            )

            currentTime = startTime.plus(duration).plus(constraints.breakDuration)
        }

        return timeSlots
    }

    private fun findNextAvailableStartTime(
        from: LocalDateTime,
        constraints: SchedulingConstraints
    ): LocalDateTime {
        val workingHours = constraints.workingHours
        val now = from

        // Check if current time is within working hours
        val currentTimeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        return if (currentTimeStr >= workingHours.startTime && currentTimeStr <= workingHours.endTime) {
            now
        } else {
            // Move to next working day
            val nextDay = now.plusDays(1).toLocalDate().atStartOfDay()
            val startOfDay = nextDay.plusHours(
                workingHours.startTime.split(":")[0].toLong()
            ).plusMinutes(
                workingHours.startTime.split(":")[1].toLong()
            )
            startOfDay
        }
    }

    private fun findNextAvailableTimeSlot(
        from: LocalDateTime,
        duration: kotlin.time.Duration,
        workingHours: TimeRange,
        restrictions: List<TimeRestriction>
    ): LocalDateTime {
        var current = from

        // Ensure we start within working hours
        while (!isWithinWorkingHours(current, workingHours)) {
            current = moveToNextWorkingDay(current, workingHours)
        }

        // Check restrictions
        while (isRestrictedTime(current, duration, restrictions)) {
            current = moveToNextAvailableSlot(current, workingHours, restrictions)
        }

        return current
    }

    private fun isWithinWorkingHours(time: LocalDateTime, workingHours: TimeRange): Boolean {
        val timeStr = time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        return timeStr >= workingHours.startTime && timeStr <= workingHours.endTime
    }

    private fun isRestrictedTime(
        time: LocalDateTime,
        duration: kotlin.time.Duration,
        restrictions: List<TimeRestriction>
    ): Boolean {
        val endTime = time.plus(duration)

        return restrictions.any { restriction ->
            when (restriction.type) {
                RestrictionType.UNAVAILABLE -> {
                    val restrictionStart = time.with(
                        java.time.LocalTime.parse(restriction.startTime)
                    )
                    val restrictionEnd = time.with(
                        java.time.LocalTime.parse(restriction.endTime)
                    )
                    (time.isAfter(restrictionStart) && time.isBefore(restrictionEnd)) ||
                    (endTime.isAfter(restrictionStart) && endTime.isBefore(restrictionEnd))
                }
                else -> false // Simplified for now
            }
        }
    }

    private fun moveToNextWorkingDay(time: LocalDateTime, workingHours: TimeRange): LocalDateTime {
        val nextDay = time.plusDays(1).toLocalDate().atStartOfDay()
        return nextDay.plusHours(
            workingHours.startTime.split(":")[0].toLong()
        ).plusMinutes(
            workingHours.startTime.split(":")[1].toLong()
        )
    }

    private fun moveToNextAvailableSlot(
        time: LocalDateTime,
        workingHours: TimeRange,
        restrictions: List<TimeRestriction>
    ): LocalDateTime {
        // Simplified implementation - move to next hour
        return time.plusHours(1)
    }

    private fun validateSchedule(
        timeSlots: List<ScheduledTimeSlot>,
        constraints: SchedulingConstraints
    ): ScheduleValidation {
        // Check for conflicts
        val conflicts = detectConflicts(timeSlots)
        if (conflicts.any { it.severity == ConflictSeverity.CRITICAL }) {
            return ScheduleValidation(
                isValid = false,
                errorMessage = "Critical conflicts detected in schedule"
            )
        }

        // Check working hours compliance
        val workingHoursViolations = timeSlots.filter { slot ->
            !isWithinWorkingHours(slot.startTime, constraints.workingHours)
        }
        if (workingHoursViolations.isNotEmpty()) {
            return ScheduleValidation(
                isValid = false,
                errorMessage = "Tasks scheduled outside working hours"
            )
        }

        return ScheduleValidation(isValid = true)
    }

    private fun detectConflicts(timeSlots: List<ScheduledTimeSlot>): List<SchedulingConflict> {
        // Implementation similar to detectSchedulingConflicts but for time slots only
        return emptyList() // Simplified
    }

    private fun calculateBufferTime(timeSlots: List<ScheduledTimeSlot>): Int {
        var totalBuffer = 0
        for (i in 0 until timeSlots.size - 1) {
            val current = timeSlots[i]
            val next = timeSlots[i + 1]
            val gap = kotlin.time.Duration.between(current.endTime, next.startTime)
            totalBuffer += gap.inMinutes.toInt()
        }
        return totalBuffer
    }

    private fun analyzeTimeDistribution(timeSlots: List<ScheduledTimeSlot>): Map<String, Int> {
        val distribution = mutableMapOf<String, Int>()

        timeSlots.forEach { slot ->
            val hour = slot.startTime.hour
            val period = when {
                hour in 6..11 -> "Morning"
                hour in 12..17 -> "Afternoon"
                hour in 18..23 -> "Evening"
                else -> "Night"
            }
            distribution[period] = distribution.getOrDefault(period, 0) + 1
        }

        return distribution
    }

    private fun calculateUtilization(schedule: TaskSchedule): Double {
        val totalWorkingMinutes = 8 * 60 // Assuming 8-hour workday
        val scheduledMinutes = schedule.taskSlots.sumOf { it.duration.inMinutes }
        return (scheduledMinutes.toDouble() / totalWorkingMinutes) * 100
    }

    private fun isTimeSlotAvailable(
        startTime: LocalDateTime,
        duration: kotlin.time.Duration?,
        constraints: SchedulingConstraints
    ): Boolean {
        if (duration == null) return true
        return isWithinWorkingHours(startTime, constraints.workingHours) &&
               !isRestrictedTime(startTime, duration, constraints.timeRestrictions)
    }

    private fun getAffectedTasks(changedTaskIds: List<String>): List<Task> {
        // Simplified - return tasks with changed IDs
        // In real implementation, would return dependent tasks as well
        return emptyList()
    }

    private fun getCurrentSchedule(scheduleId: String): TaskSchedule {
        // Placeholder - would retrieve from repository
        return TaskSchedule(
            id = scheduleId,
            taskSlots = emptyList(),
            constraints = SchedulingConstraints(
                workingHours = TimeRange("09:00", "17:00")
            ),
            generatedAt = LocalDateTime.now(),
            totalEstimatedDuration = 0,
            conflicts = emptyList()
        )
    }

    private fun generateScheduleId(): String {
        return "schedule_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Data classes for scheduling
 */
data class TaskSchedule(
    val id: String,
    val taskSlots: List<ScheduledTimeSlot>,
    val constraints: SchedulingConstraints,
    val generatedAt: LocalDateTime,
    val totalEstimatedDuration: Long,
    val conflicts: List<SchedulingConflict>
)

data class ScheduledTimeSlot(
    val taskId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val duration: kotlin.time.Duration,
    val isFlexible: Boolean = false
)

data class ScheduleValidation(
    val isValid: Boolean,
    val errorMessage: String = ""
)

data class ScheduleAnalytics(
    val totalTasks: Int,
    val totalScheduledMinutes: Long,
    val averageTaskDuration: Int,
    val bufferTimeMinutes: Int,
    val priorityDistribution: Map<TaskPriority, Int>,
    val timeDistribution: Map<String, Int>,
    val conflicts: List<SchedulingConflict>,
    val utilization: Double
)