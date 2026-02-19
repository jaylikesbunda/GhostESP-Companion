package com.example.ghostespcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * GhostESP Companion Color Palette
 * 
 * Minimalist Neo-Brutalism design with dark mode as default.
 * Clean white/gray accents on deep black background.
 * Professional and modern aesthetic.
 */

// ============================================
// PRIMARY - Main accent
// ============================================
// Dark mode: white on black
val Primary = Color(0xFFFFFFFF)  // Pure white
val PrimaryDark = Color(0xFFE0E0E0)  // Slightly dimmed white
// Light mode: dark on white
val PrimaryLight = Color(0xFF1A1A1A)  // Near black
val OnPrimary = Color(0xFF0A0A0A)  // Dark text on white
val PrimaryContainer = Color(0xFFFFFFFF).copy(alpha = 0.08f)  // Subtle tint
val PrimaryContainerDark = Color(0xFFFFFFFF).copy(alpha = 0.1f)

// ============================================
// SECONDARY - Subtle accent
// ============================================
// Dark mode: light gray on black
val Secondary = Color(0xFFB0B0B0)  // Light gray
val SecondaryDark = Color(0xFFD0D0D0)  // Brighter gray
// Light mode: medium gray on white
val SecondaryLight = Color(0xFF606060)
val OnSecondary = Color(0xFF0A0A0A)
val SecondaryContainer = Color(0xFFB0B0B0).copy(alpha = 0.08f)
val SecondaryContainerDark = Color(0xFFB0B0B0).copy(alpha = 0.1f)

// ============================================
// TERTIARY - Neutral accent
// ============================================
// Dark mode: medium gray on black
val Tertiary = Color(0xFF808080)  // Medium gray
val TertiaryDark = Color(0xFFA0A0A0)
// Light mode: darker gray on white
val TertiaryLight = Color(0xFF505050)
val OnTertiary = Color(0xFFF5F5F5)
val TertiaryContainer = Color(0xFF808080).copy(alpha = 0.08f)
val TertiaryContainerDark = Color(0xFF808080).copy(alpha = 0.1f)

// ============================================
// ERROR - Muted Red (Not too bright)
// ============================================
val Error = Color(0xFFDC554F)  // Muted red
val ErrorDark = Color(0xFFE57373)  // Softer red
val ErrorLight = Color(0xFFC62828)  // Darker red for light mode
val OnError = Color.White
val ErrorContainer = Color(0xFFDC554F).copy(alpha = 0.1f)
val ErrorContainerDark = Color(0xFFDC554F).copy(alpha = 0.15f)

// ============================================
// SUCCESS - Subtle Green
// ============================================
val Success = Color(0xFF6B9B6B)  // Muted green
val SuccessDark = Color(0xFF8BC48B)
val SuccessLight = Color(0xFF2E7D32)  // Darker green for light mode
val OnSuccess = Color(0xFF0A0A0A)

// ============================================
// WARNING - Subtle Orange/Yellow
// ============================================
val Warning = Color(0xFFE0A045)  // Muted orange
val WarningDark = Color(0xFFF0B860)
val WarningLight = Color(0xFFE65100)  // Darker orange for light mode
val OnWarning = Color(0xFF0A0A0A)

// ============================================
// BACKGROUND - Deep Black (Default)
// ============================================
// Dark Mode Colors (Default theme)
val BackgroundDark = Color(0xFF0A0A0A)  // Near black - main background
val BackgroundDarkElevated = Color(0xFF141414)  // Slightly lighter for cards
val BackgroundDarkSurface = Color(0xFF1A1A1A)  // Surface elevation
val OnBackgroundDark = Color(0xFFF5F5F5)  // Off-white text

// Light Mode Colors (Secondary theme)
val Background = Color(0xFFFAFAFA)  // Off-white
val BackgroundElevated = Color.White
val OnBackground = Color(0xFF0A0A0A)

// ============================================
// SURFACE - Card/Container backgrounds
// ============================================
// Dark Mode Surfaces
val SurfaceDark = Color(0xFF111111)  // Card background - very dark
val SurfaceDarkVariant = Color(0xFF1A1A1A)  // Alternative surface
val SurfaceVariantDark = SurfaceDarkVariant  // Alias for consistency
val SurfaceDarkElevated = Color(0xFF202020)  // Elevated surfaces
val OnSurfaceDark = Color(0xFFE5E5E5)  // Light gray text
val OnSurfaceVariantDark = Color(0xFF808080)  // Muted text

// Light Mode Surfaces
val Surface = Color.White
val SurfaceVariant = Color(0xFFF0F0F0)
val OnSurface = Color(0xFF1A1A1A)
val OnSurfaceVariant = Color(0xFF606060)

// ============================================
// OUTLINE - Borders and dividers
// ============================================
val Outline = Color(0xFF2A2A2A)  // Dark borders (light mode)
val OutlineDark = Color(0xFF404040)  // Light gray borders (dark mode)
val OutlineVariant = Color(0xFFE5E5E5)
val OutlineVariantDark = Color(0xFF252525)

// Accent colors for borders - minimal white/gray
val BorderAccent = Color(0xFFFFFFFF)  // White border accent
val BorderAccentSecondary = Color(0xFFB0B0B0)  // Gray accent
val BorderAccentWarning = Color(0xFF808080)  // Gray warning

// ============================================
// SHADOW COLORS
// ============================================
val ShadowColor = Color(0xFF000000)  // Black shadow
val ShadowColorDark = Color(0xFFFFFFFF).copy(alpha = 0.1f)  // Subtle white shadow

// ============================================
// TEXT COLORS
// ============================================
val InkPrimary = Color(0xFF0A0A0A)  // Primary text color
val InkSecondary = Color(0xFF505050)  // Secondary text
val InkMuted = Color(0xFF808080)  // Muted/disabled text
val InkPrimaryDark = Color(0xFFF5F5F5)  // Primary text (dark mode)
val InkSecondaryDark = Color(0xFFA0A0A0)  // Secondary text (dark mode)
val InkMutedDark = Color(0xFF707070)  // Muted text (dark mode)

// ============================================
// CARD COLORS
// ============================================
val CardBackground = Color.White
val CardBackgroundDark = Color(0xFF111111)
val CardBorderDark = Color(0xFF333333)  // Subtle border

// ============================================
// WIFI SIGNAL COLORS (Functional - Muted)
// ============================================
val SignalExcellent = Color(0xFF6B9B6B)  // Muted green
val SignalGood = Color(0xFF8BC48B)       // Light green
val SignalFair = Color(0xFFA0A0A0)        // Gray
val SignalWeak = Color(0xFFDC554F)        // Muted red

// ============================================
// SECURITY COLORS (Functional - Muted)
// ============================================
val SecurityOpen = Color(0xFFA0A0A0)      // Gray - Neutral
val SecurityWPA2 = Color(0xFF6B9B6B)     // Muted green - Secure
val SecurityWPA3 = Color(0xFFFFFFFF)     // White - Most secure
val SecurityWEP = Color(0xFFDC554F)      // Muted red - Weak

// ============================================
// CONNECTION STATUS COLORS
// ============================================
val StatusConnected = Color(0xFF6B9B6B)
val StatusDisconnected = Color(0xFF606060)
val StatusConnecting = Color(0xFFE0E0E0)
val StatusError = Color(0xFFDC554F)

// ============================================
// GHOST ESP BRAND COLORS - Minimal
// ============================================
val GhostWhite = Color(0xFFFFFFFF)
val GhostGray = Color(0xFF808080)
val GhostDark = Color(0xFF1A1A1A)

// ============================================
// THEME-AWARE COLOR EXTENSIONS
// Use these in Composables for proper light/dark mode support
// ============================================

/**
 * Check if the current theme is dark mode
 */
@Composable
fun isDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background == BackgroundDark

/**
 * Get the appropriate Primary accent color based on theme
 */
@Composable
fun primaryColor(): Color = if (isDarkTheme()) Primary else PrimaryLight

/**
 * Get the appropriate Secondary accent color based on theme
 */
@Composable
fun secondaryColor(): Color = if (isDarkTheme()) Secondary else SecondaryLight

/**
 * Get the appropriate Tertiary accent color based on theme
 */
@Composable
fun tertiaryColor(): Color = if (isDarkTheme()) Tertiary else TertiaryLight

/**
 * Get the appropriate Success color based on theme
 */
@Composable
fun successColor(): Color = if (isDarkTheme()) Success else SuccessLight

/**
 * Get the appropriate Error color based on theme
 */
@Composable
fun errorColor(): Color = if (isDarkTheme()) Error else ErrorLight

/**
 * Get the appropriate Warning color based on theme
 */
@Composable
fun warningColor(): Color = if (isDarkTheme()) Warning else WarningLight

/**
 * Get the appropriate OnPrimary color based on theme
 */
@Composable
fun onPrimaryColor(): Color = if (isDarkTheme()) OnPrimary else Color.White
