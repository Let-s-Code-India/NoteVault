package com.example.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderEntity
import com.example.reminder.NaturalLanguageReminderParser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSchedulingSheet(
    initialTitle: String = "",
    initialCustomNote: String = "",
    targetType: String = "STANDALONE",
    targetId: String? = null,
    linkedTags: String = "",
    onReminderSaved: (ReminderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle.ifBlank { "Reminder" }) }
    var customNote by remember { mutableStateOf(initialCustomNote) }
    var nlpInput by remember { mutableStateOf("") }

    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var selectedTimeMillis by remember { mutableLongStateOf(calendar.timeInMillis) }
    var repeatType by remember { mutableStateOf("NONE") } // "NONE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM_DAYS"
    var repeatInterval by remember { mutableIntStateOf(1) }
    var selectedWeeklyDays by remember { mutableStateOf(setOf(1)) } // 1=Mon..7=Sun
    var endType by remember { mutableStateOf("NEVER") } // "NEVER", "AFTER_COUNT", "ON_DATE"
    var maxOccurrences by remember { mutableIntStateOf(5) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }

    var isSilent by remember { mutableStateOf(false) }
    var vibrationPattern by remember { mutableStateOf("DEFAULT") }
    var snoozeMinutes by remember { mutableIntStateOf(15) }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Live NLP Parsing preview
    val nlpResult = remember(nlpInput) {
        if (nlpInput.isNotBlank()) {
            NaturalLanguageReminderParser.parse(nlpInput)
        } else null
    }

    fun applyNlpResult() {
        nlpResult?.let { res ->
            if (res.cleanTitle.isNotBlank()) title = res.cleanTitle
            selectedTimeMillis = res.triggerTime
            repeatType = res.repeatType
            repeatInterval = res.repeatInterval
            if (res.weeklyDays.isNotBlank()) {
                selectedWeeklyDays = res.weeklyDays.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Schedule Local Reminder",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Natural Language Quick Add Box
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("On-Device Smart Quick Add", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = nlpInput,
                        onValueChange = { nlpInput = it },
                        placeholder = { Text("e.g. 'Pay rent tomorrow at 9am #bills' or 'Gym every Mon at 8pm'", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    if (nlpResult != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Detected: ${nlpResult.cleanTitle}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "Time: ${dateFormatter.format(Date(nlpResult.triggerTime))} at ${timeFormatter.format(Date(nlpResult.triggerTime))}" +
                                                if (nlpResult.repeatType != "NONE") " (${nlpResult.repeatType})" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { applyNlpResult() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Apply", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Title & Custom Note
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = customNote,
                onValueChange = { customNote = it },
                label = { Text("Custom Note / Label (Optional)") },
                placeholder = { Text("e.g. 'Call client before this' or 'Bring documents'") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // Quick Presets
            Text("Quick Presets", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "In 1 hr" to {
                        val c = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                        selectedTimeMillis = c.timeInMillis
                    },
                    "Later Today (6 PM)" to {
                        val c = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                        }
                        selectedTimeMillis = c.timeInMillis
                    },
                    "Tomorrow 9 AM" to {
                        val c = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                        }
                        selectedTimeMillis = c.timeInMillis
                    },
                    "Weekend 9 AM" to {
                        val c = Calendar.getInstance().apply {
                            val diff = (Calendar.SATURDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
                            add(Calendar.DAY_OF_YEAR, if (diff == 0) 7 else diff)
                            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                        }
                        selectedTimeMillis = c.timeInMillis
                    }
                ).forEach { (label, action) ->
                    AssistChip(
                        onClick = action,
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            // Custom Date & Time Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val updated = Calendar.getInstance().apply {
                                    timeInMillis = selectedTimeMillis
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                }
                                selectedTimeMillis = updated.timeInMillis
                            },
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormatter.format(Date(selectedTimeMillis)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                OutlinedCard(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val updated = Calendar.getInstance().apply {
                                    timeInMillis = selectedTimeMillis
                                    set(Calendar.HOUR_OF_DAY, hour)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                }
                                selectedTimeMillis = updated.timeInMillis
                            },
                            c.get(Calendar.HOUR_OF_DAY),
                            c.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(timeFormatter.format(Date(selectedTimeMillis)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Recurrence Configuration
            Divider()
            Text("Recurrence / Repeat Rule", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("NONE" to "None", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "CUSTOM_DAYS" to "Custom").forEach { (typeKey, typeLabel) ->
                    FilterChip(
                        selected = repeatType == typeKey,
                        onClick = { repeatType = typeKey },
                        label = { Text(typeLabel, fontSize = 12.sp) }
                    )
                }
            }

            if (repeatType == "WEEKLY") {
                Text("Repeat on days:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S").forEach { (dayIdx, label) ->
                        val isSel = selectedWeeklyDays.contains(dayIdx)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    selectedWeeklyDays = if (isSel) selectedWeeklyDays - dayIdx else selectedWeeklyDays + dayIdx
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (repeatType == "CUSTOM_DAYS") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Repeat every")
                    OutlinedTextField(
                        value = repeatInterval.toString(),
                        onValueChange = { repeatInterval = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                        modifier = Modifier.width(70.dp),
                        singleLine = true
                    )
                    Text("days")
                }
            }

            if (repeatType != "NONE") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ends:", style = MaterialTheme.typography.labelMedium)
                    FilterChip(selected = endType == "NEVER", onClick = { endType = "NEVER" }, label = { Text("Never") })
                    FilterChip(selected = endType == "AFTER_COUNT", onClick = { endType = "AFTER_COUNT" }, label = { Text("After $maxOccurrences times") })
                }
            }

            // Sound, Vibration, Snooze Settings
            Divider()
            Text("Notification Polish", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Silent Mode (No Sound)")
                Switch(checked = isSilent, onCheckedChange = { isSilent = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration Pattern")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("DEFAULT" to "Std", "HEARTBEAT" to "Pulse", "SHORT" to "Short", "NONE" to "Off").forEach { (patternKey, patternLabel) ->
                        FilterChip(
                            selected = vibrationPattern == patternKey,
                            onClick = { vibrationPattern = patternKey },
                            label = { Text(patternLabel, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Default Snooze Duration")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(5, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = snoozeMinutes == mins,
                            onClick = { snoozeMinutes = mins },
                            label = { Text("${mins}m", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val reminder = ReminderEntity(
                            targetType = targetType,
                            targetId = targetId,
                            title = title.ifBlank { "Reminder" },
                            customNote = customNote,
                            triggerTime = selectedTimeMillis,
                            repeatType = repeatType,
                            repeatInterval = repeatInterval,
                            weeklyDays = selectedWeeklyDays.sorted().joinToString(","),
                            endType = endType,
                            maxOccurrences = maxOccurrences,
                            isSilent = isSilent,
                            vibrationPattern = vibrationPattern,
                            snoozeMinutes = snoozeMinutes,
                            linkedTags = linkedTags
                        )
                        onReminderSaved(reminder)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set Reminder")
                }
            }
        }
    }
}
