package com.example.shiftly.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyProgressBar
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters

private val ChartDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
private val SegmentedOuterShape = RoundedCornerShape(10.dp)
private val SegmentedThumbShape = RoundedCornerShape(8.dp)
private val AnalyticsCardShape = RoundedCornerShape(18.dp)

@Suppress("UNUSED_PARAMETER")
@Composable
fun AnalyticsTopBar(
    rangeLabel: String,
    onBack: (() -> Unit)? = null
) {
    ShiftlyScreenHeader(
        title = "Analytics",
        largeTitle = true,
        onBackClick = onBack,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
fun AnalyticsRangeSegmentedControl(
    selectedRange: AnalyticsRange,
    onSelectRange: (AnalyticsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    val entries = AnalyticsRange.entries
    val selectedIndex = entries.indexOf(selectedRange).coerceAtLeast(0)
    val trackColor = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFD1D1D6)
    val thumbColor = if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFF636366)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SegmentedOuterShape,
        color = trackColor,
        shadowElevation = 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            val segmentWidth = maxWidth / entries.size
            val thumbOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.8f),
                label = "analyticsSegmentThumbOffset"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                    .width(segmentWidth)
                    .height(42.dp)
                    .clip(SegmentedThumbShape)
                    .background(thumbColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEach { range ->
                    val isSelected = range == selectedRange
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(SegmentedThumbShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab
                            ) {
                                if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectRange(range)
                            }
                            .semantics(mergeDescendants = true) {
                                selected = isSelected
                                contentDescription = range.fullLabel
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = range.shortLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ).tabular(),
                            color = if (isSelected) PureWhite else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseGlassChartCard(
    state: AnalyticsUiState
) {
    val colors = ShiftlyColors.current
    val currency = state.currencySymbol
    val net = remember(state.periodIncome, state.periodExpenses) {
        state.periodIncome - state.periodExpenses
    }
    val maxVal = remember(state.chartBuckets) {
        state.chartBuckets
            .maxOfOrNull { maxOf(it.income, it.expense) }
            ?.coerceAtLeast(100.0)
            ?: 100.0
    }
    val chartDescription = remember(
        state.selectedRange,
        currency,
        state.periodIncome,
        state.periodExpenses
    ) {
        buildString {
            append("Income versus expenses bar chart for ${state.selectedRange.fullLabel}. ")
            append("Total income ${ShiftlyFormatters.formatCurrency(currency, state.periodIncome)}, ")
            append("total expenses ${ShiftlyFormatters.formatCurrency(currency, state.periodExpenses)}.")
        }
    }

    ShiftlyGlassCard(
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column {
                        Text(
                            text = "INCOME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            color = colors.textSecondary
                        )
                        Text(
                            text = ShiftlyFormatters.formatCurrency(currency, state.periodIncome, showPlus = true),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ).tabular(),
                            color = colors.mintTextOnSurface
                        )
                    }
                    Column {
                        Text(
                            text = "EXPENSES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            color = colors.textSecondary
                        )
                        Text(
                            text = ShiftlyFormatters.formatCurrency(currency, state.periodExpenses, showMinus = true),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ).tabular(),
                            color = colors.coralTextOnSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (net >= 0) colors.mintContainer else colors.coralContainer,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currency, net, showPlus = net > 0.0),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ).tabular(),
                        color = if (net >= 0) colors.mintTextOnSurface else colors.coralTextOnSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            ShiftlyHairlineDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = chartDescription
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val buckets = state.chartBuckets
                val gridBorderColor = colors.separator
                val cardDotBg = colors.card
                val mintLineColor = colors.mintBright
                val coralLineColor = colors.coralBright
                val dotCenterColor = colors.surface

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val w = size.width
                                val h = size.height
                                val paddingTop = 12.dp.toPx()
                                val paddingBottom = 12.dp.toPx()
                                val gridStrokePx = 0.5.dp.toPx()
                                val lineStroke = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                                val dotOuterRadius = 5.dp.toPx()
                                val dotMidRadius = 2.5.dp.toPx()
                                val dotInnerRadius = 1.5.dp.toPx()

                                val chartHeight = h - paddingTop - paddingBottom
                                val gridLines = 3
                                val horizontalYs = FloatArray(gridLines + 1) { i ->
                                    paddingTop + (chartHeight / gridLines) * i
                                }

                                val count = buckets.size
                                val slotWidth = if (count > 0) w / count else 0f
                                val verticalXs = FloatArray(count) { idx ->
                                    slotWidth * (idx + 0.5f)
                                }
                                val incPoints = if (count > 0) {
                                    List(count) { idx ->
                                        val b = buckets[idx]
                                        Offset(
                                            verticalXs[idx],
                                            h - paddingBottom - ((b.income / maxVal) * chartHeight).toFloat()
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                                val expPoints = if (count > 0) {
                                    List(count) { idx ->
                                        val b = buckets[idx]
                                        Offset(
                                            verticalXs[idx],
                                            h - paddingBottom - ((b.expense / maxVal) * chartHeight).toFloat()
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                                val incPath = Path().apply {
                                    incPoints.forEachIndexed { i, p ->
                                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                    }
                                }
                                val expPath = Path().apply {
                                    expPoints.forEachIndexed { i, p ->
                                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                    }
                                }
                                val hGridColor = gridBorderColor.copy(alpha = 0.75f)
                                val vGridColor = gridBorderColor.copy(alpha = 0.45f)
                                val gridStartX = 6.dp.toPx()
                                val gridEndX = (w - 6.dp.toPx()).coerceAtLeast(gridStartX)
                                val bottomY = h - paddingBottom

                                onDrawBehind {
                                    for (y in horizontalYs) {
                                        drawLine(
                                            color = hGridColor,
                                            start = Offset(gridStartX, y),
                                            end = Offset(gridEndX, y),
                                            strokeWidth = gridStrokePx,
                                            pathEffect = ChartDashEffect
                                        )
                                    }

                                    if (count > 0) {
                                        for (x in verticalXs) {
                                            drawLine(
                                                color = vGridColor,
                                                start = Offset(x, paddingTop),
                                                end = Offset(x, bottomY),
                                                strokeWidth = gridStrokePx
                                            )
                                        }

                                        drawPath(
                                            path = incPath,
                                            color = mintLineColor,
                                            style = lineStroke
                                        )
                                        for (p in incPoints) {
                                            drawCircle(color = mintLineColor, radius = dotOuterRadius, center = p)
                                            drawCircle(color = cardDotBg, radius = dotMidRadius, center = p)
                                            drawCircle(color = dotCenterColor, radius = dotInnerRadius, center = p)
                                        }

                                        drawPath(
                                            path = expPath,
                                            color = coralLineColor,
                                            style = lineStroke
                                        )
                                        for (p in expPoints) {
                                            drawCircle(color = coralLineColor, radius = dotOuterRadius, center = p)
                                            drawCircle(color = cardDotBg, radius = dotMidRadius, center = p)
                                            drawCircle(color = dotCenterColor, radius = dotInnerRadius, center = p)
                                        }
                                    }
                                }
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.chartBuckets.forEach { bucket ->
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

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
                                .background(colors.mintBright)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.coralBright)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
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
    val colors = ShiftlyColors.current
    Surface(
        shape = AnalyticsCardShape,
        color = colors.cardElevated,
        shadowElevation = 0.dp,
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
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary
                )
                if (trailingActionText != null && onTrailingAction != null) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .shiftlyPressClickable(onClick = onTrailingAction)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trailingActionText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.azureInk
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
    currencySymbol: String
) {
    val colors = ShiftlyColors.current
    InsightCardContainer(
        title = "Top Spending Categories"
    ) {
        if (categories.isEmpty()) {
            Text(
                text = "No spending recorded in this period yet.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ShiftlyIosIconTile(
                                    iconRes = categoryIcon(item.category),
                                    backgroundColor = colors.coralBright,
                                    size = 28.dp,
                                    cornerRadius = 8.dp,
                                    iconSize = 22.dp
                                )
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                            }
                            Text(
                                text = "${item.percentage}%  •  ${ShiftlyFormatters.formatCurrency(currencySymbol, item.amount)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ).tabular(),
                                color = colors.textPrimary
                            )
                        }
                        ShiftlyProgressBar(
                            progress = item.fraction.coerceIn(0.05f, 1f),
                            color = colors.mintBright,
                            trackColor = colors.separator,
                            modifier = Modifier
                                .padding(start = 38.dp)
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
    currencySymbol: String,
    earnings: Double,
    totalHours: Double,
    regularHours: Double,
    nightDiffHours: Double,
    overtimeHours: Double
) {
    val colors = ShiftlyColors.current
    val totalSplitHours = remember(regularHours, nightDiffHours, overtimeHours) {
        (regularHours + nightDiffHours + overtimeHours).coerceAtLeast(0.1)
    }
    InsightCardContainer(title = "Hours Worked & Earnings") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = ShiftlyFormatters.formatCurrency(currencySymbol, earnings),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = colors.mintInk
            )
            Text(
                text = "${ShiftlyFormatters.formatHours(totalHours)} logged",
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = colors.textSecondary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.separator)
                .semantics {
                    contentDescription = "Hours split: ${ShiftlyFormatters.formatHours(regularHours)} regular, ${ShiftlyFormatters.formatHours(nightDiffHours)} night diff, ${ShiftlyFormatters.formatHours(overtimeHours)} overtime"
                },
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (regularHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((regularHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colors.textPrimary)
                )
            }
            if (nightDiffHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((nightDiffHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colors.nightDiffPurple)
                )
            }
            if (overtimeHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((overtimeHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colors.coralBright)
                )
            }
        }

        ShiftlyHairlineDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoursSplitMiniColumn(
                label = "Regular",
                hours = regularHours,
                accentColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            ShiftlyVerticalHairlineDivider(modifier = Modifier.height(34.dp))
            HoursSplitMiniColumn(
                label = "Night Diff",
                hours = nightDiffHours,
                accentColor = colors.nightDiffPurple,
                modifier = Modifier.weight(1f)
            )
            ShiftlyVerticalHairlineDivider(modifier = Modifier.height(34.dp))
            HoursSplitMiniColumn(
                label = "Overtime",
                hours = overtimeHours,
                accentColor = colors.coralBright,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HoursSplitMiniColumn(
    label: String,
    hours: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            ),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = ShiftlyFormatters.formatHours(hours, compact = true),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = accentColor
        )
    }
}

@Composable
fun HoursOfWorkCostCard(
    currencySymbol: String,
    hourlyRate: Double,
    items: List<HoursCostInsight>
) {
    val colors = ShiftlyColors.current
    val maxHours = remember(items) {
        (items.maxOfOrNull { it.dutyHours } ?: 1.0).coerceAtLeast(1.0)
    }

    InsightCardContainer(
        title = "Hours of Work This Cost"
    ) {
        if (hourlyRate <= 0.0) {
            Text(
                text = "Set your hourly rate in Profile to translate spending into work hours.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        } else if (items.isEmpty()) {
            Text(
                text = "Log expenses to translate costs into shift duty hours at ${ShiftlyFormatters.formatCurrency(currencySymbol, hourlyRate)}/hr.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { entry ->
                    val ratio = (entry.dutyHours / maxHours).toFloat().coerceIn(0.08f, 1f)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ShiftlyIosIconTile(
                                iconRes = R.drawable.ic_lucide_clock,
                                backgroundColor = colors.mintBright,
                                size = 28.dp,
                                cornerRadius = 7.dp,
                                iconSize = 22.dp
                            )
                            Text(
                                text = "${entry.category} = ${ShiftlyFormatters.formatHours(entry.dutyHours)} of duty",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ).tabular(),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ShiftlyProgressBar(
                            progress = ratio,
                            color = colors.mintBright,
                            trackColor = colors.separator,
                            modifier = Modifier
                                .padding(start = 38.dp)
                                .semantics {
                                    contentDescription = "${entry.category} equals ${ShiftlyFormatters.formatHours(entry.dutyHours)} of duty"
                                }
                        )
                    }
                }
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun OnBudgetStreakCard(
    streakDays: Int,
    streakTargetDays: Int,
    dailyAllowance: Double,
    currencySymbol: String,
    onOpenBudgetSettings: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val hasBudget = dailyAllowance > 0.0
    val streakFraction = remember(hasBudget, streakDays, streakTargetDays) {
        if (hasBudget) {
            (streakDays.toFloat() / streakTargetDays.coerceAtLeast(1)).coerceIn(0.05f, 1f)
        } else {
            0f
        }
    }

    InsightCardContainer(title = "On Budget Streak") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShiftlyIosIconTile(
                    iconRes = R.drawable.ic_lucide_flame,
                    backgroundColor = colors.mintBright,
                    size = 28.dp,
                    cornerRadius = 9.dp,
                    iconSize = 22.dp
                )
                if (hasBudget) {
                    Text(
                        text = "$streakDays-day streak",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ).tabular(),
                        color = colors.textPrimary
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .shiftlyPressClickable(onClick = onOpenBudgetSettings)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "No budget set, tap to set monthly budget"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.surface,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "No budget set",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.azureInk
                                )
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_chevron_right),
                                    contentDescription = null,
                                    tint = colors.azureInk,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.mintContainer,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = "$streakDays / ${streakTargetDays}d",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.mintInk,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        ShiftlyProgressBar(
            progress = streakFraction,
            color = colors.mintBright,
            trackColor = colors.separator,
            modifier = Modifier.semantics {
                contentDescription = "$streakDays of $streakTargetDays days on budget"
            }
        )
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
    val colors = ShiftlyColors.current
    val settledFraction = remember(totalSplitAmount, unsettledAmount) {
        if (totalSplitAmount > 0.0) {
            ((totalSplitAmount - unsettledAmount) / totalSplitAmount).toFloat().coerceIn(0.05f, 1f)
        } else {
            1f
        }
    }

    InsightCardContainer(title = "Unsettled Split Bills") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                ShiftlyIosIconTile(
                    iconRes = R.drawable.ic_lucide_split,
                    backgroundColor = colors.azurePrimary,
                    size = 28.dp,
                    cornerRadius = 9.dp,
                    iconSize = 22.dp
                )
                Column {
                    Text(
                        text = if (unsettledAmount > 0.0) {
                            "${ShiftlyFormatters.formatCurrency(currencySymbol, unsettledAmount)} owed by friends"
                        } else {
                            "All split bills settled"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ).tabular(),
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (unsettledCount > 0) {
                            "$unsettledCount unpaid friend share(s)"
                        } else {
                            "Zero pending classmate balances"
                        },
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textSecondary
                    )
                }
            }

            val openInteraction = remember { MutableInteractionSource() }
            TextButton(
                onClick = onOpenSplitBill,
                interactionSource = openInteraction,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .shiftlyPressEffect(openInteraction)
            ) {
                Text(
                    text = "Open",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.azureAccent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_arrow_right),
                    contentDescription = "Open Split Bill",
                    tint = colors.azureAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        ShiftlyProgressBar(
            progress = settledFraction,
            color = colors.mintBright,
            trackColor = colors.separator,
            modifier = Modifier.semantics {
                contentDescription = if (unsettledAmount > 0.0) {
                    "${ShiftlyFormatters.formatCurrency(currencySymbol, unsettledAmount)} unsettled across $unsettledCount friend share(s)"
                } else {
                    "All split bills settled"
                }
            }
        )
    }
}

@Composable
fun NetSavingsRateCard(
    ratePercentage: Int,
    netAmount: Double,
    progressFraction: Float,
    currencySymbol: String
) {
    val colors = ShiftlyColors.current
    val isPositive = netAmount >= 0.0

    InsightCardContainer(title = "Net Savings Rate") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$ratePercentage% Saved",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.textPrimary
                )
                Text(
                    text = "Income retained this period",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isPositive) colors.mintContainer else colors.coralContainer,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = if (isPositive) {
                        "Net +${ShiftlyFormatters.formatCurrency(currencySymbol, netAmount)}"
                    } else {
                        "Deficit -${ShiftlyFormatters.formatCurrency(currencySymbol, -netAmount)}"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = if (isPositive) colors.mintInk else colors.coralInk,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        ShiftlyProgressBar(
            progress = progressFraction.coerceIn(0.02f, 1f),
            color = if (isPositive) colors.mintBright else colors.coralBright,
            trackColor = colors.separator,
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .semantics {
                    contentDescription = "Net savings rate $ratePercentage percent"
                }
        )
    }
}

@Composable
fun PeakShiftSpendPatternCard(
    topEarningDayLabel: String,
    highestSpendDayLabel: String
) {
    val colors = ShiftlyColors.current

    InsightCardContainer(title = "Shift & Spending Patterns") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Row 1: Best Earning Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShiftlyIosIconTile(
                        iconRes = R.drawable.ic_lucide_trending_up,
                        backgroundColor = colors.mintBright,
                        size = 28.dp,
                        cornerRadius = 8.dp,
                        iconSize = 22.dp
                    )
                    Text(
                        text = "Best Earning Day",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = topEarningDayLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            // Row 2: Peak Spend Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShiftlyIosIconTile(
                        iconRes = R.drawable.ic_lucide_receipt,
                        backgroundColor = colors.coralBright,
                        size = 28.dp,
                        cornerRadius = 8.dp,
                        iconSize = 22.dp
                    )
                    Text(
                        text = "Peak Spend Day",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = highestSpendDayLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.coralInk
                )
            }
        }
    }
}

@Composable
fun ProjectedMonthlySpendCard(
    projectedSpend: Double,
    dailyPace: Double,
    isWithinBudget: Boolean?,
    currencySymbol: String
) {
    val colors = ShiftlyColors.current

    InsightCardContainer(title = "Projected End-of-Month Spend") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, projectedSpend),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.textPrimary
                )
                Text(
                    text = "${ShiftlyFormatters.formatCurrency(currencySymbol, dailyPace)}/day pace",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.textSecondary
                )
            }

            if (isWithinBudget != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isWithinBudget) colors.mintContainer else colors.coralContainer,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = if (isWithinBudget) "Within Budget" else "Over Budget",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isWithinBudget) colors.mintInk else colors.coralInk,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
