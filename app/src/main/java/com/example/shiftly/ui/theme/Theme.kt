package com.example.shiftly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ShiftlyDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = TextPrimary,
    primaryContainer = DeepBlueContainer,
    onPrimaryContainer = SoftBlue,
    secondary = SoftBlue,
    onSecondary = ObsidianBackground,
    secondaryContainer = SlateSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = RoseAlert,
    onError = TextPrimary
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