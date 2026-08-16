package com.example.ui.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.components.NoteVaultTopBar
import com.example.ui.reminder.ReminderSchedulingSheet
import com.example.ui.theme.PrimaryGradient
import com.example.ui.viewmodel.NoteVaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: NoteVaultViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showAddTaskModal by remember { mutableStateOf(false) }
    var selectedTaskForReminder by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, COMPLETED

    val completedCount = remember(tasks) { tasks.count { it.isCompleted } }
    val totalCount = remember(tasks) { tasks.size }
    val progress = remember(completedCount, totalCount) {
        if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "TaskProgress")

    val displayedTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> tasks.filter { !it.isCompleted }
            "COMPLETED" -> tasks.filter { it.isCompleted }
            else -> tasks
        }
    }

    Scaffold(
        topBar = {
            NoteVaultTopBar(
                title = "Tasks & Action Items",
                syncStatus = syncStatus
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTaskModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "New Task") },
                text = { Text("Add Task", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_task_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Progress Banner Card
            if (totalCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Task Completion",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "$completedCount of $totalCount completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Filter Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${tasks.size})") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" },
                    label = { Text("Pending (${tasks.count { !it.isCompleted }})") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedFilter == "COMPLETED",
                    onClick = { selectedFilter = "COMPLETED" },
                    label = { Text("Done (${tasks.count { it.isCompleted }})") },
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (displayedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.CheckCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            when (selectedFilter) {
                                "PENDING" -> "No pending tasks"
                                "COMPLETED" -> "No completed tasks yet"
                                else -> "No tasks created yet"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Checklist items created in markdown notes automatically appear here!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { showAddTaskModal = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Task")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedTasks, key = { it.id }) { task ->
                        TaskRowCard(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompleted(task) },
                            onSetReminder = { selectedTaskForReminder = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddTaskModal) {
        var taskTitle by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("MEDIUM") }

        AlertDialog(
            onDismissRequest = { showAddTaskModal = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AddTask, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("New Task", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task description") },
                        placeholder = { Text("e.g. Review system architecture") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Priority Level", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                            val isSelected = priority == p
                            FilterChip(
                                selected = isSelected,
                                onClick = { priority = p },
                                label = { Text(p) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (p) {
                                        "HIGH" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                        "MEDIUM" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    }
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addNewTask(taskTitle, priority = priority)
                            showAddTaskModal = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = taskTitle.isNotBlank()
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskModal = false }) { Text("Cancel") }
            }
        )
    }

    selectedTaskForReminder?.let { task ->
        ReminderSchedulingSheet(
            initialTitle = task.title,
            targetType = "TASK",
            targetId = task.id,
            onReminderSaved = { reminder ->
                viewModel.saveReminder(reminder)
                selectedTaskForReminder = null
            },
            onDismiss = { selectedTaskForReminder = null }
        )
    }
}

@Composable
fun TaskRowCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onSetReminder: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium
                    )
                )
                if (task.priority.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (task.priority) {
                            "HIGH" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            "MEDIUM" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else -> Color(0xFF10B981).copy(alpha = 0.12f)
                        }
                    ) {
                        Text(
                            text = "${task.priority} PRIORITY",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = when (task.priority) {
                                "HIGH" -> MaterialTheme.colorScheme.error
                                "MEDIUM" -> MaterialTheme.colorScheme.primary
                                else -> Color(0xFF10B981)
                            },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onSetReminder, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.AddAlarm, contentDescription = "Set Reminder", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
            }
        }
    }
}
