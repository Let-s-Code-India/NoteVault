package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

import com.example.reminder.ReminderManager

class NoteVaultRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val noteDao = db.noteDao()
    private val folderDao = db.folderDao()
    private val taskDao = db.taskDao()
    private val diagramDao = db.diagramDao()
    private val reminderDao = db.reminderDao()
    private val tagRuleDao = db.tagReminderRuleDao()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val nodeListAdapter = moshi.adapter<List<DiagramNode>>(
        Types.newParameterizedType(List::class.java, DiagramNode::class.java)
    )
    private val edgeListAdapter = moshi.adapter<List<DiagramEdge>>(
        Types.newParameterizedType(List::class.java, DiagramEdge::class.java)
    )

    // Flow getters
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val folders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val tasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val diagrams: Flow<List<DiagramEntity>> = diagramDao.getAllDiagrams()
    val upcomingReminders: Flow<List<ReminderEntity>> = reminderDao.getUpcomingReminders()
    val pastReminders: Flow<List<ReminderEntity>> = reminderDao.getPastReminders()
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val tagReminderRules: Flow<List<TagReminderRuleEntity>> = tagRuleDao.getAllRules()

    suspend fun getNoteById(id: String): NoteEntity? = noteDao.getNoteById(id)
    suspend fun getDiagramById(id: String): DiagramEntity? = diagramDao.getDiagramById(id)

    suspend fun saveNote(note: NoteEntity) {
        noteDao.insertOrUpdateNote(note.copy(updatedAt = System.currentTimeMillis()))
        extractAndSyncTasksFromNote(note)
        evaluateTagRulesForNote(note)
    }

    suspend fun togglePin(note: NoteEntity) {
        noteDao.insertOrUpdateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun archiveNote(note: NoteEntity) {
        noteDao.insertOrUpdateNote(note.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
    }

    suspend fun restoreNote(note: NoteEntity) {
        noteDao.insertOrUpdateNote(note.copy(isArchived = false, isTrashed = false, trashedTimestamp = 0L, updatedAt = System.currentTimeMillis()))
    }

    suspend fun trashNote(note: NoteEntity) {
        noteDao.insertOrUpdateNote(note.copy(isTrashed = true, trashedTimestamp = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNotePermanently(note: NoteEntity) {
        noteDao.deleteNotePermanently(note)
        taskDao.deleteTasksByNoteId(note.id)
    }

    suspend fun purgeOldTrashedNotes() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        noteDao.purgeOldTrashedNotes(thirtyDaysAgo)
    }

    // Folders
    suspend fun saveFolder(folder: FolderEntity) {
        folderDao.insertOrUpdateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.deleteFolder(folder)
    }

    // Tasks
    suspend fun saveTask(task: TaskEntity) {
        taskDao.insertOrUpdateTask(task)
    }

    suspend fun toggleTaskCompleted(task: TaskEntity) {
        taskDao.insertOrUpdateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    private suspend fun extractAndSyncTasksFromNote(note: NoteEntity) {
        // Automatically extract checklist items like "- [ ] Task name" or "- [x] Done"
        val lines = note.content.lines()
        val checklistRegex = Regex("""^[-*]\s*\[([ xX])] (.*)$""")
        lines.forEach { line ->
            val match = checklistRegex.find(line.trim())
            if (match != null) {
                val isDone = match.groupValues[1].equals("x", ignoreCase = true)
                val taskText = match.groupValues[2].trim()
                if (taskText.isNotBlank()) {
                    // Save as synced task
                    saveTask(
                        TaskEntity(
                            id = "note_task_${note.id}_${taskText.hashCode()}",
                            noteId = note.id,
                            title = taskText,
                            isCompleted = isDone
                        )
                    )
                }
            }
        }
    }

    // Diagrams & Logic Board
    suspend fun saveDiagram(diagram: DiagramEntity) {
        diagramDao.insertOrUpdateDiagram(diagram.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteDiagram(diagram: DiagramEntity) {
        diagramDao.deleteDiagram(diagram)
    }

    fun parseNodesJson(json: String): List<DiagramNode> {
        return try {
            nodeListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseEdgesJson(json: String): List<DiagramEdge> {
        return try {
            edgeListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeNodes(nodes: List<DiagramNode>): String {
        return nodeListAdapter.toJson(nodes)
    }

    fun serializeEdges(edges: List<DiagramEdge>): String {
        return edgeListAdapter.toJson(edges)
    }

    // Full Text Offline Search
    suspend fun searchAll(
        query: String,
        selectedTag: String? = null,
        selectedFolderId: String? = null
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        val results = mutableListOf<SearchResult>()

        val allNotesList = noteDao.getAllActiveNotesList()
        val allDiagramsList = diagramDao.getAllDiagramsList()

        for (note in allNotesList) {
            if (selectedFolderId != null && note.folderId != selectedFolderId) continue
            if (selectedTag != null && !note.tags.split(",").map { it.trim() }.contains(selectedTag)) continue

            val matchesQuery = q.isEmpty() ||
                    note.title.lowercase().contains(q) ||
                    note.content.lowercase().contains(q) ||
                    note.tags.lowercase().contains(q)

            if (matchesQuery) {
                results.add(SearchResult.NoteMatch(note))
            }
        }

        for (diagram in allDiagramsList) {
            if (q.isEmpty()) {
                results.add(SearchResult.DiagramMatch(diagram))
                continue
            }
            val nodes = parseNodesJson(diagram.nodesJson)
            val matchesNodeLabel = nodes.any { it.label.lowercase().contains(q) || it.subText.lowercase().contains(q) }
            val matchesTitle = diagram.title.lowercase().contains(q) || diagram.description.lowercase().contains(q)

            if (matchesTitle || matchesNodeLabel) {
                results.add(SearchResult.DiagramMatch(diagram))
            }
        }

        results
    }

    // Whole App Backup & Restore
    suspend fun createWholeAppBackupJson(): String = withContext(Dispatchers.IO) {
        val backupObj = JSONObject()
        backupObj.put("app", "NoteVault")
        backupObj.put("version", 1)
        backupObj.put("timestamp", System.currentTimeMillis())

        val notesArr = JSONArray()
        for (n in noteDao.getAllActiveNotesList()) {
            notesArr.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("folderId", n.folderId ?: "")
                put("tags", n.tags)
                put("isPinned", n.isPinned)
                put("isArchived", n.isArchived)
                put("isLocked", n.isLocked)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
            })
        }
        backupObj.put("notes", notesArr)

        val foldersArr = JSONArray()
        for (f in folderDao.getAllFoldersList()) {
            foldersArr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("colorHex", f.colorHex)
            })
        }
        backupObj.put("folders", foldersArr)

        val diagramsArr = JSONArray()
        for (d in diagramDao.getAllDiagramsList()) {
            diagramsArr.put(JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("description", d.description)
                put("templateType", d.templateType)
                put("noteId", d.noteId ?: "")
                put("nodesJson", d.nodesJson)
                put("edgesJson", d.edgesJson)
                put("createdAt", d.createdAt)
                put("updatedAt", d.updatedAt)
            })
        }
        backupObj.put("diagrams", diagramsArr)

        val tasksArr = JSONArray()
        for (t in taskDao.getAllTasksList()) {
            tasksArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("isCompleted", t.isCompleted)
                put("dueDate", t.dueDate ?: 0L)
                put("priority", t.priority)
            })
        }
        backupObj.put("tasks", tasksArr)

        backupObj.toString(2)
    }

    suspend fun restoreFromBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupObj = JSONObject(jsonString)

            val notesArr = backupObj.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until notesArr.length()) {
                val n = notesArr.getJSONObject(i)
                val note = NoteEntity(
                    id = n.optString("id", UUID.randomUUID().toString()),
                    title = n.optString("title", "Untitled"),
                    content = n.optString("content", ""),
                    folderId = n.optString("folderId").ifEmpty { null },
                    tags = n.optString("tags", ""),
                    isPinned = n.optBoolean("isPinned", false),
                    isArchived = n.optBoolean("isArchived", false),
                    isLocked = n.optBoolean("isLocked", false),
                    createdAt = n.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = n.optLong("updatedAt", System.currentTimeMillis())
                )
                noteDao.insertOrUpdateNote(note)
            }

            val foldersArr = backupObj.optJSONArray("folders") ?: JSONArray()
            for (i in 0 until foldersArr.length()) {
                val f = foldersArr.getJSONObject(i)
                folderDao.insertOrUpdateFolder(
                    FolderEntity(
                        id = f.optString("id", UUID.randomUUID().toString()),
                        name = f.optString("name", "Folder"),
                        colorHex = f.optString("colorHex", "#6366F1")
                    )
                )
            }

            val diagramsArr = backupObj.optJSONArray("diagrams") ?: JSONArray()
            for (i in 0 until diagramsArr.length()) {
                val d = diagramsArr.getJSONObject(i)
                diagramDao.insertOrUpdateDiagram(
                    DiagramEntity(
                        id = d.optString("id", UUID.randomUUID().toString()),
                        title = d.optString("title", "Diagram"),
                        description = d.optString("description", ""),
                        templateType = d.optString("templateType", "FREEFORM"),
                        noteId = d.optString("noteId").ifEmpty { null },
                        nodesJson = d.optString("nodesJson", "[]"),
                        edgesJson = d.optString("edgesJson", "[]"),
                        createdAt = d.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = d.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val tasksArr = backupObj.optJSONArray("tasks") ?: JSONArray()
            for (i in 0 until tasksArr.length()) {
                val t = tasksArr.getJSONObject(i)
                taskDao.insertOrUpdateTask(
                    TaskEntity(
                        id = t.optString("id", UUID.randomUUID().toString()),
                        title = t.optString("title", "Task"),
                        isCompleted = t.optBoolean("isCompleted", false),
                        dueDate = t.optLong("dueDate").let { if (it == 0L) null else it },
                        priority = t.optString("priority", "MEDIUM")
                    )
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        if (folderDao.getAllFoldersList().isEmpty()) {
            val f1 = FolderEntity(id = "folder_architecture", name = "Architecture & Logic", colorHex = "#6366F1")
            val f2 = FolderEntity(id = "folder_roadmaps", name = "Learning Roadmaps", colorHex = "#EC4899")
            folderDao.insertOrUpdateFolder(f1)
            folderDao.insertOrUpdateFolder(f2)

            val starterNote = NoteEntity(
                id = "note_welcome",
                title = "🚀 Welcome to NoteVault",
                content = """
                    # Welcome to NoteVault

                    NoteVault combines rich local **Markdown Notes** with an infinite visual **Logic Board** diagram canvas.

                    ## Key Features:
                    - 📝 **Markdown Editor**: Headings, lists, code blocks, tables, and live preview mode.
                    - 🧠 **Logic Board Canvas**: Build decision trees, system flowcharts, and learning roadmaps offline.
                    - ✅ **Task Aggregation**: Checklist items automatically appear in your Tasks screen!
                    - 🔒 **Vault Security**: Lock individual notes with a secure PIN.
                    - 🌐 **100% Offline-First**: Works seamlessly without internet, with background sync readiness.

                    ### Quick Checklist:
                    - [ ] Explore the Logic Board canvas tab
                    - [ ] Create a decision node flowchart
                    - [ ] Set a PIN lock on a sensitive note
                    - [ ] Export a full backup snapshot
                """.trimIndent(),
                folderId = f1.id,
                tags = "guide,welcome,overview",
                isPinned = true
            )
            saveNote(starterNote)

            // Seed starter diagram (Decision Tree)
            val starterDiagram = createStarterTemplateDiagram("DECISION_TREE", starterNote.id)
            saveDiagram(starterDiagram)
        }
    }

    fun createStarterTemplateDiagram(templateType: String, noteId: String? = null): DiagramEntity {
        val title = when (templateType) {
            "DECISION_TREE" -> "Model Router Decision Tree"
            "ROADMAP" -> "Android Developer 2026 Roadmap"
            "FLOWCHART" -> "Offline Sync Engine Architecture"
            "MIND_MAP" -> "NoteVault Core Modules"
            else -> "New Logic Board Canvas"
        }

        val description = when (templateType) {
            "DECISION_TREE" -> "How to route incoming user prompts to Flash vs Pro models"
            "ROADMAP" -> "Step by step mastery path for modern Jetpack Compose"
            "FLOWCHART" -> "Local Room DB -> Background Sync -> Cloud Conflict Resolution"
            "MIND_MAP" -> "Structured brain dump of application capabilities"
            else -> "Visual canvas for structured thinking"
        }

        val nodes = mutableListOf<DiagramNode>()
        val edges = mutableListOf<DiagramEdge>()

        when (templateType) {
            "DECISION_TREE" -> {
                val n1 = DiagramNode(id = "dt_1", type = "CIRCLE", label = "User Request", subText = "Incoming Prompt", x = 100f, y = 200f, colorHex = "#10B981")
                val n2 = DiagramNode(id = "dt_2", type = "DIAMOND", label = "Contains Code / STEM?", subText = "Reasoning Level Required", x = 340f, y = 185f, colorHex = "#F59E0B")
                val n3 = DiagramNode(id = "dt_3", type = "ROUNDED_CARD", label = "gemini-3.5-flash", subText = "Fast, lightweight response", x = 600f, y = 100f, colorHex = "#3B82F6")
                val n4 = DiagramNode(id = "dt_4", type = "ROUNDED_CARD", label = "gemini-3.1-pro-preview", subText = "Deep reasoning & analysis", x = 600f, y = 280f, colorHex = "#8B5CF6")
                val n5 = DiagramNode(id = "dt_5", type = "OUTCOME", label = "Stream to Compose UI", subText = "Cache in Room DB", x = 860f, y = 190f, colorHex = "#84CC16")

                nodes.addAll(listOf(n1, n2, n3, n4, n5))
                edges.add(DiagramEdge(fromNodeId = n1.id, toNodeId = n2.id, label = "Pass prompt", lineStyle = "ORTHOGONAL"))
                edges.add(DiagramEdge(fromNodeId = n2.id, toNodeId = n3.id, label = "Simple / General", lineStyle = "ORTHOGONAL", colorHex = "#3B82F6"))
                edges.add(DiagramEdge(fromNodeId = n2.id, toNodeId = n4.id, label = "Complex / Coding", lineStyle = "ORTHOGONAL", colorHex = "#8B5CF6"))
                edges.add(DiagramEdge(fromNodeId = n3.id, toNodeId = n5.id, label = "Tokens returned", lineStyle = "ORTHOGONAL"))
                edges.add(DiagramEdge(fromNodeId = n4.id, toNodeId = n5.id, label = "Tokens returned", lineStyle = "ORTHOGONAL"))
            }
            "ROADMAP" -> {
                val r1 = DiagramNode(id = "rm_1", type = "ROUNDED_CARD", label = "Phase 1: Kotlin & Coroutines", subText = "Flow, StateFlow, CoroutineScope", x = 100f, y = 150f, colorHex = "#6366F1")
                val r2 = DiagramNode(id = "rm_2", type = "ROUNDED_CARD", label = "Phase 2: Jetpack Compose", subText = "Material 3, Canvas, Touch Gestures", x = 340f, y = 150f, colorHex = "#06B6D4")
                val r3 = DiagramNode(id = "rm_3", type = "ROUNDED_CARD", label = "Phase 3: Room Database", subText = "KSP, Entities, DAOs, Migrations", x = 580f, y = 150f, colorHex = "#10B981")
                val r4 = DiagramNode(id = "rm_4", type = "ROUNDED_CARD", label = "Phase 4: Gemini REST API", subText = "Multimodal AI & Logic Generation", x = 820f, y = 150f, colorHex = "#EC4899")

                nodes.addAll(listOf(r1, r2, r3, r4))
                edges.add(DiagramEdge(fromNodeId = r1.id, toNodeId = r2.id, label = "Next", lineStyle = "STRAIGHT"))
                edges.add(DiagramEdge(fromNodeId = r2.id, toNodeId = r3.id, label = "Next", lineStyle = "STRAIGHT"))
                edges.add(DiagramEdge(fromNodeId = r3.id, toNodeId = r4.id, label = "Next", lineStyle = "STRAIGHT"))
            }
            "FLOWCHART" -> {
                val f1 = DiagramNode(id = "fc_1", type = "CIRCLE", label = "User Action", subText = "Edit Note / Node", x = 100f, y = 200f, colorHex = "#10B981")
                val f2 = DiagramNode(id = "fc_2", type = "RECTANGLE", label = "Room Repository", subText = "Write locally first (Single Truth)", x = 320f, y = 200f, colorHex = "#3B82F6")
                val f3 = DiagramNode(id = "fc_3", type = "DIAMOND", label = "Internet Available?", subText = "Network check", x = 540f, y = 185f, colorHex = "#F59E0B")
                val f4 = DiagramNode(id = "fc_4", type = "STICKY", label = "Queue Change", subText = "Pending Sync Queue", x = 540f, y = 360f, colorHex = "#FACC15")
                val f5 = DiagramNode(id = "fc_5", type = "ACTION", label = "Sync to Server", subText = "Last-Write-Wins timestamps", x = 760f, y = 200f, colorHex = "#8B5CF6")

                nodes.addAll(listOf(f1, f2, f3, f4, f5))
                edges.add(DiagramEdge(fromNodeId = f1.id, toNodeId = f2.id, label = "Save", lineStyle = "ORTHOGONAL"))
                edges.add(DiagramEdge(fromNodeId = f2.id, toNodeId = f3.id, label = "Trigger Sync", lineStyle = "ORTHOGONAL"))
                edges.add(DiagramEdge(fromNodeId = f3.id, toNodeId = f5.id, label = "Yes (Online)", lineStyle = "ORTHOGONAL", colorHex = "#10B981"))
                edges.add(DiagramEdge(fromNodeId = f3.id, toNodeId = f4.id, label = "No (Offline)", lineStyle = "ORTHOGONAL", colorHex = "#F59E0B"))
                edges.add(DiagramEdge(fromNodeId = f4.id, toNodeId = f5.id, label = "When Reconnected", lineStyle = "CURVED", colorHex = "#6366F1"))
            }
            else -> { // MIND_MAP
                val m0 = DiagramNode(id = "mm_0", type = "ROUNDED_CARD", label = "NoteVault Core", subText = "Local-First Workspace", x = 400f, y = 250f, colorHex = "#6366F1", width = 180f, height = 100f)
                val m1 = DiagramNode(id = "mm_1", type = "STICKY", label = "Markdown Editor", subText = "Live Preview & WYSIWYG", x = 150f, y = 100f, colorHex = "#3B82F6")
                val m2 = DiagramNode(id = "mm_2", type = "STICKY", label = "Logic Board", subText = "Infinite Canvas & Shapes", x = 650f, y = 100f, colorHex = "#EC4899")
                val m3 = DiagramNode(id = "mm_3", type = "STICKY", label = "Tasks & Reminders", subText = "Synced Checklists", x = 150f, y = 400f, colorHex = "#10B981")
                val m4 = DiagramNode(id = "mm_4", type = "STICKY", label = "Vault Security", subText = "PIN Encryption Lock", x = 650f, y = 400f, colorHex = "#F59E0B")

                nodes.addAll(listOf(m0, m1, m2, m3, m4))
                edges.add(DiagramEdge(fromNodeId = m0.id, toNodeId = m1.id, lineStyle = "CURVED", colorHex = "#3B82F6"))
                edges.add(DiagramEdge(fromNodeId = m0.id, toNodeId = m2.id, lineStyle = "CURVED", colorHex = "#EC4899"))
                edges.add(DiagramEdge(fromNodeId = m0.id, toNodeId = m3.id, lineStyle = "CURVED", colorHex = "#10B981"))
                edges.add(DiagramEdge(fromNodeId = m0.id, toNodeId = m4.id, lineStyle = "CURVED", colorHex = "#F59E0B"))
            }
        }

        return DiagramEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            templateType = templateType,
            noteId = noteId,
            nodesJson = serializeNodes(nodes),
            edgesJson = serializeEdges(edges)
        )
    }

    // --- Reminders & Tag Rules Repository Actions ---
    suspend fun saveReminder(reminder: ReminderEntity) {
        reminderDao.insertOrUpdateReminder(reminder)
        ReminderManager.scheduleReminder(context, reminder)

        // Keep Note or Task in sync
        if (reminder.targetType == "NOTE" && reminder.targetId != null) {
            val note = noteDao.getNoteById(reminder.targetId)
            if (note != null) {
                noteDao.insertOrUpdateNote(note.copy(reminderTime = reminder.triggerTime, updatedAt = System.currentTimeMillis()))
            }
        } else if (reminder.targetType == "TASK" && reminder.targetId != null) {
            val task = taskDao.getAllTasksList().find { it.id == reminder.targetId }
            if (task != null) {
                taskDao.insertOrUpdateTask(task.copy(dueDate = reminder.triggerTime))
            }
        }
    }

    suspend fun snoozeReminder(reminder: ReminderEntity, minutes: Int = 15) {
        val snoozedTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val updated = reminder.copy(
            status = "SNOOZED",
            triggerTime = snoozedTime
        )
        reminderDao.insertOrUpdateReminder(updated)
        ReminderManager.scheduleReminder(context, updated)
    }

    suspend fun rescheduleReminder(reminder: ReminderEntity, newTriggerTime: Long) {
        val updated = reminder.copy(
            status = "UPCOMING",
            triggerTime = newTriggerTime,
            firedAt = null
        )
        reminderDao.insertOrUpdateReminder(updated)
        ReminderManager.scheduleReminder(context, updated)
    }

    suspend fun completeReminder(reminder: ReminderEntity) {
        val updated = reminder.copy(status = "COMPLETED")
        reminderDao.insertOrUpdateReminder(updated)
        ReminderManager.cancelReminder(context, reminder.id)
    }

    suspend fun cancelReminder(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
        ReminderManager.cancelReminder(context, reminder.id)
    }

    suspend fun clearPastRemindersHistory() {
        reminderDao.clearPastHistory()
    }

    // Tag Rules & Bulk Tag Actions
    suspend fun saveTagRule(rule: TagReminderRuleEntity) {
        tagRuleDao.insertOrUpdateRule(rule)
    }

    suspend fun deleteTagRule(rule: TagReminderRuleEntity) {
        tagRuleDao.deleteRule(rule)
    }

    suspend fun evaluateTagRulesForNote(note: NoteEntity) {
        if (note.tags.isBlank()) return
        val activeRules = tagRuleDao.getActiveRulesList()
        if (activeRules.isEmpty()) return

        val noteTagList = note.tags.split(",").map { it.trim().lowercase() }
        for (rule in activeRules) {
            val cleanRuleTag = rule.tagName.trim().removePrefix("#").lowercase()
            val matches = noteTagList.any { it.removePrefix("#") == cleanRuleTag }
            if (matches) {
                if (rule.ruleType == "AFTER_CREATION") {
                    val triggerTime = System.currentTimeMillis() + (rule.offsetDays * 86400 * 1000L)
                    val reminder = ReminderEntity(
                        targetType = "NOTE",
                        targetId = note.id,
                        title = "Tag Rule (#${rule.tagName}): ${note.title}",
                        customNote = rule.customNote.ifBlank { "Auto-scheduled via tag rule #${rule.tagName}" },
                        triggerTime = triggerTime,
                        linkedTags = note.tags
                    )
                    saveReminder(reminder)
                }
            }
        }
    }

    suspend fun scheduleBulkTagReminders(tagName: String, triggerTime: Long, customNote: String) {
        val cleanTag = tagName.trim().removePrefix("#").lowercase()
        val matchingNotes = noteDao.getAllActiveNotesList().filter { note ->
            note.tags.split(",").map { it.trim().removePrefix("#").lowercase() }.contains(cleanTag)
        }
        val matchingTasks = taskDao.getAllTasksList().filter { task ->
            task.title.lowercase().contains("#$cleanTag") || task.title.lowercase().contains(cleanTag)
        }

        for (note in matchingNotes) {
            val reminder = ReminderEntity(
                targetType = "NOTE",
                targetId = note.id,
                title = note.title,
                customNote = customNote.ifBlank { "Bulk tag reminder for #${cleanTag}" },
                triggerTime = triggerTime,
                linkedTags = "#$cleanTag"
            )
            saveReminder(reminder)
        }

        for (task in matchingTasks) {
            val reminder = ReminderEntity(
                targetType = "TASK",
                targetId = task.id,
                title = task.title,
                customNote = customNote.ifBlank { "Bulk tag task reminder for #${cleanTag}" },
                triggerTime = triggerTime,
                linkedTags = "#$cleanTag"
            )
            saveReminder(reminder)
        }
    }
}

sealed class SearchResult {
    data class NoteMatch(val note: NoteEntity) : SearchResult()
    data class DiagramMatch(val diagram: DiagramEntity) : SearchResult()
}
