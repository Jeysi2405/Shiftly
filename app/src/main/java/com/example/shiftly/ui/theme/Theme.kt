package com.example.shiftly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ShiftlyLightColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = PureWhite,
    primaryContainer = AzureSoftContainer,
    onPrimaryContainer = AzureInkOnWhite,
    secondary = MintInkOnWhite,
    onSecondary = PureWhite,
    secondaryContainer = MintSoftContainer,
    onSecondaryContainer = MintInkOnWhite,
    tertiary = AzureInkOnWhite,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceTintLight,
    onSurfaceVariant = InkSecondary,
    outline = HairlineLight,
    error = CoralInkOnWhite,
    onError = PureWhite,
    errorContainer = CoralSoftContainer,
    onErrorContainer = CoralInkOnWhite
)

@Composable
fun ShiftlyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShiftlyLightColorScheme,
        typography = ShiftlyTypography,
        content = content
    )
}
