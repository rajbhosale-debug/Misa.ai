package com.misa.ai.ui.apps.filehub.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misa.ai.domain.model.*

/**
 * FileHub Main Screen Component
 * Provides comprehensive file management interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileHubScreen(
    files: List<FileNode>,
    currentPath: String,
    selectedFiles: Set<String>,
    isLoading: Boolean,
    storageUsage: StorageUsage,
    onFileClick: (FileNode) -> Unit,
    onFileLongClick: (FileNode) -> Unit,
    onFolderClick: (FileNode) -> Unit,
    onNavigationClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onUploadClick: () -> Unit,
    onNewFolderClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (FileSortOption, SortDirection) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    currentSortOption: FileSortOption,
    currentSortDirection: SortDirection,
    currentViewMode: ViewMode,
    searchQuery: String
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showViewModeMenu by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with navigation and actions
        FileHubTopBar(
            currentPath = currentPath,
            onNavigationClick = onNavigationClick,
            onRefresh = onRefresh,
            onUploadClick = onUploadClick,
            onNewFolderClick = onNewFolderClick,
            showSelectionMenu = selectedFiles.isNotEmpty(),
            onSelectionMenuClick = { showSelectionMenu = true }
        )

        // Storage usage indicator
        StorageUsageIndicator(
            storageUsage = storageUsage,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Search and filter bar
        SearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onSortMenuClick = { showSortMenu = true },
            onViewModeMenuClick = { showViewModeMenu = true },
            currentSortOption = currentSortOption,
            currentSortDirection = currentSortDirection,
            currentViewMode = currentViewMode
        )

        // File content area
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    LoadingIndicator()
                }
                files.isEmpty() -> {
                    EmptyFilesState(
                        currentPath = currentPath,
                        onUploadClick = onUploadClick,
                        onNewFolderClick = onNewFolderClick
                    )
                }
                currentViewMode == ViewMode.LIST -> {
                    FileListView(
                        files = files,
                        selectedFiles = selectedFiles,
                        onFileClick = onFileClick,
                        onFileLongClick = onFileLongClick,
                        onFolderClick = onFolderClick
                    )
                }
                currentViewMode == ViewMode.GRID -> {
                    FileGridView(
                        files = files,
                        selectedFiles = selectedFiles,
                        onFileClick = onFileClick,
                        onFileLongClick = onFileLongClick,
                        onFolderClick = onFolderClick
                    )
                }
            }
        }
    }

    // Sort menu
    DropdownMenu(
        expanded = showSortMenu,
        onDismissRequest = { showSortMenu = false }
    ) {
        FileSortOption.values().forEach { option ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getSortOptionDisplayName(option),
                            modifier = Modifier.weight(1f)
                        )
                        if (option == currentSortOption) {
                            Icon(
                                imageVector = if (currentSortDirection == SortDirection.ASCENDING) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = null
                            )
                        }
                    }
                },
                onClick = {
                    val newDirection = if (option == currentSortOption) {
                        if (currentSortDirection == SortDirection.ASCENDING) {
                            SortDirection.DESCENDING
                        } else {
                            SortDirection.ASCENDING
                        }
                    } else {
                        SortDirection.ASCENDING
                    }
                    onSortOptionChange(option, newDirection)
                    showSortMenu = false
                }
            )
        }
    }

    // View mode menu
    DropdownMenu(
        expanded = showViewModeMenu,
        onDismissRequest = { showViewModeMenu = false }
    ) {
        ViewMode.values().forEach { mode ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getViewModeIcon(mode),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(getViewModeDisplayName(mode))
                        Spacer(modifier = Modifier.weight(1f))
                        if (mode == currentViewMode) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onClick = {
                    onViewModeChange(mode)
                    showViewModeMenu = false
                }
            )
        }
    }

    // Selection menu (shown when files are selected)
    if (showSelectionMenu && selectedFiles.isNotEmpty()) {
        FileSelectionMenu(
            selectedCount = selectedFiles.size,
            onSelectAll = { /* TODO: Implement select all */ },
            onDeselectAll = { /* TODO: Implement deselect all */ },
            onDeleteSelected = { /* TODO: Implement batch delete */ },
            onShareSelected = { /* TODO: Implement batch share */ },
            onMoveSelected = { /* TODO: Implement batch move */ },
            onCopySelected = { /* TODO: Implement batch copy */ },
            onDismiss = { showSelectionMenu = false }
        )
    }
}

/**
 * Top bar with navigation and actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileHubTopBar(
    currentPath: String,
    onNavigationClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onUploadClick: () -> Unit,
    onNewFolderClick: () -> Unit,
    showSelectionMenu: Boolean,
    onSelectionMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "FileHub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                PathNavigation(
                    currentPath = currentPath,
                    onNavigationClick = onNavigationClick
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }

            IconButton(onClick = onUploadClick) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload"
                )
            }

            IconButton(onClick = onNewFolderClick) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = "New folder"
                )
            }

            if (showSelectionMenu) {
                IconButton(onClick = onSelectionMenuClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Selection options"
                    )
                }
            }
        }
    )
}

/**
 * Path navigation component
 */
@Composable
private fun PathNavigation(
    currentPath: String,
    onNavigationClick: (String) -> Unit
) {
    val pathSegments = currentPath.split("/").filter { it.isNotEmpty() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Root directory
        ClickablePathSegment(
            name = "Root",
            onClick = { onNavigationClick("/") }
        )

        // Path segments
        pathSegments.forEachIndexed { index, segment ->
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ClickablePathSegment(
                name = segment,
                onClick = {
                    val path = "/" + pathSegments.take(index + 1).joinToString("/")
                    onNavigationClick(path)
                }
            )
        }
    }
}

/**
 * Clickable path segment
 */
@Composable
private fun ClickablePathSegment(
    name: String,
    onClick: () -> Unit
) {
    Text(
        text = name,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.clickable { onClick() }
    )
}

/**
 * Storage usage indicator
 */
@Composable
private fun StorageUsageIndicator(
    storageUsage: StorageUsage,
    modifier: Modifier = Modifier
) {
    val usagePercentage = (storageUsage.usedSpace.toFloat() / storageUsage.totalSpace * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage Usage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${storageUsage.usedSpace.getFormattedSize()} / ${storageUsage.totalSpace.getFormattedSize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = usagePercentage / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    usagePercentage > 90 -> MaterialTheme.colorScheme.error
                    usagePercentage > 75 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$usagePercentage% used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Search and filter bar
 */
@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSortMenuClick: () -> Unit,
    onViewModeMenuClick: () -> Unit,
    currentSortOption: FileSortOption,
    currentSortDirection: SortDirection,
    currentViewMode: ViewMode
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search files and folders...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        // Sort button
        IconButton(onClick = onSortMenuClick) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Sort options"
            )
        }

        // View mode button
        IconButton(onClick = onViewModeMenuClick) {
            Icon(
                imageVector = getViewModeIcon(currentViewMode),
                contentDescription = "View mode"
            )
        }
    }
}

/**
 * File list view
 */
@Composable
private fun FileListView(
    files: List<FileNode>,
    selectedFiles: Set<String>,
    onFileClick: (FileNode) -> Unit,
    onFileLongClick: (FileNode) -> Unit,
    onFolderClick: (FileNode) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = files,
            key = { it.id }
        ) { file ->
            FileListItem(
                file = file,
                isSelected = file.id in selectedFiles,
                onFileClick = onFileClick,
                onFileLongClick = onFileLongClick,
                onFolderClick = onFolderClick
            )
        }
    }
}

/**
 * Individual file list item
 */
@Composable
private fun FileListItem(
    file: FileNode,
    isSelected: Boolean,
    onFileClick: (FileNode) -> Unit,
    onFileLongClick: (FileNode) -> Unit,
    onFolderClick: (FileNode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                if (file.isFolder()) {
                    onFolderClick(file)
                } else {
                    onFileClick(file)
                }
            }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
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
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File/folder icon
            Icon(
                imageVector = getFileIcon(file),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = getFileIconColor(file)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // File size
                    if (file.isFile()) {
                        Text(
                            text = file.getFormattedSize(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Modified date
                    Text(
                        text = formatRelativeTime(file.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status icons
            Row {
                if (file.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (file.isShared) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Shared",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                when (file.syncStatus) {
                    SyncStatus.SYNCING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    SyncStatus.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Sync error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> { /* No icon needed */ }
                }
            }
        }
    }
}

/**
 * File grid view
 */
@Composable
private fun FileGridView(
    files: List<FileNode>,
    selectedFiles: Set<String>,
    onFileClick: (FileNode) -> Unit,
    onFileLongClick: (FileNode) -> Unit,
    onFolderClick: (FileNode) -> Unit
) {
    // Simplified grid view - in a real implementation would use LazyVerticalGrid
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = files.chunked(2), // 2 columns
            key = { chunk -> chunk.map { it.id } }
        ) { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                chunk.forEach { file ->
                    FileGridItem(
                        file = file,
                        isSelected = file.id in selectedFiles,
                        onFileClick = onFileClick,
                        onFileLongClick = onFileLongClick,
                        onFolderClick = onFolderClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Add empty item if odd number of files
                if (chunk.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * File grid item
 */
@Composable
private fun FileGridItem(
    file: FileNode,
    isSelected: Boolean,
    onFileClick: (FileNode) -> Unit,
    onFileLongClick: (FileNode) -> Unit,
    onFolderClick: (FileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable {
                if (file.isFolder()) {
                    onFolderClick(file)
                } else {
                    onFileClick(file)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getFileIcon(file),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = getFileIconColor(file)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Loading indicator
 */
@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading files...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty files state
 */
@Composable
private fun EmptyFilesState(
    currentPath: String,
    onUploadClick: () -> Unit,
    onNewFolderClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (currentPath == "/") "No files yet" else "This folder is empty",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Upload files or create a folder to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onUploadClick) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Files")
            }
            OutlinedButton(onClick = onNewFolderClick) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Folder")
            }
        }
    }
}

/**
 * File selection menu
 */
@Composable
private fun FileSelectionMenu(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onMoveSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Select All") },
            onClick = {
                onSelectAll()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Deselect All") },
            onClick = {
                onDeselectAll()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Share ($selectedCount)") },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            onClick = {
                onShareSelected()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Move ($selectedCount)") },
            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
            onClick = {
                onMoveSelected()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Copy ($selectedCount)") },
            leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null) },
            onClick = {
                onCopySelected()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Delete ($selectedCount)") },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            onClick = {
                onDeleteSelected()
                onDismiss()
            }
        )
    }
}

/**
 * View modes
 */
enum class ViewMode {
    LIST,
    GRID
}

// === Helper Functions ===

/**
 * Get file icon based on type
 */
private fun getFileIcon(file: FileNode): ImageVector {
    return if (file.isFolder()) {
        Icons.Default.Folder
    } else {
        when (file.type) {
            FileType.IMAGE -> Icons.Default.Image
            FileType.VIDEO -> Icons.Default.VideoFile
            FileType.AUDIO -> Icons.Default.AudioFile
            FileType.TEXT -> Icons.Default.TextSnippet
            FileType.PDF -> Icons.Default.PictureAsPdf
            FileType.DOCUMENT -> Icons.Default.Description
            FileType.SPREADSHEET -> Icons.Default.TableChart
            FileType.PRESENTATION -> Icons.Default.Slideshow
            FileType.ARCHIVE -> Icons.Default.Archive
            FileType.CODE -> Icons.Default.Code
            else -> Icons.Default.InsertDriveFile
        }
    }
}

/**
 * Get file icon color based on type
 */
private fun getFileIconColor(file: FileNode): Color {
    return if (file.isFolder()) {
        Color(0xFF4285F4) // Blue for folders
    } else {
        when (file.type) {
            FileType.IMAGE -> Color(0xFF34A853) // Green
            FileType.VIDEO -> Color(0xFFEA4335) // Red
            FileType.AUDIO -> Color(0xFF9C27B0) // Purple
            FileType.TEXT -> Color(0xFF607D8B) // Blue Grey
            FileType.PDF -> Color(0xFFE91E63) // Pink
            FileType.DOCUMENT -> Color(0xFF1976D2) // Blue
            FileType.SPREADSHEET -> Color(0xFF4CAF50) // Green
            FileType.PRESENTATION -> Color(0xFFFF9800) // Orange
            FileType.ARCHIVE -> Color(0xFF795548) // Brown
            FileType.CODE -> Color(0xFF9E9E9E) // Grey
            else -> Color(0xFF757575) // Medium Grey
        }
    }
}

/**
 * Get sort option display name
 */
private fun getSortOptionDisplayName(option: FileSortOption): String {
    return when (option) {
        FileSortOption.NAME -> "Name"
        FileSortOption.SIZE -> "Size"
        FileSortOption.TYPE -> "Type"
        FileSortOption.MODIFIED_DATE -> "Modified"
        FileSortOption.CREATED_DATE -> "Created"
        FileSortOption.ACCESSED_DATE -> "Accessed"
    }
}

/**
 * Get view mode icon
 */
private fun getViewModeIcon(mode: ViewMode): ImageVector {
    return when (mode) {
        ViewMode.LIST -> Icons.Default.ViewList
        ViewMode.GRID -> Icons.Default.ViewModule
    }
}

/**
 * Get view mode display name
 */
private fun getViewModeDisplayName(mode: ViewMode): String {
    return when (mode) {
        ViewMode.LIST -> "List View"
        ViewMode.GRID -> "Grid View"
    }
}

/**
 * Format relative time
 */
private fun formatRelativeTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val diff = java.time.Duration.between(dateTime, now)

    return when {
        diff.toMinutes() < 1 -> "Just now"
        diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
        diff.toHours() < 24 -> "${diff.toHours()}h ago"
        diff.toDays() < 7 -> "${diff.toDays()}d ago"
        else -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))
    }
}

/**
 * Format file size
 */
private fun Long.getFormattedSize(): String {
    return when {
        this < 1024 -> "${this}B"
        this < 1024 * 1024 -> "${this / 1024}KB"
        this < 1024 * 1024 * 1024 -> "${this / (1024 * 1024)}MB"
        else -> "${this / (1024 * 1024 * 1024)}GB"
    }
}