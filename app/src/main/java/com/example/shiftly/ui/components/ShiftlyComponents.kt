package com.example.shiftly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shiftly.R
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Full-bleed screen backdrop reacting to the active Shiftly theme.
 */
@Composable
fun ShiftlyBlueGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        content()
    }
}

/**
 * Standardized top drag handle for anchored sheet containers with uniform vertical rhythm.
 */
@Composable
fun ShiftlySheetDragHandle(modifier: Modifier = Modifier) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(if (colors.isDark) Color.White.copy(alpha = 0.16f) else colors.border)
        )
    }
}

/**
 * Elevated container with crisp subtle border and zero box shadow.
 */
@Composable
fun ShiftlyGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    showBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ShiftlyColors.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = 1.dp,
                        color = colors.cardBorder,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .padding(contentPadding)
    ) {
        Column(content = content)
    }
}

/**
 * Full-width pill primary action button.
 */
@Composable
fun ShiftlyPrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = ShiftlyColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.azurePrimary,
            contentColor = colors.onAzurePrimary,
            disabledContainerColor = colors.disabledContainer,
            disabledContentColor = colors.disabledContent
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
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
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (enabled) colors.onAzurePrimary else colors.disabledContent
        )
    }
}

/**
 * Standardized Shiftly card container with 22dp corners, crisp border, and zero box shadow.
 */
@Composable
fun ShiftlyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ShiftlyColors.current.card,
    borderColor: Color = ShiftlyColors.current.cardBorder,
    cornerRadius: Dp = 22.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Screen-level header rendered over the active Shiftly backdrop.
 */
@Composable
fun ShiftlyScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = "",
    onBackClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_chevron_left),
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Reusable `< MMM yyyy >` + `"All"` month filter bar for Expenses & Split Bill screens.
 */
@Composable
fun ShiftlyMonthFilterBar(
    selectedMonth: YearMonth?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAllMonths: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    val monthLabel = selectedMonth?.format(monthFormatter) ?: "All Months"
    val isAllSelected = selectedMonth == null

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.cardBorder),
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_chevron_left),
                        contentDescription = "Previous month",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_chevron_right),
                        contentDescription = "Next month",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isAllSelected) colors.azurePrimary else colors.surface)
                .border(
                    width = 1.dp,
                    color = if (isAllSelected) colors.azureAccent else colors.cardBorder,
                    shape = RoundedCornerShape(50)
                )
                .clickable(role = Role.Tab) { onToggleAllMonths() }
                .semantics { selected = isAllSelected }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "All",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (isAllSelected) colors.onAzurePrimary else colors.textSecondary
            )
        }
    }
}

/**
 * Consistent section header with optional count badge or trailing label/action.
 */
@Composable
fun ShiftlySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    onLightSurface: Boolean = false
) {
    val colors = ShiftlyColors.current
    val titleColor = colors.textPrimary
    val countBg = if (onLightSurface) colors.card else colors.surface
    val countTextColor = colors.textSecondary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                modifier = Modifier.semantics { heading() }
            )
            if (count != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = countBg,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    shadowElevation = 0.dp
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
            if (onTrailingClick != null) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onTrailingClick)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.azureInk
                    )
                }
            } else {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
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
    val colors = ShiftlyColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.coralContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_trash_2),
                    contentDescription = null,
                    tint = colors.coralInk,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.coralBright,
                    contentColor = colors.onAccentDark
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans
                    ),
                    color = colors.textSecondary
                )
            }
        }
    )
}
