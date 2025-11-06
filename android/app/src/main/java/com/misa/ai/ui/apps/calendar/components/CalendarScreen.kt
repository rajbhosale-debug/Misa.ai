package com.misa.ai.ui.apps.calendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main Calendar Screen
 * Displays calendar with month view, event list, and navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    IconButton(onClick = { viewModel.syncEvents() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                    }
                    IconButton(onClick = { viewModel.showImportDialog() }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Import")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateEventDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Date selector
            DateSelector(
                selectedDate = selectedDate,
                onDateSelected = viewModel::selectDate,
                modifier = Modifier.padding(16.dp)
            )

            // Month view
            MonthView(
                selectedDate = selectedDate,
                eventsByDate = uiState.eventsByDate,
                onDateSelected = viewModel::selectDate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Events list
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    ErrorMessage(
                        error = uiState.error,
                        onRetry = { viewModel.loadEvents() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                else -> {
                    EventsList(
                        events = uiState.eventsForSelectedDate,
                        onEventClick = viewModel::selectEvent,
                        onEventDelete = viewModel::deleteEvent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // Dialogs
    when (uiState.dialogState) {
        is CalendarDialogState.CreateEvent -> {
            CreateEventDialog(
                onDismiss = viewModel::dismissDialog,
                onSave = { event -> viewModel.createEvent(event) },
                selectedDate = selectedDate
            )
        }
        is CalendarDialogState.EditEvent -> {
            EditEventDialog(
                event = uiState.dialogState.event,
                onDismiss = viewModel::dismissDialog,
                onSave = { event -> viewModel.updateEvent(event) }
            )
        }
        is CalendarDialogState.Import -> {
            ImportEventsDialog(
                onDismiss = viewModel::dismissDialog,
                onImport = { format, filePath -> viewModel.importEvents(format, filePath) }
            )
        }
        else -> { /* No dialog */ }
    }
}

/**
 * Date selector component
 */
@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateSelected(selectedDate.minusMonths(1)) }) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = selectedDate.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Selected: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(onClick = { onDateSelected(selectedDate.plusMonths(1)) }) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

/**
 * Month calendar view
 */
@Composable
fun MonthView(
    selectedDate: LocalDate,
    eventsByDate: Map<LocalDate, List<com.misa.ai.domain.model.CalendarEvent>>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysInMonth = generateCalendarDays(selectedDate)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Week header
        item {
            WeekHeader()
        }

        // Calendar weeks
        items(daysInMonth.chunked(7)) { week ->
            WeekRow(
                days = week,
                selectedDate = selectedDate,
                eventsByDate = eventsByDate,
                onDateSelected = onDateSelected
            )
        }
    }
}

/**
 * Week header showing day names
 */
@Composable
fun WeekHeader() {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Row representing a week in the calendar
 */
@Composable
fun WeekRow(
    days: List<CalendarDay>,
    selectedDate: LocalDate,
    eventsByDate: Map<LocalDate, List<com.misa.ai.domain.model.CalendarEvent>>,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        days.forEach { day ->
            DayCell(
                day = day,
                isSelected = day.date == selectedDate,
                hasEvents = eventsByDate[day.date]?.isNotEmpty() == true,
                eventCount = eventsByDate[day.date]?.size ?: 0,
                onDateSelected = onDateSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual day cell
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    hasEvents: Boolean,
    eventCount: Int,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = day.date == LocalDate.now()

    Card(
        onClick = { onDateSelected(day.date) },
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isToday -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = if (isSelected) CardDefaults.cardElevation(4.dp) else CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else if (day.isCurrentMonth) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (hasEvents) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Events list for selected date
 */
@Composable
fun EventsList(
    events: List<com.misa.ai.domain.model.CalendarEvent>,
    onEventClick: (com.misa.ai.domain.model.CalendarEvent) -> Unit,
    onEventDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = if (events.isEmpty()) "No events" else "${events.size} events",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (events.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                        onDelete = { onEventDelete(event.id) }
                    )
                }
            }
        }
    }
}

/**
 * Individual event card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: com.misa.ai.domain.model.CalendarEvent,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    event.location?.let { location ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete event",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            event.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Error message display
 */
@Composable
fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Error: $error",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Data class for calendar day
 */
data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)

/**
 * Generate calendar days for a month
 */
private fun generateCalendarDays(selectedDate: LocalDate): List<CalendarDay> {
    val firstDayOfMonth = selectedDate.withDayOfMonth(1)
    val lastDayOfMonth = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0

    val days = mutableListOf<CalendarDay>()

    // Add previous month's trailing days
    val previousMonthLastDay = firstDayOfMonth.minusDays(1)
    repeat(firstDayOfWeek) { index ->
        days.add(CalendarDay(
            date = previousMonthLastDay.minusDays(firstDayOfWeek - 1 - index.toLong()),
            isCurrentMonth = false
        ))
    }

    // Add current month's days
    for (day in 1..lastDayOfMonth.dayOfMonth) {
        days.add(CalendarDay(
            date = lastDayOfMonth.withDayOfMonth(day),
            isCurrentMonth = true
        ))
    }

    // Add next month's leading days
    val remainingDays = 42 - days.size // 6 weeks × 7 days
    for (day in 1..remainingDays) {
        days.add(CalendarDay(
            date = lastDayOfMonth.plusDays(day.toLong()),
            isCurrentMonth = false
        ))
    }

    return days
}