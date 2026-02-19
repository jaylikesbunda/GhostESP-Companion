package com.example.ghostespcompanion.ui.screens.more

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
 * BadUSB Screen - List and execute BadUSB scripts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadUsbScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var selectedScript by remember { mutableStateOf<BadUsbScript?>(null) }
    var keyboardLayout by remember { mutableStateOf("US") }
    var showOverlay by remember { mutableStateOf(true) }
    
    val connectionState by viewModel.connectionState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    
    val isBadUsbSupported = deviceInfo?.hasFeature(GhostResponse.DeviceFeature.BADUSB) ?: true
    val hasDeviceInfo = deviceInfo != null
    
    // Mock scripts data
    val scripts = remember {
        listOf(
            BadUsbScript("1", "Hello World", "Simple test script that types Hello World", 1024, false),
            BadUsbScript("2", "Windows Reverse Shell", "Opens a reverse shell on Windows", 2048, true),
            BadUsbScript("3", "MacOS Terminal", "Opens Terminal on MacOS", 1536, false),
            BadUsbScript("4", "Linux Privilege Escalation", "Attempts to escalate privileges", 3072, true),
            BadUsbScript("5", "WiFi Password Grabber", "Extracts saved WiFi passwords", 4096, true),
            BadUsbScript("6", "Browser History Dumper", "Dumps browser history", 2560, true)
        )
    }
    
    val keyboardLayouts = listOf("US", "DE", "FR", "UK", "ES", "IT", "RU", "JP")
    
    MainScreen(
        onBack = onBack,
        title = "BadUSB",
        actions = {
            IconButton(onClick = {
                if (isConnected) {
                    viewModel.listBadUsbScripts()
                }
            }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = primaryColor()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Connection Status Banner
            UsbConnectionBanner(
                isConnected = isConnected,
                deviceName = "GhostESP",
                onConnect = { viewModel.connectFirstAvailable() }
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Keyboard Layout Selection
                item {
                    BrutalistCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Keyboard Layout",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor()
                            )
                            
                            Text(
                                text = "Select the target system's keyboard layout",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantDark
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                keyboardLayouts.take(4).forEach { layout ->
                                    FilterChip(
                                        selected = keyboardLayout == layout,
                                        onClick = { keyboardLayout = layout },
                                        label = { Text(layout) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary.copy(alpha = 0.2f),
                                            selectedLabelColor = Primary
                                        )
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                keyboardLayouts.drop(4).forEach { layout ->
                                    FilterChip(
                                        selected = keyboardLayout == layout,
                                        onClick = { keyboardLayout = layout },
                                        label = { Text(layout) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary.copy(alpha = 0.2f),
                                            selectedLabelColor = Primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Running Script Status
                if (isRunning && selectedScript != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Warning.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
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
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Warning,
                                        strokeWidth = 2.dp
                                    )
                                    Column {
                                        Text(
                                            text = "Running: ${selectedScript?.name}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Warning
                                        )
                                        Text(
                                            text = "Do not disconnect the device",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceVariantDark
                                        )
                                    }
                                }
                                
                                BrutalistButton(
                                    text = "Stop",
                                    onClick = {
                                        if (isConnected) {
                                            viewModel.stopBadUsb()
                                            isRunning = false
                                        }
                                    },
                                    containerColor = errorColor(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                // Script List Header
                item {
                    Text(
                        text = "Available Scripts (${scripts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor()
                    )
                }
                
                // Script List
                items(scripts, key = { it.id }) { script ->
                    BadUsbScriptCard(
                        script = script,
                        isSelected = selectedScript?.id == script.id,
                        isRunning = isRunning && selectedScript?.id == script.id,
                        onSelect = { 
                            selectedScript = if (selectedScript?.id == script.id) null else script 
                        },
                        onRun = {
                            if (isConnected) {
                                selectedScript = script
                                viewModel.runBadUsbScript(script.id)
                                isRunning = true
                            }
                        }
                    )
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
                
                // Warning Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Error.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "BadUSB scripts can be dangerous. Only use on systems you own or have permission to test. Use responsibly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Error
                            )
                        }
                    }
                }
            }
        }
        
        // Feature Not Supported or Coming Soon Overlay
        if (hasDeviceInfo && !isBadUsbSupported) {
            FeatureNotSupportedOverlay(
                show = showOverlay,
                onProceed = { showOverlay = false },
                featureName = "BadUSB",
                message = "This device does not have BadUSB hardware support. USB HID attack capabilities require compatible hardware."
            )
        } else {
            ComingSoonOverlay(
                show = showOverlay,
                onProceed = { showOverlay = false },
                viewName = "BadUSB",
                title = "Coming Soon",
                message = "BadUSB features are currently under development. USB HID attack capabilities and script management coming soon."
            )
        }
        }
    }
}

/**
 * BadUSB script data class
 */
data class BadUsbScript(
    val id: String,
    val name: String,
    val description: String,
    val size: Int,
    val isDangerous: Boolean
)

/**
 * Card displaying a BadUSB script
 */
@Composable
private fun BadUsbScriptCard(
    script: BadUsbScript,
    isSelected: Boolean,
    isRunning: Boolean,
    onSelect: () -> Unit,
    onRun: () -> Unit
) {
    BrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when {
            isRunning -> warningColor()
            script.isDangerous -> errorColor().copy(alpha = 0.5f)
            isSelected -> primaryColor()
            else -> OutlineDark
        },
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (script.isDangerous) Icons.Default.Warning else Icons.Default.Description,
                        contentDescription = null,
                        tint = when {
                            isRunning -> warningColor()
                            script.isDangerous -> errorColor()
                            isSelected -> primaryColor()
                            else -> OnSurfaceDark
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = script.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isRunning -> warningColor()
                                script.isDangerous -> errorColor()
                                else -> OnSurfaceDark
                            }
                        )
                        Text(
                            text = "${script.size} bytes",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantDark
                        )
                    }
                }
                
                BrutalistButton(
                    text = if (isRunning) "Running" else "Run",
                    onClick = onRun,
                    containerColor = when {
                        isRunning -> warningColor()
                        script.isDangerous -> errorColor()
                        else -> primaryColor()
                    },
                    enabled = !isRunning,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = script.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantDark
                )
                
                if (script.isDangerous) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "This script may cause damage. Use with caution.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Error
                        )
                    }
                }
            }
        }
    }
}

/**
 * USB Connection Banner component
 */
@Composable
private fun UsbConnectionBanner(
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
                    if (isConnected) Icons.Default.Usb else Icons.Default.UsbOff,
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
                            text = "Ready to run scripts",
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
                        contentColor = onPrimaryColor()
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Connect", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}