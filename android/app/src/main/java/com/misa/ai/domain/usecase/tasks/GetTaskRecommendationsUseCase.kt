package com.misa.ai.domain.usecase.tasks

import com.misa.ai.domain.model.*
import com.misa.ai.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for getting AI-powered task recommendations
 * Provides intelligent task suggestions based on context, productivity patterns, and goals
 */
class GetTaskRecommendationsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val aiTaskProcessor: AITaskProcessor
) {

    /**
     * Get task recommendations for a user
     */
    suspend fun getTaskRecommendations(
        userId: String,
        context: TaskRecommendationContext
    ): Result<List<TaskRecommendation>> {
        try {
            val recommendations = mutableListOf<TaskRecommendation>()

            // Get all available tasks for the user
            val allTasksResult = taskRepository.getTasks(TaskFilter(assigneeId = userId)).first()
            val allTasks = allTasksResult.getOrDefault(emptyList())

            // Filter actionable tasks (not completed, not blocked)
            val actionableTasks = allTasks.filter { task ->
                task.status != TaskStatus.COMPLETED &&
                task.status != TaskStatus.CANCELLED &&
                task.status != TaskStatus.ARCHIVED &&
                task.isActionable { dependencyIds ->
                    // Simple dependency resolution - in real implementation, check actual dependency status
                    dependencyIds.associateWith { false }
                }
            }

            // Get recommendations based on different strategies
            recommendations.addAll(getDeadlineBasedRecommendations(actionableTasks))
            recommendations.addAll(getPriorityBasedRecommendations(actionableTasks))
            recommendations.addAll(getEnergyBasedRecommendations(actionableTasks, context.energyLevel))
            recommendations.addAll(getTimeBasedRecommendations(actionableTasks, context.availableTime))
            recommendations.addAll(getGoalBasedRecommendations(actionableTasks, context.goals))
            recommendations.addAll(getMomentumBasedRecommendations(actionableTasks, context.recentTasks))
            recommendations.addAll(getBreakRecommendations(context))

            // Sort by confidence and limit to top recommendations
            val sortedRecommendations = recommendations
                .sortedByDescending { it.confidence }
                .take(10)

            return Result.success(sortedRecommendations)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Get recommendations based on approaching deadlines
     */
    private suspend fun getDeadlineBasedRecommendations(tasks: List<Task>): List<TaskRecommendation> {
        val recommendations = mutableListOf<TaskRecommendation>()
        val now = LocalDateTime.now()

        tasks.filter { task ->
            task.dueDate?.let { due ->
                val hoursUntilDue = java.time.Duration.between(now, due).toHours()
                hoursUntilDue in 0..48 // Due in next 48 hours
            } ?: false
        }.forEach { task ->
            val hoursUntilDue = task.dueDate?.let { due ->
                java.time.Duration.between(now, due).toHours()
            } ?: 0

            val (reason, confidence) = when {
                hoursUntilDue <= 2 -> Pair(RecommendationReason.DEADLINE_APPROACHING, 0.95)
                hoursUntilDue <= 8 -> Pair(RecommendationReason.DEADLINE_APPROACHING, 0.85)
                hoursUntilDue <= 24 -> Pair(RecommendationReason.DEADLINE_APPROACHING, 0.75)
                else -> Pair(RecommendationReason.DEADLINE_APPROACHING, 0.65)
            }

            val action = when {
                hoursUntilDue <= 2 -> SuggestedAction.START_NOW
                hoursUntilDue <= 8 -> SuggestedAction.START_NOW
                else -> SuggestedAction.SCHEDULE_LATER
            }

            recommendations.add(
                TaskRecommendation(
                    task = task,
                    reason = reason,
                    confidence = confidence,
                    suggestedAction = action
                )
            )
        }

        return recommendations
    }

    /**
     * Get recommendations based on task priority
     */
    private suspend fun getPriorityBasedRecommendations(tasks: List<Task>): List<TaskRecommendation> {
        val recommendations = mutableListOf<TaskRecommendation>()

        tasks.filter { task ->
            task.priority == TaskPriority.URGENT || task.priority == TaskPriority.HIGH
        }.forEach { task ->
            val (reason, confidence, action) = when (task.priority) {
                TaskPriority.URGENT -> Triple(
                    RecommendationReason.HIGH_PRIORITY,
                    0.9,
                    SuggestedAction.START_NOW
                )
                TaskPriority.HIGH -> Triple(
                    RecommendationReason.HIGH_PRIORITY,
                    0.8,
                    if (task.isHighPriority()) SuggestedAction.START_NOW else SuggestedAction.SCHEDULE_LATER
                )
                else -> Triple(
                    RecommendationReason.HIGH_PRIORITY,
                    0.6,
                    SuggestedAction.SCHEDULE_LATER
                )
            }

            recommendations.add(
                TaskRecommendation(
                    task = task,
                    reason = reason,
                    confidence = confidence,
                    suggestedAction = action
                )
            )
        }

        return recommendations
    }

    /**
     * Get recommendations based on user's current energy level
     */
    private suspend fun getEnergyBasedRecommendations(
        tasks: List<Task>,
        energyLevel: EnergyLevel
    ): List<TaskRecommendation> {
        val recommendations = mutableListOf<TaskRecommendation>()

        // Estimate task complexity based on description and duration
        val taskComplexityMap = tasks.associateWith { task ->
            val complexityScore = calculateTaskComplexity(task)
            when {
                complexityScore < 0.3 -> TaskComplexity.SIMPLE
                complexityScore < 0.6 -> TaskComplexity.MEDIUM
                complexityScore < 0.8 -> TaskComplexity.COMPLEX
                else -> TaskComplexity.VERY_COMPLEX
            }
        }

        val suitableTasks = when (energyLevel) {
            EnergyLevel.HIGH -> tasks // Can handle any task
            EnergyLevel.MEDIUM -> taskComplexityMap.filter { (_, complexity) ->
                complexity != TaskComplexity.VERY_COMPLEX
            }.keys
            EnergyLevel.LOW -> taskComplexityMap.filter { (_, complexity) ->
                complexity == TaskComplexity.SIMPLE
            }.keys
            EnergyLevel.VERY_LOW -> taskComplexityMap.filter { (_, complexity) ->
                complexity == TaskComplexity.SIMPLE && (it.estimatedDuration?.inMinutes ?: 0) <= 15
            }.keys
        }

        suitableTasks.take(5).forEach { task ->
            val confidence = when (energyLevel) {
                EnergyLevel.HIGH -> 0.7
                EnergyLevel.MEDIUM -> 0.75
                EnergyLevel.LOW -> 0.8
                EnergyLevel.VERY_LOW -> 0.85
            }

            recommendations.add(
                TaskRecommendation(
                    task = task,
                    reason = RecommendationReason.RESOURCE_AVAILABILITY,
                    confidence = confidence,
                    suggestedAction = if (energyLevel == EnergyLevel.HIGH) SuggestedAction.START_NOW else SuggestedAction.SCHEDULE_LATER
                )
            )
        }

        return recommendations
    }

    /**
     * Get recommendations based on available time
     */
    private suspend fun getTimeBasedRecommendations(
        tasks: List<Task>,
        availableTime: kotlin.time.Duration
    ): List<TaskRecommendation> {
        val recommendations = mutableListOf<TaskRecommendation>()

        // Find tasks that fit within available time
        val fittingTasks = tasks.filter { task ->
            val estimatedDuration = task.estimatedDuration
            estimatedDuration?.let { duration ->
                duration.inMinutes <= availableTime.inMinutes * 1.2 // Allow 20% buffer
            } ?: true // If no estimate, assume it fits
        }.sortedBy { it.estimatedDuration?.inMinutes ?: Int.MAX_VALUE }

        fittingTasks.take(3).forEach { task ->
            val durationFitRatio = task.estimatedDuration?.let { duration ->
                duration.inMinutes.toDouble() / availableTime.inMinutes.toDouble()
            } ?: 0.5

            val confidence = when {
                durationFitRatio <= 0.5 -> 0.9 // Very comfortable fit
                durationFitRatio <= 0.8 -> 0.85 // Good fit
                durationFitRatio <= 1.0 -> 0.8 // Exact fit
                durationFitRatio <= 1.2 -> 0.7 // Tight fit
                else -> 0.6 // Might not fit
            }

            recommendations.add(
                TaskRecommendation(
                    task = task,
                    reason = RecommendationReason.AVAILABLE_TIME_MATCH,
                    confidence = confidence,
                    suggestedAction = SuggestedAction.START_NOW
                )
            )
        }

        return recommendations
    }

    /**
     * Get recommendations based on user goals
     */
    private suspend fun getGoalBasedRecommendations(
        tasks: List<Task>,
        goals: List<String>
    ): List<TaskRecommendation> {
        val recommendations = mutableListOf<TaskRecommendation>()

        if (goals.isEmpty()) return recommendations

        tasks.forEach { task ->
            val goalAlignmentScore = calculateGoalAlignment(task, goals)
            if (goalAlignmentScore > 0.5) {
                recommendations.add(
                    TaskRecommendation(
                        task = task,
                        reason = RecommendationReason.GOAL_ALIGNMENT,
                        confidence = goalAlignmentScore,
                        suggestedAction = SuggestedAction.SCHEDULE_LATER
                    )
                )
            }
        }

        return recommendations.sortedByDescending { it.confidence }.take(3)
    }

    /**
     * Get recommendations for maintaining momentum
     */
    private suspend fun getMomentumBasedRecommendations(
        tasks: List<Task>,
        recentTasks: List<String>
    ): List<TaskRecommendation> {
        val recommendations = mutableListOf<TaskRecommendation>()

        if (recentTasks.isEmpty()) return recommendations

        // Find related tasks to recent work
        tasks.filter { task ->
            task.tags.any { tag ->
                recentTasks.any { recentTaskId ->
                    // Check if task is related to recent tasks
                    // In real implementation, use semantic similarity
                    tag.contains("continue", ignoreCase = true) ||
                    tag.contains("follow-up", ignoreCase = true)
                }
            }
        }.forEach { task ->
            recommendations.add(
                TaskRecommendation(
                    task = task,
                    reason = RecommendationReason.MOMENTUM_BUILDING,
                    confidence = 0.75,
                    suggestedAction = SuggestedAction.START_NOW
                )
            )
        }

        return recommendations.take(2)
    }

    /**
     * Get break recommendations
     */
    private suspend fun getBreakRecommendations(
        context: TaskRecommendationContext
    ): List<TaskRecommendation> {
        val recommendations = mutableListOf<TaskRecommendation>()

        // Suggest break if energy is low or available time is short
        if (context.energyLevel == EnergyLevel.LOW || context.energyLevel == EnergyLevel.VERY_LOW) {
            // Create a dummy break task
            val breakTask = Task(
                id = "break_${System.currentTimeMillis()}",
                title = "Take a break",
                description = "Rest and recharge for better productivity",
                status = TaskStatus.TODO,
                priority = TaskPriority.NORMAL,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                estimatedDuration = kotlin.time.Duration.parse("PT15M"),
                tags = listOf("wellness", "break")
            )

            recommendations.add(
                TaskRecommendation(
                    task = breakTask,
                    reason = RecommendationReason.BREAK_SUGGESTION,
                    confidence = 0.85,
                    suggestedAction = SuggestedAction.START_NOW
                )
            )
        }

        return recommendations
    }

    /**
     * Calculate task complexity score
     */
    private fun calculateTaskComplexity(task: Task): Double {
        var complexityScore = 0.0

        // Description length contributes to complexity
        val descriptionLength = task.description?.length ?: 0
        complexityScore += minOf(descriptionLength / 1000.0, 0.3)

        // Estimated duration contributes to complexity
        val estimatedMinutes = task.estimatedDuration?.inMinutes ?: 30
        complexityScore += minOf(estimatedMinutes / 480.0, 0.4) // 8 hours = max complexity

        // Number of subtasks contributes to complexity
        complexityScore += minOf(task.subtasks.size / 10.0, 0.2)

        // Dependencies contribute to complexity
        complexityScore += minOf((task.dependencies.size + task.blockers.size) / 5.0, 0.1)

        return minOf(complexityScore, 1.0)
    }

    /**
     * Calculate goal alignment score
     */
    private fun calculateGoalAlignment(task: Task, goals: List<String>): Double {
        if (goals.isEmpty()) return 0.0

        val taskText = "${task.title} ${task.description ?: ""}".lowercase()
        val taskTags = task.tags.map { it.lowercase() }

        var alignmentScore = 0.0

        goals.forEach { goal ->
            val goalLower = goal.lowercase()

            // Check direct text match
            if (taskText.contains(goalLower)) {
                alignmentScore += 0.4
            }

            // Check tag match
            taskTags.forEach { tag ->
                if (tag.contains(goalLower) || goalLower.contains(tag)) {
                    alignmentScore += 0.3
                }
            }

            // Check semantic similarity (simplified)
            val goalWords = goalLower.split("\\s+".toRegex())
            val taskWords = taskText.split("\\s+".toRegex())
            val commonWords = goalWords.intersect(taskWords.toSet())
            alignmentScore += commonWords.size * 0.1
        }

        return minOf(alignmentScore, 1.0)
    }

    /**
     * Get personalized recommendations using AI
     */
    suspend fun getPersonalizedRecommendations(
        userId: String,
        context: TaskRecommendationContext
    ): Result<List<TaskRecommendation>> {
        try {
            // Get basic recommendations first
            val basicRecommendations = getTaskRecommendations(userId, context).getOrDefault(emptyList())

            // Use AI to enhance and personalize recommendations
            val enhancedRecommendations = aiTaskProcessor.personalizeRecommendations(
                basicRecommendations,
                userId,
                context
            ).getOrDefault(basicRecommendations)

            return Result.success(enhancedRecommendations)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Get learning-based recommendations
     */
    suspend fun getLearningBasedRecommendations(
        userId: String
    ): Result<List<TaskRecommendation>> {
        try {
            // Analyze user's productivity patterns
            val metrics = taskRepository.getProductivityMetrics(userId).getOrThrow()

            // Get recommendations based on learned patterns
            val recommendations = aiTaskProcessor.generateLearningBasedRecommendations(
                userId,
                metrics
            ).getOrDefault(emptyList())

            return Result.success(recommendations)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}

/**
 * Extended AI Task Processor interface for recommendations
 */
interface AITaskProcessor {
    // ... existing methods from CreateTaskUseCase ...

    suspend fun personalizeRecommendations(
        recommendations: List<TaskRecommendation>,
        userId: String,
        context: TaskRecommendationContext
    ): Result<List<TaskRecommendation>>

    suspend fun generateLearningBasedRecommendations(
        userId: String,
        metrics: ProductivityMetrics
    ): Result<List<TaskRecommendation>>
}