package com.example.shiftly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyProgressBar
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters

private val ChartDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
private val SegmentedOuterShape = RoundedCornerShape(12.dp)
private val SegmentedInnerShape = RoundedCornerShape(9.dp)
private val AnalyticsCardShape = RoundedCornerShape(18.dp)

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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SegmentedOuterShape,
        color = colors.card,
        shadowElevation = 0.dp
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
                        .heightIn(min = 38.dp)
                        .clip(SegmentedInnerShape)
                        .background(if (isSelected) colors.azurePrimary else Color.Transparent)
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
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ).tabular(),
                        color = if (isSelected) PureWhite else colors.textSecondary
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
                                val paddingLeft = 18.dp.toPx()
                                val paddingRight = 18.dp.toPx()
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
                                val chartWidth = w - paddingLeft - paddingRight
                                val gridLines = 3
                                val horizontalYs = FloatArray(gridLines + 1) { i ->
                                    paddingTop + (chartHeight / gridLines) * i
                                }

                                val count = buckets.size
                                val stepX = if (count > 1) chartWidth / (count - 1) else 0f
                                val verticalXs = FloatArray(count) { idx ->
                                    paddingLeft + idx * stepX
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
                                val endX = w - paddingRight
                                val bottomY = h - paddingBottom

                                onDrawBehind {
                                    for (y in horizontalYs) {
                                        drawLine(
                                            color = hGridColor,
                                            start = Offset(paddingLeft, y),
                                            end = Offset(endX, y),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.chartBuckets.forEach { bucket ->
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textSecondary
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
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onTrailingAction)
                            .padding(horizontal = 10.dp),
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
            Column {
                categories.forEachIndexed { index, item ->
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
                                    size = 32.dp,
                                    cornerRadius = 8.dp,
                                    iconSize = 16.dp
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
                                .padding(start = 42.dp)
                                .semantics {
                                    contentDescription = "${item.category}, ${item.percentage}% of period spending"
                                }
                        )
                    }
                    if (index < categories.lastIndex) {
                        ShiftlyHairlineDivider(startInset = 42.dp)
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
            Column {
                items.forEachIndexed { index, entry ->
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
                                iconSize = 14.dp
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
                    if (index < items.lastIndex) {
                        ShiftlyHairlineDivider(startInset = 38.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun OnBudgetStreakCard(
    streakDays: Int,
    streakTargetDays: Int,
    dailyAllowance: Double,
    currencySymbol: String
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
                    size = 36.dp,
                    cornerRadius = 9.dp,
                    iconSize = 18.dp
                )
                Text(
                    text = if (hasBudget) "$streakDays-day streak" else "No budget set",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.textPrimary
                )
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
                    size = 36.dp,
                    cornerRadius = 9.dp,
                    iconSize = 18.dp
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
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = colors.azureContainer,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .heightIn(min = 40.dp)
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
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.azureInk
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_arrow_right),
                        contentDescription = "Open Split Bill",
                        tint = colors.azureInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
