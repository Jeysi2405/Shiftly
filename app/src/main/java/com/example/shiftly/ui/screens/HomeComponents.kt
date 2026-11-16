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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralBright
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralSoftContainer
import com.example.shiftly.ui.theme.CoralTextOnBlue
import com.example.shiftly.ui.theme.DeepAzureBottom
import com.example.shiftly.ui.theme.HairlineLight
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.InkPrimary
import com.example.shiftly.ui.theme.InkSecondary
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.MintTextOnBlue
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.SoftCloudWhite
import com.example.shiftly.ui.theme.tabular
import java.time.LocalDate

@Composable
fun HomeGreetingHeader(
    userName: String,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good day,",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = ShiftlySerif),
                color = PureWhite.copy(alpha = 0.85f)
            )
            Text(
                text = "Hello, $userName",
                style = MaterialTheme.typography.headlineLarge,
                color = PureWhite
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PureWhite.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = PureWhite
                )
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = DeepAzureBottom
                )
            }
        }
    }
}

@Composable
fun HomeHeroPager(state: HomeUiState) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val currency = state.currencySymbol

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp
        ) { page ->
            when (page) {
                0 -> {
                    ShiftlyGlassCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = "Total Balance",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                            color = PureWhite.copy(alpha = 0.88f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$currency${"%,.2f".format(state.totalBalance)}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            ).tabular(),
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroMetricPill(
                                label = "Income",
                                amount = "+$currency${"%,.0f".format(state.totalIncome)}",
                                isIncome = true,
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricPill(
                                label = "Expenses",
                                amount = "-$currency${"%,.0f".format(state.totalExpenses)}",
                                isIncome = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                1 -> {
                    ShiftlyGlassCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = "Safe to Spend",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                            color = PureWhite.copy(alpha = 0.88f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$currency${"%,.2f".format(state.safeToSpend)}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            ).tabular(),
                            color = MintTextOnBlue
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassSubStatPill(
                                label = "Buffer",
                                value = "$currency${"%,.0f".format(state.savingsBuffer)}",
                                modifier = Modifier.weight(1f)
                            )
                            GlassSubStatPill(
                                label = "Pending Splits",
                                value = "$currency${"%,.0f".format(state.pendingSplitsOwed)}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                else -> {
                    val ratio = if (state.monthlyBudget > 0.0) {
                        (state.mtdExpenses / state.monthlyBudget).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val nearLimit = ratio >= 0.80f
                    ShiftlyGlassCard(contentPadding = PaddingValues(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Month-to-Date Budget",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                                color = PureWhite.copy(alpha = 0.88f)
                            )
                            Text(
                                text = "${(ratio * 100).toInt()}% used",
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = if (nearLimit) CoralTextOnBlue else MintTextOnBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currency${"%,.0f".format(state.mtdExpenses)} / $currency${"%,.0f".format(state.monthlyBudget)}",
                            style = MaterialTheme.typography.headlineLarge.tabular(),
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = if (nearLimit) CoralBright else MintBright,
                            trackColor = PureWhite.copy(alpha = 0.20f),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "MTD Earned: +$currency${"%,.2f".format(state.mtdIncome)}",
                            style = MaterialTheme.typography.labelMedium.tabular(),
                            color = MintTextOnBlue
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (active) 20.dp else 7.dp, height = 7.dp)
                        .clip(CircleShape)
                        .background(if (active) PureWhite else PureWhite.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    amount: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = PureWhite.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isIncome) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                contentDescription = null,
                tint = if (isIncome) MintTextOnBlue else CoralTextOnBlue,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                    color = PureWhite.copy(alpha = 0.85f)
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = if (isIncome) MintTextOnBlue else CoralTextOnBlue
                )
            }
        }
    }
}

@Composable
private fun GlassSubStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = PureWhite.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                color = PureWhite.copy(alpha = 0.85f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = ShiftlySans,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = PureWhite
            )
        }
    }
}

@Composable
fun CircularWeekdaySelector(
    slots: List<WeekdaySlot>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        slots.forEach { slot ->
            val selected = slot.date == selectedDate
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .heightIn(min = 66.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (selected) PureWhite else PureWhite.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = if (selected) PureWhite else PureWhite.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onSelectDate(slot.date) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = slot.dayLetter,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                    color = if (selected) DeepAzureBottom else PureWhite.copy(alpha = 0.82f)
                )
                Text(
                    text = slot.dayNumber,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = if (selected) DeepAzureBottom else PureWhite
                )
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (slot.hasShift) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (selected) MintInkOnWhite else MintBright)
                        )
                    }
                    if (slot.hasExpense) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (selected) CoralInkOnWhite else CoralBright)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NextShiftQuickCard(
    hourlyRate: Double,
    currencySymbol: String,
    isTimerRunning: Boolean,
    onQuickTimeIn: () -> Unit
) {
    val expected4HrPay = hourlyRate * 4.0
    ShiftlyGlassCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isTimerRunning) "Shift timer active" else "Next Shift • 4h Duty",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isTimerRunning) {
                        "Earning $currencySymbol${"%.2f".format(hourlyRate)}/hr live"
                    } else {
                        "Expected pay: +$currencySymbol${"%,.2f".format(expected4HrPay)}"
                    },
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MintTextOnBlue
                )
            }

            Button(
                onClick = onQuickTimeIn,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkBlack,
                    contentColor = PureWhite
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTimerRunning) "Running" else "Time In",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
fun WeeklyEarningsSnapshotCard(
    weeklyEarnings: Double,
    weeklyHours: Double,
    bars: List<WeeklyBarData>,
    currencySymbol: String,
    onViewInsights: () -> Unit
) {
    val maxVal = (bars.maxOfOrNull { it.earned } ?: 1.0).coerceAtLeast(1.0)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SoftCloudWhite,
        border = BorderStroke(1.dp, HairlineLight),
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
                Column {
                    Text(
                        text = "Weekly Earnings",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                        color = InkSecondary
                    )
                    Text(
                        text = "$currencySymbol${"%,.2f".format(weeklyEarnings)} (${"%.1f".format(weeklyHours)}h)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = InkPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = AzureSoftContainer,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onViewInsights)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            tint = AzureInkOnWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "View insights",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                            color = AzureInkOnWhite
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val fraction = (bar.earned / maxVal).toFloat().coerceIn(0.08f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height((44 * fraction).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(if (bar.earned > 0) MintInkOnWhite else HairlineLight)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bar.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingSplitsBanner(
    pendingAmount: Double,
    billCount: Int,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AzureSoftContainer,
        border = BorderStroke(1.dp, AzureInkOnWhite.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.CallSplit,
                    contentDescription = null,
                    tint = AzureInkOnWhite
                )
                Column {
                    Text(
                        text = "$currencySymbol${"%,.2f".format(pendingAmount)} owed to you",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = InkPrimary
                    )
                    Text(
                        text = "$billCount split bill(s) awaiting payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Open Split Bill",
                tint = AzureInkOnWhite
            )
        }
    }
}

@Composable
fun HomeTimelineRow(
    item: TimelineActivityItem,
    currencySymbol: String
) {
    val isShift = item is TimelineActivityItem.ShiftActivity
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isShift) MintSoftContainer else CoralSoftContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isShift) Icons.Outlined.Schedule else Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = if (isShift) MintInkOnWhite else CoralInkOnWhite,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            when (item) {
                is TimelineActivityItem.ShiftActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.timeRange} • ${item.durationLabel}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = InkSecondary
                    )
                }

                is TimelineActivityItem.ExpenseActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.category} • ${item.timeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            }
        }

        when (item) {
            is TimelineActivityItem.ShiftActivity -> {
                Text(
                    text = "+$currencySymbol${"%,.2f".format(item.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = MintInkOnWhite
                )
            }

            is TimelineActivityItem.ExpenseActivity -> {
                Text(
                    text = "-$currencySymbol${"%,.2f".format(item.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = CoralInkOnWhite
                )
            }
        }
    }
}
