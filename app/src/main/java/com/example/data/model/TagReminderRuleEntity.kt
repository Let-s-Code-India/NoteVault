package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tag_reminder_rules")
data class TagReminderRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tagName: String, // e.g. "#bills" or "bills"
    val ruleType: String = "AFTER_CREATION", // "AFTER_CREATION", "DAILY_DIGEST"
    val offsetDays: Int = 0, // e.g. 3 days after creation
    val timeOfDayHour: Int = 8, // e.g. 8 AM
    val timeOfDayMinute: Int = 0,
    val customNote: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
