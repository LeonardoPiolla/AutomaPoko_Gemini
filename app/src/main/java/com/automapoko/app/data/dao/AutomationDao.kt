package com.automapoko.app.data.dao

import androidx.room.*
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.model.TriggerType
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun getAllAutomations(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE isEnabled = 1 AND triggerType = :triggerType")
    suspend fun getActiveAutomationsByTrigger(triggerType: TriggerType): List<AutomationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: AutomationEntity): Long

    @Delete
    suspend fun deleteAutomation(automation: AutomationEntity)

    @Query("UPDATE automations SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateStatus(id: Long, isEnabled: Boolean)

    @Query("UPDATE automations SET lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun updateLastTriggered(id: Long, timestamp: Long)
}
