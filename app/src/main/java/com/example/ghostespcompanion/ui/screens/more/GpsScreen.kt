package com.example.ghostespcompanion.ui.screens.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
 * GPS Screen - GPS tracking and wardriving
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var isWardriving by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(true) }
    
    val connectionState by viewModel.connectionState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    
    val isGpsSupported = deviceInfo?.hasFeature(GhostResponse.DeviceFeature.GPS) ?: true
    val hasDeviceInfo = deviceInfo != null
    
    // Mock GPS data
    var gpsData by remember {
        mutableStateOf(
            GpsData(
                latitude = -31.9505,
                longitude = 115.8605,
                altitude = 15.0,
                speed = 0.0,
                satellites = 8,
                fix = true,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    // Mock wardriving stats
    var wardriveStats by remember {
        mutableStateOf(
            WardriveStats(
                networksFound = 127,
                networksWithGPS = 98,
                distanceTraveled = 15.3,
                duration = 3600000
            )
        )
    }
    
    MainScreen(
        onBack = onBack,
        title = "GPS / Wardrive",
        actions = {
            IconButton(onClick = { showMap = !showMap }) {
                Icon(
                    if (showMap) Icons.Default.List else Icons.Default.Map,
                    contentDescription = if (showMap) "Show List" else "Show Map",
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
            GpsConnectionBanner(
                isConnected = isConnected,
                deviceName = "GhostESP",
                onConnect = { viewModel.connectFirstAvailable() }
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // GPS Status Card
                item {
                    BrutalistCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (gpsData.fix) successColor() else warningColor()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GPS Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        if (gpsData.fix) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                                        contentDescription = null,
                                        tint = if (gpsData.fix) Success else Warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (gpsData.fix) "Fix Acquired" else "No Fix",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (gpsData.fix) Success else Warning
                                    )
                                }
                            }
                            
                            // Coordinates
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CoordinateDisplay("Latitude", gpsData.latitude, "°")
                                CoordinateDisplay("Longitude", gpsData.longitude, "°")
                            }
                            
                            // Additional GPS info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                GpsStatItem("Altitude", "${gpsData.altitude.toInt()} m")
                                GpsStatItem("Speed", "${gpsData.speed.toInt()} km/h")
                                GpsStatItem("Satellites", gpsData.satellites.toString())
                            }
                        }
                    }
                }
                
                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrutalistButton(
                            text = if (isTracking) "Stop GPS" else "Start GPS",
                            onClick = {
                                if (isConnected) {
                                    if (isTracking) {
                                        viewModel.stopGpsInfo()
                                    } else {
                                        viewModel.getGpsInfo()
                                    }
                                    isTracking = !isTracking
                                }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = if (isTracking) errorColor() else primaryColor(),
                            enabled = isConnected,
                            leadingIcon = {
                                Icon(
                                    if (isTracking) Icons.Default.Stop else Icons.Default.GpsFixed,
                                    contentDescription = null
                                )
                            }
                        )
                        
                        BrutalistButton(
                            text = if (isWardriving) "Stop Wardrive" else "Start Wardrive",
                            onClick = {
                                if (isConnected) {
                                    if (isWardriving) {
                                        viewModel.stopWardrive()
                                    } else {
                                        viewModel.startWardrive()
                                    }
                                    isWardriving = !isWardriving
                                }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = if (isWardriving) errorColor() else successColor(),
                            enabled = isConnected,
                            leadingIcon = {
                                Icon(
                                    if (isWardriving) Icons.Default.Stop else Icons.Default.TravelExplore,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
                
                // Wardrive Stats (if wardriving)
                if (isWardriving || wardriveStats.networksFound > 0) {
                    item {
                        Text(
                            text = "Wardrive Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor()
                        )
                    }
                    
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = SurfaceVariantDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatColumn("Networks", wardriveStats.networksFound.toString())
                                StatColumn("With GPS", wardriveStats.networksWithGPS.toString())
                                StatColumn("Distance", "${wardriveStats.distanceTraveled} km")
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrutalistOutlinedButton(
                                text = "Export Data",
                                onClick = { /* TODO: Export */ },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) }
                            )
                            
                            BrutalistOutlinedButton(
                                text = "Clear Data",
                                onClick = { 
                                    wardriveStats = WardriveStats(0, 0, 0.0, 0)
                                },
                                modifier = Modifier.weight(1f),
                                borderColor = Warning,
                                textColor = Warning,
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
                
                // Map Placeholder
                if (showMap) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SurfaceVariantDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Map,
                                        contentDescription = null,
                                        tint = OnSurfaceVariantDark,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Map View",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnSurfaceVariantDark
                                    )
                                    Text(
                                        text = "Map integration coming soon",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariantDark
                                    )
                                }
                            }
                        }
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
                                text = "Wardriving records WiFi networks with GPS coordinates. This data can be exported for analysis or mapping.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantDark
                            )
                        }
                    }
                }
            }
        }
        
        // Feature Not Supported or Coming Soon Overlay
        if (hasDeviceInfo && !isGpsSupported) {
            FeatureNotSupportedOverlay(
                show = showOverlay,
                onProceed = { showOverlay = false },
                featureName = "GPS",
                message = "This device does not have GPS hardware support. Wardriving requires a GPS module."
            )
        } else {
            ComingSoonOverlay(
                show = showOverlay,
                onProceed = { showOverlay = false },
                viewName = "GPS Wardrive",
                title = "Coming Soon",
                message = "GPS and wardriving features are currently under development. Full GPS tracking and network mapping coming soon."
            )
        }
        }
    }
}

/**
 * GPS data class
 */
data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Double,
    val satellites: Int,
    val fix: Boolean,
    val timestamp: Long
)

/**
 * Wardrive statistics data class
 */
data class WardriveStats(
    val networksFound: Int,
    val networksWithGPS: Int,
    val distanceTraveled: Double,
    val duration: Long
)

@Composable
private fun CoordinateDisplay(label: String, value: Double, unit: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantDark
        )
        Text(
            text = String.format("%.4f$unit", value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceDark
        )
    }
}

@Composable
private fun GpsStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = primaryColor()
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantDark
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = primaryColor()
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantDark
        )
    }
}

/**
 * GPS Connection Banner component
 */
@Composable
private fun GpsConnectionBanner(
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
                    if (isConnected) Icons.Default.GpsFixed else Icons.Default.GpsOff,
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
                            text = "GPS ready",
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