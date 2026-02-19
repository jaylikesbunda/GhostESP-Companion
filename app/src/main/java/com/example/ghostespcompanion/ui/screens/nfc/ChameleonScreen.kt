package com.example.ghostespcompanion.ui.screens.nfc

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
import com.example.ghostespcompanion.ui.screens.MainScreen
import com.example.ghostespcompanion.ui.components.*
import com.example.ghostespcompanion.ui.theme.*
import com.example.ghostespcompanion.ui.viewmodel.MainViewModel

/**
 * Chameleon Ultra Screen - Emulate NFC tags with Chameleon Ultra device
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChameleonScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedSlot by remember { mutableStateOf(0) }
    var isEmulating by remember { mutableStateOf(false) }
    var showSlotDialog by remember { mutableStateOf(false) }
    
    val connectionState by viewModel.connectionState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    
    // Available slots (Chameleon Ultra has 8 slots)
    val slots = remember {
        listOf(
            ChameleonSlot(0, "Slot 1", "MIFARE Classic 1K", true),
            ChameleonSlot(1, "Slot 2", "NTAG213", false),
            ChameleonSlot(2, "Slot 3", "Empty", false),
            ChameleonSlot(3, "Slot 4", "Empty", false),
            ChameleonSlot(4, "Slot 5", "Empty", false),
            ChameleonSlot(5, "Slot 6", "Empty", false),
            ChameleonSlot(6, "Slot 7", "Empty", false),
            ChameleonSlot(7, "Slot 8", "Empty", false)
        )
    }
    
    MainScreen(
        onBack = onBack,
        title = "Chameleon Ultra",
        actions = {
            IconButton(onClick = { showSlotDialog = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Tag",
                    tint = primaryColor()
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection Status Banner
            ChameleonConnectionBanner(
                isConnected = isConnected,
                deviceName = "GhostESP",
                onConnect = { viewModel.connectFirstAvailable() }
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Emulation Status
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEmulating) successColor().copy(alpha = 0.1f) else SurfaceVariantDark
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
                                Icon(
                                    if (isEmulating) Icons.Default.CreditCard else Icons.Default.CreditCardOff,
                                    contentDescription = null,
                                    tint = if (isEmulating) successColor() else OnSurfaceVariantDark,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isEmulating) "Emulating" else "Not Emulating",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEmulating) successColor() else OnSurfaceVariantDark
                                    )
                                    Text(
                                        text = if (isEmulating) slots[selectedSlot].name else "Select a slot to start",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariantDark
                                    )
                                }
                            }
                            
                            if (isEmulating) {
                                BrutalistButton(
                                    text = "Stop",
                                    onClick = {
                                        if (isConnected) {
                                            viewModel.sendRaw("chameleon_stop")
                                            isEmulating = false
                                        }
                                    },
                                    containerColor = errorColor(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                // Slot Selection
                item {
                    Text(
                        text = "Available Slots",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor()
                    )
                }
                
                items(slots, key = { it.index }) { slot ->
                    ChameleonSlotCard(
                        slot = slot,
                        isSelected = selectedSlot == slot.index,
                        isEmulating = isEmulating && selectedSlot == slot.index,
                        onSelect = { selectedSlot = slot.index },
                        onEmulate = {
                            if (isConnected) {
                                if (isEmulating && selectedSlot == slot.index) {
                                    viewModel.sendRaw("chameleon_stop")
                                    isEmulating = false
                                } else {
                                    viewModel.sendRaw("chameleon_emulate ${slot.index}")
                                    selectedSlot = slot.index
                                    isEmulating = true
                                }
                            }
                        }
                    )
                }
                
                // Quick Actions
                item {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor()
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrutalistOutlinedButton(
                            text = "Scan to Slot",
                            onClick = {
                                if (isConnected) {
                                    viewModel.scanNfc()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Nfc, contentDescription = null) }
                        )
                        
                        BrutalistOutlinedButton(
                            text = "Clear Slot",
                            onClick = {
                                if (isConnected) {
                                    viewModel.sendRaw("chameleon_clear $selectedSlot")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            borderColor = warningColor(),
                            textColor = warningColor(),
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrutalistOutlinedButton(
                            text = "Save All",
                            onClick = {
                                if (isConnected) {
                                    viewModel.sendRaw("chameleon_save")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                        )
                        
                        BrutalistOutlinedButton(
                            text = "Load All",
                            onClick = {
                                if (isConnected) {
                                    viewModel.sendRaw("chameleon_load")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
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
                                text = "Chameleon Ultra can emulate up to 8 different NFC tags. Select a slot and start emulation to clone NFC cards.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantDark
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chameleon slot data class
 */
data class ChameleonSlot(
    val index: Int,
    val name: String,
    val tagType: String,
    val hasData: Boolean
)

/**
 * Card displaying a Chameleon slot
 */
@Composable
private fun ChameleonSlotCard(
    slot: ChameleonSlot,
    isSelected: Boolean,
    isEmulating: Boolean,
    onSelect: () -> Unit,
    onEmulate: () -> Unit
) {
    BrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when {
            isEmulating -> successColor()
            isSelected -> primaryColor()
            else -> OutlineDark
        },
        onClick = onSelect
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
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEmulating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = successColor(),
                            strokeWidth = 2.dp
                        )
                    }
                    Icon(
                        if (slot.hasData) Icons.Default.CreditCard else Icons.Default.CreditCardOff,
                        contentDescription = null,
                        tint = when {
                            isEmulating -> successColor()
                            isSelected -> primaryColor()
                            slot.hasData -> OnSurfaceDark
                            else -> OnSurfaceVariantDark
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = slot.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isEmulating -> successColor()
                            isSelected -> primaryColor()
                            else -> OnSurfaceDark
                        }
                    )
                    Text(
                        text = slot.tagType,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantDark
                    )
                }
            }
            
            BrutalistButton(
                text = if (isEmulating) "Stop" else "Emulate",
                onClick = onEmulate,
                containerColor = if (isEmulating) errorColor() else if (slot.hasData) primaryColor() else SurfaceVariantDark,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Chameleon Connection Banner component
 */
@Composable
private fun ChameleonConnectionBanner(
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
                    if (isConnected) Icons.Default.Nfc else Icons.Default.Nfc,
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
                            text = "Ready to emulate",
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