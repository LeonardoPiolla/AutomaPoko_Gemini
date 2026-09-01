package com.automapoko.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.model.TriggerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    automations: List<AutomationEntity>,
    onToggleStatus: (Long, Boolean) -> Unit,
    onDelete: (AutomationEntity) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
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
                Icon(Icons.Default.Add, contentDescription = "Nova")
            }
        }
    ) { padding ->
        if (automations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Nenhuma automação criada ainda.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(automations, key = { it.id }) { a ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(a.name, style = MaterialTheme.typography.titleMedium)
                                Text("Ação: Abrir ${a.targetAppName}", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = a.isEnabled, onCheckedChange = { onToggleStatus(a.id, it) })
                            IconButton(onClick = { onDelete(a) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
