package com.automapoko.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.automapoko.app.data.model.ExecutionStatus

@Entity(
    tableName = "execution_logs",
    foreignKeys = [
        ForeignKey(
            entity = AutomationEntity::class,
            parentColumns = ["id"],
            childColumns = ["automationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["automationId"])]
)
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val automationId: Long,
    val automationName: String,
    val triggeredAt: Long = System.currentTimeMillis(),
    val status: ExecutionStatus,
    val details: String? = null
)
