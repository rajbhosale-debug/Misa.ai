package com.misa.ai.domain.model

import java.time.LocalDateTime
import java.time.Duration

/**
 * Focus Session - represents a productivity session in the Focus app
 * Includes Pomodoro timers, deep work sessions, and focus tracking
 */
data class FocusSession(
    val id: String,
    val type: SessionType,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val duration: Duration? = null,
    val plannedDuration: Duration,
    val actualDuration: Duration? = null,
    val status: SessionStatus,
    val title: String? = null,
    val description: String? = null,
    val tasks: List<FocusTask> = emptyList(),
    val interruptions: List<Interruption> = emptyList(),
    val metrics: SessionMetrics,
    val settings: SessionSettings,
    val tags: List<String> = emptyList(),
    val projectId: String? = null,
    val categoryId: String? = null,
    val location: String? = null,
    val mood: MoodLevel? = null,
    val energyLevel: EnergyLevel? = null,
    val qualityScore: Float? = null,
    val notes: String? = null,
    val reflections: SessionReflection? = null,
    val achievements: List<Achievement> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {

    /**
     * Check if session is currently active
     */
    fun isActive(): Boolean = status == SessionStatus.ACTIVE

    /**
     * Check if session is completed
     */
    fun isCompleted(): Boolean = status == SessionStatus.COMPLETED

    /**
     * Check if session was interrupted
     */
    fun wasInterrupted(): Boolean = interruptions.isNotEmpty()

    /**
     * Get session efficiency (planned vs actual duration)
     */
    fun getEfficiency(): Float {
        val planned = plannedDuration.toMinutes().toFloat()
        val actual = actualDuration?.toMinutes()?.toFloat() ?: return 0f
        return if (planned > 0) actual / planned else 0f
    }

    /**
     * Get interruption rate (interruptions per hour)
     */
    fun getInterruptionRate(): Float {
        val durationHours = (actualDuration ?: Duration.ZERO).toMinutes().toFloat() / 60f
        return if (durationHours > 0) interruptions.size / durationHours else 0f
    }

    /**
     * Get focus score based on various metrics
     */
    fun getFocusScore(): Float {
        var score = 100f

        // Penalty for interruptions
        score -= interruptions.size * 10f

        // Bonus for completing planned duration
        if (actualDuration != null && actualDuration!! >= plannedDuration) {
            score += 20f
        }

        // Bonus for no interruptions
        if (interruptions.isEmpty()) {
            score += 15f
        }

        // Bonus for achieving deep work
        if (metrics.deepWorkTime > Duration.ofMinutes(30)) {
            score += 10f
        }

        return score.coerceIn(0f, 100f)
    }

    /**
     * Get total break time
     */
    fun getTotalBreakTime(): Duration {
        return interruptions.filter { it.type == InterruptionType.BREAK }
            .map { it.duration ?: Duration.ZERO }
            .fold(Duration.ZERO) { acc, duration -> acc + duration }
    }

    /**
     * Get total productive time (excluding breaks)
     */
    fun getNetProductiveTime(): Duration {
        val total = actualDuration ?: Duration.ZERO
        val breakTime = getTotalBreakTime()
        return if (total > breakTime) total.minus(breakTime) else Duration.ZERO
    }
}

/**
 * Session types
 */
enum class SessionType {
    POMODORO,
    DEEP_WORK,
    ULTRA_DEEP_WORK,
    BREAK,
    MEETING,
    LEARNING,
    CREATIVE,
    PLANNING,
    REVIEW,
    CUSTOM
}

/**
 * Session status
 */
enum class SessionStatus {
    PLANNED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
    SKIPPED
}

/**
 * Focus task within a session
 */
data class FocusTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val estimatedDuration: Duration,
    val actualDuration: Duration? = null,
    val completed: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val taskId: String? = null, // Link to TaskFlow task
    val tags: List<String> = emptyList()
)

/**
 * Task priority
 */
enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Interruption during focus session
 */
data class Interruption(
    val id: String,
    val type: InterruptionType,
    val reason: String,
    val timestamp: LocalDateTime,
    val duration: Duration? = null,
    val source: InterruptionSource,
    val severity: InterruptionSeverity,
    val wasHandled: Boolean = true,
    val notes: String? = null
)

/**
 * Interruption types
 */
enum class InterruptionType {
    EXTERNAL,
    INTERNAL,
    BREAK,
    DISTRACTION,
    TECHNICAL,
    EMERGENCY,
    MEETING,
    NOTIFICATION,
    PHONE_CALL,
    PERSONAL
}

/**
 * Interruption source
 */
enum class InterruptionSource {
    COLLEAGUE,
    FAMILY,
    SELF,
    TECHNOLOGY,
    ENVIRONMENT,
    EMAIL,
    SOCIAL_MEDIA,
    NEWS,
    UNKNOWN
}

/**
 * Interruption severity
 */
enum class InterruptionSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Session metrics and analytics
 */
data class SessionMetrics(
    val deepWorkTime: Duration = Duration.ZERO,
    val shallowWorkTime: Duration = Duration.ZERO,
    val breakTime: Duration = Duration.ZERO,
    val contextSwitches: Int = 0,
    val focusLevel: Float = 0f,
    val productivityScore: Float = 0f,
    val energyLevel: EnergyLevel,
    val stressLevel: StressLevel,
    val satisfactionLevel: SatisfactionLevel,
    val heartRateData: List<HeartRateReading> = emptyList(),
    val appUsageData: List<AppUsageEntry> = emptyList(),
    val screenTimeData: List<ScreenTimeEntry> = emptyList(),
    val environmentData: EnvironmentData? = null
)

/**
 * Energy levels during session
 */
enum class EnergyLevel {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

/**
 * Stress levels during session
 */
enum class StressLevel {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

/**
 * Satisfaction levels
 */
enum class SatisfactionLevel {
    VERY_DISSATISFIED,
    DISSATISFIED,
    NEUTRAL,
    SATISFIED,
    VERY_SATISFIED
}

/**
 * Heart rate reading
 */
data class HeartRateReading(
    val timestamp: LocalDateTime,
    val bpm: Int,
    val variability: Float? = null
)

/**
 * App usage entry
 */
data class AppUsageEntry(
    val appName: String,
    val packageName: String,
    val startTime: LocalDateTime,
    val duration: Duration,
    val category: AppCategory
)

/**
 * App categories
 */
enum class AppCategory {
    PRODUCTIVITY,
    COMMUNICATION,
    ENTERTAINMENT,
    SOCIAL,
    NEWS,
    GAMING,
    DEVELOPMENT,
    DESIGN,
    EDUCATION,
    HEALTH,
    UTILITY,
    SYSTEM,
    UNKNOWN
}

/**
 * Screen time entry
 */
data class ScreenTimeEntry(
    val timestamp: LocalDateTime,
    val screenState: ScreenState,
    val duration: Duration,
    val appInForeground: String? = null
)

/**
 * Screen states
 */
enum class ScreenState {
    ON,
    OFF,
    LOCKED,
    UNLOCKED
}

/**
 * Environment data
 */
data class EnvironmentData(
    val noiseLevel: Float? = null,
    val lightingLevel: Float? = null,
    val temperature: Float? = null,
    val location: String? = null,
    val wifiNetwork: String? = null,
    val deviceCount: Int? = null,
    val weather: String? = null
)

/**
 * Session settings
 */
data class SessionSettings(
    val notificationsBlocked: Boolean = true,
    val appRestrictionsEnabled: Boolean = false,
    val restrictedApps: List<String> = emptyList(),
    val ambientSoundsEnabled: Boolean = false,
    val soundType: AmbientSoundType? = null,
    val volumeLevel: Int = 50,
    val pomodoroLength: Duration = Duration.ofMinutes(25),
    val breakLength: Duration = Duration.ofMinutes(5),
    val longBreakLength: Duration = Duration.ofMinutes(15),
    val pomodoroCycles: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartNextPomodoro: Boolean = false,
    val dailyGoals: DailyGoals = DailyGoals(),
    val reminders: List<Reminder> = emptyList(),
    val analyticsEnabled: Boolean = true,
    val privacyMode: PrivacyMode = PrivacyMode.BASIC
)

/**
 * Ambient sound types
 */
enum class AmbientSoundType {
    RAIN,
    OCEAN,
    FOREST,
    WHITE_NOISE,
    PINK_NOISE,
    BROWN_NOISE,
    CAFE,
    LIBRARY,
    NATURE,
    CUSTOM
}

/**
 * Daily goals
 */
data class DailyGoals(
    val focusTimeGoal: Duration = Duration.ofHours(4),
    val sessionGoal: Int = 6,
    val deepWorkGoal: Duration = Duration.ofHours(2),
    val interruptionsLimit: Int = 10,
    val noDistractionPeriods: List<TimeRange> = emptyList()
)

/**
 * Time range for no distraction periods
 */
data class TimeRange(
    val startTime: String, // HH:mm format
    val endTime: String   // HH:mm format
)

/**
 * Reminder
 */
data class Reminder(
    val id: String,
    val type: ReminderType,
    val message: String,
    val triggerTime: LocalDateTime,
    val isEnabled: Boolean = true,
    val recurring: Boolean = false,
    val recurrencePattern: RecurrencePattern? = null
)

/**
 * Reminder types
 */
enum class ReminderType {
    SESSION_START,
    BREAK_TIME,
    SESSION_END,
    DAILY_GOAL,
    HYDRATION,
    EYE_REST,
    STRETCH,
    POSTURE_CHECK,
    CUSTOM
}

/**
 * Recurrence pattern
 */
data class RecurrencePattern(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val daysOfWeek: List<java.time.DayOfWeek> = emptyList(),
    val endDate: LocalDateTime? = null
)

/**
 * Recurrence frequency
 */
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    HOURLY
}

/**
 * Privacy modes
 */
enum class PrivacyMode {
    BASIC,
    ENHANCED,
    MAXIMUM,
    OFF
}

/**
 * Mood levels
 */
enum class MoodLevel {
    VERY_POOR,
    POOR,
    NEUTRAL,
    GOOD,
    VERY_GOOD
}

/**
 * Session reflection
 */
data class SessionReflection(
    val whatWentWell: String,
    val whatCouldBeImproved: String,
    val keyInsights: String,
    val distractionsIdentified: List<String>,
    val strategiesToTry: List<String>,
    val overallRating: Int, // 1-5
    val wouldRepeat: Boolean,
    val nextSessionPlan: String
)

/**
 * Achievement
 */
data class Achievement(
    val id: String,
    val type: AchievementType,
    val title: String,
    val description: String,
    val icon: String,
    val points: Int,
    val unlockedAt: LocalDateTime,
    val rarity: AchievementRarity
)

/**
 * Achievement types
 */
enum class AchievementType {
    STREAK,
    TOTAL_TIME,
    DAILY_GOAL,
    WEEKLY_GOAL,
    DEEP_WORK,
    NO_DISTRACTIONS,
    EARLY_BIRD,
    NIGHT_OWL,
    CONSISTENCY,
    MILESTONE
}

/**
 * Achievement rarity
 */
enum class AchievementRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

/**
 * Focus session statistics
 */
data class FocusStats(
    val totalSessions: Int,
    val totalFocusTime: Duration,
    val averageSessionLength: Duration,
    val longestSession: Duration,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalInterruptions: Int,
    val averageInterruptions: Int,
    val mostProductiveHour: Int,
    val mostProductiveDay: java.time.DayOfWeek,
    val weeklyFocusTime: Map<java.time.DayOfWeek, Duration>,
    val monthlyFocusTime: Map<java.time.YearMonth, Duration>,
    val productivityTrend: List<ProductivityDataPoint>,
    val sessionTypeDistribution: Map<SessionType, Int>,
    val topDistractions: List<Distractor>,
    val achievements: List<Achievement>
)

/**
 * Productivity data point for trends
 */
data class ProductivityDataPoint(
    val date: LocalDateTime,
    val focusTime: Duration,
    val sessionCount: Int,
    val averageFocusScore: Float,
    val interruptionRate: Float
)

/**
 * Distractor analysis
 */
data class Distractor(
    val type: InterruptionType,
    val source: InterruptionSource,
    val count: Int,
    val totalTimeLost: Duration,
    val frequency: Float // times per session
)

/**
 * Focus goal
 */
data class FocusGoal(
    val id: String,
    val type: GoalType,
    val title: String,
    val description: String,
    val targetValue: Float,
    val currentValue: Float = 0f,
    val unit: String,
    val deadline: LocalDateTime? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime? = null
) {
    /**
     * Check if goal is achieved
     */
    fun isAchieved(): Boolean = currentValue >= targetValue

    /**
     * Get progress percentage
     */
    fun getProgress(): Float = if (targetValue > 0) currentValue / targetValue else 0f
}

/**
 * Goal types
 */
enum class GoalType {
    DAILY_FOCUS_TIME,
    WEEKLY_FOCUS_TIME,
    MONTHLY_FOCUS_TIME,
    DAILY_SESSIONS,
    WEEKLY_SESSIONS,
    STREAK_DAYS,
    DEEP_WORK_TIME,
    REDUCE_DISTRACTIONS,
    CONSISTENCY,
    CUSTOM
}

/**
 * Focus session template
 */
data class FocusTemplate(
    val id: String,
    val name: String,
    val description: String,
    val sessionType: SessionType,
    val plannedDuration: Duration,
    val settings: SessionSettings,
    val defaultTasks: List<FocusTask>,
    val tags: List<String>,
    val isPublic: Boolean = false,
    val usageCount: Int = 0,
    val rating: Float = 0f,
    val createdBy: String,
    val createdAt: LocalDateTime
)