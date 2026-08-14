package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String? = null,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val createdAt: Long = System.currentTimeMillis()
)
