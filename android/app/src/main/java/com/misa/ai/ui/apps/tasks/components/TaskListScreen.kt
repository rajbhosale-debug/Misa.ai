package com.misa.ai.ui.apps.tasks.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misa.ai.domain.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Task List Screen Component
 * Displays a comprehensive list of tasks with filtering, sorting, and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasks: List<Task>,
    selectedTask: Task?,
    onTaskSelected: (Task) -> Unit,
    onTaskStatusChanged: (String, TaskStatus) -> Unit,
    onTaskStarToggled: (String) -> Unit,
    onTaskDeleted: (String) -> Unit,
    onAddTask: () -> Unit,
    onFilterChanged: (TaskFilter) -> Unit,
    onSortChanged: (TaskSortField, TaskSortOrder) -> Unit,
    currentFilter: TaskFilter,
    currentSortField: TaskSortField,
    currentSortOrder: TaskSortOrder,
    isLoading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with search and actions
        TaskListTopBar(
            onAddTask = onAddTask,
            onFilterChanged = onFilterChanged,
            onSortChanged = onSortChanged,
            currentFilter = currentFilter,
            currentSortField = currentSortField,
            currentSortOrder = currentSortOrder
        )

        // Filter chips
        FilterChipsRow(
            currentFilter = currentFilter,
            onFilterChanged = onFilterChanged
        )

        // Task list
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (tasks.isEmpty()) {
                EmptyTaskListState(onAddTask = onAddTask)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = tasks,
                        key = { it.id }
                    ) { task ->
                        TaskListItem(
                            task = task,
                            isSelected = selectedTask?.id == task.id,
                            onTaskSelected = onTaskSelected,
                            onTaskStatusChanged = onTaskStatusChanged,
                            onTaskStarToggled = onTaskStarToggled,
                            onTaskDeleted = onTaskDeleted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top bar with search, filter, and sort options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListTopBar(
    onAddTask: () -> Unit,
    onFilterChanged: (TaskFilter) -> Unit,
    onSortChanged: (TaskSortField, TaskSortOrder) -> Unit,
    currentFilter: TaskFilter,
    currentSortField: TaskSortField,
    currentSortOrder: TaskSortOrder
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = "TaskFlow",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter tasks"
                )
            }

            IconButton(onClick = { showSortDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort tasks"
                )
            }

            IconButton(onClick = onAddTask) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task"
                )
            }
        }
    )

    // Filter dialog
    if (showFilterDialog) {
        TaskFilterDialog(
            currentFilter = currentFilter,
            onFilterChanged = onFilterChanged,
            onDismiss = { showFilterDialog = false }
        )
    }

    // Sort dialog
    if (showSortDialog) {
        TaskSortDialog(
            currentSortField = currentSortField,
            currentSortOrder = currentSortOrder,
            onSortChanged = onSortChanged,
            onDismiss = { showSortDialog = false }
        )
    }
}

/**
 * Filter chips row
 */
@Composable
private fun FilterChipsRow(
    currentFilter: TaskFilter,
    onFilterChanged: (TaskFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status filters
        TaskStatus.values().forEach { status ->
            FilterChip(
                selected = status in currentFilter.status,
                onClick = {
                    val newStatusList = if (status in currentFilter.status) {
                        currentFilter.status - status
                    } else {
                        currentFilter.status + status
                    }
                    onFilterChanged(currentFilter.copy(status = newStatusList))
                },
                label = { Text(status.name.replace("_", " ")) }
            )
        }
    }
}

/**
 * Individual task list item
 */
@Composable
private fun TaskListItem(
    task: Task,
    isSelected: Boolean,
    onTaskSelected: (Task) -> Unit,
    onTaskStatusChanged: (String, TaskStatus) -> Unit,
    onTaskStarToggled: (String) -> Unit,
    onTaskDeleted: (String) -> Unit
) {
    var showActionMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable { onTaskSelected(task) }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Title and status
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status checkbox
                    TaskStatusCheckbox(
                        status = task.status,
                        onStatusChanged = { newStatus ->
                            onTaskStatusChanged(task.id, newStatus)
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (task.isCompleted()) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            }
                        )

                        // Description preview
                        task.description?.let { description ->
                            if (description.isNotBlank()) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Action menu
                Box {
                    IconButton(onClick = { showActionMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Task actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            },
                            onClick = {
                                showActionMenu = false
                                onTaskSelected(task)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            },
                            onClick = {
                                showActionMenu = false
                                onTaskDeleted(task.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task metadata row
            TaskMetadataRow(task = task)

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row with tags, subtasks, and attachments
            TaskBottomRow(task = task)
        }
    }
}

/**
 * Task status checkbox
 */
@Composable
private fun TaskStatusCheckbox(
    status: TaskStatus,
    onStatusChanged: (TaskStatus) -> Unit
) {
    when (status) {
        TaskStatus.COMPLETED -> {
            IconButton(onClick = { onStatusChanged(TaskStatus.TODO) }) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Mark incomplete",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        TaskStatus.TODO -> {
            IconButton(onClick = { onStatusChanged(TaskStatus.IN_PROGRESS) }) {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Start task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TaskStatus.IN_PROGRESS -> {
            IconButton(onClick = { onStatusChanged(TaskStatus.COMPLETED) }) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = "Complete task",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        else -> {
            IconButton(onClick = { onStatusChanged(TaskStatus.TODO) }) {
                Icon(
                    imageVector = getStatusIcon(status),
                    contentDescription = "Change status",
                    tint = getStatusColor(status)
                )
            }
        }
    }
}

/**
 * Task metadata row with priority, due date, and assignee
 */
@Composable
private fun TaskMetadataRow(task: Task) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority indicator
        TaskPriorityIndicator(priority = task.priority)

        Spacer(modifier = Modifier.width(8.dp))

        // Due date
        task.dueDate?.let { dueDate ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Due date",
                    modifier = Modifier.size(16.dp),
                    tint = if (task.isOverdue()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDueDate(dueDate, task.isOverdue()),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.isOverdue()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Progress indicator for subtasks
        if (task.hasSubtasks()) {
            SubtaskProgressIndicator(
                completed = task.subtasks.count { it.isCompleted() },
                total = task.subtasks.size
            )
        }
    }
}

/**
 * Bottom row with tags, subtasks, and attachments indicators
 */
@Composable
private fun TaskBottomRow(task: Task) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tags
        if (task.tags.isNotEmpty()) {
            TaskTagsChips(tags = task.tags.take(3))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Subtasks indicator
        if (task.hasSubtasks()) {
            TaskIndicator(
                icon = Icons.Default.SubdirectoryArrowRight,
                count = task.subtasks.size,
                contentDescription = "Subtasks"
            )
        }

        // Attachments indicator
        if (task.hasAttachments()) {
            TaskIndicator(
                icon = Icons.Default.AttachFile,
                count = task.attachments.size,
                contentDescription = "Attachments"
            )
        }

        // Comments indicator
        if (task.comments.isNotEmpty()) {
            TaskIndicator(
                icon = Icons.Default.Comment,
                count = task.comments.size,
                contentDescription = "Comments"
            )
        }
    }
}

/**
 * Priority indicator with color coding
 */
@Composable
private fun TaskPriorityIndicator(priority: TaskPriority) {
    val (color, label) = when (priority) {
        TaskPriority.URGENT -> Pair(MaterialTheme.colorScheme.error, "Urgent")
        TaskPriority.HIGH -> Pair(MaterialTheme.colorScheme.error.copy(alpha = 0.7f), "High")
        TaskPriority.NORMAL -> Pair(MaterialTheme.colorScheme.primary, "Normal")
        TaskPriority.LOW -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "Low")
    }

    Surface(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}

/**
 * Subtask progress indicator
 */
@Composable
private fun SubtaskProgressIndicator(completed: Int, total: Int) {
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.width(40.dp).height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$completed/$total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Tags chips
 */
@Composable
private fun TaskTagsChips(tags: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.take(3).forEach { tag ->
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 9.sp
                )
            }
        }
        if (tags.size > 3) {
            Text(
                text = "+${tags.size - 3}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generic task indicator with icon and count
 */
@Composable
private fun TaskIndicator(
    icon: ImageVector,
    count: Int,
    contentDescription: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (count > 1) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty task list state
 */
@Composable
private fun EmptyTaskListState(
    onAddTask: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tasks yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first task to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddTask) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Task")
        }
    }
}

/**
 * Helper functions
 */
private fun getStatusIcon(status: TaskStatus): ImageVector {
    return when (status) {
        TaskStatus.TODO -> Icons.Default.RadioButtonUnchecked
        TaskStatus.IN_PROGRESS -> Icons.Default.PlayArrow
        TaskStatus.BLOCKED -> Icons.Default.Block
        TaskStatus.IN_REVIEW -> Icons.Default.Visibility
        TaskStatus.TESTING -> Icons.Default.BugReport
        TaskStatus.COMPLETED -> Icons.Default.CheckCircle
        TaskStatus.CANCELLED -> Icons.Default.Cancel
        TaskStatus.ON_HOLD -> Icons.Default.Pause
        TaskStatus.ARCHIVED -> Icons.Default.Archive
    }
}

private fun getStatusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.TODO -> Color.Gray
        TaskStatus.IN_PROGRESS -> Color.Blue
        TaskStatus.BLOCKED -> Color.Red
        TaskStatus.IN_REVIEW -> Color(0xFF9C27B0)
        TaskStatus.TESTING -> Color(0xFFFF9800)
        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.CANCELLED -> Color.Gray
        TaskStatus.ON_HOLD -> Color(0xFFFF5722)
        TaskStatus.ARCHIVED -> Color.Gray
    }
}

private fun formatDueDate(dueDate: LocalDateTime, isOverdue: Boolean): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

    return when {
        isOverdue -> "Overdue"
        dueDate.toLocalDate() == now.toLocalDate() -> "Today ${dueDate.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        dueDate.toLocalDate() == now.plusDays(1).toLocalDate() -> "Tomorrow"
        dueDate.isBefore(now.plusDays(7)) -> dueDate.format(DateTimeFormatter.ofPattern("EEE"))
        else -> dueDate.format(formatter)
    }
}