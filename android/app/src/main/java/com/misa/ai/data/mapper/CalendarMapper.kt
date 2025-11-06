package com.misa.ai.data.mapper

import com.misa.ai.domain.model.*
import com.misa.ai.data.local.database.entity.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting between domain models and database entities
 * Handles serialization of complex objects for database storage
 */
@Singleton
class CalendarMapper @Inject constructor(
    private val json: Json
) {

    /**
     * Convert event entity to domain model
     */
    fun entityToDomain(
        entity: EventEntity,
        attendees: List<AttendeeEntity> = emptyList(),
        reminders: List<ReminderEntity> = emptyList(),
        attachments: List<AttachmentEntity> = emptyList()
    ): CalendarEvent {
        return CalendarEvent(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            startTime = entity.startTime,
            endTime = entity.endTime,
            location = entity.location,
            isAllDay = entity.isAllDay,
            attendees = attendees.map { attendeeEntity ->
                Attendee(
                    email = attendeeEntity.email,
                    name = attendeeEntity.name,
                    status = attendeeEntity.status,
                    isOrganizer = attendeeEntity.isOrganizer,
                    isOptional = attendeeEntity.isOptional,
                    comment = attendeeEntity.comment
                )
            },
            recurrence = entity.recurrenceJson?.let {
                json.decodeFromString<RecurrenceRule>(it)
            },
            reminders = reminders.map { reminderEntity ->
                Reminder(
                    id = reminderEntity.id,
                    type = reminderEntity.type,
                    minutesBefore = reminderEntity.minutesBefore,
                    enabled = reminderEntity.enabled
                )
            },
            attachments = attachments.map { attachmentEntity ->
                Attachment(
                    id = attachmentEntity.id,
                    name = attachmentEntity.name,
                    mimeType = attachmentEntity.mimeType,
                    size = attachmentEntity.size,
                    url = attachmentEntity.url,
                    localPath = attachmentEntity.localPath
                )
            },
            color = entity.color,
            visibility = entity.visibility,
            status = entity.status,
            organizer = entity.organizer,
            calendarId = entity.calendarId,
            source = entity.source,
            lastModified = entity.lastModified,
            metadata = try {
                json.decodeFromString<Map<String, Any>>(entity.metadataJson)
            } catch (e: Exception) {
                emptyMap()
            }
        )
    }

    /**
     * Convert domain model to event entity
     */
    fun domainToEntity(event: CalendarEvent): EventEntity {
        return EventEntity(
            id = event.id,
            title = event.title,
            description = event.description,
            startTime = event.startTime,
            endTime = event.endTime,
            location = event.location,
            isAllDay = event.isAllDay,
            attendeesJson = json.encodeToString(event.attendees),
            recurrenceJson = event.recurrence?.let { json.encodeToString(it) },
            remindersJson = json.encodeToString(event.reminders),
            attachmentsJson = json.encodeToString(event.attachments),
            color = event.color,
            visibility = event.visibility,
            status = event.status,
            organizer = event.organizer,
            calendarId = event.calendarId,
            source = event.source,
            lastModified = event.lastModified,
            metadataJson = json.encodeToString(event.metadata)
        )
    }

    /**
     * Convert calendar entity to domain model
     */
    fun calendarEntityToDomain(entity: CalendarEntity): Calendar {
        return Calendar(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            color = entity.color,
            isVisible = entity.isVisible,
            isSyncEnabled = entity.isSyncEnabled,
            source = entity.source,
            accountName = entity.accountName,
            timezone = entity.timezone,
            canWrite = entity.canWrite,
            metadata = try {
                json.decodeFromString<Map<String, Any>>(entity.metadataJson)
            } catch (e: Exception) {
                emptyMap()
            }
        )
    }

    /**
     * Convert domain model to calendar entity
     */
    fun domainToCalendarEntity(calendar: Calendar): CalendarEntity {
        return CalendarEntity(
            id = calendar.id,
            name = calendar.name,
            description = calendar.description,
            color = calendar.color,
            isVisible = calendar.isVisible,
            isSyncEnabled = calendar.isSyncEnabled,
            source = calendar.source,
            accountName = calendar.accountName,
            timezone = calendar.timezone,
            canWrite = calendar.canWrite,
            metadataJson = json.encodeToString(calendar.metadata)
        )
    }

    /**
     * Convert list of event entities to domain models
     */
    fun entitiesToDomain(
        entities: List<EventEntity>,
        attendeesMap: Map<String, List<AttendeeEntity>> = emptyMap(),
        remindersMap: Map<String, List<ReminderEntity>> = emptyMap(),
        attachmentsMap: Map<String, List<AttachmentEntity>> = emptyMap()
    ): List<CalendarEvent> {
        return entities.map { entity ->
            entityToDomain(
                entity,
                attendeesMap[entity.id] ?: emptyList(),
                remindersMap[entity.id] ?: emptyList(),
                attachmentsMap[entity.id] ?: emptyList()
            )
        }
    }

    /**
     * Convert list of domain models to event entities
     */
    fun domainsToEntities(events: List<CalendarEvent>): List<EventEntity> {
        return events.map { domainToEntity(it) }
    }

    /**
     * Convert list of calendar entities to domain models
     */
    fun calendarEntitiesToDomain(entities: List<CalendarEntity>): List<Calendar> {
        return entities.map { calendarEntityToDomain(it) }
    }

    /**
     * Convert list of domain models to calendar entities
     */
    fun domainsToCalendarEntities(calendars: List<Calendar>): List<CalendarEntity> {
        return calendars.map { domainToCalendarEntity(it) }
    }
}