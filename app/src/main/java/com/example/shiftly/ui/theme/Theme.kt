package com.example.shiftly.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ShiftlyDarkColorScheme = darkColorScheme(
    primary = AzureAccent,
    onPrimary = InkBlack,
    primaryContainer = AzureDarkContainer,
    onPrimaryContainer = AzureAccent,
    secondary = MintBright,
    onSecondary = InkBlack,
    secondaryContainer = MintSoftContainer,
    onSecondaryContainer = MintTextOnBlue,
    tertiary = AzureAccent,
    onTertiary = InkBlack,
    background = DarkBackground,
    onBackground = TextLightPrimary,
    surface = DarkSurface,
    onSurface = TextLightPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextLightSecondary,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkCard,
    surfaceContainerHighest = DarkCardElevated,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = CoralBright,
    onError = InkBlack,
    errorContainer = CoralSoftContainer,
    onErrorContainer = CoralTextOnBlue
)

private val ShiftlyLightColorScheme = lightColorScheme(
    primary = AzurePrimaryButton,
    onPrimary = PureWhite,
    primaryContainer = AzureSoftContainerLight,
    onPrimaryContainer = AzureAccentLight,
    secondary = MintBrightLight,
    onSecondary = PureWhite,
    secondaryContainer = MintSoftContainerLight,
    onSecondaryContainer = MintInkLight,
    tertiary = AzurePrimaryButton,
    onTertiary = PureWhite,
    background = LightBackground,
    onBackground = TextDarkPrimary,
    surface = LightSurface,
    onSurface = TextDarkPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextDarkSecondary,
    surfaceContainerLowest = LightBackground,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightCard,
    surfaceContainerHighest = LightCardElevated,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = CoralBrightLight,
    onError = PureWhite,
    errorContainer = CoralSoftContainerLight,
    onErrorContainer = CoralInkLight
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ShiftlyTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode.uppercase()) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) ShiftlyDarkColorScheme else ShiftlyLightColorScheme
    val shiftlyColors = if (isDark) DarkShiftlyColors else LightShiftlyColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalShiftlyColors provides shiftlyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ShiftlyTypography,
            content = content
        )
    }
}
