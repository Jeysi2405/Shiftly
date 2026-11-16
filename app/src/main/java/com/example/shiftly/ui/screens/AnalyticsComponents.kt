package com.example.shiftly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralBright
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralSoftContainer
import com.example.shiftly.ui.theme.CoralTextOnBlue
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.MintTextOnBlue
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.ui.theme.tabular
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AnalyticsTopBar(
    rangeLabel: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
                    .border(1.dp, DarkBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = TextLightPrimary
                )
            }
            Column {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextLightPrimary
                )
                Text(
                    text = "$rangeLabel • Income vs. spending",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLightSecondary
                )
            }
        }
    }
}

@Composable
fun AnalyticsRangeSegmentedControl(
    selectedRange: AnalyticsRange,
    onSelectRange: (AnalyticsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnalyticsRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) AzurePrimaryButton else Color.Transparent)
                        .clickable(role = Role.Tab) { onSelectRange(range) }
                        .semantics(mergeDescendants = true) {
                            selected = isSelected
                            contentDescription = range.fullLabel
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.shortLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = if (isSelected) PureWhite else TextLightSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseGlassChartCard(
    state: AnalyticsUiState
) {
    val currency = state.currencySymbol
    val net = state.periodIncome - state.periodExpenses
    val maxVal = state.chartBuckets
        .maxOfOrNull { maxOf(it.income, it.expense) }
        ?.coerceAtLeast(100.0)
        ?: 100.0
    val chartDescription = buildString {
        append("Income versus expenses bar chart for ${state.selectedRange.fullLabel}. ")
        append("Total income $currency${"%,.0f".format(state.periodIncome)}, ")
        append("total expenses $currency${"%,.0f".format(state.periodExpenses)}.")
    }

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "INCOME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ShiftlySerif,
                                letterSpacing = 0.6.sp
                            ),
                            color = TextLightSecondary
                        )
                        Text(
                            text = "+$currency${"%,.0f".format(state.periodIncome)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ).tabular(),
                            color = MintTextOnBlue
                        )
                    }
                    Column {
                        Text(
                            text = "EXPENSES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ShiftlySerif,
                                letterSpacing = 0.6.sp
                            ),
                            color = TextLightSecondary
                        )
                        Text(
                            text = "-$currency${"%,.0f".format(state.periodExpenses)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ).tabular(),
                            color = CoralTextOnBlue
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (net >= 0) MintSoftContainer else CoralSoftContainer,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (net >= 0) MintBright.copy(alpha = 0.45f) else CoralBright.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = "${if (net >= 0) "+" else "-"}$currency${"%,.0f".format(abs(net))}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = if (net >= 0) MintTextOnBlue else CoralTextOnBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Rounded Elevated Bar Chart Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
                    .semantics(mergeDescendants = true) {
                        contentDescription = chartDescription
                    }
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                    ) {
                        // Subtle guide lines
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(3) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = DarkBorder
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            state.chartBuckets.forEach { bucket ->
                                val incRatio = (bucket.income / maxVal).toFloat().coerceIn(0f, 1f)
                                val expRatio = (bucket.expense / maxVal).toFloat().coerceIn(0f, 1f)
                                val incHeight = if (bucket.income > 0.0) (86f * incRatio).coerceAtLeast(8f) else 4f
                                val expHeight = if (bucket.expense > 0.0) (86f * expRatio).coerceAtLeast(8f) else 4f

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.height(88.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .height(incHeight.dp)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    if (bucket.income > 0.0) MintBright
                                                    else DarkBorder
                                                )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .height(expHeight.dp)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    if (bucket.expense > 0.0) CoralBright
                                                    else DarkBorder
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = bucket.label,
                                        style = MaterialTheme.typography.labelSmall.tabular(),
                                        color = TextLightSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MintBright)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                                color = TextLightSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CoralBright)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Expenses",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                                color = TextLightSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCardContainer(
    title: String,
    trailingActionText: String? = null,
    onTrailingAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextLightPrimary
                )
                if (trailingActionText != null && onTrailingAction != null) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onTrailingAction)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trailingActionText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AzureInkOnWhite
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun TopSpendingCategoriesCard(
    categories: List<CategorySpendingInsight>,
    currencySymbol: String,
    showAll: Boolean,
    onToggleShowAll: () -> Unit
) {
    val displayed = if (showAll) categories else categories.take(3)
    val canToggle = categories.size > 3
    InsightCardContainer(
        title = "Top Spending Categories",
        trailingActionText = if (canToggle) {
            if (showAll) "Show Less" else "Show All"
        } else {
            null
        },
        onTrailingAction = if (canToggle) onToggleShowAll else null
    ) {
        if (displayed.isEmpty()) {
            Text(
                text = "No spending recorded in this period yet.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayed.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(CoralSoftContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(item.category),
                                        contentDescription = null,
                                        tint = CoralInkOnWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextLightPrimary
                                )
                            }
                            Text(
                                text = "${item.percentage}%  •  $currencySymbol${"%,.0f".format(item.amount)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = ShiftlySans,
                                    fontWeight = FontWeight.Bold
                                ).tabular(),
                                color = TextLightPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { item.fraction.coerceIn(0.05f, 1f) },
                            color = MintBright,
                            trackColor = DarkBorder,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .semantics {
                                    contentDescription = "${item.category}, ${item.percentage}% of period spending"
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsHoursBreakdownCard(
    periodLabel: String,
    currencySymbol: String,
    earnings: Double,
    totalHours: Double,
    regularHours: Double,
    nightDiffHours: Double,
    overtimeHours: Double
) {
    val totalSplitHours = (regularHours + nightDiffHours + overtimeHours).coerceAtLeast(0.1)
    InsightCardContainer(title = "Hours Worked & Earnings ($periodLabel)") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$currencySymbol${"%,.2f".format(earnings)}",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = ShiftlySans,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = MintInkOnWhite
            )
            Text(
                text = "${"%.1f".format(totalHours)}h logged",
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = TextLightSecondary
            )
        }

        // Segmented Mint Hours Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(DarkBorder)
                .semantics {
                    contentDescription = "Hours split: ${"%.1f".format(regularHours)} regular, ${"%.1f".format(nightDiffHours)} night diff, ${"%.1f".format(overtimeHours)} overtime"
                },
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (regularHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((regularHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MintBright)
                )
            }
            if (nightDiffHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((nightDiffHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(AzureInkOnWhite)
                )
            }
            if (overtimeHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((overtimeHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MintTextOnBlue)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HoursSplitMiniTile(
                label = "Regular",
                hours = regularHours,
                containerColor = MintSoftContainer,
                accentColor = MintInkOnWhite,
                modifier = Modifier.weight(1f)
            )
            HoursSplitMiniTile(
                label = "Night Diff",
                hours = nightDiffHours,
                containerColor = AzureSoftContainer,
                accentColor = AzureInkOnWhite,
                modifier = Modifier.weight(1f)
            )
            HoursSplitMiniTile(
                label = "Overtime",
                hours = overtimeHours,
                containerColor = DarkSurface,
                accentColor = TextLightPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HoursSplitMiniTile(
    label: String,
    hours: Double,
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                color = TextLightSecondary
            )
            Text(
                text = "${"%.1f".format(hours)}h",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ShiftlySans,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = accentColor
            )
        }
    }
}

@Composable
fun HoursOfWorkCostCard(
    periodLabel: String,
    currencySymbol: String,
    hourlyRate: Double,
    items: List<HoursCostInsight>,
    showAll: Boolean,
    onToggleShowAll: () -> Unit
) {
    val visibleItems = if (showAll) items else items.take(2)
    val maxHours = (items.maxOfOrNull { it.dutyHours } ?: 1.0).coerceAtLeast(1.0)
    val canToggle = items.size > 2

    InsightCardContainer(
        title = "Hours of Work This Cost",
        trailingActionText = if (canToggle) {
            if (showAll) "Show Less" else "Show All"
        } else {
            null
        },
        onTrailingAction = if (canToggle) onToggleShowAll else null
    ) {
        if (visibleItems.isEmpty()) {
            Text(
                text = "Log expenses to translate costs into shift duty hours at $currencySymbol${"%.2f".format(hourlyRate)}/hr.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleItems.forEach { entry ->
                    val ratio = (entry.dutyHours / maxHours).toFloat().coerceIn(0.08f, 1f)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = MintInkOnWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${entry.category} (${periodLabel.lowercase()}) = ${"%.1f".format(entry.dutyHours)} hours of duty",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ).tabular(),
                                        color = TextLightPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = MintBright,
                                trackColor = DarkBorder,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .semantics {
                                        contentDescription = "${entry.category} equals ${"%.1f".format(entry.dutyHours)} hours of duty"
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsGoalProgressCard(
    goalTitle: String,
    savedAmount: Double,
    targetAmount: Double,
    currencySymbol: String,
    onShowAllClick: () -> Unit
) {
    val ratio = if (targetAmount > 0.0) (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val pct = (ratio * 100f).roundToInt()

    InsightCardContainer(
        title = "Savings Goal",
        trailingActionText = "$pct% funded",
        onTrailingAction = onShowAllClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MintSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Savings,
                    contentDescription = null,
                    tint = MintInkOnWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goalTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextLightPrimary
                )
                Text(
                    text = "$currencySymbol${"%,.0f".format(savedAmount)} saved of $currencySymbol${"%,.0f".format(targetAmount)}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = TextLightSecondary
                )
            }
        }
        LinearProgressIndicator(
            progress = { ratio.coerceAtLeast(0.04f) },
            color = MintBright,
            trackColor = DarkBorder,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .semantics {
                    contentDescription = "$goalTitle savings goal, $pct% funded"
                }
        )
    }
}

@Composable
fun UnderBudgetStreakAndTipsCard(
    streakDays: Int,
    streakTargetDays: Int,
    dailyAllowance: Double,
    currencySymbol: String,
    tips: List<String>,
    showAllTips: Boolean,
    onToggleShowAll: () -> Unit
) {
    val streakFraction = (streakDays.toFloat() / streakTargetDays.coerceAtLeast(1)).coerceIn(0.05f, 1f)
    val visibleTips = if (showAllTips) tips else tips.take(1)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InsightCardContainer(title = "Under-Budget Streak") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MintSoftContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = MintInkOnWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "$streakDays-day streak",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = TextLightPrimary
                        )
                        Text(
                            text = "Daily cap ≤ $currencySymbol${"%,.0f".format(dailyAllowance)}/day",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = TextLightSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MintSoftContainer
                ) {
                    Text(
                        text = "$streakDays / ${streakTargetDays}d",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = MintInkOnWhite,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { streakFraction },
                color = MintBright,
                trackColor = DarkBorder,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .semantics {
                        contentDescription = "$streakDays of $streakTargetDays days under budget"
                    }
            )
        }

        InsightCardContainer(
            title = "Smart Tips",
            trailingActionText = if (tips.size > 1) {
                if (showAllTips) "Show Less" else "Show All"
            } else {
                null
            },
            onTrailingAction = if (tips.size > 1) onToggleShowAll else null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleTips.forEach { tip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = AzureInkOnWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLightPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnsettledSplitsTrackerCard(
    unsettledAmount: Double,
    totalSplitAmount: Double,
    unsettledCount: Int,
    currencySymbol: String,
    onOpenSplitBill: () -> Unit
) {
    val settledFraction = if (totalSplitAmount > 0.0) {
        ((totalSplitAmount - unsettledAmount) / totalSplitAmount).toFloat().coerceIn(0.05f, 1f)
    } else {
        1f
    }

    InsightCardContainer(title = "Unsettled Split Bills") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AzureSoftContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.CallSplit,
                        contentDescription = null,
                        tint = AzureInkOnWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (unsettledAmount > 0.0) {
                            "$currencySymbol${"%,.2f".format(unsettledAmount)} owed by friends"
                        } else {
                            "All split bills settled"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = TextLightPrimary
                    )
                    Text(
                        text = if (unsettledCount > 0) {
                            "$unsettledCount unpaid friend share(s)"
                        } else {
                            "Zero pending classmate balances"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = AzureSoftContainer,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenSplitBill)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = AzureInkOnWhite
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Open Split Bill",
                        tint = AzureInkOnWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { settledFraction },
            color = MintBright,
            trackColor = DarkBorder,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .semantics {
                    contentDescription = if (unsettledAmount > 0.0) {
                        "$currencySymbol${"%,.2f".format(unsettledAmount)} unsettled across $unsettledCount friend share(s)"
                    } else {
                        "All split bills settled"
                    }
                }
        )
    }
}
