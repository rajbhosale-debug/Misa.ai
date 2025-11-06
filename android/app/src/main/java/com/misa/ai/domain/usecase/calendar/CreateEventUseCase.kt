package com.misa.ai.domain.usecase.calendar

import com.misa.ai.domain.model.*
import com.misa.ai.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.Duration
import javax.inject.Inject

/**
 * Use case for creating calendar events
 * Handles validation, default values, and AI-powered suggestions
 */
class CreateEventUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val suggestMeetingTimesUseCase: SuggestMeetingTimesUseCase
) {

    /**
     * Create a new event with validation and AI suggestions
     */
    suspend fun createEvent(
        event: CalendarEvent,
        enableAISuggestions: Boolean = true
    ): Result<CalendarEvent> {
        try {
            // Validate event
            val validationResult = validateEvent(event)
            if (!validationResult.isValid) {
                return Result.failure(Exception(validationResult.errorMessage))
            }

            // Apply AI suggestions if enabled
            val enhancedEvent = if (enableAISuggestions) {
                enhanceEventWithAI(event)
            } else {
                event
            }

            // Create the event
            val result = calendarRepository.createEvent(enhancedEvent)

            return result
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create event from natural language input
     */
    suspend fun createEventFromNaturalLanguage(
        input: String,
        calendarId: String,
        enableAISuggestions: Boolean = true
    ): Result<CalendarEvent> {
        try {
            // Parse natural language input
            val parsedEvent = parseNaturalLanguageInput(input, calendarId)

            return createEvent(parsedEvent, enableAISuggestions)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create recurring event
     */
    suspend fun createRecurringEvent(
        baseEvent: CalendarEvent,
        recurrence: RecurrenceRule,
        enableAISuggestions: Boolean = true
    ): Result<CalendarEvent> {
        try {
            val recurringEvent = baseEvent.copy(recurrence = recurrence)
            return createEvent(recurringEvent, enableAISuggestions)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create meeting with attendees
     */
    suspend fun createMeeting(
        title: String,
        description: String? = null,
        duration: Duration,
        attendees: List<String>,
        preferredTimeRange: TimeRange,
        dateRange: DateRange,
        calendarId: String,
        enableAISuggestions: Boolean = true
    ): Result<CalendarEvent> {
        try {
            // Get meeting time suggestions
            val suggestionsResult = suggestMeetingTimesUseCase.suggestMeetingTimes(
                duration = duration,
                attendees = attendees,
                preferredTimes = listOf(preferredTimeRange),
                dateRange = dateRange
            )

            if (suggestionsResult.isFailure) {
                return Result.failure(Exception("Failed to get meeting time suggestions"))
            }

            val suggestions = suggestionsResult.getOrThrow()
            if (suggestions.isEmpty()) {
                return Result.failure(Exception("No suitable meeting times found"))
            }

            // Use the best suggestion
            val bestSuggestion = suggestions.first()
            val meetingEvent = CalendarEvent(
                id = generateEventId(),
                title = title,
                description = description,
                startTime = bestSuggestion.startTime,
                endTime = bestSuggestion.endTime,
                attendees = attendees.map { email ->
                    Attendee(email = email, status = AttendanceStatus.Pending)
                },
                calendarId = calendarId,
                status = EventStatus.Tentative
            )

            return createEvent(meetingEvent, enableAISuggestions)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Validate event data
     */
    private fun validateEvent(event: CalendarEvent): ValidationResult {
        // Check required fields
        if (event.title.isBlank()) {
            return ValidationResult(false, "Event title cannot be empty")
        }

        if (event.endTime.isBefore(event.startTime)) {
            return ValidationResult(false, "End time must be after start time")
        }

        // Validate recurrence rules
        event.recurrence?.let { recurrence ->
            if (recurrence.interval <= 0) {
                return ValidationResult(false, "Recurrence interval must be positive")
            }

            if (recurrence.endDate != null && recurrence.endDate!!.isBefore(event.startTime)) {
                return ValidationResult(false, "Recurrence end date must be after start time")
            }

            if (recurrence.occurrences != null && recurrence.occurrences!! <= 0) {
                return ValidationResult(false, "Number of occurrences must be positive")
            }
        }

        // Validate attendees
        event.attendees.forEach { attendee ->
            if (attendee.email.isBlank()) {
                return ValidationResult(false, "Attendee email cannot be empty")
            }
            // Simple email validation
            if (!isValidEmail(attendee.email)) {
                return ValidationResult(false, "Invalid email format: ${attendee.email}")
            }
        }

        return ValidationResult(true)
    }

    /**
     * Enhance event with AI-powered suggestions
     */
    private suspend fun enhanceEventWithAI(event: CalendarEvent): CalendarEvent {
        var enhancedEvent = event

        // Suggest optimal reminder time based on event type and attendees
        if (event.reminders.isEmpty()) {
            val suggestedReminder = suggestReminderTime(event)
            enhancedEvent = enhancedEvent.copy(
                reminders = listOf(suggestedReminder)
            )
        }

        // Suggest meeting location if not provided and it's a meeting
        if (event.location.isNullOrEmpty() && event.attendees.isNotEmpty()) {
            val suggestedLocation = suggestMeetingLocation(event)
            enhancedEvent = enhancedEvent.copy(location = suggestedLocation)
        }

        // Suggest event color based on category
        if (event.color.isNullOrEmpty()) {
            val suggestedColor = suggestEventColor(event)
            enhancedEvent = enhancedEvent.copy(color = suggestedColor)
        }

        return enhancedEvent
    }

    /**
     * Parse natural language input to create event
     */
    private fun parseNaturalLanguageInput(input: String, calendarId: String): CalendarEvent {
        val now = LocalDateTime.now()

        // Simple parsing logic (in production, would use NLP)
        var title = input
        var startTime = now
        var endTime = now.plusHours(1)
        var location: String? = null

        // Extract time patterns
        val timePattern = Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm)", RegexOption.IGNORE_CASE)
        val datePattern = Regex("(today|tomorrow|next week|\\d{4}-\\d{2}-\\d{2})", RegexOption.IGNORE_CASE)

        // Simple keyword extraction for demonstration
        when {
            input.contains("meeting", ignoreCase = true) -> {
                endTime = now.plusHours(1)
            }
            input.contains("lunch", ignoreCase = true) -> {
                startTime = now.withHour(12).withMinute(0)
                endTime = startTime.plusHours(1)
            }
            input.contains("dinner", ignoreCase = true) -> {
                startTime = now.withHour(18).withMinute(0)
                endTime = startTime.plusHours(1.5)
            }
        }

        // Extract location (simple pattern)
        val locationPattern = Regex("(?:at|in)\\s+([A-Za-z0-9\\s]+)")
        val locationMatch = locationPattern.find(input)
        if (locationMatch != null) {
            location = locationMatch.groupValues[1].trim()
            title = title.replace(locationMatch.value, "").trim()
        }

        // Extract title (remove time and location patterns)
        title = title.replace(timePattern, "").trim()
        title = title.replace(datePattern, "").trim()

        if (title.isBlank()) {
            title = "New Event"
        }

        return CalendarEvent(
            id = generateEventId(),
            title = title,
            startTime = startTime,
            endTime = endTime,
            location = location,
            calendarId = calendarId
        )
    }

    /**
     * Suggest reminder time based on event characteristics
     */
    private fun suggestReminderTime(event: CalendarEvent): Reminder {
        val duration = event.getDuration()
        val minutesBefore = when {
            duration.toHours() >= 2 -> 30
            duration.toHours() >= 1 -> 15
            duration.toMinutes() >= 30 -> 10
            else -> 5
        }

        return Reminder(
            id = generateReminderId(),
            type = ReminderType.Notification,
            minutesBefore = minutesBefore
        )
    }

    /**
     * Suggest meeting location
     */
    private suspend fun suggestMeetingLocation(event: CalendarEvent): String? {
        // Simple suggestions based on context
        return when {
            event.title.contains("coffee", ignoreCase = true) -> "Coffee Shop"
            event.title.contains("lunch", ignoreCase = true) -> "Restaurant"
            event.title.contains("meeting", ignoreCase = true) -> "Conference Room"
            event.attendees.size > 5 -> "Large Conference Room"
            else -> "Meeting Room"
        }
    }

    /**
     * Suggest event color based on category
     */
    private fun suggestEventColor(event: CalendarEvent): String {
        return when {
            event.title.contains("meeting", ignoreCase = true) -> "#2196F3" // Blue
            event.title.contains("deadline", ignoreCase = true) -> "#F44336" // Red
            event.title.contains("personal", ignoreCase = true) -> "#4CAF50" // Green
            event.title.contains("birthday", ignoreCase = true) -> "#FF9800" // Orange
            event.attendees.isNotEmpty() -> "#9C27B0" // Purple
            else -> "#607D8B" // Gray
        }
    }

    /**
     * Simple email validation
     */
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    /**
     * Generate unique event ID
     */
    private fun generateEventId(): String {
        return "event_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * Generate unique reminder ID
     */
    private fun generateReminderId(): String {
        return "reminder_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)