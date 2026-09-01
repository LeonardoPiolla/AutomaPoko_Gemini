package com.automapoko.app.engine.receivers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.automapoko.app.data.database.AutomapokoDatabase
import com.automapoko.app.data.model.TriggerConfig
import com.automapoko.app.data.model.TriggerType
import com.automapoko.app.engine.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class BluetoothTriggerReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val name = device?.name ?: return
            CoroutineScope(Dispatchers.IO).launch {
                val db = AutomapokoDatabase.getInstance(context)
                val automations = db.automationDao().getActiveAutomationsByTrigger(TriggerType.BLUETOOTH)
                for (a in automations) {
                    try {
                        val config = Json.decodeFromString<TriggerConfig.Bluetooth>(a.triggerConfigJson)
                        if (config.deviceName.equals(name, ignoreCase = true)) {
                            ActionExecutor.execute(context, a, "Bluetooth: $name")
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
