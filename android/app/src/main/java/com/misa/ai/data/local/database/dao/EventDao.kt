package com.misa.ai.data.local.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.misa.ai.data.local.database.entity.EventEntity
import com.misa.ai.data.local.database.entity.AttendeeEntity
import com.misa.ai.data.local.database.entity.ReminderEntity
import com.misa.ai.data.local.database.entity.AttachmentEntity
import java.time.LocalDateTime

/**
 * Data Access Object for calendar events
 * Provides database operations for event management
 */
@Dao
interface EventDao {

    /**
     * Get all events in a date range
     */
    @Query("""
        SELECT * FROM events
        WHERE startTime >= :startTime AND endTime <= :endTime
        ORDER BY startTime ASC
    """)
    fun getEventsInRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<EventEntity>>

    /**
     * Get events for specific calendars
     */
    @Query("""
        SELECT * FROM events
        WHERE calendarId IN (:calendarIds)
        AND startTime >= :startTime AND endTime <= :endTime
        ORDER BY startTime ASC
    """)
    fun getEventsForCalendars(
        calendarIds: List<String>,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<EventEntity>>

    /**
     * Get event by ID
     */
    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): EventEntity?

    /**
     * Search events by query
     */
    @Query("""
        SELECT * FROM events
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY startTime DESC
        LIMIT :limit
    """)
    suspend fun searchEvents(query: String, limit: Int = 50): List<EventEntity>

    /**
     * Get events with reminders
     */
    @Query("""
        SELECT e.* FROM events e
        INNER JOIN reminders r ON e.id = r.eventId
        WHERE r.enabled = 1
        ORDER BY e.startTime ASC
    """)
    suspend fun getEventsWithReminders(): List<EventEntity>

    /**
     * Insert or update event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: EventEntity)

    /**
     * Insert or update multiple events
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<EventEntity>)

    /**
     * Delete event
     */
    @Delete
    suspend fun deleteEvent(event: EventEntity)

    /**
     * Delete event by ID
     */
    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    /**
     * Delete multiple events
     */
    @Query("DELETE FROM events WHERE id IN (:eventIds)")
    suspend fun deleteEventsByIds(eventIds: List<String>)

    /**
     * Get events by status
     */
    @Query("SELECT * FROM events WHERE status = :status ORDER BY startTime ASC")
    fun getEventsByStatus(status: String): Flow<List<EventEntity>>

    /**
     * Get recurring events
     */
    @Query("SELECT * FROM events WHERE recurrenceJson IS NOT NULL ORDER BY startTime ASC")
    fun getRecurringEvents(): Flow<List<EventEntity>>

    /**
     * Get today's events
     */
    @Query("""
        SELECT * FROM events
        WHERE date(startTime) = date('now') OR date(endTime) = date('now')
        ORDER BY startTime ASC
    """)
    fun getTodayEvents(): Flow<List<EventEntity>>

    /**
     * Get events in next N days
     */
    @Query("""
        SELECT * FROM events
        WHERE startTime >= :startTime AND startTime <= :endTime
        ORDER BY startTime ASC
        LIMIT :limit
    """)
    suspend fun getUpcomingEvents(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        limit: Int
    ): List<EventEntity>

    /**
     * Get events with conflicts
     */
    @Query("""
        SELECT * FROM events
        WHERE (:startTime < endTime AND :endTime > startTime)
        AND id != :excludeEventId
        ORDER BY startTime ASC
    """)
    suspend fun getConflictingEvents(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        excludeEventId: String?
    ): List<EventEntity>

    /**
     * Get events by source
     */
    @Query("SELECT * FROM events WHERE source = :source ORDER BY startTime DESC")
    fun getEventsBySource(source: String): Flow<List<EventEntity>>

    /**
     * Count events in date range
     */
    @Query("""
        SELECT COUNT(*) FROM events
        WHERE startTime >= :startTime AND endTime <= :endTime
    """)
    suspend fun countEventsInRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Int

    /**
     * Get events with specific attendees
     */
    @Query("""
        SELECT DISTINCT e.* FROM events e
        INNER JOIN attendees a ON e.id = a.eventId
        WHERE a.email IN (:emails)
        ORDER BY e.startTime ASC
    """)
    suspend fun getEventsWithAttendees(emails: List<String>): List<EventEntity>

    /**
     * Clean up old events
     */
    @Query("DELETE FROM events WHERE endTime < :cutoffDate AND status != 'Permanent'")
    suspend fun cleanupOldEvents(cutoffDate: LocalDateTime): Int
}

/**
 * DAO for calendar management
 */
@Dao
interface CalendarDao {

    @Query("SELECT * FROM calendars ORDER BY name ASC")
    fun getAllCalendars(): Flow<List<CalendarEntity>>

    @Query("SELECT * FROM calendars WHERE id = :calendarId")
    suspend fun getCalendarById(calendarId: String): CalendarEntity?

    @Query("SELECT * FROM calendars WHERE isVisible = 1 ORDER BY name ASC")
    fun getVisibleCalendars(): Flow<List<CalendarEntity>>

    @Query("SELECT * FROM calendars WHERE source = :source ORDER BY name ASC")
    fun getCalendarsBySource(source: String): Flow<List<CalendarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendar(calendar: CalendarEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendars(calendars: List<CalendarEntity>)

    @Delete
    suspend fun deleteCalendar(calendar: CalendarEntity)

    @Query("DELETE FROM calendars WHERE id = :calendarId")
    suspend fun deleteCalendarById(calendarId: String)
}

/**
 * DAO for attendees
 */
@Dao
interface AttendeeDao {

    @Query("SELECT * FROM attendees WHERE eventId = :eventId")
    suspend fun getAttendeesForEvent(eventId: String): List<AttendeeEntity>

    @Query("SELECT * FROM attendees WHERE email = :email")
    fun getEventsForAttendee(email: String): Flow<List<AttendeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendees(attendees: List<AttendeeEntity>)

    @Delete
    suspend fun deleteAttendeesForEvent(eventId: String)

    @Query("DELETE FROM attendees WHERE eventId = :eventId")
    suspend fun deleteAttendeesByEventId(eventId: String)
}

/**
 * DAO for reminders
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE eventId = :eventId")
    suspend fun getRemindersForEvent(eventId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getEnabledReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Delete
    suspend fun deleteRemindersForEvent(eventId: String)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteRemindersByEventId(eventId: String)
}

/**
 * DAO for attachments
 */
@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE eventId = :eventId")
    suspend fun getAttachmentsForEvent(eventId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Delete
    suspend fun deleteAttachmentsForEvent(eventId: String)

    @Query("DELETE FROM attachments WHERE eventId = :eventId")
    suspend fun deleteAttachmentsByEventId(eventId: String)
}