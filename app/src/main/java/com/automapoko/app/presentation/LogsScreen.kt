package com.automapoko.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.automapoko.app.data.entity.ExecutionLogEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    logs: List<ExecutionLogEntity>,
    onClearLogs: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
                actions = { if (logs.isNotEmpty()) IconButton(onClick = onClearLogs) { Icon(Icons.Default.Delete, "Limpar") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs, key = { it.id }) { log ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(log.automationName, style = MaterialTheme.typography.titleMedium)
                        Text(fmt.format(Date(log.triggeredAt)), style = MaterialTheme.typography.bodySmall)
                        log.details?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}
