package com.example.shiftly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun ShiftlyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShiftlyDarkColorScheme,
        typography = ShiftlyTypography,
        content = content
    )
}
