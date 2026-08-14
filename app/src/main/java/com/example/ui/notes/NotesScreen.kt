package com.example.ui.notes

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.repository.SearchResult
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.NoteVaultTopBar
import com.example.ui.components.PinEntryDialog
import com.example.ui.theme.PrimaryGradient
import com.example.ui.viewmodel.NoteVaultViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NoteVaultViewModel) {
    val notes by viewModel.notes.collectAsState()
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val unlockedNoteIds by viewModel.unlockedNoteIds.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var currentViewMode by remember { mutableStateOf("ACTIVE") } // ACTIVE, ARCHIVE, TRASH
    var lockingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var unlockingNote by remember { mutableStateOf<NoteEntity?>(null) }

    // Unique tags set
    val allTags = remember(notes) {
        notes.flatMap { it.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            "Vault Folders",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    label = { 
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("All Notes", fontWeight = FontWeight.SemiBold)
                            Badge(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary) {
                                Text("${notes.size}", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    selected = currentViewMode == "ACTIVE" && selectedFolderId == null,
                    onClick = {
                        currentViewMode = "ACTIVE"
                        viewModel.selectedFolderId.value = null
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                folders.forEach { folder ->
                    NavigationDrawerItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(folder.colorHex)))
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        },
                        label = { Text(folder.name, fontWeight = FontWeight.Medium) },
                        selected = currentViewMode == "ACTIVE" && selectedFolderId == folder.id,
                        onClick = {
                            currentViewMode = "ACTIVE"
                            viewModel.selectedFolderId.value = folder.id
                            scope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                    label = { 
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Archive")
                            Text("${archivedNotes.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    },
                    selected = currentViewMode == "ARCHIVE",
                    onClick = {
                        currentViewMode = "ARCHIVE"
                        viewModel.selectedFolderId.value = null
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    label = { 
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Trash Bin")
                            Text("${trashedNotes.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    },
                    selected = currentViewMode == "TRASH",
                    onClick = {
                        currentViewMode = "TRASH"
                        viewModel.selectedFolderId.value = null
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                NoteVaultTopBar(
                    title = when (currentViewMode) {
                        "ARCHIVE" -> "Archived Notes"
                        "TRASH" -> "Trash Bin"
                        else -> folders.find { it.id == selectedFolderId }?.name ?: "NoteVault Notes"
                    },
                    syncStatus = syncStatus,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.isGridView.value = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List View",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (currentViewMode == "ACTIVE") {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openNewNote() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        icon = { Icon(Icons.Default.Add, contentDescription = "New Note") },
                        text = { Text("New Note", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("create_new_note_fab")
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search titles, content, tags, markdown...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp)
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.searchQuery.value = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Tags Chip Row
                if (allTags.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { viewModel.selectedTag.value = null },
                                label = { Text("All (${notes.size})") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        items(allTags.toList()) { tag ->
                            val isSelected = selectedTag == tag
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectedTag.value = if (isSelected) null else tag
                                },
                                label = { Text("#$tag") },
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }

                val displayedNotes = when (currentViewMode) {
                    "ARCHIVE" -> archivedNotes
                    "TRASH" -> trashedNotes
                    else -> {
                        if (searchQuery.isNotBlank() || selectedTag != null || selectedFolderId != null) {
                            searchResults.filterIsInstance<SearchResult.NoteMatch>().map { it.note }
                        } else {
                            notes
                        }
                    }
                }

                if (displayedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when (currentViewMode) {
                                            "ARCHIVE" -> Icons.Outlined.Archive
                                            "TRASH" -> Icons.Outlined.Delete
                                            else -> Icons.Outlined.NoteAdd
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                when (currentViewMode) {
                                    "ARCHIVE" -> "No archived notes"
                                    "TRASH" -> "Trash bin is empty"
                                    else -> if (searchQuery.isNotBlank()) "No notes match '$searchQuery'" else "No notes in this vault"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                when (currentViewMode) {
                                    "ARCHIVE" -> "Archived notes will appear here for safe keeping."
                                    "TRASH" -> "Items removed from notes will stay here until emptied."
                                    else -> "Capture rich markdown, checklists, or switch to Logic Board."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (currentViewMode == "ACTIVE") {
                                Button(
                                    onClick = { viewModel.openNewNote() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create First Note")
                                }
                            }
                        }
                    }
                } else {
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 220.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedNotes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    folder = folders.find { it.id == note.folderId },
                                    onNoteClick = {
                                        if (note.isLocked && !unlockedNoteIds.contains(note.id)) {
                                            unlockingNote = note
                                        } else {
                                            viewModel.openNote(note)
                                        }
                                    },
                                    onPinClick = { viewModel.togglePinNote(note) },
                                    onArchiveClick = { viewModel.archiveNote(note) },
                                    onTrashClick = { viewModel.trashNote(note) },
                                    onRestoreClick = { viewModel.restoreNote(note) },
                                    onDeletePermanent = { viewModel.deleteNotePermanently(note) },
                                    onLockClick = { lockingNote = note },
                                    isTrashMode = currentViewMode == "TRASH",
                                    isGrid = true
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedNotes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    folder = folders.find { it.id == note.folderId },
                                    onNoteClick = {
                                        if (note.isLocked && !unlockedNoteIds.contains(note.id)) {
                                            unlockingNote = note
                                        } else {
                                            viewModel.openNote(note)
                                        }
                                    },
                                    onPinClick = { viewModel.togglePinNote(note) },
                                    onArchiveClick = { viewModel.archiveNote(note) },
                                    onTrashClick = { viewModel.trashNote(note) },
                                    onRestoreClick = { viewModel.restoreNote(note) },
                                    onDeletePermanent = { viewModel.deleteNotePermanently(note) },
                                    onLockClick = { lockingNote = note },
                                    isTrashMode = currentViewMode == "TRASH",
                                    isGrid = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onFolderCreated = { name, colorHex ->
                viewModel.createFolder(name, colorHex)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (lockingNote != null) {
        PinEntryDialog(
            title = "Set Note Security PIN",
            onPinSubmitted = { pin ->
                viewModel.lockNoteWithPin(lockingNote!!, pin)
                lockingNote = null
            },
            onDismiss = { lockingNote = null }
        )
    }

    if (unlockingNote != null) {
        PinEntryDialog(
            title = "Unlock Note: ${unlockingNote!!.title.ifBlank { "Untitled" }}",
            onPinSubmitted = { pin ->
                val success = viewModel.verifyAndUnlockNote(unlockingNote!!, pin)
                if (success) {
                    val target = unlockingNote!!
                    unlockingNote = null
                    viewModel.openNote(target)
                } else {
                    viewModel.userNotification.value = "Incorrect PIN code"
                }
            },
            onDismiss = { unlockingNote = null }
        )
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    folder: FolderEntity?,
    onNoteClick: () -> Unit,
    onPinClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onTrashClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeletePermanent: () -> Unit,
    onLockClick: () -> Unit,
    isTrashMode: Boolean,
    isGrid: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val folderColor = remember(folder) {
        if (folder != null) Color(android.graphics.Color.parseColor(folder.colorHex))
        else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onNoteClick() }
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (note.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else (folderColor?.copy(alpha = 0.35f) ?: MaterialTheme.colorScheme.outlineVariant)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (note.isPinned) 3.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Folder tag + Locked + Pin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (folder != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = folderColor!!.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(folderColor)
                                )
                                Text(
                                    folder.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                    color = folderColor
                                )
                            }
                        }
                    }
                    if (note.isLocked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "Locked",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                if (!isTrashMode) {
                    IconButton(
                        onClick = onPinClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = note.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = if (isGrid) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Snippet Preview
            Text(
                text = if (note.isLocked) "Encrypted note content. Tap with security PIN to unlock." else note.content.take(120),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isGrid) 3 else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Tags Row
            if (note.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    note.tags.split(",").take(3).forEach { tag ->
                        val cleanTag = tag.trim()
                        if (cleanTag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(vertical = 1.dp)
                            ) {
                                Text(
                                    "#$cleanTag",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Date & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline
                )

                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    if (isTrashMode) {
                        IconButton(onClick = onRestoreClick, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDeletePermanent, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        IconButton(onClick = onLockClick, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Outlined.Lock, contentDescription = "Lock", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = onArchiveClick, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Outlined.Archive, contentDescription = "Archive", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = onTrashClick, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Trash", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}
