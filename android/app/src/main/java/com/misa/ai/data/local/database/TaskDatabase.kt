package com.misa.ai.data.local.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.misa.ai.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Room database for task management
 * Provides comprehensive task data storage with relationships and indexing
 */
@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        TaskAttachmentEntity::class,
        TaskCommentEntity::class,
        TimeEntryEntity::class,
        TaskLabelEntity::class,
        TaskReminderEntity::class,
        TaskTemplateEntity::class,
        WorkflowTemplateEntity::class,
        TaskAutomationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun taskAttachmentDao(): TaskAttachmentDao
    abstract fun taskCommentDao(): TaskCommentDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun taskLabelDao(): TaskLabelDao
    abstract fun taskReminderDao(): TaskReminderDao
    abstract fun taskTemplateDao(): TaskTemplateDao
    abstract fun workflowTemplateDao(): WorkflowTemplateDao
    abstract fun taskAutomationDao(): TaskAutomationDao

    companion object {
        const val DATABASE_NAME = "task_database"
    }
}

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTaskStatus(status: String): TaskStatus {
        return TaskStatus.valueOf(status)
    }

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String {
        return priority.name
    }

    @TypeConverter
    fun toTaskPriority(priority: String): TaskPriority {
        return TaskPriority.valueOf(priority)
    }

    @TypeConverter
    fun fromAttachmentType(type: AttachmentType): String {
        return type.name
    }

    @TypeConverter
    fun toAttachmentType(type: String): AttachmentType {
        return AttachmentType.valueOf(type)
    }

    @TypeConverter
    fun fromRecurrenceType(type: RecurrenceType): String {
        return type.name
    }

    @TypeConverter
    fun toRecurrenceType(type: String): RecurrenceType {
        return RecurrenceType.valueOf(type)
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
    fun fromEstimationMethod(method: EstimationMethod): String {
        return method.name
    }

    @TypeConverter
    fun toEstimationMethod(method: String): EstimationMethod {
        return EstimationMethod.valueOf(method)
    }

    @TypeConverter
    fun fromDuration(duration: kotlin.time.Duration): Long {
        return duration.inWholeSeconds
    }

    @TypeConverter
    fun toDuration(seconds: Long): kotlin.time.Duration {
        return kotlin.time.Duration.parse("PT${seconds}S")
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toStringList(data: String): List<String> {
        return if (data.isBlank()) emptyList() else data.split(",")
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, Any>): String {
        return map.entries.joinToString(";") { "${it.key}=${it.value}" }
    }

    @TypeConverter
    fun toStringMap(data: String): Map<String, Any> {
        return if (data.isBlank()) {
            emptyMap()
        } else {
            data.split(";").associate { entry ->
                val parts = entry.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }
        }
    }
}

/**
 * DAO for task operations
 */
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY createdAt DESC")
    fun getTasksByPriority(priority: TaskPriority): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE assigneeId = :assigneeId ORDER BY createdAt DESC")
    fun getTasksByAssignee(assigneeId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun getTasksByTag(tag: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE dueDate >= :startDate AND dueDate <= :endDate
        ORDER BY dueDate ASC
    """)
    fun getTasksByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE date(dueDate) = date('now')
        ORDER BY priority DESC, dueDate ASC
    """)
    fun getTasksDueToday(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE dueDate < datetime('now') AND status != 'COMPLETED'
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE dueDate BETWEEN datetime('now') AND datetime('now', '+7 days')
        ORDER BY dueDate ASC
    """)
    fun getUpcomingTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE status = 'COMPLETED'
        ORDER BY completedAt DESC
        LIMIT :limit
    """)
    fun getCompletedTasks(limit: Int = 50): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    suspend fun searchTasks(query: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("UPDATE tasks SET tags = :tags WHERE id = :taskId")
    suspend fun updateTaskTags(taskId: String, tags: List<String>)

    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus)

    @Query("UPDATE tasks SET completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskCompletedAt(taskId: String, completedAt: LocalDateTime?)
}

/**
 * DAO for subtask operations
 */
@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE parentTaskId = :parentTaskId ORDER BY position ASC")
    fun getSubtasksByParent(parentTaskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE id = :subtaskId")
    suspend fun getSubtaskById(subtaskId: String): SubtaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtaskById(subtaskId: String)

    @Query("DELETE FROM subtasks WHERE parentTaskId = :parentTaskId")
    suspend fun deleteSubtasksByParent(parentTaskId: String)
}

/**
 * DAO for task attachment operations
 */
@Dao
interface TaskAttachmentDao {
    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun getAttachmentsByTask(taskId: String): Flow<List<TaskAttachmentEntity>>

    @Query("SELECT * FROM task_attachments WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): TaskAttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: TaskAttachmentEntity): Long

    @Update
    suspend fun updateAttachment(attachment: TaskAttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: TaskAttachmentEntity)

    @Query("DELETE FROM task_attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: String)
}

/**
 * DAO for task comment operations
 */
@Dao
interface TaskCommentDao {
    @Query("SELECT * FROM task_comments WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun getCommentsByTask(taskId: String): Flow<List<TaskCommentEntity>>

    @Query("SELECT * FROM task_comments WHERE id = :commentId")
    suspend fun getCommentById(commentId: String): TaskCommentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: TaskCommentEntity): Long

    @Update
    suspend fun updateComment(comment: TaskCommentEntity)

    @Delete
    suspend fun deleteComment(comment: TaskCommentEntity)

    @Query("DELETE FROM task_comments WHERE id = :commentId")
    suspend fun deleteCommentById(commentId: String)
}

/**
 * DAO for time entry operations
 */
@Dao
interface TimeEntryDao {
    @Query("SELECT * FROM time_entries WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getTimeEntriesByTask(taskId: String): Flow<List<TimeEntryEntity>>

    @Query("SELECT * FROM time_entries WHERE id = :timeEntryId")
    suspend fun getTimeEntryById(timeEntryId: String): TimeEntryEntity?

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND endTime IS NULL")
    suspend fun getActiveTimeEntry(userId: String): TimeEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeEntry(timeEntry: TimeEntryEntity): Long

    @Update
    suspend fun updateTimeEntry(timeEntry: TimeEntryEntity)

    @Delete
    suspend fun deleteTimeEntry(timeEntry: TimeEntryEntity)

    @Query("DELETE FROM time_entries WHERE id = :timeEntryId")
    suspend fun deleteTimeEntryById(timeEntryId: String)
}

/**
 * DAO for task label operations
 */
@Dao
interface TaskLabelDao {
    @Query("SELECT * FROM task_labels ORDER BY name ASC")
    fun getAllLabels(): Flow<List<TaskLabelEntity>>

    @Query("SELECT * FROM task_labels WHERE id = :labelId")
    suspend fun getLabelById(labelId: String): TaskLabelEntity?

    @Query("SELECT * FROM task_labels WHERE projectId = :projectId ORDER BY name ASC")
    fun getLabelsByProject(projectId: String): Flow<List<TaskLabelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: TaskLabelEntity): Long

    @Update
    suspend fun updateLabel(label: TaskLabelEntity)

    @Delete
    suspend fun deleteLabel(label: TaskLabelEntity)

    @Query("DELETE FROM task_labels WHERE id = :labelId")
    suspend fun deleteLabelById(labelId: String)
}

/**
 * DAO for task reminder operations
 */
@Dao
interface TaskReminderDao {
    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY triggerTime ASC")
    fun getRemindersByTask(taskId: String): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: String): TaskReminderEntity?

    @Query("SELECT * FROM task_reminders WHERE triggerTime <= :currentTime AND isEnabled = 1")
    suspend fun getPendingReminders(currentTime: LocalDateTime): List<TaskReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: TaskReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: TaskReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: TaskReminderEntity)

    @Query("DELETE FROM task_reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: String)
}

/**
 * DAO for task template operations
 */
@Dao
interface TaskTemplateDao {
    @Query("SELECT * FROM task_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: String): TaskTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TaskTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TaskTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: TaskTemplateEntity)

    @Query("DELETE FROM task_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: String)
}

/**
 * DAO for workflow template operations
 */
@Dao
interface WorkflowTemplateDao {
    @Query("SELECT * FROM workflow_templates ORDER BY name ASC")
    fun getAllWorkflowTemplates(): Flow<List<WorkflowTemplateEntity>>

    @Query("SELECT * FROM workflow_templates WHERE id = :templateId")
    suspend fun getWorkflowTemplateById(templateId: String): WorkflowTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflowTemplate(template: WorkflowTemplateEntity): Long

    @Update
    suspend fun updateWorkflowTemplate(template: WorkflowTemplateEntity)

    @Delete
    suspend fun deleteWorkflowTemplate(template: WorkflowTemplateEntity)

    @Query("DELETE FROM workflow_templates WHERE id = :templateId")
    suspend fun deleteWorkflowTemplateById(templateId: String)
}

/**
 * DAO for task automation operations
 */
@Dao
interface TaskAutomationDao {
    @Query("SELECT * FROM task_automations WHERE isEnabled = 1 ORDER BY name ASC")
    fun getEnabledAutomations(): Flow<List<TaskAutomationEntity>>

    @Query("SELECT * FROM task_automations ORDER BY name ASC")
    fun getAllAutomations(): Flow<List<TaskAutomationEntity>>

    @Query("SELECT * FROM task_automations WHERE id = :automationId")
    suspend fun getAutomationById(automationId: String): TaskAutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: TaskAutomationEntity): Long

    @Update
    suspend fun updateAutomation(automation: TaskAutomationEntity)

    @Delete
    suspend fun deleteAutomation(automation: TaskAutomationEntity)

    @Query("DELETE FROM task_automations WHERE id = :automationId")
    suspend fun deleteAutomationById(automationId: String)

    @Query("UPDATE task_automations SET executionCount = executionCount + 1, lastExecuted = :timestamp WHERE id = :automationId")
    suspend fun incrementExecutionCount(automationId: String, timestamp: LocalDateTime)
}

/**
 * Database migrations
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns or tables as needed
        // Example: database.execSQL("ALTER TABLE tasks ADD COLUMN newColumn TEXT")
    }
}