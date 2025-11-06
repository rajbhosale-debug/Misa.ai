package com.misa.ai.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.misa.ai.domain.model.*

/**
 * Event entity for Room database
 * Represents a calendar event in local storage
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = CalendarEntity::class,
            parentColumns = ["id"],
            childColumns = ["calendarId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["calendarId"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["status"])
    ]
)
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val startTime: java.time.LocalDateTime,
    val endTime: java.time.LocalDateTime,
    val location: String?,
    val isAllDay: Boolean,
    val attendeesJson: String, // JSON array of Attendee objects
    val recurrenceJson: String?, // JSON of RecurrenceRule
    val remindersJson: String, // JSON array of Reminder objects
    val attachmentsJson: String, // JSON array of Attachment objects
    val color: String?,
    val visibility: EventVisibility,
    val status: EventStatus,
    val organizer: String?,
    val calendarId: String,
    val source: EventSource,
    val lastModified: java.time.LocalDateTime,
    val metadataJson: String // JSON of metadata map
)

/**
 * Calendar entity for Room database
 * Represents a calendar in local storage
 */
@Entity(
    tableName = "calendars",
    indices = [
        Index(value = ["source"]),
        Index(value = ["isVisible"])
    ]
)
data class CalendarEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val color: String,
    val isVisible: Boolean,
    val isSyncEnabled: Boolean,
    val source: EventSource,
    val accountName: String?,
    val timezone: String,
    val canWrite: Boolean,
    val metadataJson: String
)

/**
 * Attendee entity for Room database
 * Represents event attendees
 */
@Entity(
    tableName = "attendees",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["email"])
    ]
)
data class AttendeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val email: String,
    val name: String?,
    val status: AttendanceStatus,
    val isOrganizer: Boolean,
    val isOptional: Boolean,
    val comment: String?
)

/**
 * Reminder entity for Room database
 * Represents event reminders
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["eventId"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val type: ReminderType,
    val minutesBefore: Int,
    val enabled: Boolean
)

/**
 * Attachment entity for Room database
 * Represents event attachments
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["eventId"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val url: String?,
    val localPath: String?
)