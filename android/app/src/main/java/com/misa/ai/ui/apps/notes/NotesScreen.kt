package com.misa.ai.ui.apps.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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

/**
 * Notes Application Screen
 *
 * Features:
 * - Rich text editor with voice input
 * - Handwriting recognition integration
 * - Auto-summarization and smart tagging
 * - Contextual linking to calendar events and tasks
 * - Encrypted sync with local-first design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

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
                text = "Misa Notes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = { viewModel.startVoiceInput() }) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                }
                IconButton(onClick = { viewModel.takePhotoForOCR() }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Photo OCR")
                }
                IconButton(onClick = { viewModel.createNote() }) {
                    Icon(Icons.Default.Add, contentDescription = "New Note")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search and Filter
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            placeholder = { Text("Search notes...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedTag.isEmpty(),
                onClick = { viewModel.clearTagFilter() },
                label = { Text("All") }
            )

            uiState.availableTags.take(5).forEach { tag ->
                FilterChip(
                    selected = uiState.selectedTag == tag,
                    onClick = { viewModel.selectTag(tag) },
                    label = { Text(tag) }
                )
            }

            if (uiState.availableTags.size > 5) {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.showTagSelector() },
                    label = { Text("More...") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Note,
                        contentDescription = "No notes",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notes found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { viewModel.createNote() }) {
                        Text("Create your first note")
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredNotes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { viewModel.openNote(note.id) },
                        onEdit = { viewModel.editNote(note.id) },
                        onDelete = { viewModel.deleteNote(note.id) },
                        onShare = { viewModel.shareNote(note.id) }
                    )
                }
            }
        }
    }

    // AI Enhancement Floating Action Button
    FloatingActionButton(
        onClick = { viewModel.enhanceWithAI() },
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Enhancement")
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    if (note.title.isNotEmpty()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date
                        Text(
                            text = formatDate(note.updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Tags
                        note.tags.take(2).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag) }
                            )
                        }
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
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
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

            // Content preview
            if (note.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3
                )
            }

            // AI-generated summary
            if (note.aiSummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Summary",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = note.aiSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Linked items
            if (note.linkedEvents.isNotEmpty() || note.linkedTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (note.linkedEvents.isNotEmpty()) {
                        AssistChip(
                            onClick = { },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = "Events",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("${note.linkedEvents.size} Events") }
                        )
                    }

                    if (note.linkedTasks.isNotEmpty()) {
                        AssistChip(
                            onClick = { },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Task,
                                    contentDescription = "Tasks",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("${note.linkedTasks.size} Tasks") }
                        )
                    }
                }
            }

            // Attachments
            if (note.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attachments",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${note.attachments.size} attachment${if (note.attachments.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDate(date: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val diff = java.time.Duration.between(date, now)

    return when {
        diff.toMinutes() < 1 -> "Just now"
        diff.toHours() < 1 -> "${diff.toMinutes()}m ago"
        diff.toDays() < 1 -> "${diff.toHours()}h ago"
        diff.toDays() < 7 -> "${diff.toDays()}d ago"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
    }
}

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime,
    val aiSummary: String = "",
    val linkedEvents: List<String> = emptyList(),
    val linkedTasks: List<String> = emptyList(),
    val attachments: List<NoteAttachment> = emptyList(),
    val isEncrypted: Boolean = false,
    val priority: NotePriority = NotePriority.NORMAL
)

data class NoteAttachment(
    val id: String,
    val name: String,
    val type: String,
    val size: Long,
    val path: String
)

enum class NotePriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}