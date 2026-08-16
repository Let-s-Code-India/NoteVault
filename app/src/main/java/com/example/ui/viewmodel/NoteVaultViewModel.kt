package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AiDiagramService
import com.example.data.repository.NoteVaultRepository
import com.example.data.repository.SearchResult
import com.example.data.security.SecurityManager
import com.example.export.ExportManager
import com.example.reminder.ReminderManager
import com.example.platform.PlatformSettings
import com.example.platform.PermissionManager
import com.example.platform.createPlatformSettings
import com.example.platform.createPermissionManager
import com.example.ui.legal.LegalTab
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

enum class AppNavDestination {
    NOTES,
    NOTE_EDIT,
    LOGIC_BOARD,
    CANVAS_EDIT,
    TASKS,
    REMINDERS,
    SETTINGS_VAULT,
    LEGAL
}

class NoteVaultViewModel(application: Application) : AndroidViewModel(application) {
    val repository = NoteVaultRepository(application)
    private val aiDiagramService = AiDiagramService()
    val platformSettings: PlatformSettings = createPlatformSettings()
    val permissionManager: PermissionManager = createPermissionManager()

    // Screen navigation
    private val _currentDestination = MutableStateFlow(AppNavDestination.NOTES)
    val currentDestination: StateFlow<AppNavDestination> = _currentDestination.asStateFlow()

    // Onboarding Consent Gate
    val hasConsentedOnboarding = MutableStateFlow(
        platformSettings.getBoolean(PlatformSettings.KEY_ONBOARDING_CONSENTED, false)
    )

    // Legal View Tab
    val activeLegalTab = MutableStateFlow(LegalTab.PRIVACY)
    private var previousDestinationBeforeLegal: AppNavDestination = AppNavDestination.SETTINGS_VAULT

    // Active Note / Active Diagram being edited
    private val _activeNote = MutableStateFlow<NoteEntity?>(null)
    val activeNote: StateFlow<NoteEntity?> = _activeNote.asStateFlow()

    private val _activeDiagram = MutableStateFlow<DiagramEntity?>(null)
    val activeDiagram: StateFlow<DiagramEntity?> = _activeDiagram.asStateFlow()

    // Filters
    val searchQuery = MutableStateFlow("")
    val selectedFolderId = MutableStateFlow<String?>(null)
    val selectedTag = MutableStateFlow<String?>(null)
    val isGridView = MutableStateFlow(false)

    // Data flows from Room
    val notes: StateFlow<List<NoteEntity>> = repository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<NoteEntity>> = repository.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diagrams: StateFlow<List<DiagramEntity>> = repository.diagrams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingReminders: StateFlow<List<ReminderEntity>> = repository.upcomingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pastReminders: StateFlow<List<ReminderEntity>> = repository.pastReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tagReminderRules: StateFlow<List<TagReminderRuleEntity>> = repository.tagReminderRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search results
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    // Sync status & UI Feedback
    val syncStatus = MutableStateFlow("100% Local Encrypted Database (SQLCipher)")
    val isGeneratingAiDiagram = MutableStateFlow(false)
    val userNotification = MutableStateFlow<String?>(null)

    // Security
    val unlockedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val isBiometricEnabled = MutableStateFlow(SecurityManager.isBiometricEnabled(application))

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            repository.purgeOldTrashedNotes()
        }

        // Trigger search on query changes
        viewModelScope.launch {
            combine(searchQuery, selectedTag, selectedFolderId) { query, tag, folder ->
                repository.searchAll(query, tag, folder)
            }.collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun navigateTo(destination: AppNavDestination) {
        _currentDestination.value = destination
    }

    // --- Onboarding & Legal Navigation ---
    fun agreeToOnboardingConsent() {
        platformSettings.putBoolean(PlatformSettings.KEY_ONBOARDING_CONSENTED, true)
        hasConsentedOnboarding.value = true
        _currentDestination.value = AppNavDestination.NOTES
    }

    fun openLegalScreen(tab: LegalTab = LegalTab.PRIVACY) {
        if (_currentDestination.value != AppNavDestination.LEGAL) {
            previousDestinationBeforeLegal = _currentDestination.value
        }
        activeLegalTab.value = tab
        _currentDestination.value = AppNavDestination.LEGAL
    }

    fun exitLegalScreen() {
        if (!hasConsentedOnboarding.value) {
            // Stay in consent mode
            _currentDestination.value = AppNavDestination.NOTES
        } else {
            _currentDestination.value = previousDestinationBeforeLegal
        }
    }

    // --- Security PIN & Biometrics ---
    fun saveAppPin(pin: String) {
        SecurityManager.saveAppPin(getApplication(), pin)
        userNotification.value = "App Security PIN saved with SHA-256 salted hash"
    }

    fun verifyAppPin(pin: String): Boolean {
        return SecurityManager.verifyAppPin(getApplication(), pin)
    }

    fun hasAppPin(): Boolean {
        return SecurityManager.hasAppPin(getApplication())
    }

    fun setBiometricEnabled(enabled: Boolean) {
        SecurityManager.setBiometricEnabled(getApplication(), enabled)
        isBiometricEnabled.value = enabled
        userNotification.value = if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled"
    }

    fun lockNoteWithPin(note: NoteEntity, pin: String) {
        val salt = SecurityManager.getSalt(getApplication())
        val hash = SecurityManager.hashPin(pin, salt)
        val updated = note.copy(isLocked = true, pinHash = hash)
        viewModelScope.launch {
            repository.saveNote(updated)
            unlockNote(note.id)
            userNotification.value = "Note locked with salted hash PIN"
        }
    }

    fun verifyAndUnlockNote(note: NoteEntity, pin: String): Boolean {
        val valid = SecurityManager.verifyNotePin(getApplication(), pin, note.pinHash)
        if (valid) {
            unlockNote(note.id)
        }
        return valid
    }

    fun unlockNote(noteId: String) {
        val currentSet = unlockedNoteIds.value.toMutableSet()
        currentSet.add(noteId)
        unlockedNoteIds.value = currentSet
    }

    // --- Notes Actions ---
    fun openNewNote() {
        _activeNote.value = NoteEntity(
            title = "",
            content = "",
            folderId = selectedFolderId.value
        )
        _currentDestination.value = AppNavDestination.NOTE_EDIT
    }

    fun openNote(note: NoteEntity) {
        if (note.isLocked && !unlockedNoteIds.value.contains(note.id)) {
            userNotification.value = "Note is locked. Enter PIN or biometric to view."
            return
        }
        _activeNote.value = note
        _currentDestination.value = AppNavDestination.NOTE_EDIT
    }

    fun saveActiveNote(title: String, content: String, tags: String) {
        val current = _activeNote.value ?: return
        val updated = current.copy(
            title = title.ifBlank { "Untitled Note" },
            content = content,
            tags = tags
        )
        _activeNote.value = updated
        viewModelScope.launch {
            repository.saveNote(updated)
            userNotification.value = "Note saved locally"
        }
    }

    fun togglePinNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note)
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.archiveNote(note)
            userNotification.value = "Note archived"
        }
    }

    fun trashNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.trashNote(note)
            userNotification.value = "Moved to Trash"
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreNote(note)
            userNotification.value = "Note restored"
        }
    }

    fun deleteNotePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNotePermanently(note)
            userNotification.value = "Note deleted permanently"
        }
    }

    // --- Reminders ---
    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.saveReminder(reminder)
            userNotification.value = "Reminder set for ${reminder.title}"
        }
    }

    fun snoozeReminder(reminder: ReminderEntity, minutes: Int = 15) {
        viewModelScope.launch {
            repository.snoozeReminder(reminder, minutes)
            userNotification.value = "Snoozed for $minutes minutes"
        }
    }

    fun rescheduleReminder(reminder: ReminderEntity, newTimeMillis: Long) {
        viewModelScope.launch {
            repository.rescheduleReminder(reminder, newTimeMillis)
            userNotification.value = "Reminder rescheduled"
        }
    }

    fun completeReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.completeReminder(reminder)
            userNotification.value = "Reminder marked completed"
        }
    }

    fun cancelReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.cancelReminder(reminder)
            userNotification.value = "Reminder deleted"
        }
    }

    fun clearPastRemindersHistory() {
        viewModelScope.launch {
            repository.clearPastRemindersHistory()
            userNotification.value = "Past reminders history cleared"
        }
    }

    fun saveTagRule(rule: TagReminderRuleEntity) {
        viewModelScope.launch {
            repository.saveTagRule(rule)
            userNotification.value = "Tag rule saved for #${rule.tagName}"
        }
    }

    fun deleteTagRule(rule: TagReminderRuleEntity) {
        viewModelScope.launch {
            repository.deleteTagRule(rule)
            userNotification.value = "Tag rule deleted"
        }
    }

    fun scheduleBulkTagReminders(tagName: String, triggerTime: Long, customNote: String) {
        viewModelScope.launch {
            repository.scheduleBulkTagReminders(tagName, triggerTime, customNote)
            userNotification.value = "Bulk reminders scheduled for #${tagName}"
        }
    }

    // --- Folders ---
    fun createFolder(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveFolder(FolderEntity(name = name, colorHex = colorHex))
            userNotification.value = "Folder created"
        }
    }

    // --- Diagrams & Logic Board Actions ---
    fun openNewDiagram(templateType: String = "FREEFORM") {
        viewModelScope.launch {
            val newDiagram = repository.createStarterTemplateDiagram(templateType, _activeNote.value?.id)
            repository.saveDiagram(newDiagram)
            _activeDiagram.value = newDiagram
            _currentDestination.value = AppNavDestination.CANVAS_EDIT
        }
    }

    fun openDiagram(diagram: DiagramEntity) {
        _activeDiagram.value = diagram
        _currentDestination.value = AppNavDestination.CANVAS_EDIT
    }

    fun closeActiveDiagram() {
        _activeDiagram.value = null
        _currentDestination.value = AppNavDestination.LOGIC_BOARD
    }

    fun saveActiveDiagram(diagram: DiagramEntity) {
        _activeDiagram.value = diagram
        viewModelScope.launch {
            repository.saveDiagram(diagram)
        }
    }

    fun deleteDiagram(diagram: DiagramEntity) {
        viewModelScope.launch {
            repository.deleteDiagram(diagram)
            if (_activeDiagram.value?.id == diagram.id) {
                _activeDiagram.value = null
                _currentDestination.value = AppNavDestination.LOGIC_BOARD
            }
            userNotification.value = "Diagram deleted"
        }
    }

    fun generateAiDiagram(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            isGeneratingAiDiagram.value = true
            userNotification.value = "Generating diagram from AI..."
            val result = aiDiagramService.generateDiagramFromPrompt(prompt)

            val newDiagram = DiagramEntity(
                title = result.title,
                description = result.description,
                templateType = "DECISION_TREE",
                noteId = _activeNote.value?.id,
                nodesJson = repository.serializeNodes(result.nodes),
                edgesJson = repository.serializeEdges(result.edges)
            )

            repository.saveDiagram(newDiagram)
            _activeDiagram.value = newDiagram
            isGeneratingAiDiagram.value = false
            _currentDestination.value = AppNavDestination.CANVAS_EDIT
            userNotification.value = "Diagram generated!"
        }
    }

    // --- Tasks Actions ---
    fun addNewTask(title: String, dueDate: Long? = null, priority: String = "MEDIUM") {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                dueDate = dueDate,
                priority = priority
            )
            repository.saveTask(task)
            if (dueDate != null && dueDate > System.currentTimeMillis()) {
                ReminderManager.scheduleTaskReminder(getApplication(), task.id, title, dueDate)
            }
            userNotification.value = "Task created"
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            ReminderManager.cancelReminder(getApplication(), task.id)
        }
    }

    // --- Exports ---
    fun exportNotePdf(context: Context, note: NoteEntity) {
        val file = ExportManager.exportNoteAsPdf(context, note)
        ExportManager.shareFile(context, file, "application/pdf", "Export Note as PDF")
    }

    fun exportNoteHtml(context: Context, note: NoteEntity) {
        val file = ExportManager.exportNoteAsHtml(context, note)
        ExportManager.shareFile(context, file, "text/html", "Export Note as HTML")
    }

    fun exportNoteSlides(context: Context, note: NoteEntity) {
        val file = ExportManager.exportNoteAsPresentationSlides(context, note)
        ExportManager.shareFile(context, file, "text/html", "Export Note as Presentation Slides (PPT)")
    }

    fun exportNoteMarkdown(context: Context, note: NoteEntity) {
        val file = ExportManager.exportNoteAsMarkdownFile(context, note)
        ExportManager.shareFile(context, file, "text/markdown", "Export Note as Markdown (.md)")
    }

    fun exportNotePlainText(context: Context, note: NoteEntity) {
        val file = ExportManager.exportNoteAsPlainText(context, note)
        ExportManager.shareFile(context, file, "text/plain", "Export Note as Plain Text (.txt)")
    }

    fun exportFolderPdf(context: Context, folder: FolderEntity) {
        viewModelScope.launch {
            val folderNotes = notes.value.filter { it.folderId == folder.id }
            val file = ExportManager.exportFolderAsPdf(context, folder.name, folderNotes)
            ExportManager.shareFile(context, file, "application/pdf", "Export Folder PDF")
        }
    }

    fun exportFolderZip(context: Context, folder: FolderEntity) {
        viewModelScope.launch {
            val folderNotes = notes.value.filter { it.folderId == folder.id }
            val file = ExportManager.exportFolderAsZip(context, folder.name, folderNotes)
            ExportManager.shareFile(context, file, "application/zip", "Export Folder ZIP")
        }
    }

    fun exportDiagramPng(context: Context, diagram: DiagramEntity) {
        val nodes = repository.parseNodesJson(diagram.nodesJson)
        val edges = repository.parseEdgesJson(diagram.edgesJson)
        val file = ExportManager.exportDiagramAsPng(context, diagram, nodes, edges)
        ExportManager.shareFile(context, file, "image/png", "Export Diagram PNG")
    }

    fun exportDiagramSvg(context: Context, diagram: DiagramEntity) {
        val nodes = repository.parseNodesJson(diagram.nodesJson)
        val edges = repository.parseEdgesJson(diagram.edgesJson)
        val file = ExportManager.exportDiagramAsSvg(context, diagram, nodes, edges)
        ExportManager.shareFile(context, file, "image/svg+xml", "Export Diagram SVG")
    }

    fun exportDiagramMermaid(context: Context, diagram: DiagramEntity) {
        val nodes = repository.parseNodesJson(diagram.nodesJson)
        val edges = repository.parseEdgesJson(diagram.edgesJson)
        val file = ExportManager.exportDiagramAsMermaid(context, diagram, nodes, edges)
        ExportManager.shareFile(context, file, "text/plain", "Export Mermaid Flowchart (.mmd)")
    }

    fun exportFullBackup(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.createWholeAppBackupJson()
            onExportReady(json)
            userNotification.value = "Full backup exported"
        }
    }

    fun exportFullZipBackup(context: Context) {
        viewModelScope.launch {
            val json = repository.createWholeAppBackupJson()
            val zipFile = ExportManager.exportFullBackupZip(context, json)
            ExportManager.shareFile(context, zipFile, "application/zip", "Share Full NoteVault Backup (.zip)")
        }
    }

    fun restoreFullZipBackup(context: Context, inputStream: InputStream) {
        viewModelScope.launch {
            val json = ExportManager.restoreFullBackupZip(context, inputStream)
            if (json != null && repository.restoreFromBackupJson(json)) {
                userNotification.value = "Full ZIP Backup restored with attached images!"
            } else {
                userNotification.value = "Failed to restore ZIP backup"
            }
        }
    }

    fun restoreFullBackup(jsonString: String) {
        viewModelScope.launch {
            val success = repository.restoreFromBackupJson(jsonString)
            if (success) {
                userNotification.value = "Backup restored successfully!"
            } else {
                userNotification.value = "Failed to restore backup format"
            }
        }
    }

    fun clearNotification() {
        userNotification.value = null
    }
}
