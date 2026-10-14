package com.perenniasystems.lascito.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R

// ── Font Families ─────────────────────────────────────────────────────────────
/**
 * Playfair Display — Serif typeface for display/headline text.
 * Conveys elegance and gravitas befitting a memorial application.
 * Font files must be placed in res/font/ (see project README for download link).
 */
val PlayfairDisplay = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal),
    Font(R.font.playfair_display_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.playfair_display_bold, FontWeight.Bold),
)

/**
 * Lato — Clean sans-serif for body text, labels and secondary information.
 */
val Lato = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_light, FontWeight.Light),
    Font(R.font.lato_bold, FontWeight.Bold),
)

// ── Typography Scale ───────────────────────────────────────────────────────────
val LascitoTypography = Typography(
    // Titles – Playfair Display Serif
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = TextSecondary
    ),
    titleLarge = TextStyle(
        fontFamily = Lato,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = Lato,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    // Body – Lato Sans-serif
    bodyLarge = TextStyle(
        fontFamily = Lato,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = Lato,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = Lato,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily   = Lato,
        fontWeight   = FontWeight.Bold,
        fontSize     = 14.sp,
        letterSpacing = 1.5.sp
        // No color here — inherits LocalContentColor so buttons can set their own text color
    ),
    labelMedium = TextStyle(
        fontFamily = Lato,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
        color = TextSecondary
    ),
)
