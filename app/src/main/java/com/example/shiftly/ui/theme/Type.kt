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

private val interGoogleFont = GoogleFont("Inter")

/**
 * Inter via Google Fonts downloadable fonts provider with local Inter fallback
 * for offline/preview resilience across all weights and italic styles.
 */
val InterFontFamily = FontFamily(
    GoogleFontResource(
        googleFont = interGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal
    ),
    Font(resId = R.font.inter, weight = FontWeight.Normal),
    GoogleFontResource(
        googleFont = interGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium
    ),
    Font(resId = R.font.inter, weight = FontWeight.Medium),
    GoogleFontResource(
        googleFont = interGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(resId = R.font.inter, weight = FontWeight.SemiBold),
    GoogleFontResource(
        googleFont = interGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold
    ),
    Font(resId = R.font.inter, weight = FontWeight.Bold),
    GoogleFontResource(
        googleFont = interGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal,
        style = FontStyle.Italic
    ),
    Font(resId = R.font.inter, weight = FontWeight.Normal, style = FontStyle.Italic),
    GoogleFontResource(
        googleFont = interGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium,
        style = FontStyle.Italic
    ),
    Font(resId = R.font.inter, weight = FontWeight.Medium, style = FontStyle.Italic),
    GoogleFontResource(
        googleFont = interGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold,
        style = FontStyle.Italic
    ),
    Font(resId = R.font.inter, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    GoogleFontResource(
        googleFont = interGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold,
        style = FontStyle.Italic
    ),
    Font(resId = R.font.inter, weight = FontWeight.Bold, style = FontStyle.Italic)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    ),
    // iOS Large Title: 34sp Bold, -0.4sp tracking
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    // iOS Headline: 17sp SemiBold
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    // iOS Body: 15-17sp Regular
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // iOS Caption: 12-13sp Medium
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

/**
 * Applies tabular numerals (`tnum`) so live timers, currency totals, and counts
 * maintain fixed horizontal alignment while updating.
 */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
