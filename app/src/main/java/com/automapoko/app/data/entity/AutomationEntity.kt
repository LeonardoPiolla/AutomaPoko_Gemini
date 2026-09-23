package com.automapoko.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.automapoko.app.data.model.TriggerType

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val triggerType: TriggerType,
    val triggerConfigJson: String,
    val actionsJson: String // Agora armazena uma lista de ações em vez de apenas 1 app
)
