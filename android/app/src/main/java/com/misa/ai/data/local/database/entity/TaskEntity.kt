package com.misa.ai.data.local.database.entity

import androidx.room.*
import com.misa.ai.domain.model.*
import java.time.LocalDateTime

/**
 * Room entity for Task model
 * Represents a task in the local database
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["priority"]),
        Index(value = ["assigneeId"]),
        Index(value = ["projectId"]),
        Index(value = ["dueDate"]),
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "status")
    val status: TaskStatus,

    @ColumnInfo(name = "priority")
    val priority: TaskPriority,

    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: LocalDateTime,

    @ColumnInfo(name = "dueDate")
    val dueDate: LocalDateTime?,

    @ColumnInfo(name = "completedAt")
    val completedAt: LocalDateTime?,

    @ColumnInfo(name = "estimatedDuration")
    val estimatedDuration: kotlin.time.Duration?,

    @ColumnInfo(name = "actualDuration")
    val actualDuration: kotlin.time.Duration?,

    @ColumnInfo(name = "assigneeId")
    val assigneeId: String?,

    @ColumnInfo(name = "projectId")
    val projectId: String?,

    @ColumnInfo(name = "tags")
    val tags: List<String>,

    @ColumnInfo(name = "dependencies")
    val dependencies: List<String>, // Task IDs this task depends on

    @ColumnInfo(name = "blockers")
    val blockers: List<String>,   // Task IDs blocking this task

    @ColumnInfo(name = "checklist")
    val checklist: List<String>, // Serialized checklist items

    @ColumnInfo(name = "labels")
    val labels: List<String>, // Label IDs

    @ColumnInfo(name = "recurrence")
    val recurrence: String?, // Serialized recurrence data

    @ColumnInfo(name = "estimatedEffort")
    val estimatedEffort: String?, // Serialized effort estimate

    @ColumnInfo(name = "actualEffort")
    val actualEffort: String?, // Serialized effort estimate

    @ColumnInfo(name = "metadata")
    val metadata: Map<String, Any>
)

/**
 * Room entity for Subtask model
 */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentTaskId"]),
        Index(value = ["status"])
    ]
)
data class SubtaskEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "parentTaskId")
    val parentTaskId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "status")
    val status: TaskStatus,

    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime,

    @ColumnInfo(name = "completedAt")
    val completedAt: LocalDateTime?,

    @ColumnInfo(name = "assigneeId")
    val assigneeId: String?,

    @ColumnInfo(name = "estimatedDuration")
    val estimatedDuration: kotlin.time.Duration?,

    @ColumnInfo(name = "actualDuration")
    val actualDuration: kotlin.time.Duration?,

    @ColumnInfo(name = "position")
    val position: Int
)

/**
 * Room entity for TaskAttachment model
 */
@Entity(
    tableName = "task_attachments",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["type"]),
        Index(value = ["createdAt"])
    ]
)
data class TaskAttachmentEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "taskId")
    val taskId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: AttachmentType,

    @ColumnInfo(name = "size")
    val size: Long,

    @ColumnInfo(name = "localPath")
    val localPath: String?,

    @ColumnInfo(name = "cloudUrl")
    val cloudUrl: String?,

    @ColumnInfo(name = "mimeType")
    val mimeType: String,

    @ColumnInfo(name = "metadata")
    val metadata: Map<String, Any>,

    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime
)

/**
 * Room entity for TaskComment model
 */
@Entity(
    tableName = "task_comments",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["authorId"]),
        Index(value = ["createdAt"])
    ]
)
data class TaskCommentEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "taskId")
    val taskId: String,

    @ColumnInfo(name = "authorId")
    val authorId: String,

    @ColumnInfo(name = "authorName")
    val authorName: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: LocalDateTime?,

    @ColumnInfo(name = "mentions")
    val mentions: List<String>,

    @ColumnInfo(name = "attachments")
    val attachments: List<String> // Attachment IDs
)

/**
 * Room entity for TimeEntry model
 */
@Entity(
    tableName = "time_entries",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["userId"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"])
    ]
)
data class TimeEntryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "taskId")
    val taskId: String,

    @ColumnInfo(name = "userId")
    val userId: String,

    @ColumnInfo(name = "startTime")
    val startTime: LocalDateTime,

    @ColumnInfo(name = "endTime")
    val endTime: LocalDateTime?,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "tags")
    val tags: List<String>,

    @ColumnInfo(name = "isBillable")
    val isBillable: Boolean
)

/**
 * Room entity for TaskLabel model
 */
@Entity(
    tableName = "task_labels",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["name"])
    ]
)
data class TaskLabelEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: String,

    @ColumnInfo(name = "projectId")
    val projectId: String?,

    @ColumnInfo(name = "description")
    val description: String?
)

/**
 * Room entity for TaskReminder model
 */
@Entity(
    tableName = "task_reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["triggerTime"]),
        Index(value = ["isEnabled"])
    ]
)
data class TaskReminderEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "taskId")
    val taskId: String,

    @ColumnInfo(name = "type")
    val type: ReminderType,

    @ColumnInfo(name = "triggerTime")
    val triggerTime: LocalDateTime,

    @ColumnInfo(name = "isEnabled")
    val isEnabled: Boolean,

    @ColumnInfo(name = "message")
    val message: String?
)

/**
 * Room entity for TaskTemplate model
 */
@Entity(
    tableName = "task_templates",
    indices = [
        Index(value = ["name"]),
        Index(value = ["defaultPriority"])
    ]
)
data class TaskTemplateEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "defaultPriority")
    val defaultPriority: TaskPriority,

    @ColumnInfo(name = "defaultEstimatedDuration")
    val defaultEstimatedDuration: kotlin.time.Duration?,

    @ColumnInfo(name = "defaultTags")
    val defaultTags: List<String>,

    @ColumnInfo(name = "defaultChecklist")
    val defaultChecklist: List<String>, // Serialized checklist items

    @ColumnInfo(name = "metadata")
    val metadata: Map<String, Any>,

    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: LocalDateTime
)

/**
 * Room entity for WorkflowTemplate model
 */
@Entity(
    tableName = "workflow_templates",
    indices = [
        Index(value = ["name"]),
        Index(value = ["createdAt"])
    ]
)
data class WorkflowTemplateEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "steps")
    val steps: List<String>, // Serialized workflow steps

    @ColumnInfo(name = "triggers")
    val triggers: List<String>, // Serialized workflow triggers

    @ColumnInfo(name = "conditions")
    val conditions: List<String>, // Serialized workflow conditions

    @ColumnInfo(name = "metadata")
    val metadata: Map<String, Any>,

    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: LocalDateTime
)

/**
 * Room entity for TaskAutomation model
 */
@Entity(
    tableName = "task_automations",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isEnabled"]),
        Index(value = ["lastExecuted"])
    ]
)
data class TaskAutomationEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "trigger")
    val trigger: String, // Serialized trigger data

    @ColumnInfo(name = "conditions")
    val conditions: List<String>, // Serialized conditions

    @ColumnInfo(name = "actions")
    val actions: List<String>, // Serialized actions

    @ColumnInfo(name = "isEnabled")
    val isEnabled: Boolean,

    @ColumnInfo(name = "executionCount")
    val executionCount: Int,

    @ColumnInfo(name = "lastExecuted")
    val lastExecuted: LocalDateTime?,

    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: LocalDateTime
)