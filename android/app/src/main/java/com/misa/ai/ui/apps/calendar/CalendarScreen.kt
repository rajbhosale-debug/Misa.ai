package com.misa.ai.ui.apps.calendar

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Calendar Application Screen
 *
 * Features:
 * - AI-powered scheduling with OCR import
 * - PDF/image import with semantic parsing
 * - Google Calendar/Outlook sync
 * - Predictive scheduling based on habits
 * - Meeting invitations and RSVP assistant
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Misa Calendar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = { viewModel.importCalendarFile() }) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Import")
                }
                IconButton(onClick = { viewModel.createEvent() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Event")
                }
                IconButton(onClick = { viewModel.syncCalendars() }) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Month View
        CalendarMonthView(
            currentDate = uiState.currentDate,
            selectedDate = uiState.selectedDate,
            events = uiState.events,
            onDateSelected = viewModel::selectDate,
            onEventClick = viewModel::openEvent
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Events List
        Text(
            text = "Events for ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.selectedDateEvents) { event ->
                EventCard(
                    event = event,
                    onEdit = { viewModel.editEvent(event.id) },
                    onDelete = { viewModel.deleteEvent(event.id) }
                )
            }
        }

        // AI Suggestion Floating Action Button
        FloatingActionButton(
            onClick = { viewModel.getAISchedulingSuggestions() },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Suggestions")
        }
    }
}

@Composable
private fun CalendarMonthView(
    currentDate: LocalDate,
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit
) {
    // Calendar grid implementation
    Column {
        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Calendar days grid
        val daysInMonth = currentDate.lengthOfMonth()
        val firstDayOfWeek = currentDate.withDayOfMonth(1).dayOfWeek.value % 7

        LazyColumn {
            items((1..daysInMonth).chunked(7)) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (day, index) in week.withIndex()) {
                        val date = currentDate.withDayOfMonth(day)
                        val dayEvents = events.filter {
                            it.startTime.toLocalDate() == date
                        }
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()

                        CalendarDay(
                            day = day,
                            date = date,
                            dayEvents = dayEvents,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateSelected(date) },
                            onEventClick = onEventClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    date: LocalDate,
    dayEvents: List<CalendarEvent>,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit
) {
    Card(
        modifier = Modifier
            .size(40.dp)
            .padding(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isToday -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else if (isToday) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            // Event indicators
            if (dayEvents.isNotEmpty()) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    dayEvents.take(3).forEach { event ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = event.color,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${event.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${event.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (event.location.isNotEmpty()) {
                        Text(
                            text = "📍 ${event.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (event.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // AI-generated insights
            if (event.aiInsights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Insight",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.aiInsights.first(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}