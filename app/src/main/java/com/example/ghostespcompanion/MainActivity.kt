package com.example.ghostespcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ghostespcompanion.data.repository.AppSettings
import com.example.ghostespcompanion.data.repository.PreferencesRepository
import com.example.ghostespcompanion.data.serial.SerialManager
import com.example.ghostespcompanion.ui.navigation.GhostESPNavGraph
import com.example.ghostespcompanion.ui.navigation.Screen
import com.example.ghostespcompanion.ui.navigation.bottomNavItems
import com.example.ghostespcompanion.ui.theme.*
import com.example.ghostespcompanion.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for GhostESP Companion
 * 
 * Single activity that hosts all Compose screens with bottom navigation.
 * Minimalist dark theme with white accents.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            // Collect settings to apply dark mode
            val appSettings by preferencesRepository.appSettings.collectAsState(AppSettings())
            
            GhostESPTheme(darkTheme = appSettings.darkMode) {
                GhostESPApp(
                    autoConnect = appSettings.autoConnect
                )
            }
        }
    }
}

/**
 * Main app composable with modern minimalist bottom navigation
 */
@Composable
fun GhostESPApp(
    autoConnect: Boolean = false,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    
    // Handle auto-connect on startup
    val connectionState by viewModel.connectionState.collectAsState()
    val hasAutoConnected = remember { mutableStateOf(false) }
    
    LaunchedEffect(autoConnect, connectionState) {
        if (autoConnect && !hasAutoConnected.value && connectionState == SerialManager.ConnectionState.DISCONNECTED) {
            hasAutoConnected.value = true
            viewModel.connectFirstAvailable()
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route
            
            // Show bottom bar on all screens, but determine which tab to highlight
            // For sub-screens, highlight the parent tab
            val highlightedRoute = when {
                currentRoute == null -> Screen.Dashboard.route
                currentRoute.startsWith("more/") && currentRoute != "more" -> Screen.More.route
                currentRoute.startsWith("wifi/") && currentRoute != "wifi" -> Screen.Wifi.route
                currentRoute.startsWith("ble/") && currentRoute != "ble" -> Screen.Ble.route
                currentRoute.startsWith("ir/") && currentRoute != "ir" -> Screen.Ir.route
                currentRoute.startsWith("nfc/") && currentRoute != "nfc" -> Screen.More.route // NFC is under More
                else -> currentRoute
            }
            
            ModernNavigationBar(
                items = bottomNavItems,
                currentRoute = highlightedRoute,
                onItemClick = { screen ->
                    val startDestId = navController.graph.findStartDestination().id
                    
                    // Check if we're in a sub-screen of the tapped tab
                    // If so, pop back to the parent screen instead of navigating
                    val isInSubmenuOfTappedTab = when {
                        screen.route == Screen.More.route && currentRoute != null -> 
                            currentRoute.startsWith("more/") && currentRoute != "more"
                        screen.route == Screen.Wifi.route && currentRoute != null -> 
                            currentRoute.startsWith("wifi/") && currentRoute != "wifi"
                        screen.route == Screen.Ble.route && currentRoute != null -> 
                            currentRoute.startsWith("ble/") && currentRoute != "ble"
                        screen.route == Screen.Ir.route && currentRoute != null -> 
                            currentRoute.startsWith("ir/") && currentRoute != "ir"
                        else -> false
                    }
                    
                    when {
                        screen.route == Screen.Dashboard.route -> {
                            // For Dashboard (start destination), just pop back to it
                            navController.popBackStack(startDestId, inclusive = false)
                        }
                        isInSubmenuOfTappedTab -> {
                            // Pop back to the parent screen (e.g., from more/settings to more)
                            navController.popBackStack(screen.route, inclusive = false)
                        }
                        else -> {
                            // For other tabs, use standard save/restore state pattern
                            navController.navigate(screen.route) {
                                popUpTo(startDestId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GhostESPNavGraph(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                sharedViewModel = viewModel
            )
        }
    }
}

/**
 * Modern minimalist navigation bar
 */
@Composable
fun ModernNavigationBar(
    items: List<Screen>,
    currentRoute: String?,
    onItemClick: (Screen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { screen ->
                val selected = currentRoute == screen.route
                
                ModernNavItem(
                    icon = screen.icon.toImageVector(),
                    label = screen.title,
                    selected = selected,
                    onClick = { onItemClick(screen) }
                )
            }
        }
    }
}

/**
 * Modern nav item with minimalist styling
 */
@Composable
fun ModernNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

/**
 * Extension function to convert icon string to ImageVector
 */
private fun String.toImageVector(): ImageVector = when (this) {
    "dashboard" -> Icons.Default.Dashboard
    "wifi" -> Icons.Default.Wifi
    "bluetooth" -> Icons.Default.Bluetooth
    "nfc" -> Icons.Default.Nfc
    "remote" -> Icons.Default.SettingsRemote
    "more_horiz" -> Icons.Default.MoreHoriz
    else -> Icons.Default.Help
}
