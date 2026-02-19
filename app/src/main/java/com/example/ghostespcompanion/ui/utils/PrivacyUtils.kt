package com.example.ghostespcompanion.ui.utils

/**
 * Utility object for censoring PII (Personally Identifiable Information)
 * Used when privacy mode is enabled to hide sensitive data in screenshots/screen recordings
 */
object PrivacyUtils {
    
    /**
     * Censor a MAC address by showing only the first and last octets
     * Example: AA:BB:CC:DD:EE:FF -> AA:XX:XX:XX:XX:FF
     */
    fun censorMacAddress(mac: String?): String {
        if (mac.isNullOrBlank()) return "—"
        val parts = mac.split(":")
        if (parts.size != 6) return censorText(mac, 2)
        return "${parts[0]}:XX:XX:XX:XX:${parts[5]}"
    }
    
    /**
     * Censor an IP address by hiding the last octets
     * Example: 192.168.1.100 -> 192.168.X.X
     */
    fun censorIpAddress(ip: String?): String {
        if (ip.isNullOrBlank()) return "—"
        val parts = ip.split(".")
        if (parts.size != 4) return censorText(ip, 2)
        return "${parts[0]}.${parts[1]}.X.X"
    }
    
    /**
     * Censor a BSSID (similar to MAC address)
     */
    fun censorBssid(bssid: String?): String {
        return censorMacAddress(bssid)
    }
    
    /**
     * Censor an SSID by showing only first few characters
     * Example: "MyHomeNetwork" -> "MyH***"
     */
    fun censorSsid(ssid: String?): String {
        if (ssid.isNullOrBlank()) return "—"
        if (ssid.length <= 3) return "***"
        return "${ssid.take(3)}${"*".repeat(ssid.length - 3)}"
    }
    
    /**
     * Censor a device name
     * Example: "John's iPhone" -> "Joh***"
     */
    fun censorDeviceName(name: String?): String {
        if (name.isNullOrBlank()) return "—"
        if (name.length <= 3) return "***"
        return "${name.take(3)}${"*".repeat(minOf(name.length - 3, 10))}"
    }
    
    /**
     * Censor email address
     * Example: "john.doe@example.com" -> "joh***@e***.com"
     */
    fun censorEmail(email: String?): String {
        if (email.isNullOrBlank()) return "—"
        val parts = email.split("@")
        if (parts.size != 2) return censorText(email, 2)
        
        val localPart = parts[0]
        val domainParts = parts[1].split(".")
        
        val censoredLocal = if (localPart.length <= 2) {
            "${localPart.first()}***"
        } else {
            "${localPart.take(2)}***"
        }
        
        val censoredDomain = if (domainParts.size >= 2) {
            val tld = domainParts.last()
            val domain = domainParts.dropLast(1).joinToString(".")
            "${domain.first()}***.$tld"
        } else {
            "${parts[1].first()}***"
        }
        
        return "$censoredLocal@$censoredDomain"
    }
    
    /**
     * Censor a phone number
     * Example: "+1 555-123-4567" -> "+1 ***-***-4567"
     */
    fun censorPhoneNumber(phone: String?): String {
        if (phone.isNullOrBlank()) return "—"
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 4) return "***"
        val lastFour = digits.takeLast(4)
        val prefix = phone.takeWhile { !it.isDigit() }.take(3)
        return "$prefix***-$lastFour"
    }
    
    /**
     * Censor generic text by showing only first N characters
     * Example: censorText("Hello World", 3) -> "Hel***"
     */
    fun censorText(text: String?, visibleChars: Int = 3): String {
        if (text.isNullOrBlank()) return "—"
        if (text.length <= visibleChars) return "*".repeat(text.length)
        return "${text.take(visibleChars)}${"*".repeat(minOf(text.length - visibleChars, 10))}"
    }
    
    /**
     * Censor NFC UID
     * Example: "04:A3:B2:C1:D4:E5:F6" -> "04:XX:XX:XX:XX:XX:F6"
     */
    fun censorNfcUid(uid: String?): String {
        if (uid.isNullOrBlank()) return "—"
        val parts = uid.split(":")
        if (parts.size < 2) return censorText(uid, 2)
        return "${parts[0]}:${"XX:".repeat(parts.size - 2)}${parts.last()}"
    }
    
    /**
     * Censor BLE UUID
     * Example: "12345678-1234-1234-1234-123456789abc" -> "12345***-****-****-****-***9abc"
     */
    fun censorBleUuid(uuid: String?): String {
        if (uuid.isNullOrBlank()) return "—"
        val parts = uuid.split("-")
        if (parts.size != 5) return censorText(uuid, 4)
        
        val first = parts[0]
        val last = parts[4]
        
        return "${first.take(4)}***-****-****-****-${last.takeLast(4)}"
    }
    
    /**
     * Censor coordinates (GPS location)
     * Example: "37.7749, -122.4194" -> "37.XXXX, -122.XXXX"
     */
    fun censorCoordinates(lat: Double?, lon: Double?): String {
        if (lat == null || lon == null) return "—"
        // Round to 1 decimal place to obscure exact location
        val censoredLat = kotlin.math.round(lat * 10.0) / 10.0
        val censoredLon = kotlin.math.round(lon * 10.0) / 10.0
        return "$censoredLat, $censoredLon"
    }
    
    /**
     * Censor password/credentials - always fully hidden
     */
    fun censorPassword(password: String?): String {
        if (password.isNullOrBlank()) return "—"
        return "••••••••"
    }
    
    /**
     * Apply privacy filter to a value based on the type
     */
    fun <T> applyPrivacy(value: T, privacyMode: Boolean, censorFn: (T) -> String, displayFn: (T) -> String): String {
        return if (privacyMode) censorFn(value) else displayFn(value)
    }
}

/**
 * Extension functions for common use cases
 */

/**
 * Censor MAC address if privacy mode is enabled
 */
fun String?.censorMac(privacyMode: Boolean): String {
    return if (privacyMode) PrivacyUtils.censorMacAddress(this) else (this ?: "—")
}

/**
 * Censor IP address if privacy mode is enabled
 */
fun String?.censorIp(privacyMode: Boolean): String {
    return if (privacyMode) PrivacyUtils.censorIpAddress(this) else (this ?: "—")
}

/**
 * Censor SSID if privacy mode is enabled
 */
fun String?.censorSsid(privacyMode: Boolean): String {
    return if (privacyMode) PrivacyUtils.censorSsid(this) else (this ?: "—")
}

/**
 * Censor device name if privacy mode is enabled
 */
fun String?.censorDevice(privacyMode: Boolean): String {
    return if (privacyMode) PrivacyUtils.censorDeviceName(this) else (this ?: "—")
}

/**
 * Censor NFC UID if privacy mode is enabled
 */
fun String?.censorNfc(privacyMode: Boolean): String {
    return if (privacyMode) PrivacyUtils.censorNfcUid(this) else (this ?: "—")
}

/**
 * Censor BLE UUID if privacy mode is enabled
 */
fun String?.censorBleUuid(privacyMode: Boolean): String {
    return if (privacyMode) PrivacyUtils.censorBleUuid(this) else (this ?: "—")
}

/**
 * Censor generic text if privacy mode is enabled
 */
fun String?.censorText(privacyMode: Boolean, visibleChars: Int = 3): String {
    return if (privacyMode) PrivacyUtils.censorText(this, visibleChars) else (this ?: "—")
}
