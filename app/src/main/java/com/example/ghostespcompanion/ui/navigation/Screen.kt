package com.example.ghostespcompanion.ui.navigation

/**
 * Navigation destinations for GhostESP Companion
 * 
 * Defines all screens and their routes for type-safe navigation.
 */
sealed class Screen(val route: String, val title: String, val icon: String) {
    // Main bottom navigation screens
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        icon = "dashboard"
    )
    
    data object Wifi : Screen(
        route = "wifi",
        title = "WiFi",
        icon = "wifi"
    )
    
    data object Ble : Screen(
        route = "ble",
        title = "BLE",
        icon = "bluetooth"
    )
    
    data object Ir : Screen(
        route = "ir",
        title = "Infrared",
        icon = "remote"
    )
    
    data object More : Screen(
        route = "more",
        title = "More",
        icon = "more_horiz"
    )
    
    // NFC - now in More section
    data object Nfc : Screen(
        route = "more/nfc",
        title = "NFC",
        icon = "nfc"
    )
    
    // WiFi sub-screens
    data object WifiScan : Screen(
        route = "wifi/scan",
        title = "Scan Networks",
        icon = "wifi"
    )
    
    data object WifiApDetail : Screen(
        route = "wifi/ap/{apIndex}",
        title = "Network Details",
        icon = "wifi"
    ) {
        fun createRoute(apIndex: Int) = "wifi/ap/$apIndex"
    }
    
    data object TrackAp : Screen(
        route = "wifi/track/{apIndex}",
        title = "Track AP",
        icon = "location_on"
    ) {
        fun createRoute(apIndex: Int) = "wifi/track/$apIndex"
    }
    
    data object HandshakeCapture : Screen(
        route = "wifi/handshake/{apIndex}",
        title = "Handshake Capture",
        icon = "key"
    ) {
        fun createRoute(apIndex: Int) = "wifi/handshake/$apIndex"
    }
    
    data object EvilPortal : Screen(
        route = "wifi/portal",
        title = "Evil Portal",
        icon = "router"
    )
    
    // BLE sub-screens
    data object BleScan : Screen(
        route = "ble/scan",
        title = "Scan Devices",
        icon = "bluetooth"
    )
    
    data object FlipperDetect : Screen(
        route = "ble/flipper",
        title = "Flipper Detection",
        icon = "search"
    )
    
    data object TrackGatt : Screen(
        route = "ble/track/{gattIndex}",
        title = "Track GATT Device",
        icon = "location_on"
    ) {
        fun createRoute(gattIndex: Int) = "ble/track/$gattIndex"
    }

    data object TrackFlipper : Screen(
        route = "ble/trackflipper/{flipperIndex}",
        title = "Track Flipper",
        icon = "location_on"
    ) {
        fun createRoute(flipperIndex: Int) = "ble/trackflipper/$flipperIndex"
    }
    
    data object GattDetail : Screen(
        route = "ble/gatt/{gattIndex}",
        title = "GATT Services",
        icon = "settings_bluetooth"
    ) {
        fun createRoute(gattIndex: Int) = "ble/gatt/$gattIndex"
    }
    
    // NFC sub-screens
    data object NfcScan : Screen(
        route = "nfc/scan",
        title = "Scan Tag",
        icon = "nfc"
    )
    
    data object Chameleon : Screen(
        route = "nfc/chameleon",
        title = "Chameleon Ultra",
        icon = "credit_card"
    )
    
    data object SavedTags : Screen(
        route = "nfc/saved",
        title = "Saved Tags",
        icon = "folder"
    )
    
    // IR sub-screens
    data object IrRemote : Screen(
        route = "ir/remote",
        title = "Remotes",
        icon = "remote"
    )
    
    data object IrRemoteDetail : Screen(
        route = "ir/remote/{remoteIndex}",
        title = "Remote Details",
        icon = "remote"
    ) {
        fun createRoute(remoteIndex: Int) = "ir/remote/$remoteIndex"
    }
    
    data object IrLearn : Screen(
        route = "ir/learn",
        title = "Learn Signal",
        icon = "settings_remote"
    )
    
    // More menu screens
    data object BadUsb : Screen(
        route = "more/badusb",
        title = "BadUSB",
        icon = "usb"
    )
    
    data object Gps : Screen(
        route = "more/gps",
        title = "GPS / Wardrive",
        icon = "gps_fixed"
    )
    
    data object Ethernet : Screen(
        route = "more/ethernet",
        title = "Ethernet",
        icon = "lan"
    )
    
    data object SdManager : Screen(
        route = "more/sd",
        title = "SD Manager",
        icon = "sd_card"
    )
    
    data object Settings : Screen(
        route = "more/settings",
        title = "Settings",
        icon = "settings"
    )
    
    data object Terminal : Screen(
        route = "more/terminal",
        title = "Terminal",
        icon = "terminal"
    )
}

/**
 * Bottom navigation items
 */
val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Wifi,
    Screen.Ble,
    Screen.Ir,
    Screen.More
)
