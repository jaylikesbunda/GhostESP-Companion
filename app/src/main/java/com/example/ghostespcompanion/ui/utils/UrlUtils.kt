package com.example.ghostespcompanion.ui.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Utility functions for URL handling
 */

/**
 * Opens a URL in the default browser
 */
fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Composable function to get URL opener
 */
@Composable
fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return { url -> openUrl(context, url) }
}

/**
 * GhostESP related URLs
 */
object GhostESPUrls {
    const val DOCUMENTATION = "https://docs.ghostesp.net"
    const val GITHUB_REPO = "https://github.com/jaylikesbunda/Ghost_ESP"
    const val GITHUB_RELEASES = "$GITHUB_REPO/releases"
}
