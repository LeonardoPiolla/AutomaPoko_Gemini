package com.automapoko.app.engine.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.automapoko.app.data.database.AutomapokoDatabase
import com.automapoko.app.data.model.GeofenceTransitionType
import com.automapoko.app.data.model.TriggerConfig
import com.automapoko.app.data.model.TriggerType
import com.automapoko.app.engine.ActionExecutor
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transType = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransitionType.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransitionType.EXIT
            Geofence.GEOFENCE_TRANSITION_DWELL -> GeofenceTransitionType.DWELL
            else -> return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AutomapokoDatabase.getInstance(context)
            val automations = db.automationDao().getActiveAutomationsByTrigger(TriggerType.LOCATION)
            for (a in automations) {
                try {
                    val config = Json.decodeFromString<TriggerConfig.Location>(a.triggerConfigJson)
                    if (config.transitionType == transType) {
                        ActionExecutor.execute(context, a, "Localização: ${transType.name}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
