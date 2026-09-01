package com.automapoko.app.data.dao

import androidx.room.*
import com.automapoko.app.data.entity.ExecutionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionLogDao {
    @Query("SELECT * FROM execution_logs ORDER BY triggeredAt DESC")
    fun getAllLogs(): Flow<List<ExecutionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLogEntity): Long

    @Query("DELETE FROM execution_logs WHERE triggeredAt < :cutoffTimestamp")
    suspend fun deleteLogsOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM execution_logs")
    suspend fun clearAllLogs()
}
