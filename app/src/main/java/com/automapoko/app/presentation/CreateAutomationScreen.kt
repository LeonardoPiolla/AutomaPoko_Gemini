package com.automapoko.app.presentation

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.model.GeofenceTransitionType
import com.automapoko.app.data.model.TriggerConfig
import com.automapoko.app.data.model.TriggerType
import com.automapoko.app.data.repository.AppRepository
import com.automapoko.app.data.repository.InstalledApp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAutomationScreen(
    onNavigateBack: () -> Unit,
    onSave: (AutomationEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedTrigger by remember { mutableStateOf(TriggerType.BLUETOOTH) }
    var bluetoothDeviceName by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var cooldownMinutes by remember { mutableStateOf("2") }

    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var showAppSelectionDialog by remember { mutableStateOf(false) }

    val pairedDevices = remember {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        try {
            bm?.adapter?.bondedDevices?.mapNotNull { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    LaunchedEffect(Unit) {
        val repo = AppRepository(context)
        installedApps = repo.getInstalledApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Automação") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da automação") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Gatilho:", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTrigger == TriggerType.BLUETOOTH,
                    onClick = { selectedTrigger = TriggerType.BLUETOOTH },
                    label = { Text("Bluetooth") }
                )
                FilterChip(
                    selected = selectedTrigger == TriggerType.WIFI,
                    onClick = { selectedTrigger = TriggerType.WIFI },
                    label = { Text("Wi-Fi") }
                )
            }

            if (selectedTrigger == TriggerType.BLUETOOTH) {
                Text("Dispositivo Pareado:", style = MaterialTheme.typography.bodyMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                    items(pairedDevices) { dev ->
                        ListItem(
                            headlineContent = { Text(dev) },
                            trailingContent = {
                                RadioButton(
                                    selected = bluetoothDeviceName == dev,
                                    onClick = { bluetoothDeviceName = dev }
                                )
                            },
                            modifier = Modifier.clickable { bluetoothDeviceName = dev }
                        )
                    }
                }
            } else if (selectedTrigger == TriggerType.WIFI) {
                OutlinedTextField(
                    value = wifiSsid,
                    onValueChange = { wifiSsid = it },
                    label = { Text("Nome da Rede Wi-Fi (SSID)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = { showAppSelectionDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedApp?.let { "App: ${it.name}" } ?: "Selecionar Aplicativo")
            }

            OutlinedTextField(
                value = cooldownMinutes,
                onValueChange = { cooldownMinutes = it },
                label = { Text("Cooldown (minutos)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val app = selectedApp ?: return@Button
                    val configJson = when (selectedTrigger) {
                        TriggerType.BLUETOOTH -> Json.encodeToString(TriggerConfig.Bluetooth(bluetoothDeviceName))
                        TriggerType.WIFI -> Json.encodeToString(TriggerConfig.Wifi(wifiSsid))
                        TriggerType.LOCATION -> Json.encodeToString(
                            TriggerConfig.Location(-23.5505, -46.6333, 100f, GeofenceTransitionType.ENTER)
                        )
                    }

                    val entity = AutomationEntity(
                        name = name.ifEmpty { "Automação sem nome" },
                        triggerType = selectedTrigger,
                        triggerConfigJson = configJson,
                        targetPackageName = app.packageName,
                        targetAppName = app.name,
                        cooldownMinutes = cooldownMinutes.toIntOrNull() ?: 2
                    )
                    onSave(entity)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && selectedApp != null
            ) {
                Text("Salvar Automação")
            }
        }
    }

    if (showAppSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showAppSelectionDialog = false },
            title = { Text("Selecione um App") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(installedApps) { app ->
                        ListItem(
                            headlineContent = { Text(app.name) },
                            modifier = Modifier.clickable {
                                selectedApp = app
                                showAppSelectionDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}
