package com.example.data.db

import androidx.room.*
import com.example.data.model.DiagramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagramDao {
    @Query("SELECT * FROM diagrams ORDER BY updatedAt DESC")
    fun getAllDiagrams(): Flow<List<DiagramEntity>>

    @Query("SELECT * FROM diagrams WHERE id = :id")
    suspend fun getDiagramById(id: String): DiagramEntity?

    @Query("SELECT * FROM diagrams WHERE noteId = :noteId")
    fun getDiagramsForNote(noteId: String): Flow<List<DiagramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDiagram(diagram: DiagramEntity)

    @Delete
    suspend fun deleteDiagram(diagram: DiagramEntity)

    @Query("SELECT * FROM diagrams")
    suspend fun getAllDiagramsList(): List<DiagramEntity>
}
