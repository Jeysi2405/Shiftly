package com.example.shiftly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontResource
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.shiftly.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val plusJakartaSansGoogleFont = GoogleFont("Plus Jakarta Sans")

/**
 * Plus Jakarta Sans via Google Fonts downloadable fonts provider with local Inter fallback
 * for offline/preview resilience across all weights and italic styles.
 */
val PlusJakartaSans = FontFamily(
    GoogleFontResource(
        googleFont = plusJakartaSansGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal
    ),
    Font(resId = R.font.inter, weight = FontWeight.Normal),
    GoogleFontResource(
        googleFont = plusJakartaSansGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium
    ),
    Font(resId = R.font.inter, weight = FontWeight.Medium),
    GoogleFontResource(
        googleFont = plusJakartaSansGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(resId = R.font.inter, weight = FontWeight.SemiBold),
    GoogleFontResource(
        googleFont = plusJakartaSansGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold
    ),
    Font(resId = R.font.inter, weight = FontWeight.Bold),
    GoogleFontResource(
        googleFont = plusJakartaSansGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal,
        style = FontStyle.Italic
    ),
    Font(resId = R.font.inter, weight = FontWeight.Normal, style = FontStyle.Italic),
    GoogleFontResource(
        googleFont = plusJakartaSansGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium,
        style = FontStyle.Italic
    ),
    Font(resId = R.font.inter, weight = FontWeight.Medium, style = FontStyle.Italic),
    GoogleFontResource(
        googleFont = plusJakartaSansGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold,
        style = FontStyle.Italic
    ),
    Font(resId = R.font.inter, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    GoogleFontResource(
        googleFont = plusJakartaSansGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold,
        style = FontStyle.Italic
    ),
    Font(resId = R.font.inter, weight = FontWeight.Bold, style = FontStyle.Italic)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.0).sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
)

/**
 * Applies tabular numerals (`tnum`) so live timers, currency totals, and counts
 * maintain fixed horizontal alignment while updating.
 */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
