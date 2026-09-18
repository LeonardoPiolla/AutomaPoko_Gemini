package com.automapoko.app.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.automapoko.app.data.database.AutomapokoDatabase
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.model.GeofenceTransitionType
import com.automapoko.app.data.model.TriggerConfig
import com.automapoko.app.data.model.TriggerType
import com.automapoko.app.data.repository.AppRepository
import com.automapoko.app.data.repository.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAutomationScreen(
    automationId: Long? = null,
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
    var searchQuery by remember { mutableStateOf("") }

    var pairedDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasBluetoothPermission by remember { mutableStateOf(false) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
        if (isGranted) {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            pairedDevices = bm?.adapter?.bondedDevices?.mapNotNull { it.name } ?: emptyList()
        }
    }

    LaunchedEffect(automationId) {
        if (automationId != null && automationId > 0) {
            val db = AutomapokoDatabase.getInstance(context)
            val existing = db.automationDao().getAutomationById(automationId)
            if (existing != null) {
                name = existing.name
                selectedTrigger = existing.triggerType
                cooldownMinutes = existing.cooldownMinutes.toString()
                selectedApp = InstalledApp(name = existing.targetAppName, packageName = existing.targetPackageName)

                try {
                    when (existing.triggerType) {
                        TriggerType.BLUETOOTH -> {
                            val config = Json.decodeFromString<TriggerConfig.Bluetooth>(existing.triggerConfigJson)
                            bluetoothDeviceName = config.deviceName
                        }
                        TriggerType.WIFI -> {
                            val config = Json.decodeFromString<TriggerConfig.Wifi>(existing.triggerConfigJson)
                            wifiSsid = config.ssid
                        }
                        else -> {}
                    }
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        val repo = AppRepository(context)
        installedApps = repo.getInstalledApps()
        val permissionToCheck = Manifest.permission.BLUETOOTH_CONNECT
        
        if (ContextCompat.checkSelfPermission(context, permissionToCheck) == PackageManager.PERMISSION_GRANTED) {
            hasBluetoothPermission = true
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            pairedDevices = bm?.adapter?.bondedDevices?.mapNotNull { it.name } ?: emptyList()
        } else {
            bluetoothPermissionLauncher.launch(permissionToCheck)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (automationId == null || automationId == 0L) "Nova Automação" else "Editar Automação") },
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
                FilterChip(selected = selectedTrigger == TriggerType.BLUETOOTH, onClick = { selectedTrigger = TriggerType.BLUETOOTH }, label = { Text("Bluetooth") })
                FilterChip(selected = selectedTrigger == TriggerType.WIFI, onClick = { selectedTrigger = TriggerType.WIFI }, label = { Text("Wi-Fi") })
            }

            if (selectedTrigger == TriggerType.BLUETOOTH) {
                if (!hasBluetoothPermission) {
                    Text("Permissão de Bluetooth necessária.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                        items(pairedDevices) { dev ->
                            ListItem(
                                headlineContent = { Text(dev) },
                                trailingContent = { RadioButton(selected = bluetoothDeviceName == dev, onClick = { bluetoothDeviceName = dev }) },
                                modifier = Modifier.clickable { bluetoothDeviceName = dev }
                            )
                        }
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

            OutlinedButton(onClick = { showAppSelectionDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedApp?.let { "App Selecionado: ${it.name}" } ?: "Selecionar Aplicativo para Abrir")
            }

            OutlinedTextField(
                value = cooldownMinutes,
                onValueChange = { cooldownMinutes = it },
                label = { Text("Cooldown (minutos)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        selectedApp?.let { app ->
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent?.let { context.startActivity(it) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedApp != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Testar Ação")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Testar Ação")
                }

                Button(
                    onClick = {
                        val app = selectedApp ?: return@Button
                        val configJson = when (selectedTrigger) {
                            TriggerType.BLUETOOTH -> Json.encodeToString(TriggerConfig.Bluetooth(bluetoothDeviceName))
                            TriggerType.WIFI -> Json.encodeToString(TriggerConfig.Wifi(wifiSsid))
                            TriggerType.LOCATION -> Json.encodeToString(TriggerConfig.Location(-23.55, -46.63, 100f, GeofenceTransitionType.ENTER))
                        }

                        val entity = AutomationEntity(
                            id = automationId ?: 0L,
                            name = name.ifEmpty { "Automação sem nome" },
                            triggerType = selectedTrigger,
                            triggerConfigJson = configJson,
                            targetPackageName = app.packageName,
                            targetAppName = app.name,
                            cooldownMinutes = cooldownMinutes.toIntOrNull() ?: 2
                        )
                        onSave(entity)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && selectedApp != null
                ) {
                    Text("Salvar")
                }
            }
        }
    }

    if (showAppSelectionDialog) {
        val filteredApps = installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { showAppSelectionDialog = false },
            title = { Text("Selecione um App") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar app...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredApps) { app ->
                            ListItem(
                                headlineContent = { Text(app.name) },
                                modifier = Modifier.clickable {
                                    selectedApp = app
                                    showAppSelectionDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
