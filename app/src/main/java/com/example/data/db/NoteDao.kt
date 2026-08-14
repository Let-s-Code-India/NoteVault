package com.example.data.db

import androidx.room.*
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY trashedTimestamp DESC")
    fun getTrashedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNotePermanently(note: NoteEntity)

    @Query("DELETE FROM notes WHERE isTrashed = 1 AND trashedTimestamp < :cutoffTime")
    suspend fun purgeOldTrashedNotes(cutoffTime: Long)

    @Query("SELECT * FROM notes WHERE isTrashed = 0")
    suspend fun getAllActiveNotesList(): List<NoteEntity>
}
