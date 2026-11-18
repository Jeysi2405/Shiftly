package com.example.shiftly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters

@Composable
fun AnalyticsTopBar(
    rangeLabel: String,
    onBack: (() -> Unit)? = null
) {
    val colors = ShiftlyColors.current
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
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_arrow_left),
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
        }
    }
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
        shape = RoundedCornerShape(50),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
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
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
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
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = if (isSelected) colors.onAzurePrimary else colors.textSecondary
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
    val net = state.periodIncome - state.periodExpenses
    val maxVal = state.chartBuckets
        .maxOfOrNull { maxOf(it.income, it.expense) }
        ?.coerceAtLeast(100.0)
        ?: 100.0
    val chartDescription = buildString {
        append("Income versus expenses bar chart for ${state.selectedRange.fullLabel}. ")
        append("Total income ${ShiftlyFormatters.formatCurrency(currency, state.periodIncome)}, ")
        append("total expenses ${ShiftlyFormatters.formatCurrency(currency, state.periodExpenses)}.")
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
                                fontFamily = PlusJakartaSans,
                                letterSpacing = 0.6.sp
                            ),
                            color = colors.textSecondary
                        )
                        Text(
                            text = ShiftlyFormatters.formatCurrency(currency, state.periodIncome, showPlus = true),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = PlusJakartaSans,
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
                                fontFamily = PlusJakartaSans,
                                letterSpacing = 0.6.sp
                            ),
                            color = colors.textSecondary
                        )
                        Text(
                            text = ShiftlyFormatters.formatCurrency(currency, state.periodExpenses, showMinus = true),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = PlusJakartaSans,
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
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (net >= 0) colors.mintBright.copy(alpha = 0.45f) else colors.coralBright.copy(alpha = 0.45f)
                    ),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currency, net, showPlus = net > 0.0),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = if (net >= 0) colors.mintTextOnSurface else colors.coralTextOnSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.card)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .semantics(mergeDescendants = true) {
                        contentDescription = chartDescription
                    }
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val buckets = state.chartBuckets
                    val count = buckets.size
                    val gridBorderColor = colors.border
                    val cardDotBg = colors.card
                    val mintLineColor = colors.mintBright
                    val coralLineColor = colors.coralBright
                    val dotCenterColor = colors.surface

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val paddingTop = 12.dp.toPx()
                            val paddingBottom = 12.dp.toPx()
                            val paddingLeft = 24.dp.toPx()
                            val paddingRight = 24.dp.toPx()

                            val chartHeight = h - paddingTop - paddingBottom
                            val chartWidth = w - paddingLeft - paddingRight

                            val gridLines = 3
                            for (i in 0..gridLines) {
                                val y = paddingTop + (chartHeight / gridLines) * i
                                drawLine(
                                    color = gridBorderColor.copy(alpha = 0.6f),
                                    start = Offset(paddingLeft, y),
                                    end = Offset(w - paddingRight, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                            }

                            if (count > 0) {
                                val stepX = if (count > 1) chartWidth / (count - 1) else 0f

                                buckets.forEachIndexed { idx, _ ->
                                    val x = paddingLeft + idx * stepX
                                    drawLine(
                                        color = gridBorderColor.copy(alpha = 0.35f),
                                        start = Offset(x, paddingTop),
                                        end = Offset(x, h - paddingBottom),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                val incPoints = buckets.mapIndexed { idx, b ->
                                    val x = paddingLeft + idx * stepX
                                    val y = h - paddingBottom - ((b.income / maxVal) * chartHeight).toFloat()
                                    Offset(x, y)
                                }

                                val expPoints = buckets.mapIndexed { idx, b ->
                                    val x = paddingLeft + idx * stepX
                                    val y = h - paddingBottom - ((b.expense / maxVal) * chartHeight).toFloat()
                                    Offset(x, y)
                                }

                                val incPath = Path().apply {
                                    incPoints.forEachIndexed { i, p ->
                                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                    }
                                }
                                drawPath(
                                    path = incPath,
                                    color = mintLineColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )

                                incPoints.forEach { p ->
                                    drawCircle(color = mintLineColor, radius = 5.dp.toPx(), center = p)
                                    drawCircle(color = cardDotBg, radius = 2.5.dp.toPx(), center = p)
                                    drawCircle(color = dotCenterColor, radius = 1.5.dp.toPx(), center = p)
                                }

                                val expPath = Path().apply {
                                    expPoints.forEachIndexed { i, p ->
                                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                    }
                                }
                                drawPath(
                                    path = expPath,
                                    color = coralLineColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )

                                expPoints.forEach { p ->
                                    drawCircle(color = coralLineColor, radius = 5.dp.toPx(), center = p)
                                    drawCircle(color = cardDotBg, radius = 2.5.dp.toPx(), center = p)
                                    drawCircle(color = dotCenterColor, radius = 1.5.dp.toPx(), center = p)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.chartBuckets.forEach { bucket ->
                            Text(
                                text = bucket.label,
                                style = MaterialTheme.typography.labelSmall.tabular(),
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
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlusJakartaSans),
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
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlusJakartaSans),
                                color = colors.textSecondary
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
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
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
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary
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
                                fontFamily = PlusJakartaSans,
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categories.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
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
                                        .background(colors.coralContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(categoryIcon(item.category)),
                                        contentDescription = null,
                                        tint = colors.coralInk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                            }
                            Text(
                                text = "${item.percentage}%  •  ${ShiftlyFormatters.formatCurrency(currencySymbol, item.amount)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold
                                ).tabular(),
                                color = colors.textPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { item.fraction.coerceIn(0.05f, 1f) },
                            color = colors.mintBright,
                            trackColor = colors.border,
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
    currencySymbol: String,
    earnings: Double,
    totalHours: Double,
    regularHours: Double,
    nightDiffHours: Double,
    overtimeHours: Double
) {
    val colors = ShiftlyColors.current
    val totalSplitHours = (regularHours + nightDiffHours + overtimeHours).coerceAtLeast(0.1)
    InsightCardContainer(title = "Hours Worked & Earnings") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = ShiftlyFormatters.formatCurrency(currencySymbol, earnings),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = PlusJakartaSans,
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
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.border)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HoursSplitMiniTile(
                label = "Regular",
                hours = regularHours,
                containerColor = colors.surface,
                accentColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            HoursSplitMiniTile(
                label = "Night Diff",
                hours = nightDiffHours,
                containerColor = colors.nightDiffPurpleContainer,
                accentColor = colors.nightDiffPurple,
                modifier = Modifier.weight(1f)
            )
            HoursSplitMiniTile(
                label = "Overtime",
                hours = overtimeHours,
                containerColor = colors.coralContainer,
                accentColor = colors.coralBright,
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
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlusJakartaSans),
                color = colors.textSecondary
            )
            Text(
                text = ShiftlyFormatters.formatHours(hours, compact = true),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = accentColor
            )
        }
    }
}

@Composable
fun HoursOfWorkCostCard(
    currencySymbol: String,
    hourlyRate: Double,
    items: List<HoursCostInsight>
) {
    val colors = ShiftlyColors.current
    val maxHours = (items.maxOfOrNull { it.dutyHours } ?: 1.0).coerceAtLeast(1.0)

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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { entry ->
                    val ratio = (entry.dutyHours / maxHours).toFloat().coerceIn(0.08f, 1f)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surface,
                        shadowElevation = 0.dp,
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
                                        painter = painterResource(R.drawable.ic_lucide_clock),
                                        contentDescription = null,
                                        tint = colors.mintInk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${entry.category} = ${ShiftlyFormatters.formatHours(entry.dutyHours)} of duty",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.SemiBold
                                        ).tabular(),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = colors.mintBright,
                                trackColor = colors.border,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
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
    val streakFraction = if (hasBudget) {
        (streakDays.toFloat() / streakTargetDays.coerceAtLeast(1)).coerceIn(0.05f, 1f)
    } else {
        0f
    }

    InsightCardContainer(title = "On Budget Streak") {
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
                        .background(colors.mintContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_flame),
                        contentDescription = null,
                        tint = colors.mintInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = if (hasBudget) "$streakDays-day streak" else "No budget set",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
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
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.mintInk,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        LinearProgressIndicator(
            progress = { streakFraction },
            color = colors.mintBright,
            trackColor = colors.border,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .semantics {
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
                        .background(colors.azureContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_split),
                        contentDescription = null,
                        tint = colors.azureInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (unsettledAmount > 0.0) {
                            "${ShiftlyFormatters.formatCurrency(currencySymbol, unsettledAmount)} owed by friends"
                        } else {
                            "All split bills settled"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
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
                            fontFamily = PlusJakartaSans,
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

        LinearProgressIndicator(
            progress = { settledFraction },
            color = colors.mintBright,
            trackColor = colors.border,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .semantics {
                    contentDescription = if (unsettledAmount > 0.0) {
                        "${ShiftlyFormatters.formatCurrency(currencySymbol, unsettledAmount)} unsettled across $unsettledCount friend share(s)"
                    } else {
                        "All split bills settled"
                    }
                }
        )
    }
}
