package com.example.ui.notes

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.platform.PermissionType
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.imageeditor.DesignCanvasScreen
import com.example.ui.imageeditor.ImageEditorDialog
import com.example.ui.imageeditor.ImageProcessingUtils
import com.example.ui.reminder.ReminderSchedulingSheet
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.NoteVaultViewModel

enum class NoteViewMode {
    EDITOR,
    COMPILED_PREVIEW,
    SPLIT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(viewModel: NoteVaultViewModel) {
    val note = viewModel.activeNote.collectAsState().value ?: return
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current

    var titleText by remember(note.id) { mutableStateOf(note.title) }
    var contentText by remember(note.id) { mutableStateOf(note.content) }
    var tagsText by remember(note.id) { mutableStateOf(note.tags) }
    var selectedFolderId by remember(note.id) { mutableStateOf(note.folderId) }

    var viewMode by remember { mutableStateOf(NoteViewMode.EDITOR) }
    var showExportModal by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showCanvaStudio by remember { mutableStateOf(false) }
    var showSchedulingSheet by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var photoEditorBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showSchedulingSheet = true
    }

    fun openReminderScheduling() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.permissionManager.isPermissionGranted(PermissionType.NOTIFICATIONS)) {
            showPermissionRationale = true
        } else {
            showSchedulingSheet = true
        }
    }

    // Word Count & Stats
    val wordCount = remember(contentText) {
        contentText.split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    // Undo / Redo Text Stacks
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    fun applyContentChange(newText: String) {
        if (newText != contentText) {
            if (undoStack.size > 30) undoStack.removeAt(0)
            undoStack.add(contentText)
            redoStack.clear()
            contentText = newText
        }
    }

    fun undoText() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(contentText)
            contentText = prev
        }
    }

    fun redoText() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(contentText)
            contentText = next
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bmp = ImageProcessingUtils.loadBitmapFromUri(context, it)
            if (bmp != null) {
                photoEditorBitmap = bmp
            }
        }
    }

    // Auto save debouncer
    LaunchedEffect(titleText, contentText, tagsText, selectedFolderId) {
        viewModel.saveActiveNote(titleText, contentText, tagsText)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        // If wide screen and viewMode hasn't been explicitly changed, user can use Split View
        val effectiveViewMode = if (isWideScreen && viewMode == NoteViewMode.EDITOR) NoteViewMode.SPLIT else viewMode

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isWideScreen) {
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.height(34.dp)) {
                                    SegmentedButton(
                                        selected = viewMode == NoteViewMode.EDITOR,
                                        onClick = { viewMode = NoteViewMode.EDITOR },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                        icon = {}
                                    ) {
                                        Text("Edit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    SegmentedButton(
                                        selected = viewMode == NoteViewMode.SPLIT,
                                        onClick = { viewMode = NoteViewMode.SPLIT },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                        icon = {}
                                    ) {
                                        Text("Split", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    SegmentedButton(
                                        selected = viewMode == NoteViewMode.COMPILED_PREVIEW,
                                        onClick = { viewMode = NoteViewMode.COMPILED_PREVIEW },
                                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                        icon = {}
                                    ) {
                                        Text("Preview", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            } else {
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.height(34.dp)) {
                                    SegmentedButton(
                                        selected = viewMode != NoteViewMode.COMPILED_PREVIEW,
                                        onClick = { viewMode = NoteViewMode.EDITOR },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                        icon = {}
                                    ) {
                                        Text("Edit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    SegmentedButton(
                                        selected = viewMode == NoteViewMode.COMPILED_PREVIEW,
                                        onClick = { viewMode = NoteViewMode.COMPILED_PREVIEW },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                        icon = {}
                                    ) {
                                        Text("Preview", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    "$wordCount words",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(AppNavDestination.NOTES) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { undoText() },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Text",
                                tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { redoText() },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo Text",
                                tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { openReminderScheduling() }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (note.reminderTime != null) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Set Local Reminder",
                                tint = if (note.reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { showCanvaStudio = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Brush, contentDescription = "Canva Design Studio", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { viewModel.openNewDiagram() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.AccountTree, contentDescription = "New Logic Diagram", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showExportModal = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.IosShare, contentDescription = "Export Hub", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Folder Selector & Tags Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        AssistChip(
                            onClick = { showFolderMenu = true },
                            label = {
                                Text(
                                    folders.find { it.id == selectedFolderId }?.name ?: "No Folder",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Root Vault)") },
                                onClick = {
                                    selectedFolderId = null
                                    showFolderMenu = false
                                }
                            )
                            folders.forEach { f ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(f.colorHex)))
                                            )
                                            Text(f.name)
                                        }
                                    },
                                    onClick = {
                                        selectedFolderId = f.id
                                        showFolderMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = tagsText,
                            onValueChange = { tagsText = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                            decorationBox = { innerTextField ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (tagsText.isEmpty()) {
                                        Text("tags (e.g. project, notes)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                // Quick Formatting & Markdown Builder Toolbar
                if (effectiveViewMode != NoteViewMode.COMPILED_PREVIEW) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { applyContentChange(contentText + "\n# ") }, modifier = Modifier.size(32.dp)) {
                                Text("H1", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { applyContentChange(contentText + "\n## ") }, modifier = Modifier.size(32.dp)) {
                                Text("H2", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { applyContentChange(contentText + "\n### ") }, modifier = Modifier.size(32.dp)) {
                                Text("H3", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp))
                            IconButton(onClick = { applyContentChange(contentText + "**bold**") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(17.dp))
                            }
                            IconButton(onClick = { applyContentChange(contentText + "*italic*") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(17.dp))
                            }
                            IconButton(onClick = { applyContentChange(contentText + "\n- [ ] ") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.CheckBox, contentDescription = "Checklist Task", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp))
                            IconButton(onClick = { applyContentChange(contentText + "\n```kotlin\n// Code snippet\n```\n") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Code, contentDescription = "Code Block", modifier = Modifier.size(17.dp))
                            }
                            IconButton(onClick = { applyContentChange(contentText + "\n> [!NOTE]\n> Important concept to remember\n") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Info, contentDescription = "Callout Box", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.tertiary)
                            }
                            IconButton(onClick = { applyContentChange(contentText + "\n| Item | Description | Status |\n|---|---|---|\n| Task 1 | Implement feature | Done |\n") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.TableChart, contentDescription = "Markdown Table", modifier = Modifier.size(17.dp))
                            }
                            IconButton(onClick = { applyContentChange(contentText + "\n---\n") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.HorizontalRule, contentDescription = "Divider", modifier = Modifier.size(17.dp))
                            }
                            VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp))
                            IconButton(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach & Edit Photo", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { showCanvaStudio = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DesignServices, contentDescription = "Design Canvas", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }

                // Title Field
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    placeholder = { Text("Note Title...") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .testTag("note_title_input")
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Main Editor vs Compiled Output vs Split View
                if (effectiveViewMode == NoteViewMode.SPLIT) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Editor
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            OutlinedTextField(
                                value = contentText,
                                onValueChange = { contentText = it },
                                placeholder = { Text("Type notes, markdown, checklists, or code blocks...") },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif, lineHeight = 22.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .testTag("note_content_input")
                            )
                        }

                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Right Live Preview
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                MarkdownCompilerView(
                                    content = contentText,
                                    onContentChange = { updated -> applyContentChange(updated) }
                                )
                            }
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = effectiveViewMode,
                        transitionSpec = {
                            fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                        },
                        label = "MarkdownViewModeTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { mode ->
                        when (mode) {
                            NoteViewMode.COMPILED_PREVIEW -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    MarkdownCompilerView(
                                        content = contentText,
                                        onContentChange = { updated -> applyContentChange(updated) }
                                    )
                                }
                            }
                            else -> {
                                OutlinedTextField(
                                    value = contentText,
                                    onValueChange = { contentText = it },
                                    placeholder = { Text("Type notes, markdown, checklists, or code blocks...") },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif, lineHeight = 22.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                        .testTag("note_content_input")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSchedulingSheet) {
        ReminderSchedulingSheet(
            initialTitle = titleText,
            targetType = "NOTE",
            targetId = note.id,
            linkedTags = tagsText,
            onReminderSaved = { reminder ->
                viewModel.saveReminder(reminder)
                showSchedulingSheet = false
            },
            onDismiss = { showSchedulingSheet = false }
        )
    }

    if (showCanvaStudio) {
        DesignCanvasScreen(
            onDismiss = { showCanvaStudio = false },
            onSaveComposition = { bitmap ->
                val savedFile = ImageProcessingUtils.saveBitmapToLocalFiles(context, bitmap)
                contentText += "\n\n![Design Canvas](file://${savedFile.absolutePath})\n"
                showCanvaStudio = false
                viewModel.userNotification.value = "Design Canvas saved and attached to Note"
            }
        )
    }

    photoEditorBitmap?.let { bmp ->
        ImageEditorDialog(
            initialBitmap = bmp,
            onDismiss = { photoEditorBitmap = null },
            onSaveEdited = { editedBmp ->
                val savedFile = ImageProcessingUtils.saveBitmapToLocalFiles(context, editedBmp)
                contentText += "\n\n![Attached Image](file://${savedFile.absolutePath})\n"
                photoEditorBitmap = null
                viewModel.userNotification.value = "Edited photo attached to Note"
            }
        )
    }

    if (showPermissionRationale) {
        PermissionRationaleDialog(
            explanation = viewModel.permissionManager.getPermissionExplanation(PermissionType.NOTIFICATIONS),
            onConfirm = {
                showPermissionRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    showSchedulingSheet = true
                }
            },
            onDismiss = {
                showPermissionRationale = false
                showSchedulingSheet = true
            }
        )
    }

    if (showSchedulingSheet) {
        ReminderSchedulingSheet(
            targetType = "NOTE",
            targetId = note.id,
            initialTitle = note.title.ifBlank { "Note Reminder" },
            onReminderSaved = { reminder ->
                viewModel.saveReminder(reminder)
                showSchedulingSheet = false
            },
            onDismiss = { showSchedulingSheet = false }
        )
    }

    // Comprehensive Export & Sharing Hub
    if (showExportModal) {
        ExportHubDialog(
            note = note.copy(title = titleText, content = contentText, tags = tagsText),
            viewModel = viewModel,
            onDismiss = { showExportModal = false }
        )
    }
}

@Composable
fun ExportHubDialog(
    note: NoteEntity,
    viewModel: NoteVaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.IosShare, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Text("Export & Share Hub", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Compile and export '${note.title.ifBlank { "Untitled Note" }}' into your desired document format:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. PDF Document
                ExportOptionCard(
                    icon = Icons.Default.PictureAsPdf,
                    iconTint = Color(0xFFEF4444),
                    title = "PDF Document (.pdf)",
                    subtitle = "Paginated document with formatting and checklists",
                    onClick = {
                        viewModel.exportNotePdf(context, note)
                        onDismiss()
                    }
                )

                // 2. Presentation Slides (PPT / HTML Deck)
                ExportOptionCard(
                    icon = Icons.Default.Slideshow,
                    iconTint = Color(0xFFF59E0B),
                    title = "Presentation Slides (PPT / HTML)",
                    subtitle = "Interactive presentation deck with slide transitions",
                    onClick = {
                        viewModel.exportNoteSlides(context, note)
                        onDismiss()
                    }
                )

                // 3. Web Page (.html)
                ExportOptionCard(
                    icon = Icons.Default.Html,
                    iconTint = Color(0xFF3B82F6),
                    title = "Web Page (.html)",
                    subtitle = "Styled HTML with modern CSS and typography",
                    onClick = {
                        viewModel.exportNoteHtml(context, note)
                        onDismiss()
                    }
                )

                // 4. Markdown Source (.md)
                ExportOptionCard(
                    icon = Icons.Default.Description,
                    iconTint = Color(0xFF8B5CF6),
                    title = "Markdown Document (.md)",
                    subtitle = "Clean raw Markdown with YAML frontmatter tags",
                    onClick = {
                        viewModel.exportNoteMarkdown(context, note)
                        onDismiss()
                    }
                )

                // 5. Plain Text (.txt)
                ExportOptionCard(
                    icon = Icons.Default.TextFields,
                    iconTint = Color(0xFF64748B),
                    title = "Plain Text (.txt)",
                    subtitle = "Pure unformatted text for universal compatibility",
                    onClick = {
                        viewModel.exportNotePlainText(context, note)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ExportOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}
