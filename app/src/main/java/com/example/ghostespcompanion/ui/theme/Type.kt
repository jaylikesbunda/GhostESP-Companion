package com.example.ghostespcompanion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ghostespcompanion.R

/**
 * SpaceGrotesk Font Family - Main font for headings and body text
 * Loaded from bundled font resources
 */
val SpaceGroteskFontFamily = FontFamily(
    Font(resId = R.font.spacegrotesk, weight = FontWeight.Black),
    Font(resId = R.font.spacegrotesk, weight = FontWeight.ExtraBold),
    Font(resId = R.font.spacegrotesk, weight = FontWeight.Bold),
    Font(resId = R.font.spacegrotesk, weight = FontWeight.SemiBold),
    Font(resId = R.font.spacegrotesk, weight = FontWeight.Medium),
    Font(resId = R.font.spacegrotesk, weight = FontWeight.Normal),
    Font(resId = R.font.spacegrotesk, weight = FontWeight.Light),
    Font(resId = R.font.spacegrotesk, weight = FontWeight.ExtraLight),
    Font(resId = R.font.spacegrotesk, weight = FontWeight.Thin)
)

/**
 * Doto Font Family - Subtext font for labels, captions, and secondary text
 * Loaded from bundled font resources
 */
val DotoFontFamily = FontFamily(
    Font(resId = R.font.doto, weight = FontWeight.Black),
    Font(resId = R.font.doto, weight = FontWeight.ExtraBold),
    Font(resId = R.font.doto, weight = FontWeight.Bold),
    Font(resId = R.font.doto, weight = FontWeight.SemiBold),
    Font(resId = R.font.doto, weight = FontWeight.Medium),
    Font(resId = R.font.doto, weight = FontWeight.Normal),
    Font(resId = R.font.doto, weight = FontWeight.Light),
    Font(resId = R.font.doto, weight = FontWeight.ExtraLight),
    Font(resId = R.font.doto, weight = FontWeight.Thin)
)

/**
 * GhostESP Companion Typography
 * 
 * Modern typography with SpaceGrotesk for main content
 * and Doto for subtext elements. Features strong weight contrasts
 * and generous spacing for excellent readability.
 */
val GhostESPTypography = Typography(
    // ============================================
    // DISPLAY - Large impact headlines (SpaceGrotesk)
    // ============================================
    displayLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // ============================================
    // HEADLINE - Section headers (SpaceGrotesk)
    // ============================================
    headlineLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // ============================================
    // TITLE - Card titles, app bars (SpaceGrotesk)
    // ============================================
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // ============================================
    // BODY - Main content (SpaceGrotesk)
    // ============================================
    bodyLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DotoFontFamily,  // Doto for subtext
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    
    // ============================================
    // LABEL - Buttons, chips, tabs (Doto for subtext)
    // ============================================
    labelLarge = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

/**
 * Monospace typography for terminal/code display
 * Uses SpaceGrotesk for consistent code display
 */
val MonospaceTextStyle = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)

/**
 * Button Text Style
 * Bold, slightly larger for impactful buttons
 */
val BrutalistButtonText = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.25.sp
)

/**
 * Caption Style
 * For small labels and tags with bold presence (Doto)
 */
val BrutalistCaption = TextStyle(
    fontFamily = DotoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.5.sp
)
