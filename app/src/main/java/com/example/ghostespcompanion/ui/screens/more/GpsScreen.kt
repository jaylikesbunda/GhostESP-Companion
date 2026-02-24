package com.example.ghostespcompanion.ui.screens.more

import android.Manifest
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.ghostespcompanion.data.PhoneLocation
import com.example.ghostespcompanion.data.serial.SerialManager
import com.example.ghostespcompanion.domain.model.GhostResponse
import com.example.ghostespcompanion.ui.screens.MainScreen
import com.example.ghostespcompanion.ui.components.*
import com.example.ghostespcompanion.ui.theme.*
import com.example.ghostespcompanion.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var showMap by remember { mutableStateOf(true) }
    var showOverlay by remember { mutableStateOf(true) }
    var showSdCardWarning by remember { mutableStateOf(false) }
    var sdCardCheckDone by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    var phoneLocation by remember { mutableStateOf<PhoneLocation?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                viewModel.locationHelper.getLocationUpdates().collect { location ->
                    phoneLocation = location
                }
            } catch (_: SecurityException) {
                // Permission revoked at runtime — silently ignore
            }
        }
    }
    
    val connectionState by viewModel.connectionState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isConnected = connectionState == SerialManager.ConnectionState.CONNECTED
    
    val sdEntries by viewModel.sdEntries.collectAsState()
    
    LaunchedEffect(isConnected, sdCardCheckDone) {
        if (isConnected && !sdCardCheckDone) {
            sdCardCheckDone = true
            viewModel.checkSdCard()
        }
    }
    
    LaunchedEffect(statusMessage, sdCardCheckDone) {
        if (sdCardCheckDone && statusMessage?.contains("SD Error") == true) {
            showSdCardWarning = true
        }
    }
    
    val gpsPosition by viewModel.gpsPosition.collectAsState()
    val wardriveStats by viewModel.wardriveStats.collectAsState()
    val isWardriving by viewModel.isWardriving.collectAsState()
    val isBleWardriving by viewModel.isBleWardriving.collectAsState()
    val isGpsTracking by viewModel.isGpsTracking.collectAsState()
    
    val appSettings by viewModel.appSettings.collectAsState()
    val privacyMode = appSettings.privacyMode
    
    val isGpsSupported = deviceInfo?.hasFeature(GhostResponse.DeviceFeature.GPS) ?: true
    val hasDeviceInfo = deviceInfo != null
    
    val mapView = remember { MapView(context) }
    
    DisposableEffect(Unit) {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.onResume()
        
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }
    
    LaunchedEffect(phoneLocation, gpsPosition, showMap) {
        if (!showMap) return@LaunchedEffect

        withContext(Dispatchers.Main) {
            try {
                mapView.overlays.clear()

                // Create tinted marker icons
                val defaultIcon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
                val phoneIcon = defaultIcon?.constantState?.newDrawable()?.mutate()?.also {
                    DrawableCompat.setTint(it, android.graphics.Color.rgb(33, 150, 243)) // Blue
                }
                val deviceIcon = defaultIcon?.constantState?.newDrawable()?.mutate()?.also {
                    DrawableCompat.setTint(it, android.graphics.Color.rgb(244, 67, 54)) // Red
                }

                phoneLocation?.let { phone ->
                    val phoneMarker = Marker(mapView).apply {
                        position = GeoPoint(phone.latitude, phone.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Phone GPS"
                        snippet = "Your current location"
                        icon = phoneIcon
                    }
                    mapView.overlays.add(phoneMarker)

                    if (gpsPosition?.fix == true) {
                        mapView.controller.setCenter(GeoPoint(gpsPosition!!.latitude, gpsPosition!!.longitude))
                    } else {
                        mapView.controller.setCenter(GeoPoint(phone.latitude, phone.longitude))
                    }
                }

                gpsPosition?.let { gps ->
                    if (gps.fix == true) {
                        val ghostMarker = Marker(mapView).apply {
                            position = GeoPoint(gps.latitude, gps.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "GhostESP GPS"
                            snippet = "Device location"
                            icon = deviceIcon
                        }
                        mapView.overlays.add(ghostMarker)
                    }
                }

                mapView.invalidate()
            } catch (e: Exception) {
                // Map view may be detached or in invalid state
            }
        }
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
        // Single Box with the map view always in the composition tree.
        // Removing AndroidView from composition triggers MapView.onDetachedFromWindow()
        // which calls mTileProvider.detach(), killing tile loading. Keeping it permanently
        // present and overlaying the list view on top avoids this entirely.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map view — always present, never detached from the window.
            AndroidView(
                factory = { mapView },
                update = { mv ->
                    mv.onResume()
                    mv.invalidate()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (showMap && privacyMode) Modifier.blur(25.dp) else Modifier)
            )

            // Privacy overlay (map mode only)
            if (showMap && privacyMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Privacy Mode",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Privacy Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Map hidden to protect location",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Feature-not-supported overlay (shown in both modes)
            if (hasDeviceInfo && !isGpsSupported) {
                FeatureNotSupportedOverlay(
                    show = showOverlay,
                    onProceed = { showOverlay = false },
                    featureName = "GPS",
                    message = "This device does not have GPS hardware support. Wardriving requires a GPS module."
                )
            }

            // SD card required warning overlay
            if (showSdCardWarning) {
                FeatureNotSupportedOverlay(
                    show = true,
                    onProceed = { showSdCardWarning = false },
                    featureName = "SD Card Required",
                    message = "Wardriving requires an SD card in the GhostESP device. CSV files are saved to ghostesp/gps on the device SD card."
                )
            }

            if (showMap) {
                // Bottom stats/controls card (map mode)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp)
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (isConnected) Icons.Default.Link else Icons.Default.LinkOff,
                                    contentDescription = null,
                                    tint = if (isConnected) Success else Error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isConnected) "Connected" else "Disconnected",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isConnected) Success else Error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrutalistButton(
                                text = if (isWardriving) "Stop Wardrive" else "Start Wardrive",
                                onClick = {
                                    if (isConnected) {
                                        if (isWardriving) {
                                            viewModel.stopWardrive()
                                        } else {
                                            viewModel.startWardrive()
                                        }
                                    }
                                },
                                containerColor = if (isWardriving) errorColor() else successColor(),
                                enabled = isConnected && !isBleWardriving,
                                modifier = Modifier.weight(1f),
                                leadingIcon = {
                                    Icon(
                                        if (isWardriving) Icons.Default.Stop else Icons.Default.TravelExplore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            BrutalistButton(
                                text = if (isBleWardriving) "Stop BLE WD" else "BLE Wardrive",
                                onClick = {
                                    if (isConnected) {
                                        if (isBleWardriving) {
                                            viewModel.stopBleWardrive()
                                        } else {
                                            viewModel.startBleWardrive()
                                        }
                                    }
                                },
                                containerColor = if (isBleWardriving) errorColor() else primaryColor(),
                                enabled = isConnected && !isWardriving,
                                modifier = Modifier.weight(1f),
                                leadingIcon = {
                                    Icon(
                                        if (isBleWardriving) Icons.Default.Stop else Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (privacyMode) "**.**°" else if (phoneLocation != null) String.format("%.5f°", phoneLocation!!.latitude) else "--",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (phoneLocation != null) Success else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Lat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (privacyMode) "**.**°" else if (phoneLocation != null) String.format("%.5f°", phoneLocation!!.longitude) else "--",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (phoneLocation != null) Success else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Lon",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${wardriveStats?.gpsSatellites ?: gpsPosition?.satellites ?: 0}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((wardriveStats?.gpsSatellites ?: 0) > 0 || gpsPosition?.fix == true) primaryColor() else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Sats",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${gpsPosition?.altitude?.toInt() ?: 0}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gpsPosition?.fix == true) primaryColor() else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Alt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isBleWardriving) "${wardriveStats?.bleDevices ?: 0}" else "${wardriveStats?.accessPoints ?: 0}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWardriving || isBleWardriving) successColor() else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isBleWardriving) "BLE" else "APs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isWardriving && (wardriveStats?.gpsRejected ?: 0) > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${wardriveStats?.gpsRejected} GPS rejected",
                                style = MaterialTheme.typography.labelSmall,
                                color = Warning
                            )
                        }
                    }
                }
            } else {
                // List view — opaque overlay on top of the (always-present) map view.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
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

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BrutalistButton(
                                    text = if (isWardriving) "Stop Wardrive" else "Start Wardrive",
                                    onClick = {
                                        if (isConnected) {
                                            if (isWardriving) {
                                                viewModel.stopWardrive()
                                            } else {
                                                viewModel.startWardrive()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    containerColor = if (isWardriving) errorColor() else successColor(),
                                    enabled = isConnected && !isBleWardriving,
                                    leadingIcon = {
                                        Icon(
                                            if (isWardriving) Icons.Default.Stop else Icons.Default.TravelExplore,
                                            contentDescription = null
                                        )
                                    }
                                )

                                BrutalistButton(
                                    text = if (isBleWardriving) "Stop BLE WD" else "BLE WD",
                                    onClick = {
                                        if (isConnected) {
                                            if (isBleWardriving) {
                                                viewModel.stopBleWardrive()
                                            } else {
                                                viewModel.startBleWardrive()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    containerColor = if (isBleWardriving) errorColor() else primaryColor(),
                                    enabled = isConnected && !isWardriving,
                                    leadingIcon = {
                                        Icon(
                                            if (isBleWardriving) Icons.Default.Stop else Icons.Default.Bluetooth,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }

                        if (isWardriving || isBleWardriving || wardriveStats != null) {
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
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            StatColumn(if (isBleWardriving) "BLE Devices" else "APs Seen", if (isBleWardriving) (wardriveStats?.bleDevices?.toString() ?: "0") else (wardriveStats?.accessPoints?.toString() ?: "0"))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            StatColumn("GPS Status", wardriveStats?.gpsFixStatus ?: "-")
                                            StatColumn("Sats", wardriveStats?.gpsSatellites?.toString() ?: "0")
                                        }
                                        if ((wardriveStats?.gpsRejected ?: 0) > 0) {
                                            Text(
                                                text = "GPS Rejected: ${wardriveStats?.gpsRejected ?: 0} entries",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Warning
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (statusMessage != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = statusMessage!!,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "CSVs are saved to the device SD card under ghostesp/gps.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoordinateDisplay(label: String, value: Double, unit: String, privacyMode: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (privacyMode) "**.****$unit" else String.format("%.6f$unit", value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
