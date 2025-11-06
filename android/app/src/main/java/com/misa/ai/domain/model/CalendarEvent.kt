package com.misa.ai.domain.model

import java.time.LocalDateTime
import java.time.Duration

/**
 * Calendar event domain model
 * Represents a single calendar event with all its properties
 */
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String? = null,
    val isAllDay: Boolean = false,
    val attendees: List<Attendee> = emptyList(),
    val recurrence: RecurrenceRule? = null,
    val reminders: List<Reminder> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val color: String? = null,
    val visibility: EventVisibility = EventVisibility.Default,
    val status: EventStatus = EventStatus.Confirmed,
    val organizer: String? = null,
    val calendarId: String,
    val source: EventSource = EventSource.Local,
    val lastModified: LocalDateTime = LocalDateTime.now(),
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Check if the event is currently ongoing
     */
    fun isOngoing(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(startTime) && now.isBefore(endTime)
    }

    /**
     * Check if the event is upcoming
     */
    fun isUpcoming(): Boolean {
        return startTime.isAfter(LocalDateTime.now())
    }

    /**
     * Check if the event has ended
     */
    fun hasEnded(): Boolean {
        return endTime.isBefore(LocalDateTime.now())
    }

    /**
     * Get the duration of the event
     */
    fun getDuration(): Duration {
        return Duration.between(startTime, endTime)
    }

    /**
     * Check if the event is a multi-day event
     */
    fun isMultiDay(): Boolean {
        return !startTime.toLocalDate().isEqual(endTime.toLocalDate())
    }

    /**
     * Check if the event conflicts with another event
     */
    fun conflictsWith(other: CalendarEvent): Boolean {
        return startTime.isBefore(other.endTime) && endTime.isAfter(other.startTime)
    }
}

/**
 * Event attendee information
 */
data class Attendee(
    val email: String,
    val name: String? = null,
    val status: AttendanceStatus = AttendanceStatus.Pending,
    val isOrganizer: Boolean = false,
    val isOptional: Boolean = false,
    val comment: String? = null
)

/**
 * Recurrence rule for repeating events
 */
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val endDate: LocalDateTime? = null,
    val occurrences: Int? = null,
    val daysOfWeek: List<Int> = emptyList(), // For weekly recurrence
    val dayOfMonth: Int? = null, // For monthly recurrence
    val exceptions: List<LocalDateTime> = emptyList() // Exception dates
)

/**
 * Event reminder
 */
data class Reminder(
    val id: String,
    val type: ReminderType,
    val minutesBefore: Int,
    val enabled: Boolean = true
)

/**
 * Event attachment
 */
data class Attachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val url: String? = null,
    val localPath: String? = null
)

/**
 * Event visibility levels
 */
enum class EventVisibility {
    Default,
    Public,
    Private,
    Confidential
}

/**
 * Event status
 */
enum class EventStatus {
    Tentative,
    Confirmed,
    Cancelled
}

/**
 * Attendance status
 */
enum class AttendanceStatus {
    Pending,
    Accepted,
    Declined,
    Tentative,
    Delegated
}

/**
 * Event source
 */
enum class EventSource {
    Local,
    GoogleCalendar,
    Outlook,
    AppleCalendar,
    Other
}

/**
 * Recurrence frequency
 */
enum class RecurrenceFrequency {
    Daily,
    Weekly,
    Monthly,
    Yearly
}

/**
 * Reminder type
 */
enum class ReminderType {
    Notification,
    Email,
    SMS
}