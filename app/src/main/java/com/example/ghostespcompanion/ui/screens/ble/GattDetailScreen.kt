package com.example.ghostespcompanion.ui.screens.ble

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ghostespcompanion.data.serial.SerialManager
import com.example.ghostespcompanion.domain.model.GhostResponse
import com.example.ghostespcompanion.ui.screens.MainScreen
import com.example.ghostespcompanion.ui.components.*
import com.example.ghostespcompanion.ui.theme.*
import com.example.ghostespcompanion.ui.utils.censorMac
import com.example.ghostespcompanion.ui.utils.censorDevice
import com.example.ghostespcompanion.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GattDetailScreen(
    gattIndex: Int,
    viewModel: MainViewModel,
    onNavigateToTrack: (Int) -> Unit,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val gattDevices by viewModel.gattDevices.collectAsState()
    val gattServices by viewModel.gattServices.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    val privacyMode = appSettings.privacyMode
    
    val gattDevice = gattDevices.find { it.index == gattIndex }
    var isEnumerating by remember { mutableStateOf(false) }
    
    LaunchedEffect(isConnected, gattDevice) {
        if (isConnected && gattDevice != null && gattServices.isEmpty()) {
            isEnumerating = true
            viewModel.selectAndEnumGatt(gattIndex.toString())
        }
    }
    
    LaunchedEffect(gattServices) {
        if (gattServices.isNotEmpty()) {
            isEnumerating = false
        }
    }
    
    MainScreen(
        onBack = onBack,
        title = "GATT Device",
        actions = {
            IconButton(onClick = {
                if (isConnected && gattDevice != null) {
                    isEnumerating = true
                    viewModel.selectAndEnumGatt(gattIndex.toString())
                }
            }) {
                if (isEnumerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = primaryColor(),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = primaryColor()
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GattDetailConnectionBanner(
                isConnected = isConnected,
                isEnumerating = isEnumerating,
                onConnect = { viewModel.connectFirstAvailable() }
            )
            
            gattDevice?.let { device ->
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = primaryColor(),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = (device.name ?: "Unknown").censorDevice(privacyMode),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Text(
                                    text = device.mac.censorMac(privacyMode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariantDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${device.rssi} dBm",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        device.rssi >= -60 -> SignalExcellent
                                        device.rssi >= -80 -> SignalGood
                                        else -> SignalFair
                                    }
                                )
                                device.type?.let { type ->
                                    Text(
                                        text = type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnSurfaceVariantDark
                                    )
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BrutalistOutlinedButton(
                        text = "Track Device",
                        onClick = { onNavigateToTrack(gattIndex) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    )
                }
            }
            
            if (isEnumerating && gattServices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = primaryColor().copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = primaryColor(),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Enumerating services...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryColor()
                        )
                    }
                }
            } else if (gattServices.isNotEmpty()) {
                Text(
                    text = "Services (${gattServices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gattServices) { service ->
                        GattServiceCard(
                            service = service,
                            privacyMode = privacyMode
                        )
                    }
                }
            } else if (!isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = warningColor().copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = warningColor()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Connect to device to enumerate services",
                            style = MaterialTheme.typography.bodyMedium,
                            color = warningColor()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GattServiceCard(
    service: GhostResponse.GattService,
    privacyMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.SettingsBluetooth,
                    contentDescription = null,
                    tint = primaryColor(),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name ?: "Unknown Service",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "UUID: ${service.uuid}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantDark
                    )
                }
            }
            
            if (service.characteristics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Characteristics:",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariantDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                service.characteristics.forEach { char ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = OnSurfaceVariantDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = char.name ?: char.uuid,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (char.properties.isNotEmpty()) {
                            Text(
                                text = " [${char.properties.joinToString(", ")}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantDark
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Handles: ${service.startHandle}-${service.endHandle}",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariantDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun GattDetailConnectionBanner(
    isConnected: Boolean,
    isEnumerating: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isConnected -> errorColor().copy(alpha = 0.1f)
                isEnumerating -> primaryColor().copy(alpha = 0.1f)
                else -> successColor().copy(alpha = 0.1f)
            }
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
                    when {
                        !isConnected -> Icons.Default.UsbOff
                        isEnumerating -> Icons.Default.SettingsBluetooth
                        else -> Icons.Default.Check
                    },
                    contentDescription = null,
                    tint = when {
                        !isConnected -> errorColor()
                        isEnumerating -> primaryColor()
                        else -> successColor()
                    }
                )
                Column {
                    Text(
                        text = when {
                            !isConnected -> "Not Connected"
                            isEnumerating -> "Enumerating..."
                            else -> "Connected"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            !isConnected -> errorColor()
                            isEnumerating -> primaryColor()
                            else -> successColor()
                        }
                    )
                }
            }
            
            if (!isConnected) {
                BrutalistButton(
                    text = "Connect",
                    onClick = onConnect,
                    containerColor = primaryColor(),
                    modifier = Modifier
                )
            }
        }
    }
}
