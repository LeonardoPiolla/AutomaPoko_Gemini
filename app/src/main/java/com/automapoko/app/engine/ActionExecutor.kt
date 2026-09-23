package com.automapoko.app.engine

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.automapoko.app.data.database.AutomapokoDatabase
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.entity.ExecutionLogEntity
import com.automapoko.app.data.model.ActionConfig
import com.automapoko.app.data.model.ExecutionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object ActionExecutor {
    fun execute(context: Context, automation: AutomationEntity, triggerInfo: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AutomapokoDatabase.getInstance(context)
            val now = System.currentTimeMillis()
            
            db.executionLogDao().deleteLogsOlderThan(now - (7 * 24 * 60 * 60 * 1000L))
            
            try {
                val actions = Json.decodeFromString<List<ActionConfig>>(automation.actionsJson)
                
                actions.forEach { action ->
                    when (action) {
                        is ActionConfig.OpenApp -> {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                context.startActivity(launchIntent)
                            }
                        }
                        is ActionConfig.SetVolume -> {
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            val maxVolume = audioManager.getStreamMaxVolume(action.streamType)
                            val target = (maxVolume * (action.volumePercentage / 100f)).toInt()
                            audioManager.setStreamVolume(action.streamType, target, AudioManager.FLAG_SHOW_UI)
                        }
                    }
                }

                db.executionLogDao().insertLog(
                    ExecutionLogEntity(
                        automationId = automation.id,
                        automationName = automation.name,
                        status = ExecutionStatus.SUCCESS,
                        details = "Executado via $triggerInfo (${actions.size} ações)"
                    )
                )
            } catch (e: Exception) {
                db.executionLogDao().insertLog(
                    ExecutionLogEntity(
                        automationId = automation.id,
                        automationName = automation.name,
                        status = ExecutionStatus.FAILED,
                        details = "Erro: ${e.localizedMessage}"
                    )
                )
            }
        }
    }
}
