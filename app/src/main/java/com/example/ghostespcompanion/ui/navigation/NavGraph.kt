package com.example.ghostespcompanion.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ghostespcompanion.ui.screens.dashboard.DashboardScreen
import com.example.ghostespcompanion.ui.screens.wifi.WifiScreen
import com.example.ghostespcompanion.ui.screens.wifi.EvilPortalScreen
import com.example.ghostespcompanion.ui.screens.wifi.ApDetailScreen
import com.example.ghostespcompanion.ui.screens.wifi.TrackApScreen
import com.example.ghostespcompanion.ui.screens.wifi.HandshakeCaptureScreen
import com.example.ghostespcompanion.ui.screens.ble.BleScreen
import com.example.ghostespcompanion.ui.screens.ble.FlipperDetectScreen
import com.example.ghostespcompanion.ui.screens.ble.TrackGattScreen
import com.example.ghostespcompanion.ui.screens.ble.TrackFlipperScreen
import com.example.ghostespcompanion.ui.screens.ble.GattDetailScreen
import com.example.ghostespcompanion.ui.screens.nfc.NfcScreen
import com.example.ghostespcompanion.ui.screens.nfc.ChameleonScreen
import com.example.ghostespcompanion.ui.screens.nfc.SavedTagsScreen
import com.example.ghostespcompanion.ui.screens.ir.IrScreen
import com.example.ghostespcompanion.ui.screens.ir.IrLearnScreen
import com.example.ghostespcompanion.ui.screens.ir.IrRemoteDetailScreen
import com.example.ghostespcompanion.ui.screens.more.MoreMenuScreen
import com.example.ghostespcompanion.ui.screens.more.BadUsbScreen
import com.example.ghostespcompanion.ui.screens.more.GpsScreen
import com.example.ghostespcompanion.ui.screens.more.EthernetScreen
import com.example.ghostespcompanion.ui.screens.more.SdManagerScreen
import com.example.ghostespcompanion.ui.screens.more.SettingsScreen
import com.example.ghostespcompanion.ui.screens.terminal.TerminalScreen
import com.example.ghostespcompanion.ui.viewmodel.MainViewModel

// Fast fade for bottom nav tab switches — feels nearly instant
private val tabEnter = fadeIn(animationSpec = tween(100, easing = FastOutSlowInEasing))
private val tabExit = fadeOut(animationSpec = tween(100, easing = FastOutSlowInEasing))

// Slide transitions for sub-screen push/pop navigation with spring physics
private val subScreenEnter = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))

private val subScreenExit = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth / 4 },
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
) + fadeOut(animationSpec = tween(150))

private val subScreenPopEnter = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth / 4 },
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))

private val subScreenPopExit = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
) + fadeOut(animationSpec = tween(150))

/**
 * Main navigation graph for GhostESP Companion
 *
 * Defines all navigation destinations and their composable screens.
 * Uses bottom navigation for main sections with nested navigation for sub-screens.
 *
 * IMPORTANT: All screens share the same MainViewModel instance by passing it
 * from the activity level. This ensures the serial connection is shared across
 * all screens and not recreated when navigating.
 *
 * @param navController The NavHostController for navigation
 * @param startDestination The initial screen to display
 * @param sharedViewModel The shared MainViewModel instance from the activity
 */
@Composable
fun GhostESPNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route,
    sharedViewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { tabEnter },
        exitTransition = { tabExit },
        popEnterTransition = { tabEnter },
        popExitTransition = { tabExit }
    ) {
        // Main bottom navigation screens — use default (fast fade) transitions
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = sharedViewModel,
                onNavigateToWifi = { navController.navigate(Screen.Wifi.route) },
                onNavigateToBle = { navController.navigate(Screen.Ble.route) },
                onNavigateToIr = { navController.navigate(Screen.Ir.route) },
                onNavigateToMore = { navController.navigate(Screen.More.route) },
                onNavigateToNfc = { navController.navigate(Screen.Nfc.route) },
                onNavigateToGps = { navController.navigate(Screen.Gps.route) },
                onNavigateToBadUsb = { navController.navigate(Screen.BadUsb.route) },
                onNavigateToSd = { navController.navigate(Screen.SdManager.route) },
                onScanWifiAndNavigate = {
                    sharedViewModel.scanWifi()
                    navController.navigate(Screen.Wifi.route)
                },
                onScanBleAndNavigate = {
                    navController.navigate(Screen.FlipperDetect.route)
                },
                onScanNfcAndNavigate = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(route = Screen.Wifi.route) {
            WifiScreen(
                viewModel = sharedViewModel,
                onNavigateToApDetail = { apIndex ->
                    navController.navigate(Screen.WifiApDetail.createRoute(apIndex))
                },
                onNavigateToPortal = {
                    navController.navigate(Screen.EvilPortal.route)
                },
                onNavigateToTrack = { apIndex ->
                    navController.navigate(Screen.TrackAp.createRoute(apIndex))
                }
            )
        }

        composable(route = Screen.Ble.route) {
            BleScreen(
                viewModel = sharedViewModel,
                onNavigateToFlipper = {
                    navController.navigate(Screen.FlipperDetect.route)
                },
                onNavigateToGattDetail = { gattIndex ->
                    navController.navigate(Screen.GattDetail.createRoute(gattIndex))
                },
                onNavigateToTrackGatt = { gattIndex ->
                    navController.navigate(Screen.TrackGatt.createRoute(gattIndex))
                },
                onNavigateToTrackFlipper = { flipperIndex ->
                    navController.navigate(Screen.TrackFlipper.createRoute(flipperIndex))
                }
            )
        }

        composable(route = Screen.Ir.route) {
            IrScreen(
                viewModel = sharedViewModel,
                onNavigateToLearn = {
                    navController.navigate(Screen.IrLearn.route)
                },
                onNavigateToRemoteDetail = { remoteIndex ->
                    navController.navigate(Screen.IrRemoteDetail.createRoute(remoteIndex))
                }
            )
        }

        composable(route = Screen.More.route) {
            MoreMenuScreen(
                viewModel = sharedViewModel,
                onNavigateToNfc = { navController.navigate(Screen.Nfc.route) },
                onNavigateToBadUsb = { navController.navigate(Screen.BadUsb.route) },
                onNavigateToGps = { navController.navigate(Screen.Gps.route) },
                onNavigateToEthernet = { navController.navigate(Screen.Ethernet.route) },
                onNavigateToSd = { navController.navigate(Screen.SdManager.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTerminal = { navController.navigate(Screen.Terminal.route) }
            )
        }

        // NFC screen (now in More section)
        composable(
            route = Screen.Nfc.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            NfcScreen(
                viewModel = sharedViewModel,
                onNavigateToChameleon = { navController.navigate(Screen.Chameleon.route) },
                onNavigateToSaved = { navController.navigate(Screen.SavedTags.route) },
                onBack = { navController.navigateUp() }
            )
        }

        // WiFi sub-screens — slide transitions
        composable(
            route = Screen.EvilPortal.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            EvilPortalScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.WifiApDetail.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) { backStackEntry ->
            val apIndex = backStackEntry.arguments?.getString("apIndex")?.toIntOrNull() ?: 0
            ApDetailScreen(
                apIndex = apIndex,
                viewModel = sharedViewModel,
                onNavigateToTrack = { idx -> navController.navigate(Screen.TrackAp.createRoute(idx)) },
                onNavigateToHandshake = { idx -> navController.navigate(Screen.HandshakeCapture.createRoute(idx)) },
                onBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.TrackAp.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) { backStackEntry ->
            val apIndex = backStackEntry.arguments?.getString("apIndex")?.toIntOrNull() ?: 0
            TrackApScreen(apIndex = apIndex, viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.HandshakeCapture.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) { backStackEntry ->
            val apIndex = backStackEntry.arguments?.getString("apIndex")?.toIntOrNull() ?: 0
            HandshakeCaptureScreen(apIndex = apIndex, viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        // BLE sub-screens
        composable(
            route = Screen.FlipperDetect.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            FlipperDetectScreen(
                viewModel = sharedViewModel,
                onNavigateToTrackFlipper = { flipperIndex ->
                    navController.navigate(Screen.TrackFlipper.createRoute(flipperIndex))
                },
                onBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.TrackGatt.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) { backStackEntry ->
            val gattIndex = backStackEntry.arguments?.getString("gattIndex")?.toIntOrNull() ?: 0
            TrackGattScreen(gattIndex = gattIndex, viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.TrackFlipper.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) { backStackEntry ->
            val flipperIndex = backStackEntry.arguments?.getString("flipperIndex")?.toIntOrNull() ?: 0
            TrackFlipperScreen(flipperIndex = flipperIndex, viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.GattDetail.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) { backStackEntry ->
            val gattIndex = backStackEntry.arguments?.getString("gattIndex")?.toIntOrNull() ?: 0
            GattDetailScreen(
                gattIndex = gattIndex,
                viewModel = sharedViewModel,
                onNavigateToTrack = { idx -> navController.navigate(Screen.TrackGatt.createRoute(idx)) },
                onBack = { navController.navigateUp() }
            )
        }

        // NFC sub-screens
        composable(
            route = Screen.Chameleon.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            ChameleonScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.SavedTags.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            SavedTagsScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        // IR sub-screens
        composable(
            route = Screen.IrLearn.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            IrLearnScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.IrRemoteDetail.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) { backStackEntry ->
            val remoteIndex = backStackEntry.arguments?.getString("remoteIndex")?.toIntOrNull() ?: 0
            IrRemoteDetailScreen(remoteIndex = remoteIndex, viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        // More menu sub-screens
        composable(
            route = Screen.BadUsb.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            BadUsbScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.Gps.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            GpsScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.Ethernet.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            EthernetScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.SdManager.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            SdManagerScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            SettingsScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.Terminal.route,
            enterTransition = { subScreenEnter },
            exitTransition = { subScreenExit },
            popEnterTransition = { subScreenPopEnter },
            popExitTransition = { subScreenPopExit }
        ) {
            TerminalScreen(viewModel = sharedViewModel, onBack = { navController.navigateUp() })
        }
    }
}
