package com.misa.ai.domain.repository

import com.misa.ai.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Calendar repository interface
 * Defines the contract for calendar data operations
 */
interface CalendarRepository {

    /**
     * Get all events for a specific date range
     */
    fun getEvents(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        calendarIds: List<String>? = null
    ): Flow<Result<List<CalendarEvent>>>

    /**
     * Get a specific event by ID
     */
    suspend fun getEventById(eventId: String): Result<CalendarEvent?>

    /**
     * Create a new event
     */
    suspend fun createEvent(event: CalendarEvent): Result<CalendarEvent>

    /**
     * Update an existing event
     */
    suspend fun updateEvent(event: CalendarEvent): Result<CalendarEvent>

    /**
     * Delete an event
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>

    /**
     * Get all calendars
     */
    fun getCalendars(): Flow<Result<List<Calendar>>>

    /**
     * Sync events with remote calendar services
     */
    suspend fun syncEvents(calendarId: String): Result<SyncResult>

    /**
     * Search events by query
     */
    suspend fun searchEvents(
        query: String,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null
    ): Result<List<CalendarEvent>>

    /**
     * Get events with reminders
     */
    suspend fun getEventsWithReminders(): Result<List<CalendarEvent>>

    /**
     * Import events from file (ICS, CSV, etc.)
     */
    suspend fun importEvents(
        filePath: String,
        calendarId: String,
        format: ImportFormat
    ): Result<ImportResult>

    /**
     * Export events to file
     */
    suspend fun exportEvents(
        events: List<CalendarEvent>,
        filePath: String,
        format: ExportFormat
    ): Result<Unit>

    /**
     * Get availability for a time slot
     */
    suspend fun getAvailability(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        attendeeEmails: List<String>
    ): Result<List<TimeSlot>>

    /**
     * Suggest optimal meeting times
     */
    suspend fun suggestMeetingTimes(
        duration: kotlin.time.Duration,
        attendees: List<String>,
        preferredTimes: List<TimeRange>,
        dateRange: DateRange
    ): Result<List<MeetingSuggestion>>

    /**
     * Batch operations
     */
    suspend fun batchCreateEvents(events: List<CalendarEvent>): Result<List<CalendarEvent>>
    suspend fun batchUpdateEvents(events: List<CalendarEvent>): Result<List<CalendarEvent>>
    suspend fun batchDeleteEvents(eventIds: List<String>): Result<Unit>

    /**
     * Subscribe to real-time updates
     */
    fun subscribeToCalendarUpdates(calendarId: String): Flow<CalendarUpdate>
}

/**
 * Calendar domain model
 */
data class Calendar(
    val id: String,
    val name: String,
    val description: String? = null,
    val color: String,
    val isVisible: Boolean = true,
    val isSyncEnabled: Boolean = true,
    val source: EventSource,
    val accountName: String? = null,
    val timezone: String,
    val canWrite: Boolean = true,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Sync result
 */
data class SyncResult(
    val success: Boolean,
    val eventsCreated: Int,
    val eventsUpdated: Int,
    val eventsDeleted: Int,
    val errors: List<String>,
    val lastSyncTime: LocalDateTime
)

/**
 * Import result
 */
data class ImportResult(
    val success: Boolean,
    val eventsImported: Int,
    val eventsSkipped: Int,
    val errors: List<ImportError>
)

data class ImportError(
    val line: Int,
    val message: String,
    val rawData: String? = null
)

/**
 * Time slot availability
 */
data class TimeSlot(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val availability: AvailabilityStatus,
    val conflicts: List<String> = emptyList()
)

enum class AvailabilityStatus {
    Available,
    Busy,
    Tentative,
    OutOfOffice
}

/**
 * Time range
 */
data class TimeRange(
    val startTime: String, // "09:00"
    val endTime: String    // "17:00"
)

/**
 * Date range
 */
data class DateRange(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

/**
 * Meeting suggestion
 */
data class MeetingSuggestion(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val score: Double, // 0.0 to 1.0
    val reasons: List<String>,
    val conflicts: List<String> = emptyList()
)

/**
 * Calendar update event
 */
sealed class CalendarUpdate {
    data class EventCreated(val event: CalendarEvent) : CalendarUpdate()
    data class EventUpdated(val event: CalendarEvent) : CalendarUpdate()
    data class EventDeleted(val eventId: String) : CalendarUpdate()
    data class CalendarChanged(val calendar: Calendar) : CalendarUpdate()
}

/**
 * Import format
 */
enum class ImportFormat {
    ICS,
    CSV,
    JSON
}

/**
 * Export format
 */
enum class ExportFormat {
    ICS,
    CSV,
    JSON,
    PDF
}