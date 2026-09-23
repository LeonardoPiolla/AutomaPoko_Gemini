package com.automapoko.app.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.automapoko.app.data.database.AutomapokoDatabase
import com.automapoko.app.data.entity.AutomationEntity
import com.automapoko.app.data.model.ActionConfig
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
    automationId: Long? = null,
    onNavigateBack: () -> Unit,
    onSave: (AutomationEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedTrigger by remember { mutableStateOf(TriggerType.BLUETOOTH) }
    var bluetoothDeviceName by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }

    // Lista de ações múltiplas
    var actions by remember { mutableStateOf<List<ActionConfig>>(emptyList()) }

    var showAddActionMenu by remember { mutableStateOf(false) }
    
    // Estados dos modais de Ações
    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    var showVolumeDialog by remember { mutableStateOf(false) }
    var volumeStreamType by remember { mutableStateOf(AudioManager.STREAM_MUSIC) }
    var volumePercentage by remember { mutableStateOf(50f) }

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
                
                try {
                    actions = Json.decodeFromString(existing.actionsJson)
                } catch (e: Exception) { actions = emptyList() }

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

            // TRIGGER
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

            // AÇÕES
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ações (${actions.size}):", style = MaterialTheme.typography.titleSmall)
                Box {
                    TextButton(onClick = { showAddActionMenu = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar Ação")
                    }
                    DropdownMenu(expanded = showAddActionMenu, onDismissRequest = { showAddActionMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Abrir Aplicativo") },
                            onClick = {
                                showAddActionMenu = false
                                showAppSelectionDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Ajustar Volume") },
                            onClick = {
                                showAddActionMenu = false
                                showVolumeDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.VolumeUp, contentDescription = null) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(actions) { action ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                when (action) {
                                    is ActionConfig.OpenApp -> {
                                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Abrir: ${action.appName}")
                                    }
                                    is ActionConfig.SetVolume -> {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val nome = when(action.streamType) {
                                            AudioManager.STREAM_MUSIC -> "Mídia"
                                            AudioManager.STREAM_RING -> "Toque"
                                            AudioManager.STREAM_ALARM -> "Alarme"
                                            else -> "Volume"
                                        }
                                        Text("Ajustar $nome: ${action.volumePercentage}%")
                                    }
                                }
                            }
                            IconButton(onClick = { actions = actions.filter { it != action } }) {
                                Icon(Icons.Default.Close, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        // Lógica de Teste de Múltiplas Ações
                        actions.forEach { action ->
                            when (action) {
                                is ActionConfig.OpenApp -> {
                                    val intent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    intent?.let { context.startActivity(it) }
                                }
                                is ActionConfig.SetVolume -> {
                                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                    val maxVolume = audioManager.getStreamMaxVolume(action.streamType)
                                    val target = (maxVolume * (action.volumePercentage / 100f)).toInt()
                                    audioManager.setStreamVolume(action.streamType, target, AudioManager.FLAG_SHOW_UI)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = actions.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Testar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Testar")
                }

                Button(
                    onClick = {
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
                            actionsJson = Json.encodeToString(actions)
                        )
                        onSave(entity)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && actions.isNotEmpty()
                ) {
                    Text("Salvar")
                }
            }
        }
    }

    // Modal: Escolher App (Com ícones via Coil)
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
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredApps) { app ->
                            ListItem(
                                leadingContent = {
                                    // Coil carregando o ícone perfeitamente otimizado
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(context.packageManager.getApplicationIcon(app.packageName))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Ícone do app",
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                headlineContent = { Text(app.name) },
                                modifier = Modifier.clickable {
                                    actions = actions + ActionConfig.OpenApp(app.packageName, app.name)
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

    // Modal: Ajustar Volume
    if (showVolumeDialog) {
        AlertDialog(
            onDismissRequest = { showVolumeDialog = false },
            title = { Text("Ajustar Volume") },
            text = {
                Column {
                    Text("Qual volume alterar?")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = volumeStreamType == AudioManager.STREAM_MUSIC, onClick = { volumeStreamType = AudioManager.STREAM_MUSIC })
                                Text("Mídia")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = volumeStreamType == AudioManager.STREAM_RING, onClick = { volumeStreamType = AudioManager.STREAM_RING })
                                Text("Toque")
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = volumeStreamType == AudioManager.STREAM_ALARM, onClick = { volumeStreamType = AudioManager.STREAM_ALARM })
                                Text("Alarme")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = volumeStreamType == AudioManager.STREAM_NOTIFICATION, onClick = { volumeStreamType = AudioManager.STREAM_NOTIFICATION })
                                Text("Notificações")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nível desejado: ${volumePercentage.toInt()}%")
                    Slider(
                        value = volumePercentage,
                        onValueChange = { volumePercentage = it },
                        valueRange = 0f..100f,
                        steps = 100
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    actions = actions + ActionConfig.SetVolume(volumeStreamType, volumePercentage.toInt())
                    showVolumeDialog = false
                }) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { showVolumeDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
