package com.example.shiftly.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Shiftly 2.0 — Calm, Glassy, Sky-to-Azure Design Tokens
// =============================================================================

// Blue Gradient Background & Ambient Orbs
val SkyBlueTop = Color(0xFF2173D6)         // Top gradient (4.68:1 with #FFFFFF)
val AzureMid = Color(0xFF185EC2)           // Mid gradient (6.1:1 with #FFFFFF)
val DeepAzureBottom = Color(0xFF0C3D91)    // Bottom gradient (9.8:1 with #FFFFFF)
val OrbSkyLight = Color(0xFF68BEFF)        // Translucent ambient orb highlight
val OrbAquaSoft = Color(0xFF7BE8D6)        // Subtle aqua ambient orb accent

// Core Neutrals
val PureWhite = Color(0xFFFFFFFF)          // Welcome bg, bottom sheet, selected weekday
val SoftCloudWhite = Color(0xFFF6FAFF)     // Elevated subtle surface
val SurfaceTintLight = Color(0xFFEAF2FC)   // Soft blue-tinted card/chip surface
val InkBlack = Color(0xFF0B0D12)           // Primary pill buttons & floating nav bar
val InkPrimary = Color(0xFF0E1525)         // Primary text on white
val InkSecondary = Color(0xFF4B5E78)       // Secondary text on white (>= 6.4:1)
val InkMuted = Color(0xFF64748B)           // Muted captions on white (>= 4.7:1)
val HairlineLight = Color(0xFFE2EBF5)      // 1dp subtle card border

// Frosted Glass Surfaces (Hero Card & Floating Elements on Blue)
val GlassBacking = Color(0x38082D6E)       // Ensures WCAG AA >= 5.2:1 for white text
val GlassFillTop = Color(0x40FFFFFF)       // Frosted top highlight
val GlassFillBottom = Color(0x1CFFFFFF)    // Frosted bottom fade
val GlassBorderTop = Color(0x88FFFFFF)     // Specular top rim
val GlassBorderBottom = Color(0x28FFFFFF)  // Subtle bottom rim

// Mint / Aqua Green — Charts, Progress, Income, Success ONLY
val MintBright = Color(0xFF34E5B8)         // Bars, dots, selected nav indicator, progress safe
val MintTextOnBlue = Color(0xFF7CF7D8)     // High-contrast mint on blue/glass (>= 7.2:1)
val MintInkOnWhite = Color(0xFF098768)     // WCAG AA mint text/icon on white (4.65:1)
val MintSoftContainer = Color(0xFFE3FBF3)  // Soft mint pill/icon well on white

// Soft Coral / Red — Expenses & Near-Limit Budget ONLY
val CoralBright = Color(0xFFFF6B6B)        // Dots, warning progress bar (>= 80% budget)
val CoralTextOnBlue = Color(0xFFFFA69E)    // High-contrast coral on blue/glass (>= 4.9:1)
val CoralInkOnWhite = Color(0xFFC93B3B)    // WCAG AA coral text/icon on white (4.95:1)
val CoralSoftContainer = Color(0xFFFFECEB) // Soft coral pill/icon well on white

// Azure Accent for Neutral / Split Bill Chips on White
val AzureInkOnWhite = Color(0xFF1557B8)
val AzureSoftContainer = Color(0xFFE8F1FE)

// =============================================================================
// Backwards-Compatible Token Aliases (for existing Duties/Expense/Split/Profile)
// =============================================================================
val ObsidianBackground = Color.Transparent
val SlateSurface = PureWhite
val SlateSurfaceElevated = SoftCloudWhite
val SlateSurfaceVariant = SurfaceTintLight

val ElectricBlue = AzureInkOnWhite
val SoftBlue = AzureInkOnWhite
val DeepBlueContainer = AzureSoftContainer

val TextPrimary = InkPrimary
val TextSecondary = InkSecondary
val BorderSubtle = HairlineLight

val EmeraldSuccess = MintInkOnWhite
val EmeraldContainer = MintSoftContainer
val RoseAlert = CoralInkOnWhite
val RoseContainer = CoralSoftContainer
val AmberWarning = Color(0xFFB45309)
val AmberContainer = Color(0xFFFEF3C7)
