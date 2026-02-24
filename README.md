# GhostESP Companion

Android companion app for [GhostESP](https://ghostesp.net) devices. Provides a mobile interface for ESP32-based WiFi/BLE security tools.

https://ghostesp.net/companion

## Requirements

- Android 7.0+ (API 24)
- Android Studio Hedgehog or later
- JDK 17

## Build

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK
```

## Features

### WiFi
- AP/STA scanning with live mode
- Handshake capture (EAPOL)
- RSSI tracking with signal strength visualization
- Beacon spam (random, rickroll, custom SSID list)
- Karma attacks
- Evil Portal with credential capture
- Channel sweeping and congestion analysis

### Bluetooth
- BLE device scanning (standard, Flipper detection, AirTag, GATT)
- BLE spam attacks (Apple, Microsoft, Samsung, Google)
- GATT service enumeration
- Device tracking via RSSI
- AirTag spoofing

### Infrared
- IR remote database with preset devices
- Signal learning mode
- Universal remote codes
- IR dazzler mode

### Additional Tools
- GPS wardriving with coordinate logging
- SD card file browser
- Serial terminal for raw commands

## Tech Stack

- Kotlin with Jetpack Compose
- Hilt dependency injection
- MVVM architecture
- USB serial communication via usb-serial-for-android
- OSMDroid for map display
- Material 3 theming

## Permissions

- USB host mode (required)
- Location (fine/coarse) - required for WiFi/BLE scanning on Android
- Internet - for map tiles and network tools
- Vibrate - haptic feedback
- Post notifications - background operation alerts

## License

GPL-3.0
