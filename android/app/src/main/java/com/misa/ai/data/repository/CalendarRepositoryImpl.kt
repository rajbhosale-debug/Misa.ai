package com.misa.ai.data.repository

import com.misa.ai.domain.repository.CalendarRepository
import com.misa.ai.domain.model.*
import com.misa.ai.data.local.database.CalendarDatabase
import com.misa.ai.data.local.database.entity.*
import com.misa.ai.data.remote.CalendarApiService
import com.misa.ai.data.mapper.CalendarMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calendar repository implementation
 * Provides offline-first calendar data management with sync capabilities
 */
@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val database: CalendarDatabase,
    private val apiService: CalendarApiService,
    private val mapper: CalendarMapper
) : CalendarRepository {

    private val eventDao = database.eventDao()
    private val calendarDao = database.calendarDao()
    private val attendeeDao = database.attendeeDao()
    private val reminderDao = database.reminderDao()
    private val attachmentDao = database.attachmentDao()

    override fun getEvents(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        calendarIds: List<String>?
    ): Flow<Result<List<CalendarEvent>>> {
        return if (calendarIds.isNullOrEmpty()) {
            eventDao.getEventsInRange(startTime, endTime)
        } else {
            eventDao.getEventsForCalendars(calendarIds, startTime, endTime)
        }.map { entities ->
            Result.success(
                coroutineScope {
                    entities.map { entity ->
                        async {
                            val attendees = attendeeDao.getAttendeesForEvent(entity.id)
                            val reminders = reminderDao.getRemindersForEvent(entity.id)
                            val attachments = attachmentDao.getAttachmentsForEvent(entity.id)
                            mapper.entityToDomain(entity, attendees, reminders, attachments)
                        }
                    }.awaitAll()
                }
            )
        }
    }

    override suspend fun getEventById(eventId: String): Result<CalendarEvent?> {
        return try {
            val eventEntity = eventDao.getEventById(eventId)
            if (eventEntity != null) {
                val attendees = attendeeDao.getAttendeesForEvent(eventId)
                val reminders = reminderDao.getRemindersForEvent(eventId)
                val attachments = attachmentDao.getAttachmentsForEvent(eventId)
                Result.success(mapper.entityToDomain(eventEntity, attendees, reminders, attachments))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createEvent(event: CalendarEvent): Result<CalendarEvent> {
        return try {
            // Save to local database first
            val eventEntity = mapper.domainToEntity(event)
            eventDao.upsertEvent(eventEntity)

            // Save related entities
            if (event.attendees.isNotEmpty()) {
                val attendeeEntities = event.attendees.map { attendee ->
                    AttendeeEntity(
                        eventId = event.id,
                        email = attendee.email,
                        name = attendee.name,
                        status = attendee.status,
                        isOrganizer = attendee.isOrganizer,
                        isOptional = attendee.isOptional,
                        comment = attendee.comment
                    )
                }
                attendeeDao.insertAttendees(attendeeEntities)
            }

            if (event.reminders.isNotEmpty()) {
                val reminderEntities = event.reminders.map { reminder ->
                    ReminderEntity(
                        id = reminder.id,
                        eventId = event.id,
                        type = reminder.type,
                        minutesBefore = reminder.minutesBefore,
                        enabled = reminder.enabled
                    )
                }
                reminderDao.insertReminders(reminderEntities)
            }

            if (event.attachments.isNotEmpty()) {
                val attachmentEntities = event.attachments.map { attachment ->
                    AttachmentEntity(
                        id = attachment.id,
                        eventId = event.id,
                        name = attachment.name,
                        mimeType = attachment.mimeType,
                        size = attachment.size,
                        url = attachment.url,
                        localPath = attachment.localPath
                    )
                }
                attachmentDao.insertAttachments(attachmentEntities)
            }

            // Sync with remote API if enabled
            if (event.source != EventSource.Local) {
                try {
                    val remoteEvent = apiService.createEvent(event)
                    // Update local with remote data if needed
                    Result.success(remoteEvent)
                } catch (e: Exception) {
                    // Remote sync failed, but local save succeeded
                    Result.success(event)
                }
            } else {
                Result.success(event)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: CalendarEvent): Result<CalendarEvent> {
        return try {
            // Delete existing related entities
            attendeeDao.deleteAttendeesByEventId(event.id)
            reminderDao.deleteRemindersByEventId(event.id)
            attachmentDao.deleteAttachmentsByEventId(event.id)

            // Update main event
            val eventEntity = mapper.domainToEntity(event)
            eventDao.upsertEvent(eventEntity)

            // Re-insert related entities
            if (event.attendees.isNotEmpty()) {
                val attendeeEntities = event.attendees.map { attendee ->
                    AttendeeEntity(
                        eventId = event.id,
                        email = attendee.email,
                        name = attendee.name,
                        status = attendee.status,
                        isOrganizer = attendee.isOrganizer,
                        isOptional = attendee.isOptional,
                        comment = attendee.comment
                    )
                }
                attendeeDao.insertAttendees(attendeeEntities)
            }

            if (event.reminders.isNotEmpty()) {
                val reminderEntities = event.reminders.map { reminder ->
                    ReminderEntity(
                        id = reminder.id,
                        eventId = event.id,
                        type = reminder.type,
                        minutesBefore = reminder.minutesBefore,
                        enabled = reminder.enabled
                    )
                }
                reminderDao.insertReminders(reminderEntities)
            }

            if (event.attachments.isNotEmpty()) {
                val attachmentEntities = event.attachments.map { attachment ->
                    AttachmentEntity(
                        id = attachment.id,
                        eventId = event.id,
                        name = attachment.name,
                        mimeType = attachment.mimeType,
                        size = attachment.size,
                        url = attachment.url,
                        localPath = attachment.localPath
                    )
                }
                attachmentDao.insertAttachments(attachmentEntities)
            }

            // Sync with remote API if applicable
            if (event.source != EventSource.Local) {
                try {
                    val remoteEvent = apiService.updateEvent(event)
                    Result.success(remoteEvent)
                } catch (e: Exception) {
                    Result.success(event)
                }
            } else {
                Result.success(event)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            // Delete from local database
            eventDao.deleteEventById(eventId)
            attendeeDao.deleteAttendeesByEventId(eventId)
            reminderDao.deleteRemindersByEventId(eventId)
            attachmentDao.deleteAttachmentsByEventId(eventId)

            // Delete from remote API if applicable
            try {
                apiService.deleteEvent(eventId)
            } catch (e: Exception) {
                // Remote delete failed, but local delete succeeded
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCalendars(): Flow<Result<List<Calendar>>> {
        return calendarDao.getAllCalendars().map { entities ->
            Result.success(entities.map { mapper.calendarEntityToDomain(it) })
        }
    }

    override suspend fun syncEvents(calendarId: String): Result<SyncResult> {
        return try {
            val remoteEvents = apiService.getEventsForCalendar(calendarId)
            var eventsCreated = 0
            var eventsUpdated = 0
            var eventsDeleted = 0
            val errors = mutableListOf<String>()

            for (remoteEvent in remoteEvents) {
                try {
                    val localEvent = eventDao.getEventById(remoteEvent.id)
                    if (localEvent == null) {
                        // New event
                        createEvent(remoteEvent)
                        eventsCreated++
                    } else {
                        // Update existing event
                        updateEvent(remoteEvent)
                        eventsUpdated++
                    }
                } catch (e: Exception) {
                    errors.add("Failed to sync event ${remoteEvent.id}: ${e.message}")
                }
            }

            val syncResult = SyncResult(
                success = errors.isEmpty(),
                eventsCreated = eventsCreated,
                eventsUpdated = eventsUpdated,
                eventsDeleted = eventsDeleted,
                errors = errors,
                lastSyncTime = LocalDateTime.now()
            )

            Result.success(syncResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchEvents(
        query: String,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): Result<List<CalendarEvent>> {
        return try {
            val entities = eventDao.searchEvents(query)
            val events = coroutineScope {
                entities.map { entity ->
                    async {
                        val attendees = attendeeDao.getAttendeesForEvent(entity.id)
                        val reminders = reminderDao.getRemindersForEvent(entity.id)
                        val attachments = attachmentDao.getAttachmentsForEvent(entity.id)
                        mapper.entityToDomain(entity, attendees, reminders, attachments)
                    }
                }.awaitAll()
            }.let { events ->
                if (startTime != null && endTime != null) {
                    events.filter { it.startTime >= startTime && it.endTime <= endTime }
                } else {
                    events
                }
            }

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventsWithReminders(): Result<List<CalendarEvent>> {
        return try {
            val entities = eventDao.getEventsWithReminders()
            val events = coroutineScope {
                entities.map { entity ->
                    async {
                        val attendees = attendeeDao.getAttendeesForEvent(entity.id)
                        val reminders = reminderDao.getRemindersForEvent(entity.id)
                        val attachments = attachmentDao.getAttachmentsForEvent(entity.id)
                        mapper.entityToDomain(entity, attendees, reminders, attachments)
                    }
                }.awaitAll()
            }

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importEvents(
        filePath: String,
        calendarId: String,
        format: ImportFormat
    ): Result<ImportResult> {
        return try {
            // Parse file based on format
            val events = when (format) {
                ImportFormat.ICS -> parseICSFile(filePath)
                ImportFormat.CSV -> parseCSVFile(filePath)
                ImportFormat.JSON -> parseJSONFile(filePath)
            }

            var eventsImported = 0
            var eventsSkipped = 0
            val errors = mutableListOf<ImportError>()

            events.forEachIndexed { index, event ->
                try {
                    createEvent(event.copy(calendarId = calendarId))
                    eventsImported++
                } catch (e: Exception) {
                    eventsSkipped++
                    errors.add(ImportError(index + 1, e.message ?: "Unknown error"))
                }
            }

            val importResult = ImportResult(
                success = errors.isEmpty(),
                eventsImported = eventsImported,
                eventsSkipped = eventsSkipped,
                errors = errors
            )

            Result.success(importResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportEvents(
        events: List<CalendarEvent>,
        filePath: String,
        format: ExportFormat
    ): Result<Unit> {
        return try {
            when (format) {
                ExportFormat.ICS -> exportToICS(events, filePath)
                ExportFormat.CSV -> exportToCSV(events, filePath)
                ExportFormat.JSON -> exportToJSON(events, filePath)
                ExportFormat.PDF -> exportToPDF(events, filePath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailability(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        attendeeEmails: List<String>
    ): Result<List<TimeSlot>> {
        return try {
            val timeSlots = mutableListOf<TimeSlot>()
            var currentTime = startTime

            // Generate 15-minute slots
            while (currentTime.plusMinutes(15).isBefore(endTime)) {
                val slotEnd = currentTime.plusMinutes(15)
                val conflicts = eventDao.getConflictingEvents(currentTime, slotEnd, null)

                val availability = if (conflicts.isEmpty()) {
                    AvailabilityStatus.Available
                } else {
                    AvailabilityStatus.Busy
                }

                timeSlots.add(TimeSlot(
                    startTime = currentTime,
                    endTime = slotEnd,
                    availability = availability,
                    conflicts = conflicts.map { it.id }
                ))

                currentTime = slotEnd
            }

            Result.success(timeSlots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun suggestMeetingTimes(
        duration: kotlin.time.Duration,
        attendees: List<String>,
        preferredTimes: List<TimeRange>,
        dateRange: DateRange
    ): Result<List<MeetingSuggestion>> {
        return try {
            val suggestions = mutableListOf<MeetingSuggestion>()
            val durationMinutes = duration.inWholeMinutes.toInt()
            var currentTime = dateRange.startDate

            while (currentTime.plusMinutes(durationMinutes.toLong()).isBefore(dateRange.endDate)) {
                val slotEnd = currentTime.plusMinutes(durationMinutes.toLong())
                val conflicts = eventDao.getConflictingEvents(currentTime, slotEnd, null)

                if (conflicts.isEmpty()) {
                    val score = calculateTimeSlotScore(currentTime, preferredTimes)
                    if (score >= 0.5) {
                        suggestions.add(MeetingSuggestion(
                            startTime = currentTime,
                            endTime = slotEnd,
                            score = score,
                            reasons = listOf("No conflicts detected", "Preferred time")
                        ))
                    }
                }

                currentTime = currentTime.plusMinutes(15) // Check next 15-minute slot
            }

            Result.success(suggestions.sortedByDescending { it.score }.take(10))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun batchCreateEvents(events: List<CalendarEvent>): Result<List<CalendarEvent>> {
        return try {
            val results = events.map { event ->
                createEvent(event).getOrThrow()
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun batchUpdateEvents(events: List<CalendarEvent>): Result<List<CalendarEvent>> {
        return try {
            val results = events.map { event ->
                updateEvent(event).getOrThrow()
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun batchDeleteEvents(eventIds: List<String>): Result<Unit> {
        return try {
            eventIds.forEach { eventId ->
                deleteEvent(eventId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun subscribeToCalendarUpdates(calendarId: String): Flow<CalendarUpdate> {
        // In a real implementation, this would use Room's flow of changes
        // or a more sophisticated change detection mechanism
        return eventDao.getEventsForCalendars(listOf(calendarId),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7)
        ).map { events ->
            CalendarUpdate.CalendarChanged(
                Calendar(
                    id = calendarId,
                    name = "Calendar",
                    color = "#2196F3",
                    isVisible = true,
                    isSyncEnabled = true,
                    source = EventSource.Local,
                    accountName = null,
                    timezone = "UTC",
                    canWrite = true,
                    metadata = emptyMap()
                )
            )
        }
    }

    // Private helper methods

    private suspend fun parseICSFile(filePath: String): List<CalendarEvent> {
        // Implementation for ICS file parsing
        // This would use a proper ICS parser library
        return emptyList()
    }

    private suspend fun parseCSVFile(filePath: String): List<CalendarEvent> {
        // Implementation for CSV file parsing
        return emptyList()
    }

    private suspend fun parseJSONFile(filePath: String): List<CalendarEvent> {
        // Implementation for JSON file parsing
        return emptyList()
    }

    private suspend fun exportToICS(events: List<CalendarEvent>, filePath: String) {
        // Implementation for ICS export
    }

    private suspend fun exportToCSV(events: List<CalendarEvent>, filePath: String) {
        // Implementation for CSV export
    }

    private suspend fun exportToJSON(events: List<CalendarEvent>, filePath: String) {
        // Implementation for JSON export
    }

    private suspend fun exportToPDF(events: List<CalendarEvent>, filePath: String) {
        // Implementation for PDF export
    }

    private fun calculateTimeSlotScore(
        time: LocalDateTime,
        preferredTimes: List<TimeRange>
    ): Double {
        val hour = time.hour
        val timeString = String.format("%02d:%02d", hour, time.minute)

        return preferredTimes.find { range ->
            timeString >= range.startTime && timeString <= range.endTime
        }?.let { 1.0 } ?: 0.3
    }
}