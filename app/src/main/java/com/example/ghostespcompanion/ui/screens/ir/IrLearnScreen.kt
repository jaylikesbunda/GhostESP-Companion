package com.example.ghostespcompanion.ui.screens.ir

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ghostespcompanion.data.serial.SerialManager
import com.example.ghostespcompanion.domain.model.GhostResponse
import com.example.ghostespcompanion.ui.screens.MainScreen
import com.example.ghostespcompanion.ui.components.*
import com.example.ghostespcompanion.ui.theme.*
import com.example.ghostespcompanion.ui.viewmodel.MainViewModel

/**
 * IR Learn Screen - Learn and save IR signals
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrLearnScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var isLearning by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var signalName by remember { mutableStateOf("") }
    var learnedSignals by remember { mutableStateOf<List<LearnedIrSignal>>(emptyList()) }
    var showOverlay by remember { mutableStateOf(true) }
    
    val connectionState by viewModel.connectionState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    
    val hasIrRx = deviceInfo?.hasFeature(GhostResponse.DeviceFeature.INFRARED_RX) ?: true
    val hasDeviceInfo = deviceInfo != null
    
    // Observe parsed IR learn responses from repository
    val irLearnedSignal by viewModel.irLearnedSignal.collectAsState()
    val irLearnSavedPath by viewModel.irLearnSavedPath.collectAsState()
    val irLearnStatus by viewModel.irLearnStatus.collectAsState()
    
    // Convert GhostResponse.IrLearned to LearnedIrSignal for UI
    val learnedSignal = irLearnedSignal?.let { ir ->
        LearnedIrSignal(
            id = System.currentTimeMillis().toString(),
            name = "",
            protocol = ir.protocol ?: "Unknown",
            address = ir.address ?: "N/A",
            command = ir.command ?: "N/A",
            rawData = ir.rawSamples?.let { listOf(it) } ?: emptyList()
        )
    }
    
    // Update learning state based on status
    LaunchedEffect(irLearnStatus) {
        when (irLearnStatus) {
            "STARTED", "WAITING" -> isLearning = true
            "TIMEOUT" -> isLearning = false
        }
    }
    
    // Auto-stop learning after 30 seconds
    LaunchedEffect(isLearning) {
        if (isLearning) {
            kotlinx.coroutines.delay(30000)
            if (isLearning) {
                viewModel.stopAll()
                isLearning = false
            }
        }
    }
    
    // Clear state when starting new learn
    fun startLearning() {
        viewModel.clearIrLearnState()
        viewModel.learnIr()
        isLearning = true
    }
    
    MainScreen(
        onBack = onBack,
        title = "Learn Signal",
        actions = {
            if (learnedSignal != null) {
                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add to List",
                        tint = primaryColor()
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection Status Banner
            IrConnectionBanner(
                isConnected = isConnected,
                deviceName = "GhostESP",
                onConnect = { viewModel.connectFirstAvailable() }
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Learning Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLearning) primaryColor().copy(alpha = 0.1f) else SurfaceVariantDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isLearning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(64.dp),
                                    color = primaryColor(),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Waiting for IR signal...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor()
                                )
                                Text(
                                    text = "Point your remote at the GhostESP IR receiver",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariantDark
                                )
                                Text(
                                    text = "Timeout in 30 seconds",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariantDark
                                )
                            } else if (learnedSignal != null) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = successColor(),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Signal Learned!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = successColor()
                                )
                                Text(
                                    text = "Protocol: ${learnedSignal?.protocol}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceDark
                                )
                            } else {
                                Icon(
                                    Icons.Default.SettingsRemote,
                                    contentDescription = null,
                                    tint = OnSurfaceVariantDark,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Ready to Learn",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceDark
                                )
                                Text(
                                    text = "Press the button below to start learning",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariantDark
                                )
                            }
                        }
                    }
                }
                
                // Learn Button
                item {
                    BrutalistButton(
                        text = if (isLearning) "Stop Learning" else "Start Learning",
                        onClick = {
                            if (isConnected) {
                                if (isLearning) {
                                    viewModel.stopAll()
                                    viewModel.clearIrLearnState()
                                    isLearning = false
                                } else {
                                    startLearning()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (isLearning) errorColor() else primaryColor(),
                        enabled = isConnected,
                        leadingIcon = {
                            Icon(
                                if (isLearning) Icons.Default.Stop else Icons.Default.SettingsRemote,
                                contentDescription = null
                            )
                        }
                    )
                }
                
                // Learned Signal Details
                if (learnedSignal != null) {
                    item {
                        BrutalistCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Signal Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor()
                                )

                                SignalDetailRow("Protocol", learnedSignal?.protocol ?: "Unknown")
                                SignalDetailRow("Address", learnedSignal?.address ?: "N/A")
                                SignalDetailRow("Command", learnedSignal?.command ?: "N/A")
                                if (learnedSignal?.rawData?.isNotEmpty() == true) {
                                    SignalDetailRow("Raw Samples", "${learnedSignal?.rawData?.firstOrNull() ?: 0}")
                                }
                                
                                // Show saved path if firmware saved the file
                                if (irLearnSavedPath != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = successColor(),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Saved to: $irLearnSavedPath",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = successColor()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Previously Learned Signals
                if (learnedSignals.isNotEmpty()) {
                    item {
                        Text(
                            text = "Learned Signals (${learnedSignals.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor()
                        )
                    }

                    items(learnedSignals, key = { it.id }) { signal ->
                        LearnedSignalCard(
                            signal = signal,
                            onReplay = {
                                if (isConnected) {
                                    viewModel.sendIr(signal.name, null)
                                }
                            },
                            onDelete = {
                                learnedSignals = learnedSignals.filter { it.id != signal.id }
                            }
                        )
                    }
                }
                
                // Status Message
                if (statusMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = SurfaceVariantDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = statusMessage!!,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceDark
                            )
                        }
                    }
                }
                
                // Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceVariantDark.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = OnSurfaceVariantDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Point your IR remote at the GhostESP IR receiver and press a button to capture the signal. The signal can then be saved or replayed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantDark
                            )
                        }
                    }
                }
            }
        }
        
        // Feature Not Supported Overlay
        if (hasDeviceInfo && !hasIrRx) {
            FeatureNotSupportedOverlay(
                show = showOverlay,
                onProceed = { showOverlay = false },
                featureName = "IR Learn",
                message = "This device does not have Infrared RX hardware. Learning IR signals requires an IR receiver."
            )
        }
        }
    }
    
    // Save Dialog (saves locally in app list)
    if (showSaveDialog && learnedSignal != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Add to Local List") },
            text = {
                Column {
                    Text("Enter a name for this signal:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = signalName,
                        onValueChange = { signalName = it },
                        label = { Text("Signal Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., TV Power") }
                    )
                    if (irLearnSavedPath != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Firmware saved to: $irLearnSavedPath",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantDark
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newSignal = learnedSignal!!.copy(
                            id = System.currentTimeMillis().toString(),
                            name = signalName.ifBlank { "Signal ${learnedSignals.size + 1}" }
                        )
                        learnedSignals = learnedSignals + newSignal
                        signalName = ""
                        showSaveDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSaveDialog = false
                    signalName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Learned IR signal data class
 */
data class LearnedIrSignal(
    val id: String = "",
    val name: String = "",
    val protocol: String,
    val address: String,
    val command: String,
    val rawData: List<Int> = emptyList()
)

@Composable
private fun SignalDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariantDark
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceDark
        )
    }
}

/**
 * Card displaying a learned signal
 */
@Composable
private fun LearnedSignalCard(
    signal: LearnedIrSignal,
    onReplay: () -> Unit,
    onDelete: () -> Unit
) {
    BrutalistCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SettingsRemote,
                    contentDescription = null,
                    tint = primaryColor(),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = signal.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = signal.protocol,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantDark
                    )
                }
            }
            
            Row {
                IconButton(onClick = onReplay) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Replay",
                        tint = successColor()
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = errorColor()
                    )
                }
            }
        }
    }
}

/**
 * IR Connection Banner component
 */
@Composable
private fun IrConnectionBanner(
    isConnected: Boolean,
    deviceName: String,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) successColor().copy(alpha = 0.1f) else errorColor().copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isConnected) Icons.Default.SettingsRemote else Icons.Default.SettingsRemote,
                    contentDescription = null,
                    tint = if (isConnected) successColor() else errorColor()
                )
                Column {
                    Text(
                        text = if (isConnected) "$deviceName Connected" else "Not Connected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnected) successColor() else errorColor()
                    )
                    if (isConnected) {
                        Text(
                            text = "Ready to learn IR signals",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantDark
                        )
                    }
                }
            }
            
            if (!isConnected) {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor(),
                        contentColor = OnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Connect", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}