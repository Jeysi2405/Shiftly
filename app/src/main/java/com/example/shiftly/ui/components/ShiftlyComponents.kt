package com.example.shiftly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shiftly.ui.theme.AzureMid
import com.example.shiftly.ui.theme.BorderSubtle
import com.example.shiftly.ui.theme.DeepAzureBottom
import com.example.shiftly.ui.theme.DeepBlueContainer
import com.example.shiftly.ui.theme.GlassBacking
import com.example.shiftly.ui.theme.GlassBorderBottom
import com.example.shiftly.ui.theme.GlassBorderTop
import com.example.shiftly.ui.theme.GlassFillBottom
import com.example.shiftly.ui.theme.GlassFillTop
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.OrbAquaSoft
import com.example.shiftly.ui.theme.OrbSkyLight
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.RoseAlert
import com.example.shiftly.ui.theme.RoseContainer
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.SkyBlueTop
import com.example.shiftly.ui.theme.SlateSurface
import com.example.shiftly.ui.theme.SlateSurfaceElevated
import com.example.shiftly.ui.theme.SlateSurfaceVariant
import com.example.shiftly.ui.theme.SoftBlue
import com.example.shiftly.ui.theme.TextPrimary
import com.example.shiftly.ui.theme.TextSecondary

/**
 * Full-bleed calm sky-to-deep-azure gradient backdrop with soft translucent circular shapes.
 */
@Composable
fun ShiftlyBlueGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SkyBlueTop,
                        AzureMid,
                        DeepAzureBottom
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Large soft translucent orb top-right behind greeting/hero
            drawCircle(
                color = OrbSkyLight.copy(alpha = 0.18f),
                radius = size.width * 0.48f,
                center = Offset(x = size.width * 0.88f, y = size.height * 0.10f)
            )
            // Soft aqua-tinted circle mid-left for depth behind hero glass card
            drawCircle(
                color = OrbAquaSoft.copy(alpha = 0.11f),
                radius = size.width * 0.38f,
                center = Offset(x = size.width * 0.10f, y = size.height * 0.28f)
            )
            // Subtle large outer halo top-left
            drawCircle(
                color = PureWhite.copy(alpha = 0.06f),
                radius = size.width * 0.62f,
                center = Offset(x = size.width * 0.20f, y = size.height * 0.02f)
            )
            // Deep azure glow bottom-right
            drawCircle(
                color = OrbSkyLight.copy(alpha = 0.10f),
                radius = size.width * 0.42f,
                center = Offset(x = size.width * 0.92f, y = size.height * 0.48f)
            )
        }
        content()
    }
}

/**
 * Frosted glass container with specular rim highlight, depth backing for WCAG AA white text,
 * and subtle azure-tinted elevation shadow.
 */
@Composable
fun ShiftlyGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = DeepAzureBottom.copy(alpha = 0.35f),
                spotColor = DeepAzureBottom.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(GlassBacking)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GlassFillTop, GlassFillBottom),
                    start = Offset(0f, 0f),
                    end = Offset(700f, 900f)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(GlassBorderTop, GlassBorderBottom),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 600f)
                ),
                shape = shape
            )
            .padding(contentPadding)
    ) {
        Column(content = content)
    }
}

/**
 * Full-width black pill primary action button with refined editorial serif label.
 */
@Composable
fun ShiftlyPrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = InkBlack,
            contentColor = PureWhite,
            disabledContainerColor = InkBlack.copy(alpha = 0.45f),
            disabledContentColor = PureWhite.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = ShiftlySerif,
                fontWeight = FontWeight.SemiBold
            ),
            color = PureWhite
        )
    }
}

/**
 * Standardized Shiftly card container with 22dp corners, crisp border, and gentle elevation.
 */
@Composable
fun ShiftlyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SlateSurface,
    borderColor: Color = BorderSubtle,
    cornerRadius: Dp = 22.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Screen-level header rendered directly over the blue gradient backdrop (white for AA contrast).
 */
@Composable
fun ShiftlyScreenHeader(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = PureWhite
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite.copy(alpha = 0.85f)
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Consistent section header with optional count badge or trailing label.
 */
@Composable
fun ShiftlySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    trailingText: String? = null,
    onLightSurface: Boolean = false
) {
    val titleColor = if (onLightSurface) TextPrimary else PureWhite
    val countBg = if (onLightSurface) SlateSurfaceVariant else PureWhite.copy(alpha = 0.22f)
    val countTextColor = if (onLightSurface) TextSecondary else PureWhite

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
            if (count != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = countBg
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = countTextColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = if (onLightSurface) TextSecondary else PureWhite.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Compact pill badge for semantic states (LIVE, Paid, Unpaid, Logged, etc.).
 */
@Composable
fun ShiftlyStatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingContent != null) {
                leadingContent()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

/**
 * Centered empty state with a tinted circular outline icon well and title.
 */
@Composable
fun ShiftlyEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    iconTint: Color = SoftBlue,
    iconContainerColor: Color = DeepBlueContainer
) {
    ShiftlyCard(
        modifier = modifier,
        contentPadding = 24.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Consistent destructive delete confirmation modal.
 */
@Composable
fun ShiftlyConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RoseContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = RoseAlert,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoseAlert,
                    contentColor = PureWhite
                )
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
