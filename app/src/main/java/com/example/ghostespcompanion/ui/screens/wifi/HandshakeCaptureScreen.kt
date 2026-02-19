package com.example.ghostespcompanion.ui.screens.wifi

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostespcompanion.data.serial.SerialManager
import com.example.ghostespcompanion.domain.model.GhostResponse
import com.example.ghostespcompanion.ui.screens.MainScreen
import com.example.ghostespcompanion.ui.components.*
import com.example.ghostespcompanion.ui.theme.*
import com.example.ghostespcompanion.ui.utils.censorMac
import com.example.ghostespcompanion.ui.utils.censorSsid
import com.example.ghostespcompanion.ui.viewmodel.MainViewModel

/**
 * Handshake Capture Screen - Dedicated screen for capturing WPA/WPA2 handshakes
 * 
 * Displays:
 * - Target AP information
 * - Capture status and progress
 * - Handshake detection results
 * - PCAP file location
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandshakeCaptureScreen(
    apIndex: Int,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val accessPoints by viewModel.accessPoints.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val pcapFilePath by viewModel.pcapFile.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    val privacyMode = appSettings.privacyMode
    
    // Get the specific AP from the list
    val accessPoint = accessPoints.find { it.index == apIndex }
    
    // Track capture state
    var isCapturing by remember { mutableStateOf(false) }
    var handshakeCount by remember { mutableStateOf(0) }
    var captureStatus by remember { mutableStateOf("Ready to capture") }
    
    // Animation for the capture indicator
    val infiniteTransition = rememberInfiniteTransition(label = "capture")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Collect handshake events - each event is emitted individually
    LaunchedEffect(Unit) {
        viewModel.handshakeEvents.collect { handshake ->
            handshakeCount++
            captureStatus = "Handshake captured! (${handshake.pairType})"
        }
    }
    
    // Parse status messages for capture state changes
    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            when {
                message.contains("Starting EAPOL", ignoreCase = true) -> {
                    captureStatus = "Listening for handshakes..."
                }
                message.contains("EAPOL: locked to channel", ignoreCase = true) -> {
                    captureStatus = "Listening on channel ${accessPoint?.channel}..."
                }
            }
        }
    }
    
    // Stop capturing when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            if (isCapturing) {
                viewModel.stopAll()
            }
        }
    }
    
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
    
    MainScreen(
        onBack = onBack,
        title = "Handshake Capture",
        actions = {
            IconButton(onClick = {
                if (isCapturing) {
                    viewModel.stopAll()
                    isCapturing = false
                    captureStatus = "Capture stopped"
                } else if (isConnected) {
                    viewModel.selectAp(apIndex.toString())
                    viewModel.startEapolCapture(displayAp.channel)
                    isCapturing = true
                    handshakeCount = 0
                    captureStatus = "Starting capture..."
                }
            }) {
                Icon(
                    if (isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isCapturing) "Stop" else "Start",
                    tint = if (isCapturing) errorColor() else primaryColor()
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Connection Status Banner
            HandshakeWifiBanner(
                isConnected = isConnected,
                isCapturing = isCapturing,
                handshakeCount = handshakeCount,
                onConnect = { viewModel.connectFirstAvailable() }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Target AP Card
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Target Network",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariantDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = displayAp.ssid.censorSsid(privacyMode),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            text = displayAp.bssid.censorMac(privacyMode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariantDark
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Ch ${displayAp.channel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantDark
                            )
                            Text(
                                text = displayAp.security,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantDark
                            )
                        }
                    }
                }
                
                // Capture Status Display
                CaptureStatusDisplay(
                    isCapturing = isCapturing,
                    handshakeCount = handshakeCount,
                    status = captureStatus,
                    pulseScale = if (isCapturing) pulseScale else 1f
                )
                
                // PCAP File Info
                pcapFilePath?.let { pcapPath ->
                    BrutalistCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                tint = successColor()
                            )
                            Column {
                                Text(
                                    text = "Capture Saved",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = successColor()
                                )
                                Text(
                                    text = pcapPath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariantDark
                                )
                            }
                        }
                    }
                }
                
                // Handshake Counter
                if (handshakeCount > 0) {
                    BrutalistCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = successColor().copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null,
                                    tint = successColor()
                                )
                                Text(
                                    text = "Handshakes Captured",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceDark
                                )
                            }
                            Text(
                                text = "$handshakeCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = successColor()
                            )
                        }
                    }
                }
                
                // Instructions
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "How it works",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Press play to start capturing\n" +
                                    "2. Wait for a device to connect to the target network\n" +
                                    "3. Handshakes are captured automatically\n" +
                                    "4. PCAP files are saved to SD card",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantDark
                        )
                    }
                }
                
                // Tips
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Tips",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = warningColor()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Toggle WiFi on a connected device to trigger a handshake\n" +
                                    "• Multiple handshakes improve cracking chances\n" +
                                    "• Deauth attacks can force reconnections",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantDark
                        )
                    }
                }
                
                // Start/Stop Button
                BrutalistButton(
                    text = if (isCapturing) "Stop Capture" else "Start Capture",
                    onClick = {
                        if (isCapturing) {
                            viewModel.stopAll()
                            isCapturing = false
                            captureStatus = "Capture stopped"
                        } else if (isConnected) {
                            viewModel.selectAp(apIndex.toString())
                            viewModel.startEapolCapture(displayAp.channel)
                            isCapturing = true
                            handshakeCount = 0
                            captureStatus = "Starting capture..."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (isCapturing) errorColor() else primaryColor(),
                    enabled = isConnected,
                    leadingIcon = {
                        Icon(
                            if (isCapturing) Icons.Default.Stop else Icons.Default.Key,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CaptureStatusDisplay(
    isCapturing: Boolean,
    handshakeCount: Int,
    status: String,
    pulseScale: Float
) {
    val statusColor = when {
        handshakeCount > 0 -> successColor()
        isCapturing -> primaryColor()
        else -> OnSurfaceVariantDark
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .scale(if (isCapturing && handshakeCount == 0) pulseScale else 1f)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        statusColor.copy(alpha = 0.3f),
                        statusColor.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (handshakeCount > 0) Icons.Default.CheckCircle else if (isCapturing) Icons.Default.WifiTethering else Icons.Default.Key,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = statusColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = statusColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HandshakeWifiBanner(
    isConnected: Boolean,
    isCapturing: Boolean,
    handshakeCount: Int,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isConnected -> errorColor().copy(alpha = 0.1f)
                handshakeCount > 0 -> successColor().copy(alpha = 0.1f)
                isCapturing -> primaryColor().copy(alpha = 0.1f)
                else -> warningColor().copy(alpha = 0.1f)
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
                        !isConnected -> Icons.Default.WifiOff
                        handshakeCount > 0 -> Icons.Default.CheckCircle
                        isCapturing -> Icons.Default.WifiTethering
                        else -> Icons.Default.Wifi
                    },
                    contentDescription = null,
                    tint = when {
                        !isConnected -> errorColor()
                        handshakeCount > 0 -> successColor()
                        isCapturing -> primaryColor()
                        else -> warningColor()
                    }
                )
                Column {
                    Text(
                        text = when {
                            !isConnected -> "Not Connected"
                            handshakeCount > 0 -> "Handshake Captured!"
                            isCapturing -> "Listening..."
                            else -> "Ready to Capture"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            !isConnected -> errorColor()
                            handshakeCount > 0 -> successColor()
                            isCapturing -> primaryColor()
                            else -> warningColor()
                        }
                    )
                    if (isConnected) {
                        Text(
                            text = if (isCapturing) "Waiting for device authentication" else "Press start to begin",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantDark
                        )
                    }
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
