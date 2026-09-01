package com.automapoko.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.automapoko.app.data.converter.RoomConverters
import com.automapoko.app.data.dao.AutomationDao
import com.automapoko.app.data.dao.ExecutionLogDao
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.entity.ExecutionLogEntity

@Database(entities = [AutomationEntity::class, ExecutionLogEntity::class], version = 1, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class AutomapokoDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao
    abstract fun executionLogDao(): ExecutionLogDao

    companion object {
        @Volatile private var INSTANCE: AutomapokoDatabase? = null
        fun getInstance(context: Context): AutomapokoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutomapokoDatabase::class.java,
                    "automapoko_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
