package com.misa.ai.data.local.database.converter

import androidx.room.TypeConverter
import com.misa.ai.domain.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room type converters for complex types
 * Handles serialization of domain model types for database storage
 */
class Converters {

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String {
        return value.format(DATE_TIME_FORMATTER)
    }

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER)
    }

    @TypeConverter
    fun fromEventStatus(status: EventStatus): String {
        return status.name
    }

    @TypeConverter
    fun toEventStatus(status: String): EventStatus {
        return EventStatus.valueOf(status)
    }

    @TypeConverter
    fun fromEventSource(source: EventSource): String {
        return source.name
    }

    @TypeConverter
    fun toEventSource(source: String): EventSource {
        return EventSource.valueOf(source)
    }

    @TypeConverter
    fun fromEventVisibility(visibility: EventVisibility): String {
        return visibility.name
    }

    @TypeConverter
    fun toEventVisibility(visibility: String): EventVisibility {
        return EventVisibility.valueOf(visibility)
    }

    @TypeConverter
    fun fromReminderType(type: ReminderType): String {
        return type.name
    }

    @TypeConverter
    fun toReminderType(type: String): ReminderType {
        return ReminderType.valueOf(type)
    }

    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus): String {
        return status.name
    }

    @TypeConverter
    fun toAttendanceStatus(status: String): AttendanceStatus {
        return AttendanceStatus.valueOf(status)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toStringList(data: String): List<String> {
        return if (data.isEmpty()) emptyList() else data.split(",")
    }

    @TypeConverter
    fun fromMap(map: Map<String, Any>): String {
        return map.entries.joinToString(";") { "${it.key}=${it.value}" }
    }

    @TypeConverter
    fun toMap(data: String): Map<String, Any> {
        return if (data.isEmpty()) {
            emptyMap()
        } else {
            data.split(";").associate { entry ->
                val parts = entry.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }
        }
    }
}