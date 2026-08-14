package com.example.data.db

import androidx.room.*
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'UPCOMING' ORDER BY triggerTime ASC")
    fun getUpcomingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status != 'UPCOMING' ORDER BY firedAt DESC, triggerTime DESC")
    fun getPastReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'UPCOMING'")
    suspend fun getUpcomingRemindersList(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE targetId = :targetId")
    fun getRemindersForTarget(targetId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE targetId = :targetId")
    suspend fun getRemindersForTargetList(targetId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("DELETE FROM reminders WHERE targetId = :targetId")
    suspend fun deleteRemindersForTarget(targetId: String)

    @Query("DELETE FROM reminders WHERE status != 'UPCOMING'")
    suspend fun clearPastHistory()
}
