package com.misa.ai.domain.usecase.calendar

import com.misa.ai.domain.model.CalendarEvent
import com.misa.ai.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for retrieving calendar events
 * Handles business logic for event retrieval with filtering and sorting
 */
class GetEventsUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository
) {

    /**
     * Get events for a specific date range with optional filtering
     */
    fun getEvents(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        filters: EventFilters = EventFilters(),
        sorting: EventSorting = EventSorting()
    ): Flow<Result<List<CalendarEvent>>> {
        return calendarRepository.getEvents(startTime, endTime, filters.calendarIds)
            .map { result ->
                result.map { events ->
                    events
                        .filter { event -> applyFilters(event, filters) }
                        .sortedWith { a, b -> applySorting(a, b, sorting) }
                }
            }
    }

    /**
     * Get events for today
     */
    fun getTodayEvents(filters: EventFilters = EventFilters()): Flow<Result<List<CalendarEvent>>> {
        val now = LocalDateTime.now()
        val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999)

        return getEvents(startOfDay, endOfDay, filters)
    }

    /**
     * Get events for this week
     */
    fun getWeekEvents(filters: EventFilters = EventFilters()): Flow<Result<List<CalendarEvent>>> {
        val now = LocalDateTime.now()
        val startOfWeek = now.minusDays(now.dayOfWeek.value.toLong() - 1L)
            .withHour(0).withMinute(0).withSecond(0).withNano(0)
        val endOfWeek = startOfWeek.plusDays(6L)
            .withHour(23).withMinute(59).withSecond(59).withNano(999999999)

        return getEvents(startOfWeek, endOfWeek, filters)
    }

    /**
     * Get upcoming events
     */
    suspend fun getUpcomingEvents(
        limit: Int = 10,
        filters: EventFilters = EventFilters()
    ): Result<List<CalendarEvent>> {
        val now = LocalDateTime.now()
        val futureDate = now.plusMonths(1)

        return calendarRepository.getEvents(now, futureDate, filters.calendarIds)
            .map { events ->
                events
                    .filter { event -> applyFilters(event, filters) }
                    .filter { it.isUpcoming() }
                    .sortedBy { it.startTime }
                    .take(limit)
            }
    }

    /**
     * Get ongoing events
     */
    suspend fun getOngoingEvents(filters: EventFilters = EventFilters()): Result<List<CalendarEvent>> {
        val now = LocalDateTime.now()
        val oneHourAgo = now.minusHours(1)
        val oneHourLater = now.plusHours(1)

        return calendarRepository.getEvents(oneHourAgo, oneHourLater, filters.calendarIds)
            .map { events ->
                events
                    .filter { event -> applyFilters(event, filters) }
                    .filter { it.isOngoing() }
            }
    }

    /**
     * Get events by calendar
     */
    fun getEventsByCalendar(
        calendarId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<Result<List<CalendarEvent>>> {
        return getEvents(startTime, endTime, EventFilters(calendarIds = listOf(calendarId)))
    }

    /**
     * Search events
     */
    suspend fun searchEvents(
        query: String,
        filters: EventFilters = EventFilters()
    ): Result<List<CalendarEvent>> {
        return calendarRepository.searchEvents(query)
            .map { events -> events.filter { applyFilters(it, filters) } }
    }

    /**
     * Get conflicting events for a time slot
     */
    suspend fun getConflictingEvents(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        excludeEventId: String? = null
    ): Result<List<CalendarEvent>> {
        return calendarRepository.getEvents(startTime, endTime)
            .map { events ->
                events.filter { event ->
                    event.conflictsWith(
                        CalendarEvent(
                            id = excludeEventId ?: "temp",
                            title = "temp",
                            startTime = startTime,
                            endTime = endTime,
                            calendarId = "temp"
                        )
                    )
                }
            }
    }

    /**
     * Apply filters to events
     */
    private fun applyFilters(event: CalendarEvent, filters: EventFilters): Boolean {
        // Calendar filter
        if (filters.calendarIds.isNotEmpty() && event.calendarId !in filters.calendarIds) {
            return false
        }

        // Status filter
        if (filters.statuses.isNotEmpty() && event.status !in filters.statuses) {
            return false
        }

        // Source filter
        if (filters.sources.isNotEmpty() && event.source !in filters.sources) {
            return false
        }

        // Keywords filter
        if (filters.keywords.isNotEmpty()) {
            val searchText = (event.title + " " + (event.description ?: "")).lowercase()
            if (!filters.keywords.any { keyword ->
                searchText.contains(keyword.lowercase())
            }) {
                return false
            }
        }

        // All-day filter
        if (filters.allDayOnly && !event.isAllDay) {
            return false
        }

        // Multi-day filter
        if (filters.multiDayOnly && !event.isMultiDay()) {
            return false
        }

        // Has attendees filter
        if (filters.hasAttendeesOnly && event.attendees.isEmpty()) {
            return false
        }

        // Location filter
        if (filters.hasLocationOnly && event.location.isNullOrEmpty()) {
            return false
        }

        return true
    }

    /**
     * Apply sorting to events
     */
    private fun applySorting(a: CalendarEvent, b: CalendarEvent, sorting: EventSorting): Int {
        return when (sorting.field) {
            SortingField.START_TIME -> {
                val comparison = a.startTime.compareTo(b.startTime)
                if (sorting.ascending) comparison else -comparison
            }
            SortingField.TITLE -> {
                val comparison = a.title.compareTo(b.title, ignoreCase = true)
                if (sorting.ascending) comparison else -comparison
            }
            SortingField.DURATION -> {
                val comparison = a.getDuration().compareTo(b.getDuration())
                if (sorting.ascending) comparison else -comparison
            }
            SortingField.CREATED -> {
                val comparison = a.lastModified.compareTo(b.lastModified)
                if (sorting.ascending) comparison else -comparison
            }
        }
    }
}

/**
 * Event filters
 */
data class EventFilters(
    val calendarIds: List<String> = emptyList(),
    val statuses: List<com.misa.ai.domain.model.EventStatus> = emptyList(),
    val sources: List<com.misa.ai.domain.model.EventSource> = emptyList(),
    val keywords: List<String> = emptyList(),
    val allDayOnly: Boolean = false,
    val multiDayOnly: Boolean = false,
    val hasAttendeesOnly: Boolean = false,
    val hasLocationOnly: Boolean = false
)

/**
 * Event sorting
 */
data class EventSorting(
    val field: SortingField = SortingField.START_TIME,
    val ascending: Boolean = true
)

enum class SortingField {
    START_TIME,
    TITLE,
    DURATION,
    CREATED
}