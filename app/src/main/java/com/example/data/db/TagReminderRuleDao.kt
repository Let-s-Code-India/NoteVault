package com.example.data.db

import androidx.room.*
import com.example.data.model.TagReminderRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagReminderRuleDao {
    @Query("SELECT * FROM tag_reminder_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<TagReminderRuleEntity>>

    @Query("SELECT * FROM tag_reminder_rules WHERE isActive = 1")
    suspend fun getActiveRulesList(): List<TagReminderRuleEntity>

    @Query("SELECT * FROM tag_reminder_rules WHERE tagName = :tagName AND isActive = 1")
    suspend fun getActiveRulesForTag(tagName: String): List<TagReminderRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: TagReminderRuleEntity)

    @Delete
    suspend fun deleteRule(rule: TagReminderRuleEntity)

    @Query("DELETE FROM tag_reminder_rules WHERE id = :id")
    suspend fun deleteRuleById(id: String)
}
