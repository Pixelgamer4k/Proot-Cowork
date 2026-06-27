package com.proot.cowork.ui.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.proot.cowork.R
import com.proot.cowork.data.files.GuestFileEntry
import com.proot.cowork.data.files.GuestFileRepository
import com.proot.cowork.ui.design.CoworkTokens

@Composable
fun GuestFileBrowser(
    entries: List<GuestFileEntry>,
    currentPath: String,
    isLoading: Boolean,
    error: String?,
    selectionMode: Boolean,
    selectedPaths: Set<String>,
    onNavigateUp: () -> Unit,
    onNavigateToPath: (String) -> Unit,
    onOpenEntry: (GuestFileEntry) -> Unit,
    onToggleSelect: (GuestFileEntry) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onUpload: () -> Unit,
    onCreateFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    val segments = remember(currentPath) { GuestFileRepository.breadcrumbSegments(currentPath) }
    val canGoUp = GuestFileRepository.parentPath(currentPath) != null

    Column(modifier = modifier.fillMaxSize()) {
        if (selectionMode) {
            SelectionTopBar(
                count = selectedPaths.size,
                onClose = onExitSelectionMode,
                onShare = onShareSelected,
                onDelete = onDeleteSelected,
            )
        } else if (canGoUp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.files_up),
                        tint = CoworkTokens.TextSecondary,
                    )
                }
            }
        }

        BreadcrumbBar(
            segments = segments,
            onNavigate = onNavigateToPath,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Box(Modifier.weight(1f)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoworkTokens.Mint, strokeWidth = 2.dp)
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(error, color = CoworkTokens.Failed, textAlign = TextAlign.Center)
                    }
                }
                entries.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.files_empty),
                            color = CoworkTokens.TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 8.dp,
                        ),
                    ) {
                        items(entries, key = { it.guestPath }) { entry ->
                            BrowserEntryRow(
                                entry = entry,
                                selected = entry.guestPath in selectedPaths,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) {
                                        onToggleSelect(entry)
                                    } else {
                                        onOpenEntry(entry)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) onEnterSelectionMode()
                                    onToggleSelect(entry)
                                },
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = CoworkTokens.Border)
        BrowserActionBar(
            selectionMode = selectionMode,
            onNewFolder = { showNewFolderDialog = true },
            onUpload = onUpload,
            onSelect = {
                if (selectionMode) onExitSelectionMode() else onEnterSelectionMode()
            },
        )
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name ->
                showNewFolderDialog = false
                onCreateFolder(name)
            },
        )
    }
}

@Composable
private fun BreadcrumbBar(
    segments: List<Pair<String, String>>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEachIndexed { index, (label, path) ->
            if (index > 0) {
                Text(
                    text = "  ›  ",
                    color = CoworkTokens.TextMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = label,
                color = CoworkTokens.Mint,
                fontWeight = if (index == segments.lastIndex) FontWeight.SemiBold else FontWeight.Medium,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable { onNavigate(path) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserEntryRow(
    entry: GuestFileEntry,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val accent = if (entry.isDirectory) CoworkTokens.FolderAccent else CoworkTokens.FileAccent
    val displayName = if (entry.isDirectory) "${entry.name}/" else entry.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (selected) CoworkTokens.Mint else CoworkTokens.TextMuted,
                modifier = Modifier
                    .size(22.dp)
                    .padding(end = 10.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .border(1.dp, accent.copy(alpha = 0.45f), CoworkTokens.ShapeIconTile)
                .background(CoworkTokens.SurfaceElevated, CoworkTokens.ShapeIconTile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = displayName,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            color = CoworkTokens.TextPrimary,
            fontWeight = FontWeight.Medium,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Column(horizontalAlignment = Alignment.End) {
            if (!entry.isDirectory) {
                Text(
                    text = GuestFileRepository.formatSize(entry.sizeBytes),
                    color = CoworkTokens.TextMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = GuestFileRepository.formatShortDate(entry.lastModified),
                color = CoworkTokens.TextMuted,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun BrowserActionBar(
    selectionMode: Boolean,
    onNewFolder: () -> Unit,
    onUpload: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CoworkTokens.Surface)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionChip(stringResource(R.string.files_new_folder), onNewFolder, enabled = !selectionMode)
        ActionDivider()
        ActionChip(stringResource(R.string.files_upload), onUpload, enabled = !selectionMode)
        ActionDivider()
        ActionChip(
            label = if (selectionMode) {
                stringResource(R.string.files_cancel_select)
            } else {
                stringResource(R.string.files_select)
            },
            onClick = onSelect,
            highlighted = selectionMode,
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    Text(
        text = label,
        color = when {
            !enabled -> CoworkTokens.TextMuted.copy(alpha = 0.5f)
            highlighted -> CoworkTokens.Mint
            else -> CoworkTokens.TextSecondary
        },
        fontWeight = FontWeight.SemiBold,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ActionDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(18.dp)
            .background(CoworkTokens.Border),
    )
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CoworkTokens.SurfaceElevated)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, stringResource(R.string.files_cancel_select), tint = CoworkTokens.TextSecondary)
        }
        Text(
            text = stringResource(R.string.files_selected_count, count),
            color = CoworkTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onShare, enabled = count > 0) {
            Icon(Icons.Default.IosShare, stringResource(R.string.files_share), tint = CoworkTokens.Mint)
        }
        IconButton(onClick = onDelete, enabled = count > 0) {
            Icon(Icons.Default.Delete, stringResource(R.string.files_delete), tint = CoworkTokens.Failed)
        }
    }
}

@Composable
private fun NewFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.files_new_folder)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.files_folder_name_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) onCreate(name.trim())
                }),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.files_create), color = CoworkTokens.Mint)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = CoworkTokens.TextSecondary)
            }
        },
    )
}
