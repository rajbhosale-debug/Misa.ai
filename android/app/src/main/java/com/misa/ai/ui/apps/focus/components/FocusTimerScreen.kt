package com.misa.ai.ui.apps.focus.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misa.ai.domain.model.*
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * Focus Timer Screen Component
 * Provides comprehensive focus session timer with Pomodoro support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    session: FocusSession,
    remainingTime: Duration,
    isRunning: Boolean,
    currentCycle: Int,
    totalCycles: Int,
    sessionType: SessionType,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onSessionTypeChange: (SessionType) -> Unit,
    onSettingsClick: () -> Unit,
    onAddInterruption: (InterruptionType, String) -> Unit,
    onAddNote: (String) -> Unit,
    onTaskComplete: (String) -> Unit,
    onEnergyLevelChange: (EnergyLevel) -> Unit,
    onFocusLevelChange: (Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = calculateProgress(session, remainingTime),
        animationSpec = tween(1000, easing = LinearEasing),
        label = "progress"
    )

    val rotationAnimation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                getGradientForSessionType(sessionType)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section with session info
            SessionInfoSection(
                session = session,
                sessionType = sessionType,
                currentCycle = currentCycle,
                totalCycles = totalCycles,
                onSettingsClick = onSettingsClick,
                onSessionTypeChange = onSessionTypeChange
            )

            // Timer section
            TimerSection(
                session = session,
                remainingTime = remainingTime,
                animatedProgress = animatedProgress,
                rotationAnimation = rotationAnimation,
                isRunning = isRunning,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onSkip = onSkip
            )

            // Tasks section
            TasksSection(
                tasks = session.tasks,
                onTaskComplete = onTaskComplete
            )

            // Controls and metrics section
            ControlsSection(
                session = session,
                onAddInterruption = onAddInterruption,
                onAddNote = onAddNote,
                onEnergyLevelChange = onEnergyLevelChange,
                onFocusLevelChange = onFocusLevelChange
            )
        }
    }
}

/**
 * Session information section
 */
@Composable
private fun SessionInfoSection(
    session: FocusSession,
    sessionType: SessionType,
    currentCycle: Int,
    totalCycles: Int,
    onSettingsClick: () -> Unit,
    onSessionTypeChange: (SessionType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Session type selector
        SessionTypeSelector(
            currentType = sessionType,
            onTypeChange = onSessionTypeChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Session title and description
        session.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        session.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cycle indicator for Pomodoro
        if (sessionType == SessionType.POMODORO && totalCycles > 1) {
            CycleIndicator(
                currentCycle = currentCycle,
                totalCycles = totalCycles
            )
        }

        // Settings button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Session type selector
 */
@Composable
private fun SessionTypeSelector(
    currentType: SessionType,
    onTypeChange: (SessionType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = getSessionTypeIcon(currentType),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(getSessionTypeDisplayName(currentType))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        SessionType.values().forEach { type ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getSessionTypeIcon(type),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(getSessionTypeDisplayName(type))
                        if (type == currentType) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onClick = {
                    onTypeChange(type)
                    expanded = false
                }
            )
        }
    }
}

/**
 * Cycle indicator for Pomodoro sessions
 */
@Composable
private fun CycleIndicator(
    currentCycle: Int,
    totalCycles: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalCycles) { index ->
            val isCompleted = index < currentCycle - 1
            val isCurrent = index == currentCycle - 1

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                        },
                        shape = CircleShape
                    )
            )
        }

        Text(
            text = "$currentCycle/$totalCycles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
    }
}

/**
 * Main timer section with circular progress
 */
@Composable
private fun TimerSection(
    session: FocusSession,
    remainingTime: Duration,
    animatedProgress: Float,
    rotationAnimation: Float,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        )

        // Progress ring
        CircularProgress(
            progress = animatedProgress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            backgroundColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
            progressColor = MaterialTheme.colorScheme.primary
        )

        // Animated background elements
        if (isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .graphicsLayer {
                        rotationZ = rotationAnimation
                    }
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Timer display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Time display
            Text(
                text = formatDuration(remainingTime),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    !session.isActive() -> {
                        // Start button
                        FloatingActionButton(
                            onClick = onStart,
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    isRunning -> {
                        // Pause button
                        FloatingActionButton(
                            onClick = onPause,
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                    else -> {
                        // Resume button
                        FloatingActionButton(
                            onClick = onResume,
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Stop button
                FloatingActionButton(
                    onClick = onStop,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.onError
                    )
                }

                // Skip button
                FloatingActionButton(
                    onClick = onSkip,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Tasks section
 */
@Composable
private fun TasksSection(
    tasks: List<FocusTask>,
    onTaskComplete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Focus Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks added",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                tasks.forEach { task ->
                    TaskItem(
                        task = task,
                        onComplete = { onTaskComplete(task.id) }
                    )
                }
            }
        }
    }
}

/**
 * Individual task item
 */
@Composable
private fun TaskItem(
    task: FocusTask,
    onComplete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onComplete() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.completed,
            onCheckedChange = { onComplete() }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (task.completed) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )

            task.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Task duration indicator
        Text(
            text = formatDuration(task.estimatedDuration),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Controls and metrics section
 */
@Composable
private fun ControlsSection(
    session: FocusSession,
    onAddInterruption: (InterruptionType, String) -> Unit,
    onAddNote: (String) -> Unit,
    onEnergyLevelChange: (EnergyLevel) -> Unit,
    onFocusLevelChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Quick controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Interruption button
            OutlinedButton(
                onClick = { /* TODO: Show interruption dialog */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.NotificationsOff, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Interruption")
            }

            // Note button
            OutlinedButton(
                onClick = { /* TODO: Show note dialog */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Note")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Energy level selector
            MetricSelector(
                title = "Energy",
                value = session.metrics.energyLevel,
                options = EnergyLevel.values(),
                onValueChange = onEnergyLevelChange,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Focus level slider
            MetricSlider(
                title = "Focus Level",
                value = session.metrics.focusLevel,
                onValueChange = onFocusLevelChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Metric selector for energy level
 */
@Composable
private fun <T> MetricSelector(
    title: String,
    value: T,
    options: Array<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option == value
                    val color = when (option) {
                        is EnergyLevel -> getEnergyLevelColor(option)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = color.copy(alpha = if (isSelected) 1f else 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onValueChange(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getEnergyLevelSymbol(option as EnergyLevel),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Metric slider for focus level
 */
@Composable
private fun MetricSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                steps = 9
            )

            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Custom circular progress component
 */
@Composable
private fun CircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 8.dp,
    backgroundColor: Color = Color.Gray,
    progressColor: Color = Color.Blue
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // Progress arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                ),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                )
            )
        }
    }
}

// === Helper Functions ===

/**
 * Calculate progress for circular timer
 */
private fun calculateProgress(session: FocusSession, remainingTime: Duration): Float {
    val total = session.plannedDuration
    val elapsed = total.minus(remainingTime)
    return if (total.seconds > 0) {
        elapsed.seconds.toFloat() / total.seconds.toFloat()
    } else 0f
}

/**
 * Format duration for display
 */
private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()

    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Get gradient for session type
 */
private fun getGradientForSessionType(sessionType: SessionType): androidx.compose.ui.graphics.Brush {
    return when (sessionType) {
        SessionType.POMODORO -> androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
        )
        SessionType.DEEP_WORK -> androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color(0xFF2196F3), Color(0xFF3F51B5))
        )
        SessionType.ULTRA_DEEP_WORK -> androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color(0xFF673AB7), Color(0xFF3F51B5))
        )
        SessionType.BREAK -> androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
        )
        else -> androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color(0xFF607D8B), Color(0xFF455A64))
        )
    }
}

/**
 * Get session type icon
 */
private fun getSessionTypeIcon(sessionType: SessionType): ImageVector {
    return when (sessionType) {
        SessionType.POMODORO -> Icons.Default.Timer
        SessionType.DEEP_WORK -> Icons.Default.Psychology
        SessionType.ULTRA_DEEP_WORK -> Icons.Default.Bolt
        SessionType.BREAK -> Icons.Default.Coffee
        SessionType.MEETING -> Icons.Default.Groups
        SessionType.LEARNING -> Icons.Default.School
        SessionType.CREATIVE -> Icons.Default.Palette
        SessionType.PLANNING -> Icons.Default.EventNote
        SessionType.REVIEW -> Icons.Default.FindInPage
        SessionType.CUSTOM -> Icons.Default.Settings
    }
}

/**
 * Get session type display name
 */
private fun getSessionTypeDisplayName(sessionType: SessionType): String {
    return when (sessionType) {
        SessionType.POMODORO -> "Pomodoro"
        SessionType.DEEP_WORK -> "Deep Work"
        SessionType.ULTRA_DEEP_WORK -> "Ultra Deep Work"
        SessionType.BREAK -> "Break"
        SessionType.MEETING -> "Meeting"
        SessionType.LEARNING -> "Learning"
        SessionType.CREATIVE -> "Creative"
        SessionType.PLANNING -> "Planning"
        SessionType.REVIEW -> "Review"
        SessionType.CUSTOM -> "Custom"
    }
}

/**
 * Get energy level color
 */
private fun getEnergyLevelColor(energyLevel: EnergyLevel): Color {
    return when (energyLevel) {
        EnergyLevel.VERY_LOW -> Color(0xFFD32F2F)
        EnergyLevel.LOW -> Color(0xFFFF9800)
        EnergyLevel.MEDIUM -> Color(0xFFFFC107)
        EnergyLevel.HIGH -> Color(0xFF4CAF50)
        EnergyLevel.VERY_HIGH -> Color(0xFF2196F3)
    }
}

/**
 * Get energy level symbol
 */
private fun getEnergyLevelSymbol(energyLevel: EnergyLevel): String {
    return when (energyLevel) {
        EnergyLevel.VERY_LOW -> "😴"
        EnergyLevel.LOW -> "😔"
        EnergyLevel.MEDIUM -> "😐"
        EnergyLevel.HIGH -> "😊"
        EnergyLevel.VERY_HIGH -> "🤩"
    }
}