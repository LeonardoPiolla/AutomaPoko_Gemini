package com.automapoko.app.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.automapoko.app.data.database.AutomapokoDatabase
import com.automapoko.app.presentation.theme.AutomapokoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        val db = AutomapokoDatabase.getInstance(this)
        setContent {
            AutomapokoTheme {
                val nav = rememberNavController()
                val automations by db.automationDao().getAllAutomations().collectAsState(initial = emptyList())
                val logs by db.executionLogDao().getAllLogs().collectAsState(initial = emptyList())

                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            automations = automations,
                            onToggleStatus = { id, enabled -> lifecycleScope.launch(Dispatchers.IO) { db.automationDao().updateStatus(id, enabled) } },
                            onDelete = { entity -> lifecycleScope.launch(Dispatchers.IO) { db.automationDao().deleteAutomation(entity) } },
                            onNavigateToCreate = { nav.navigate("create") },
                            onNavigateToLogs = { nav.navigate("logs") }
                        )
                    }
                    composable("create") {
                        CreateAutomationScreen(
                            onNavigateBack = { nav.popBackStack() },
                            onSave = { entity ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.automationDao().insertAutomation(entity)
                                    launch(Dispatchers.Main) { nav.popBackStack() }
                                }
                            }
                        )
                    }
                    composable("logs") {
                        LogsScreen(logs, onClearLogs = { lifecycleScope.launch(Dispatchers.IO) { db.executionLogDao().clearAllLogs() } }, onNavigateBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
