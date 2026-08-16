package com.example.data.db

import androidx.room.*
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDate ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE noteId = :noteId")
    suspend fun deleteTasksByNoteId(noteId: String)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<TaskEntity>
}
