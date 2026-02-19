package com.example.ghostespcompanion.ui.screens.wifi

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Track AP Screen - Polished view for tracking access point signal strength
 * 
 * Displays real-time RSSI data with visual indicators for:
 * - Current signal strength
 * - Min/Max signal range
 * - Direction indicators (CLOSER/FARTHER)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackApScreen(
    apIndex: Int,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val accessPoints by viewModel.accessPoints.collectAsState()
    val trackData by viewModel.trackData.collectAsState()
    val trackHeader by viewModel.trackHeader.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    val privacyMode = appSettings.privacyMode
    
    // Get the specific AP from the list
    val accessPoint = accessPoints.find { it.index == apIndex }
    
    // Track if we're actively tracking
    var isTracking by remember { mutableStateOf(false) }
    
    // Animation for the signal indicator
    val infiniteTransition = rememberInfiniteTransition(label = "signal")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Start tracking when screen opens
    LaunchedEffect(isConnected) {
        if (isConnected && !isTracking) {
            viewModel.selectAp(apIndex.toString())
            viewModel.trackAp()
            isTracking = true
        }
    }
    
    // Stop tracking when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            if (isTracking) {
                viewModel.stopAll()
            }
        }
    }
    
    // Use track header or fall back to AP data
    val targetName = trackHeader?.targetName ?: accessPoint?.ssid ?: "Unknown"
    val targetBssid = trackHeader?.targetBssid ?: accessPoint?.bssid
    val targetChannel = trackHeader?.channel ?: accessPoint?.channel
    
    MainScreen(
        onBack = onBack,
        title = "Track AP",
        actions = {
            IconButton(onClick = {
                if (isTracking) {
                    viewModel.stopAll()
                    isTracking = false
                } else {
                    viewModel.selectAp(apIndex.toString())
                    viewModel.trackAp()
                    isTracking = true
                }
            }) {
                Icon(
                    if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isTracking) "Stop" else "Start",
                    tint = if (isTracking) errorColor() else primaryColor()
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Connection Status Banner
            TrackWifiBanner(
                isConnected = isConnected,
                isTracking = isTracking,
                onConnect = { viewModel.connectFirstAvailable() }
            )
            
            // Target Info Card
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
                        text = "Tracking Target",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariantDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = targetName.censorSsid(privacyMode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    if (targetBssid != null) {
                        Text(
                            text = targetBssid.censorMac(privacyMode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariantDark
                        )
                    }
                    if (targetChannel != null) {
                        Text(
                            text = "Channel $targetChannel",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantDark
                        )
                    }
                }
            }
            
            // Main Signal Display
            if (isTracking && trackData != null) {
                SignalDisplay(
                    trackData = trackData!!,
                    pulseScale = if (isTracking) pulseScale else 1f,
                    privacyMode = privacyMode
                )
            } else if (isTracking) {
                // Waiting for data
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = primaryColor()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Waiting for signal data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )
                }
            } else {
                // Not tracking
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = OnSurfaceVariantDark
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Press play to start tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )
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
                        text = "How to use",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Move closer to see signal increase\n• Move away to see signal decrease\n• Use to locate the physical AP",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantDark
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalDisplay(
    trackData: GhostResponse.TrackData,
    pulseScale: Float,
    privacyMode: Boolean
) {
    val rssi = trackData.rssi
    val minRssi = trackData.minRssi
    val maxRssi = trackData.maxRssi
    val direction = trackData.direction
    
    // Calculate signal quality (0-100)
    val signalQuality = ((rssi + 100).coerceIn(0, 100))
    
    // Color based on signal strength
    val signalColor = when {
        rssi >= -50 -> successColor()
        rssi >= -60 -> primaryColor()
        rssi >= -70 -> warningColor()
        else -> errorColor()
    }
    
    // Direction indicator
    val directionColor = when (direction) {
        GhostResponse.TrackDirection.CLOSER -> successColor()
        GhostResponse.TrackDirection.FARTHER -> errorColor()
        GhostResponse.TrackDirection.STABLE -> OnSurfaceVariantDark
    }
    
    val directionIcon = when (direction) {
        GhostResponse.TrackDirection.CLOSER -> Icons.Default.ArrowUpward
        GhostResponse.TrackDirection.FARTHER -> Icons.Default.ArrowDownward
        GhostResponse.TrackDirection.STABLE -> Icons.Default.Remove
    }
    
    val directionText = when (direction) {
        GhostResponse.TrackDirection.CLOSER -> "GETTING CLOSER"
        GhostResponse.TrackDirection.FARTHER -> "MOVING AWAY"
        GhostResponse.TrackDirection.STABLE -> "STABLE"
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        // Main signal circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            signalColor.copy(alpha = 0.3f),
                            signalColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${rssi}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = signalColor
                )
                Text(
                    text = "dBm",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceVariantDark
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Direction indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                directionIcon,
                contentDescription = null,
                tint = directionColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = directionText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = directionColor
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Min/Max display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MinMaxCard(
                label = "MIN",
                value = minRssi,
                color = Error
            )
            MinMaxCard(
                label = "MAX",
                value = maxRssi,
                color = Success
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Signal quality bar
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Signal Quality",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariantDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { signalQuality / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = signalColor,
                trackColor = SurfaceVariantDark
            )
        }
    }
}

@Composable
private fun MinMaxCard(
    label: String,
    value: Int,
    color: Color
) {
    BrutalistCard {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariantDark
            )
            Text(
                text = "${value} dBm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TrackWifiBanner(
    isConnected: Boolean,
    isTracking: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isConnected -> errorColor().copy(alpha = 0.1f)
                isTracking -> successColor().copy(alpha = 0.1f)
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
                        isTracking -> Icons.Default.LocationOn
                        else -> Icons.Default.Wifi
                    },
                    contentDescription = null,
                    tint = when {
                        !isConnected -> errorColor()
                        isTracking -> successColor()
                        else -> warningColor()
                    }
                )
                Column {
                    Text(
                        text = when {
                            !isConnected -> "Not Connected"
                            isTracking -> "Tracking Active"
                            else -> "Ready to Track"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            !isConnected -> errorColor()
                            isTracking -> successColor()
                            else -> warningColor()
                        }
                    )
                    if (isConnected) {
                        Text(
                            text = if (isTracking) "Move to find signal source" else "Press play to start",
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
