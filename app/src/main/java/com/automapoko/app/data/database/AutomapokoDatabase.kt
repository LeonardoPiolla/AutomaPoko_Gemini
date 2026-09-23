package com.automapoko.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.automapoko.app.data.entity.AutomationEntity

@Database(entities = [AutomationEntity::class], version = 2, exportSchema = false)
abstract class AutomapokoDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao

    companion object {
        @Volatile
        private var INSTANCE: AutomapokoDatabase? = null

        fun getInstance(context: Context): AutomapokoDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AutomapokoDatabase::class.java,
                    "automapoko.db"
                )
                .fallbackToDestructiveMigration() // Recria o banco para as novas regras
                .build().also { INSTANCE = it }
            }
        }
    }
}
