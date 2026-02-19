package com.example.ghostespcompanion.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Helper function to find the Activity from a Context
 * Safely unwraps ContextWrapper to find the underlying Activity
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Light Color Scheme for GhostESP Companion
 * Neo-brutalist light theme with bold accents
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A),  // Dark gray for light mode
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    secondary = Color(0xFF505050),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFF707070),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0F0F0),
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE5E5E5)
)

/**
 * Dark Color Scheme for GhostESP Companion (DEFAULT)
 * Soft neo-brutalist dark theme with vibrant accents and signature teal borders
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainerDark,
    error = ErrorDark,
    onError = OnError,
    errorContainer = ErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,  // Signature teal border
    outlineVariant = OutlineVariantDark
)

/**
 * GhostESP Companion Theme
 * 
 * Soft Neo-Brutalism design with dark mode as default.
 * Features bold accent colors, high contrast elements, and signature teal borders.
 * 
 * @param darkTheme Whether to use dark theme (defaults to TRUE for neo-brutalist aesthetic)
 * @param dynamicColor Whether to use dynamic colors (Material You) - disabled for consistent branding
 * @param content The content to theme
 */
@Composable
fun GhostESPTheme(
    darkTheme: Boolean = true,  // DARK MODE DEFAULT - key neo-brutalist feature
    dynamicColor: Boolean = false, // Disabled for consistent GhostESP branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            activity?.window?.let { window ->
                // Transparent status bar for edge-to-edge
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                // Transparent navigation bar for edge-to-edge
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GhostESPTypography,
        content = content
    )
}
