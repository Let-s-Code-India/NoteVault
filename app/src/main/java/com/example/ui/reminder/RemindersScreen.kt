package com.example.ui.reminder

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderEntity
import com.example.data.model.TagReminderRuleEntity
import com.example.platform.PermissionType
import com.example.reminder.ReminderManager
import com.example.ui.components.NoteVaultTopBar
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.viewmodel.NoteVaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: NoteVaultViewModel) {
    val context = LocalContext.current
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()
    val pastReminders by viewModel.pastReminders.collectAsState()
    val tagRules by viewModel.tagReminderRules.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Upcoming, 1=Past/History, 2=Calendar, 3=Tag Rules
    var showSchedulingSheet by remember { mutableStateOf(false) }
    var showTagRuleDialog by remember { mutableStateOf(false) }
    var showBulkTagSheet by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(viewModel.permissionManager.isPermissionGranted(PermissionType.NOTIFICATIONS))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            showSchedulingSheet = true
        }
    }

    val canScheduleExact = remember { ReminderManager.canScheduleExactAlarms(context) }
    val timeFormatter = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    fun checkAndOpenSchedulingSheet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.permissionManager.isPermissionGranted(PermissionType.NOTIFICATIONS)) {
            showPermissionRationale = true
        } else {
            showSchedulingSheet = true
        }
    }

    Scaffold(
        topBar = {
            NoteVaultTopBar(
                title = "Local Reminders",
                syncStatus = "${upcomingReminders.size} Active • 100% Local"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { checkAndOpenSchedulingSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddAlarm, contentDescription = "New Reminder")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notifications runtime permission banner (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Column {
                                Text("Enable Notifications", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Allow NoteVault to notify you when scheduled reminders are due.", fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Enable", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Exact Alarm Reliability Banner
            if (!canScheduleExact) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Exact Alarms Permission Required for reliable delivery on Android 12+", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        ) {
                            Text("Grant", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab Bar
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Upcoming (${upcomingReminders.size})", fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Past History", fontSize = 12.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Week View", fontSize = 12.sp) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Tag Rules", fontSize = 12.sp) })
            }

            when (selectedTab) {
                0 -> UpcomingRemindersList(
                    reminders = upcomingReminders,
                    onSnooze = { rem, mins -> viewModel.snoozeReminder(rem, mins) },
                    onComplete = { rem -> viewModel.completeReminder(rem) },
                    onDelete = { rem -> viewModel.cancelReminder(rem) },
                    timeFormatter = timeFormatter
                )
                1 -> PastRemindersList(
                    reminders = pastReminders,
                    onClearHistory = { viewModel.clearPastRemindersHistory() },
                    onDelete = { rem -> viewModel.cancelReminder(rem) },
                    timeFormatter = timeFormatter
                )
                2 -> CalendarWeekView(
                    reminders = upcomingReminders,
                    onSnooze = { rem, mins -> viewModel.snoozeReminder(rem, mins) },
                    onComplete = { rem -> viewModel.completeReminder(rem) },
                    timeFormatter = timeFormatter
                )
                3 -> TagRulesView(
                    rules = tagRules,
                    onAddRule = { showTagRuleDialog = true },
                    onDeleteRule = { rule -> viewModel.deleteTagRule(rule) },
                    onBulkTagReminder = { showBulkTagSheet = true }
                )
            }
        }
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
            onReminderSaved = { reminder ->
                viewModel.saveReminder(reminder)
                showSchedulingSheet = false
            },
            onDismiss = { showSchedulingSheet = false }
        )
    }

    if (showTagRuleDialog) {
        CreateTagRuleDialog(
            onRuleCreated = { rule ->
                viewModel.saveTagRule(rule)
                showTagRuleDialog = false
            },
            onDismiss = { showTagRuleDialog = false }
        )
    }

    if (showBulkTagSheet) {
        BulkTagReminderSheet(
            onBulkApplied = { tag, timeMillis ->
                viewModel.scheduleBulkTagReminders(tag, timeMillis, "Bulk reminder for #$tag")
                showBulkTagSheet = false
            },
            onDismiss = { showBulkTagSheet = false }
        )
    }
}

@Composable
fun UpcomingRemindersList(
    reminders: List<ReminderEntity>,
    onSnooze: (ReminderEntity, Int) -> Unit,
    onComplete: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    timeFormatter: SimpleDateFormat
) {
    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.AlarmOn, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text("No active reminders", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                Text("Tap the '+' button to schedule an on-device reminder for notes, tasks, or tags.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderItemCard(
                    reminder = reminder,
                    isUpcoming = true,
                    onSnooze = { mins -> onSnooze(reminder, mins) },
                    onComplete = { onComplete(reminder) },
                    onDelete = { onDelete(reminder) },
                    timeFormatter = timeFormatter
                )
            }
        }
    }
}

@Composable
fun PastRemindersList(
    reminders: List<ReminderEntity>,
    onClearHistory: () -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    timeFormatter: SimpleDateFormat
) {
    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text("No reminder history", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                Text("Completed and past due alarms will be logged here for your records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${reminders.size} Past Logs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    TextButton(onClick = onClearHistory) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All History", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
            items(reminders, key = { it.id }) { reminder ->
                ReminderItemCard(
                    reminder = reminder,
                    isUpcoming = false,
                    onSnooze = {},
                    onComplete = {},
                    onDelete = { onDelete(reminder) },
                    timeFormatter = timeFormatter
                )
            }
        }
    }
}

@Composable
fun CalendarWeekView(
    reminders: List<ReminderEntity>,
    onSnooze: (ReminderEntity, Int) -> Unit,
    onComplete: (ReminderEntity) -> Unit,
    timeFormatter: SimpleDateFormat
) {
    val calendar = Calendar.getInstance()
    var selectedDayOffset by remember { mutableIntStateOf(0) } // 0=Today, 1=Tomorrow, etc.

    val daysList = remember {
        (0..6).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dayName = if (offset == 0) "Today" else SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
            Triple(offset, dayName, dayNum)
        }
    }

    val targetDayStart = remember(selectedDayOffset) {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, selectedDayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val targetDayEnd = targetDayStart + (24 * 60 * 60 * 1000)

    val dayReminders = reminders.filter { it.triggerTime in targetDayStart..targetDayEnd }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week days selector row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysList) { (offset, dayName, dayNum) ->
                val isSelected = selectedDayOffset == offset
                Surface(
                    onClick = { selectedDayOffset = offset },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.width(62.dp).height(68.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            dayNum,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        if (dayReminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No reminders scheduled for this day", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dayReminders, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        isUpcoming = true,
                        onSnooze = { mins -> onSnooze(reminder, mins) },
                        onComplete = { onComplete(reminder) },
                        onDelete = {},
                        timeFormatter = timeFormatter
                    )
                }
            }
        }
    }
}

@Composable
fun TagRulesView(
    rules: List<TagReminderRuleEntity>,
    onAddRule: () -> Unit,
    onDeleteRule: (TagReminderRuleEntity) -> Unit,
    onBulkTagReminder: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tag-Based Auto Scheduling", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Define automatic reminder offsets for notes or tasks tagged with specific labels (e.g. automatically set a reminder 2 hours after creating an '#urgent' item).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAddRule, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Tag Rule", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = onBulkTagReminder, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.BatchPrediction, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bulk Schedule Tag", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (rules.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No tag rules configured yet.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text("#${rule.tagName}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                            }
                            Column {
                                Text("Auto Offset: ${rule.offsetDays} day(s) @ ${rule.timeOfDayHour}:00", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text(if (rule.isActive) "Active auto-scheduler" else "Paused", style = MaterialTheme.typography.labelSmall, color = if (rule.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            }
                        }
                        IconButton(onClick = { onDeleteRule(rule) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItemCard(
    reminder: ReminderEntity,
    isUpcoming: Boolean,
    onSnooze: (Int) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    timeFormatter: SimpleDateFormat
) {
    var showSnoozeMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = when (reminder.targetType) {
                            "TASK" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            "TAG" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (reminder.targetType) {
                                    "TASK" -> Icons.Default.CheckCircle
                                    "TAG" -> Icons.Default.Tag
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = when (reminder.targetType) {
                                    "TASK" -> MaterialTheme.colorScheme.secondary
                                    "TAG" -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                    Column {
                        Text(
                            reminder.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            timeFormatter.format(Date(reminder.triggerTime)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reminder.triggerTime < System.currentTimeMillis() && isUpcoming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUpcoming) {
                        IconButton(onClick = onComplete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Done, contentDescription = "Mark Complete", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Box {
                            IconButton(onClick = { showSnoozeMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Snooze, contentDescription = "Snooze", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(expanded = showSnoozeMenu, onDismissRequest = { showSnoozeMenu = false }) {
                                DropdownMenuItem(text = { Text("+ 5 mins") }, onClick = { onSnooze(5); showSnoozeMenu = false })
                                DropdownMenuItem(text = { Text("+ 15 mins") }, onClick = { onSnooze(15); showSnoozeMenu = false })
                                DropdownMenuItem(text = { Text("+ 1 hour") }, onClick = { onSnooze(60); showSnoozeMenu = false })
                                DropdownMenuItem(text = { Text("+ 1 day") }, onClick = { onSnooze(1440); showSnoozeMenu = false })
                            }
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (reminder.customNote.isNotBlank()) {
                Text(
                    reminder.customNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (reminder.repeatType != "NONE") {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Repeats: ${reminder.repeatType} (every ${reminder.repeatInterval})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTagRuleDialog(
    onRuleCreated: (TagReminderRuleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var tag by remember { mutableStateOf("") }
    var offsetDays by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Tag-Based Rule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it.removePrefix("#") },
                    label = { Text("Tag Name (e.g. urgent, followup)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Schedule offset after creation:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(0, 1, 2, 3, 7).forEach { days ->
                        FilterChip(
                            selected = offsetDays == days,
                            onClick = { offsetDays = days },
                            label = { Text(if (days == 0) "Same Day" else "${days}d") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tag.isNotBlank()) {
                        onRuleCreated(
                            TagReminderRuleEntity(
                                tagName = tag.trim().lowercase(),
                                offsetDays = offsetDays,
                                timeOfDayHour = 9,
                                timeOfDayMinute = 0
                            )
                        )
                    }
                },
                enabled = tag.isNotBlank()
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BulkTagReminderSheet(
    onBulkApplied: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var tag by remember { mutableStateOf("") }
    var selectedOffsetHours by remember { mutableIntStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Schedule for Tag", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Schedule an upcoming reminder for every note and task currently carrying this tag.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it.removePrefix("#") },
                    label = { Text("Target Tag Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Remind in:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 6, 24, 48).forEach { hours ->
                        FilterChip(
                            selected = selectedOffsetHours == hours,
                            onClick = { selectedOffsetHours = hours },
                            label = { Text("${hours}h") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tag.isNotBlank()) {
                        val trigger = System.currentTimeMillis() + (selectedOffsetHours * 60 * 60 * 1000L)
                        onBulkApplied(tag.trim().lowercase(), trigger)
                    }
                },
                enabled = tag.isNotBlank()
            ) {
                Text("Apply Bulk Reminders")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
