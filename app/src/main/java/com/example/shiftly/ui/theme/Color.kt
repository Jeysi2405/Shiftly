package com.example.shiftly.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Shiftly Dark Mode Design Tokens
// =============================================================================

// Core Dark Surfaces & Borders
val DarkBackground = Color(0xFF121212)       // Main screen background
val DarkSurface = Color(0xFF1E1E1E)          // Secondary background, sheets, glass cards
val DarkCard = Color(0xFF252525)             // Elevated cards, tiles, chips
val DarkCardElevated = Color(0xFF2C2C2C)     // Nested elevated rows / hover-like containers
val DarkBorder = Color(0xFF333333)           // Subtle dark gray borders and dividers
val DarkDisabledContainer = Color(0xFF2C2F36)
val DarkDisabledContent = Color(0xFF8A909A)

// Dark Mode Text Hierarchy
val TextLightPrimary = Color(0xFFF5F5F5)     // Soft white primary text
val TextLightSecondary = Color(0xFFA0A0A0)   // Muted gray secondary text
val TextLightMuted = Color(0xFF8A8A8A)       // Subtle captions & placeholders

// Brand Azure Accents (Preserved & Adapted for Dark Mode)
val SkyBlueTop = Color(0xFF2173D6)           // Brand azure accent
val AzurePrimaryButton = Color(0xFF2173D6)   // Primary pill CTA button fill
val AzureMid = Color(0xFF185EC2)
val DeepAzureBottom = Color(0xFF0C3D91)
val AzureAccent = Color(0xFF4C9AFF)          // High-contrast azure text/icons/borders on dark
val AzureDarkContainer = Color(0xFF1A2B42)   // Dark tinted azure container
val OrbSkyLight = Color(0xFF68BEFF)
val OrbAquaSoft = Color(0xFF7BE8D6)

// Core Neutrals (Kept for explicit semantic/foreground or contrast usage)
val PureWhite = Color(0xFFFFFFFF)
val SoftCloudWhite = DarkCard
val SurfaceTintLight = DarkCard
val InkBlack = Color(0xFF0B0D12)
val InkPrimary = TextLightPrimary
val InkSecondary = TextLightSecondary
val InkMuted = TextLightMuted
val HairlineLight = DarkBorder

// Dark Frosted Glass & Bottom Bar Surfaces
val GlassBacking = DarkSurface
val GlassFillTop = Color(0x14FFFFFF)
val GlassFillBottom = Color(0x05FFFFFF)
val GlassBorderTop = Color(0x28FFFFFF)
val GlassBorderBottom = Color(0x14FFFFFF)
val GlassNavTop = Color(0xE024272E)
val GlassNavBottom = Color(0xEB17191E)

// Mint / Aqua Green — Charts, Progress, Income, Success ONLY
val MintBright = Color(0xFF34E5B8)           // Bars, dots, selected nav indicator, progress safe
val MintTextOnBlue = Color(0xFF7CF7D8)       // High-contrast mint on dark
val MintInkOnWhite = Color(0xFF34E5B8)       // High-contrast mint on dark cards
val MintSoftContainer = Color(0xFF132B24)    // Dark tinted mint pill/icon well

// Soft Coral / Red — Expenses & Near-Limit Budget ONLY
val CoralBright = Color(0xFFFF6B6B)          // Bars, dots, warning progress (>= 80% budget)
val CoralTextOnBlue = Color(0xFFFF9E96)      // High-contrast coral on dark
val CoralInkOnWhite = Color(0xFFFF7A7A)      // High-contrast coral on dark cards
val CoralSoftContainer = Color(0xFF331B1D)   // Dark tinted coral pill/icon well

// Azure Accent for Neutral / Split Bill Chips
val AzureInkOnWhite = AzureAccent
val AzureSoftContainer = AzureDarkContainer

// =============================================================================
// Token Aliases (Mapped to Dark Mode Hierarchy)
// =============================================================================
val ObsidianBackground = DarkBackground
val SlateSurface = DarkCard
val SlateSurfaceElevated = DarkCard
val SlateSurfaceVariant = DarkSurface

val ElectricBlue = AzureAccent
val SoftBlue = AzureAccent
val DeepBlueContainer = AzureDarkContainer

val TextPrimary = TextLightPrimary
val TextSecondary = TextLightSecondary
val BorderSubtle = DarkBorder

val EmeraldSuccess = MintBright
val EmeraldContainer = MintSoftContainer
val RoseAlert = CoralInkOnWhite
val RoseContainer = CoralSoftContainer
val AmberWarning = Color(0xFFFBBF24)
val AmberContainer = Color(0xFF332813)
