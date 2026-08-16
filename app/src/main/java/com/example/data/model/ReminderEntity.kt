package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val targetType: String = "STANDALONE", // "NOTE", "TASK", "TAG_DIGEST", "STANDALONE"
    val targetId: String? = null, // noteId, taskId, or tagName
    val title: String, // Main reminder title
    val customNote: String = "", // Optional label/note attached to the reminder itself
    val triggerTime: Long, // Epoch time millis when alarm should trigger
    val repeatType: String = "NONE", // "NONE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM_DAYS"
    val repeatInterval: Int = 1, // e.g., 1 week, or 2 days
    val weeklyDays: String = "", // Comma-separated days of week e.g. "1,3,5" (1=Mon..7=Sun)
    val endType: String = "NEVER", // "NEVER", "AFTER_COUNT", "ON_DATE"
    val maxOccurrences: Int = 0,
    val occurrenceCount: Int = 0,
    val endDate: Long? = null,
    val soundUri: String? = null, // Ringtone/Alarm sound URI or null/DEFAULT, "SILENT"
    val vibrationPattern: String = "DEFAULT", // "DEFAULT", "HEARTBEAT", "SHORT", "NONE"
    val isSilent: Boolean = false,
    val snoozeMinutes: Int = 15,
    val status: String = "UPCOMING", // "UPCOMING", "FIRED", "COMPLETED", "SNOOZED", "DISMISSED"
    val firedAt: Long? = null,
    val linkedTags: String = "", // Comma-separated tags e.g. "#bills,#urgent"
    val createdAt: Long = System.currentTimeMillis()
)
