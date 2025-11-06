package com.misa.ai.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.misa.ai.data.local.database.converter.Converters
import com.misa.ai.data.local.database.dao.CalendarDao
import com.misa.ai.data.local.database.dao.EventDao
import com.misa.ai.data.local.database.entity.CalendarEntity
import com.misa.ai.data.local.database.entity.EventEntity
import com.misa.ai.data.local.database.entity.AttendeeEntity
import com.misa.ai.data.local.database.entity.ReminderEntity
import com.misa.ai.data.local.database.entity.AttachmentEntity

/**
 * Room database for local calendar storage
 * Provides offline-first storage with sync capabilities
 */
@Database(
    entities = [
        CalendarEntity::class,
        EventEntity::class,
        AttendeeEntity::class,
        ReminderEntity::class,
        AttachmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CalendarDatabase : RoomDatabase() {

    abstract fun calendarDao(): CalendarDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: CalendarDatabase? = null

        fun getDatabase(context: Context): CalendarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalendarDatabase::class.java,
                    "misa_calendar_database"
                )
                .addCallback(CalendarDatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class CalendarDatabaseCallback : RoomDatabase.Callback() {
            // Handle database creation and migration
        }
    }
}