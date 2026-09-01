package com.automapoko.app.engine

import android.content.Context
import android.content.Intent
import com.automapoko.app.data.database.AutomapokoDatabase
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.entity.ExecutionLogEntity
import com.automapoko.app.data.model.ExecutionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ActionExecutor {
    fun execute(context: Context, automation: AutomationEntity, triggerInfo: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AutomapokoDatabase.getInstance(context)
            val now = System.currentTimeMillis()
            val cooldownMs = automation.cooldownMinutes * 60 * 1000L
            if (automation.lastTriggeredAt != null && (now - automation.lastTriggeredAt) < cooldownMs) {
                db.executionLogDao().insertLog(
                    ExecutionLogEntity(
                        automationId = automation.id,
                        automationName = automation.name,
                        status = ExecutionStatus.SKIPPED_COOLDOWN,
                        details = "Bloqueado pelo cooldown ($triggerInfo)"
                    )
                )
                return@launch
            }
            db.executionLogDao().deleteLogsOlderThan(now - (7 * 24 * 60 * 60 * 1000L))
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(automation.targetPackageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(launchIntent)
                    db.automationDao().updateLastTriggered(automation.id, now)
                    db.executionLogDao().insertLog(
                        ExecutionLogEntity(
                            automationId = automation.id,
                            automationName = automation.name,
                            status = ExecutionStatus.SUCCESS,
                            details = "Executado via $triggerInfo"
                        )
                    )
                }
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
