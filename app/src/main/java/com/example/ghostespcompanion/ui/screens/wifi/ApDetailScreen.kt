package com.example.ghostespcompanion.ui.screens.wifi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ghostespcompanion.ui.utils.censorMac
import com.example.ghostespcompanion.ui.utils.censorSsid
import com.example.ghostespcompanion.ui.viewmodel.MainViewModel
import androidx.compose.material3.CheckboxDefaults

/**
 * AP Detail Screen - Shows details and actions for a specific access point
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApDetailScreen(
    apIndex: Int,
    viewModel: MainViewModel,
    onNavigateToTrack: (Int) -> Unit = {},
    onNavigateToHandshake: (Int) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var isDeauthing by remember { mutableStateOf(false) }
    var isScanningStations by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var selectedStationIndexes by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    val connectionState by viewModel.connectionState.collectAsState()
    val accessPoints by viewModel.accessPoints.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val wifiConnection by viewModel.wifiConnection.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    val privacyMode = appSettings.privacyMode
    
    // Get the specific AP from the list
    val accessPoint = accessPoints.find { it.index == apIndex }
    
    // Preview data if not connected or AP not found
    val displayAp = accessPoint ?: GhostResponse.AccessPoint(
        index = apIndex,
        ssid = "Network_$apIndex",
        bssid = "AA:BB:CC:DD:EE:FF",
        rssi = -45,
        channel = 6,
        security = "WPA2",
        vendor = "TP-Link"
    )
    
    // Filter stations associated with this AP (by BSSID)
    val associatedStations = remember(stations, accessPoint) {
        if (accessPoint != null) {
            stations.filter { it.apBssid == accessPoint.bssid }
        } else {
            emptyList()
        }
    }
    
    // Check if this is the currently connected network
    val isCurrentConnection = remember(wifiConnection, displayAp) {
        val conn = wifiConnection
        conn?.isConnected == true && conn.ssid == displayAp.ssid
    }
    val connectedIp = if (isCurrentConnection) wifiConnection?.ip else null
    
    LaunchedEffect(isConnected) {
        if (isConnected && accessPoints.isEmpty()) {
            viewModel.listAccessPoints()
        }
    }
    
    MainScreen(
        onBack = onBack,
        title = displayAp.ssid.censorSsid(privacyMode)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection Status Banner
            ApDetailWifiBanner(
                isConnected = isConnected,
                deviceName = "GhostESP",
                onConnect = { viewModel.connectFirstAvailable() }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Network Info Card
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Connection status header
                        if (isCurrentConnection) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = successColor().copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Connected",
                                        tint = successColor(),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Currently Connected",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = successColor()
                                        )
                                        connectedIp?.let {
                                            Text(
                                                text = "IP: $it",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = successColor()
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Text(
                            text = "Network Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor()
                        )
                        
                        NetworkInfoRow("SSID", displayAp.ssid.censorSsid(privacyMode))
                        NetworkInfoRow("BSSID", displayAp.bssid.censorMac(privacyMode))
                        NetworkInfoRow("Channel", displayAp.channel.toString())
                        NetworkInfoRow("Security", displayAp.security)
                        NetworkInfoRow("Signal", "${displayAp.rssi} dBm")
                        displayAp.vendor?.let { NetworkInfoRow("Vendor", it) }
                        
                        // Signal strength indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Signal Quality",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariantDark,
                                modifier = Modifier.weight(1f)
                            )
                            SignalStrengthIndicator(displayAp.rssi)
                        }
                    }
                }
                
                // Action Buttons
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                
                // Primary Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrutalistOutlinedButton(
                        text = "Set Target",
                        onClick = {
                            if (isConnected) {
                                viewModel.selectAp(apIndex.toString())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.GpsFixed, contentDescription = null) }
                    )
                    
                    BrutalistOutlinedButton(
                        text = "Track",
                        onClick = {
                            if (isConnected) {
                                onNavigateToTrack(apIndex)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    )
                }
                
                // Deauth Button
                BrutalistButton(
                    text = if (isDeauthing) "Stop Deauth" else "Start Deauth",
                    onClick = {
                        if (isConnected) {
                            if (isDeauthing) {
                                viewModel.stopDeauth()
                            } else {
                                viewModel.selectAp(apIndex.toString())
                                viewModel.startDeauth()
                            }
                            isDeauthing = !isDeauthing
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (isDeauthing) warningColor() else errorColor(),
                    enabled = isConnected,
                    leadingIcon = {
                        Icon(
                            if (isDeauthing) Icons.Default.Stop else Icons.Default.Warning,
                            contentDescription = null
                        )
                    }
                )
                
                // Connect Button
                if (isCurrentConnection) {
                    // Show connected status
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = successColor().copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, successColor())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = successColor(),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Currently Connected${connectedIp?.let { " ($it)" } ?: ""}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = successColor()
                            )
                        }
                    }
                } else {
                    BrutalistOutlinedButton(
                        text = "Connect to Network",
                        onClick = { showConnectDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = successColor(),
                        textColor = successColor(),
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) }
                    )
                }
                
                // Station Scan Section
                Text(
                    text = "Station Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor()
                )
                
                BrutalistButton(
                    text = if (isScanningStations) "Stop Station Scan" else "Scan for Stations",
                    onClick = {
                        if (isConnected) {
                            if (isScanningStations) {
                                viewModel.stopAll()
                            } else {
                                viewModel.selectAp(apIndex.toString())
                                viewModel.clearStations()
                                viewModel.scanSta()
                            }
                            isScanningStations = !isScanningStations
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (isScanningStations) warningColor() else primaryColor(),
                    enabled = isConnected,
                    leadingIcon = {
                        Icon(
                            if (isScanningStations) Icons.Default.Stop else Icons.Default.Devices,
                            contentDescription = null
                        )
                    }
                )
                
                // Station List - Only show stations associated with this AP
                if (associatedStations.isNotEmpty()) {
                    Text(
                        text = "Associated Stations (${associatedStations.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )
                    
                    // Bulk deauth button for selected stations
                    if (selectedStationIndexes.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedStationIndexes.size} selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = errorColor()
                            )
                            BrutalistButton(
                                text = "Deauth Selected",
                                onClick = {
                                    if (isConnected) {
                                        // Start deauth for selected stations
                                        selectedStationIndexes.forEach { stationIndex ->
                                            viewModel.selectStation(stationIndex.toString())
                                        }
                                        viewModel.startDeauth()
                                        isDeauthing = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = errorColor(),
                                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) }
                            )
                            BrutalistOutlinedButton(
                                text = "Clear",
                                onClick = { selectedStationIndexes = emptySet() },
                                borderColor = OnSurfaceVariantDark,
                                textColor = OnSurfaceVariantDark
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
associatedStations.forEach { station ->
                        StationCardWithSelection(
                            station = station,
                            isSelected = selectedStationIndexes.contains(station.index),
                            privacyMode = privacyMode,
                            onSelect = {
                                if (isConnected) {
                                    viewModel.selectStation(station.index.toString())
                                }
                            },
                            onToggleSelection = {
                                selectedStationIndexes = if (selectedStationIndexes.contains(station.index)) {
                                    selectedStationIndexes - station.index
                                } else {
                                    selectedStationIndexes + station.index
                                }
                            }
                        )
                    }
                } else if (isScanningStations) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = primaryColor()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scanning for stations...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariantDark
                        )
                    }
                }
                
                // Capture Options
                Text(
                    text = "Capture Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrutalistButton(
                        text = "Capture Handshake",
                        onClick = {
                            onNavigateToHandshake(apIndex)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                    )
                }
                
                // Status Message
                if (statusMessage != null) {
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
        }
    }
    
    // Connect Dialog
    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = { Text("Connect to ${displayAp.ssid}") },
            text = {
                Column {
                    Text("Enter password if required:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isConnected) {
                            viewModel.connectWifi(displayAp.ssid, password.ifBlank { null })
                        }
                        showConnectDialog = false
                        password = ""
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConnectDialog = false
                    password = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NetworkInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantDark
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceDark
        )
    }
}

@Composable
private fun SignalStrengthIndicator(rssi: Int) {
    val color = when {
        rssi >= -50 -> successColor()
        rssi >= -60 -> primaryColor()
        rssi >= -70 -> warningColor()
        else -> errorColor()
    }
    
    val bars = when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -70 -> 2
        else -> 1
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((8 + index * 4).dp)
                    .background(
                        color = if (index < bars) color else color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
    
    Spacer(modifier = Modifier.width(8.dp))
    
    Text(
        text = when {
            rssi >= -50 -> "Excellent"
            rssi >= -60 -> "Good"
            rssi >= -70 -> "Fair"
            else -> "Weak"
        },
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

/**
 * WiFi Connection Banner component
 */
@Composable
private fun ApDetailWifiBanner(
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
                    if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isConnected) Success else Error
                )
                Column {
                    Text(
                        text = if (isConnected) "$deviceName Connected" else "Not Connected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnected) Success else Error
                    )
                    if (isConnected) {
                        Text(
                            text = "Ready to interact",
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

/**
 * Station Card component for displaying station info with selection
 */
@Composable
private fun StationCardWithSelection(
    station: GhostResponse.Station,
    isSelected: Boolean,
    privacyMode: Boolean = false,
    onSelect: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val borderColor = when {
        isSelected -> Error
        else -> OutlineDark
    }
    val backgroundColor = when {
        isSelected -> Error.copy(alpha = 0.1f)
        else -> SurfaceVariantDark
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = errorColor(),
                    uncheckedColor = OnSurfaceVariantDark
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.mac.censorMac(privacyMode),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceDark
                )
                station.vendor?.let { vendor ->
                    Text(
                        text = vendor,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantDark
                    )
                }
                // Show RSSI if available
                if (station.rssi > -100) {
                    Text(
                        text = "Signal: ${station.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            station.rssi >= -50 -> Success
                            station.rssi >= -60 -> Primary
                            station.rssi >= -70 -> Warning
                            else -> Error
                        }
                    )
                }
            }
            
            // Deauth indicator or action button
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Error.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "SELECTED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Station Card component for displaying station info (legacy, kept for compatibility)
 */
@Composable
private fun StationCard(
    station: GhostResponse.Station,
    onSelect: () -> Unit
) {
    StationCardWithSelection(
        station = station,
        isSelected = false,
        onSelect = onSelect,
        onToggleSelection = {}
    )
}