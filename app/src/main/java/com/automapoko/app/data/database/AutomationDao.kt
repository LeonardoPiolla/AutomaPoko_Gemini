package com.automapoko.app.data.database // Pacote corrigido para bater com a pasta e o Banco de Dados

import androidx.room.*
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.model.TriggerType
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {

    // Alterado para ordenar pelo ID, já que removemos o createdAt
    @Query("SELECT * FROM automations ORDER BY id DESC")
    fun getAllAutomations(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE isEnabled = 1")
    suspend fun getActiveAutomations(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE isEnabled = 1 AND triggerType = :triggerType")
    suspend fun getActiveAutomationsByTrigger(triggerType: TriggerType): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE id = :id LIMIT 1")
    suspend fun getAutomationById(id: Long): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: AutomationEntity): Long

    @Update
    suspend fun updateAutomation(automation: AutomationEntity)

    @Delete
    suspend fun deleteAutomation(automation: AutomationEntity)

    @Query("UPDATE automations SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateStatus(id: Long, isEnabled: Boolean)
}
