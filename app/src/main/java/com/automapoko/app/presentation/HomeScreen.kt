package com.automapoko.app.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.model.TriggerType

import android.media.AudioManager
import kotlinx.serialization.json.Json
import com.automapoko.app.data.model.ActionConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    automations: List<AutomationEntity>,
    onToggleStatus: (Long, Boolean) -> Unit,
    onDelete: (AutomationEntity) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToLogs: () -> Unit
) {
    var automationToDelete by remember { mutableStateOf<AutomationEntity?>(null) }
    
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var isBatteryOptimized by remember {
        mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    var userDismissedBanner by remember { mutableStateOf(false) }

    val shouldShowBanner = isBatteryOptimized && !userDismissedBanner

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AutomaPoko") },
                actions = {
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(Icons.Default.History, contentDescription = "Histórico")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Default.Add, contentDescription = "Nova Automação")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            // Banner Inteligente e Neutro
            if (shouldShowBanner) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = "Aviso", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Execução em Segundo Plano", style = MaterialTheme.typography.titleMedium)
                                }
                                IconButton(
                                    onClick = { userDismissedBanner = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar aviso", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Para garantir que os gatilhos funcionem quando o app estiver fechado, remova as restrições de bateria nas configurações.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Abrir Configurações")
                            }
                        }
                    }
                }
            }

            // Estado vazio
            if (automations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma automação cadastrada.\nToque no '+' para criar uma!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Lista de automações
                items(automations, key = { it.id }) { automation ->
                    AutomationCard(
                        automation = automation,
                        onToggle = { isChecked -> onToggleStatus(automation.id, isChecked) },
                        onClick = { onNavigateToEdit(automation.id) },
                        onDeleteClick = { automationToDelete = automation }
                    )
                }
            }
        }
    }

    if (automationToDelete != null) {
        val target = automationToDelete!!
        AlertDialog(
            onDismissRequest = { automationToDelete = null },
            title = { Text("Excluir Automação") },
            text = { Text("Tem certeza que deseja excluir a automação \"${target.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target)
                        automationToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { automationToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun AutomationCard(
    automation: AutomationEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val icon = when (automation.triggerType) {
        TriggerType.BLUETOOTH -> Icons.Default.Bluetooth
        TriggerType.WIFI -> Icons.Default.Wifi
        TriggerType.LOCATION -> Icons.Default.LocationOn
    }

    val actions = try { 
        Json.decodeFromString<List<ActionConfig>>(automation.actionsJson) 
    } catch(e: Exception) { emptyList() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = automation.name, style = MaterialTheme.typography.titleMedium)
                }
                Switch(checked = automation.isEnabled, onCheckedChange = onToggle)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Ações:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            if (actions.isEmpty()) {
                Text(text = "Nenhuma ação definida", style = MaterialTheme.typography.bodyMedium)
            } else {
                actions.forEach { action ->
                    val actionText = when (action) {
                        is ActionConfig.OpenApp -> "📱 Abrir ${action.appName}"
                        is ActionConfig.SetVolume -> {
                            val streamName = when(action.streamType) {
                                AudioManager.STREAM_MUSIC -> "Mídia"
                                AudioManager.STREAM_RING -> "Toque"
                                AudioManager.STREAM_NOTIFICATION -> "Notificações"
                                AudioManager.STREAM_ALARM -> "Alarme"
                                AudioManager.STREAM_VOICE_CALL -> "Chamada"
                                else -> "Sistema"
                            }
                            "🔊 Volume ($streamName): ${action.volumePercentage}%"
                        }
                    }
                    Text(text = actionText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
