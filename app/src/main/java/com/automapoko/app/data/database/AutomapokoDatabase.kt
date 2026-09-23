package com.automapoko.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.entity.ExecutionLogEntity
// Se o seu ExecutionLogDao estiver na pasta 'dao', descomente a linha abaixo:
// import com.automapoko.app.data.dao.ExecutionLogDao 

@Database(
    entities = [AutomationEntity::class, ExecutionLogEntity::class], // Tabela de logs adicionada
    version = 2, 
    exportSchema = false
)
abstract class AutomapokoDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao
    abstract fun executionLogDao(): ExecutionLogDao // Conexão com os logs restaurada

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
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}
