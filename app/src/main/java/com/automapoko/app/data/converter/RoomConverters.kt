package com.automapoko.app.data.converter

import androidx.room.TypeConverter
import com.automapoko.app.data.model.ExecutionStatus
import com.automapoko.app.data.model.TriggerType

class RoomConverters {
    @TypeConverter fun fromTriggerType(value: TriggerType): String = value.name
    @TypeConverter fun toTriggerType(value: String): TriggerType = TriggerType.valueOf(value)
    @TypeConverter fun fromExecutionStatus(value: ExecutionStatus): String = value.name
    @TypeConverter fun toExecutionStatus(value: String): ExecutionStatus = ExecutionStatus.valueOf(value)
}
