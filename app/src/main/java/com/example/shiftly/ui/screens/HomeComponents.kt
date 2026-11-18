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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shiftly.R
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.screens.expense.CategorySpendSummary
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Top header displaying the day name on top and numeric date (MM / dd / yyyy) below,
 * plus notifications and profile avatar.
 */
@Composable
fun HomeGreetingHeader(
    profilePhotoPath: String? = null,
    pendingAlertsCount: Int = 0,
    todayOverride: LocalDate? = null,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val today = remember(todayOverride) { todayOverride ?: LocalDate.now() }
    val dayNameFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE", locale) }
    val numericDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MM / dd / yyyy", locale) }

    val notificationDescription = if (pendingAlertsCount > 0) {
        "Notifications, $pendingAlertsCount pending split bill(s)"
    } else {
        "Notifications, no pending alerts"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = today.format(dayNameFormatter),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = ShiftlySerif),
                color = colors.textSecondary
            )
            Text(
                text = today.format(numericDateFormatter),
                style = MaterialTheme.typography.headlineLarge.tabular(),
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(1.dp, colors.border, CircleShape)
                        .semantics { contentDescription = notificationDescription }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_bell),
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                if (pendingAlertsCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.coralBright)
                            .border(1.dp, colors.card, CircleShape)
                    )
                }
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.azureContainer)
                    .border(1.dp, colors.azureAccent.copy(alpha = 0.45f), CircleShape)
            ) {
                val photoFile = remember(profilePhotoPath) {
                    profilePhotoPath?.let { File(it) }?.takeIf { it.exists() && it.isFile }
                }
                if (photoFile != null) {
                    AsyncImage(
                        model = photoFile,
                        contentDescription = "Open Profile and Settings",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_user),
                        contentDescription = "Open Profile and Settings",
                        tint = colors.azureAccent,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardGreetingBanner(
    userName: String,
    greetingHour: Int = LocalTime.now().hour
) {
    val colors = ShiftlyColors.current
    val greeting = remember(greetingHour) { greetingForHour(greetingHour) }
    val display = userName.trim().ifEmpty { "Shiftly" }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = ShiftlySerif),
            color = colors.textSecondary
        )
        Text(
            text = display,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = ShiftlySans,
                fontWeight = FontWeight.Bold
            ),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
fun HomeHeroPager(
    state: HomeUiState,
    onOpenGoalDetail: (goalId: Long?) -> Unit,
    onOpenAddGoal: () -> Unit,
    onSelectActiveGoal: (Long) -> Unit
) {
    val colors = ShiftlyColors.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val currency = state.currencySymbol
    val uniformHeroHeight = 198.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp
        ) { page ->
            when (page) {
                0 -> {
                    ShiftlyGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(uniformHeroHeight),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Balance",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currency, state.totalBalance),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = ShiftlySans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ).tabular(),
                                    color = colors.textPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HeroMetricPill(
                                    label = "Income",
                                    amount = ShiftlyFormatters.formatCurrency(currency, state.totalIncome, showPlus = true),
                                    isIncome = true,
                                    modifier = Modifier.weight(1f)
                                )
                                HeroMetricPill(
                                    label = "Expenses",
                                    amount = ShiftlyFormatters.formatCurrency(currency, state.totalExpenses, showMinus = true),
                                    isIncome = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    val cutoff = state.cutoffSummary
                    ShiftlyGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(uniformHeroHeight),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Shift Earnings",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = cutoff.window.label,
                                        style = MaterialTheme.typography.labelSmall.tabular(),
                                        color = colors.mintTextOnSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currency, cutoff.totalEarnings),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = ShiftlySans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ).tabular(),
                                    color = colors.mintTextOnSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val daysLabel = if (cutoff.shiftDaysCount == 1) "1 day" else "${cutoff.shiftDaysCount} days"
                                GlassSubStatPill(
                                    label = "Shift Days",
                                    value = daysLabel,
                                    modifier = Modifier.weight(1f)
                                )
                                GlassSubStatPill(
                                    label = "Shift Hours",
                                    value = ShiftlyFormatters.formatHours(cutoff.totalHours),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                else -> {
                    SavingsGoalsHeroCard(
                        goals = state.savingsGoals,
                        currencySymbol = currency,
                        onOpenGoalDetail = onOpenGoalDetail,
                        onOpenAddGoal = onOpenAddGoal,
                        onSelectActiveGoal = onSelectActiveGoal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(uniformHeroHeight)
                    )
                }
            }
        }

        val pageLabels = listOf("Total Balance", "Shift Earnings", "Savings Goals")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            role = Role.Tab,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                        .semantics {
                            selected = active
                            contentDescription = "${pageLabels[index]}, card ${index + 1} of 3"
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = if (active) 20.dp else 7.dp, height = 7.dp)
                            .clip(CircleShape)
                            .background(if (active) colors.azureAccent else colors.border)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalsHeroCard(
    goals: List<SavingsGoalCardItem>,
    currencySymbol: String,
    onOpenGoalDetail: (goalId: Long?) -> Unit,
    onOpenAddGoal: () -> Unit,
    onSelectActiveGoal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    var goalIndex by remember(goals.size) { mutableIntStateOf(0) }
    val safeIdx = goalIndex.coerceIn(0, (goals.size - 1).coerceAtLeast(0))
    val current = goals.getOrNull(safeIdx)

    ShiftlyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        if (current == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Savings Goals",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                            color = colors.textSecondary
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_piggy_bank),
                            contentDescription = null,
                            tint = colors.mintTextOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, 0.0),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        ).tabular(),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Set a target amount and date goal to track daily savings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.azurePrimary,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenAddGoal)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_plus),
                            contentDescription = null,
                            tint = colors.onAzurePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Add Savings Goal",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                            color = colors.onAzurePrimary
                        )
                    }
                }
            }
        } else {
            val goal = current.goalWithDeposits.goal
            val progress = current.progress
            val targetDateLabel = current.targetDate.format(dateFormatter)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (goals.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.card)
                                        .border(1.dp, colors.border, CircleShape)
                                        .clickable {
                                            val prev = if (safeIdx > 0) safeIdx - 1 else goals.lastIndex
                                            goalIndex = prev
                                            onSelectActiveGoal(goals[prev].goalWithDeposits.goal.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_chevron_left),
                                        contentDescription = "Previous goal",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "${safeIdx + 1}/${goals.size}",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = colors.textSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.card)
                                        .border(1.dp, colors.border, CircleShape)
                                        .clickable {
                                            val next = if (safeIdx < goals.lastIndex) safeIdx + 1 else 0
                                            goalIndex = next
                                            onSelectActiveGoal(goals[next].goalWithDeposits.goal.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_chevron_right),
                                        contentDescription = "Next goal",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Goal • $targetDateLabel",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = colors.mintTextOnSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, progress.savedAmount),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ).tabular(),
                        color = colors.mintTextOnSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        color = colors.mintBright,
                        trackColor = colors.border,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassSubStatPill(
                            label = "Left to Save",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, progress.leftToSave),
                            modifier = Modifier.weight(1f),
                            compact = true
                        )
                        val perDayValue = if (progress.isCompleted) {
                            "Goal reached"
                        } else {
                            ShiftlyFormatters.formatCurrency(currencySymbol, progress.neededPerDay)
                        }
                        GlassSubStatPill(
                            label = "Per Day",
                            value = perDayValue,
                            modifier = Modifier.weight(1f),
                            compact = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.border),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { onOpenGoalDetail(goal.id) }
                        ) {
                            Text(
                                text = "View Goal",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.azurePrimary,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenAddGoal)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_plus),
                                    contentDescription = null,
                                    tint = colors.onAzurePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Add Goal",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                                    color = colors.onAzurePrimary
                                )
                            }
                        }
                    }
                }
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
    val colors = ShiftlyColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isIncome) R.drawable.ic_lucide_trending_up else R.drawable.ic_lucide_trending_down
                ),
                contentDescription = null,
                tint = if (isIncome) colors.mintTextOnSurface else colors.coralTextOnSurface,
                modifier = Modifier.size(17.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                    color = colors.textSecondary
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = if (isIncome) colors.mintTextOnSurface else colors.coralTextOnSurface
                )
            }
        }
    }
}

@Composable
private fun GlassSubStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = ShiftlyColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 11.dp,
                vertical = if (compact) 6.dp else 8.dp
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                color = colors.textSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = ShiftlySans,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Monthly top expense categories analytics card with "View insights" placed at the top-right corner.
 */
@Composable
fun MonthlyTopCategoriesCard(
    monthName: String,
    monthlyTotalSpent: Double,
    topCategories: List<CategorySpendSummary>,
    currencySymbol: String,
    onViewInsights: () -> Unit
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onViewInsights)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chart_column),
                    contentDescription = null,
                    tint = colors.azureInk,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "View insights",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                    color = colors.azureInk
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.padding(end = 118.dp)) {
                    Text(
                        text = "Top Spending • $monthName",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                        color = colors.textSecondary
                    )
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotalSpent),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary
                    )
                }

                if (topCategories.isEmpty()) {
                    Text(
                        text = "No expenses logged in $monthName yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        topCategories.take(3).forEach { item ->
                            val pct = (item.fraction * 100f).roundToInt()
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(colors.coralContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(categoryIcon(item.category)),
                                                contentDescription = null,
                                                tint = colors.coralInk,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                    }
                                    Text(
                                        text = "$pct% • ${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = ShiftlySans,
                                            fontWeight = FontWeight.Bold
                                        ).tabular(),
                                        color = colors.textPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { item.fraction.coerceIn(0.05f, 1f) },
                                    color = colors.coralBright,
                                    trackColor = colors.cardElevated,
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-month hours indicator card placed below Top Spending (Regular/Total = primary text, Night Diff = Purple, Overtime = Red).
 */
@Composable
fun MonthlyHoursIndicatorCard(
    monthName: String,
    summary: MonthlyHoursSummary
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
                    text = "Shift Hours • $monthName",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                    color = colors.textSecondary
                )
                Text(
                    text = "Full month",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                    color = colors.textSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonthlyHoursMetricTile(
                    label = "Total Hours",
                    hours = summary.totalHours,
                    valueColor = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                MonthlyHoursMetricTile(
                    label = "Night Diff",
                    hours = summary.nightDiffHours,
                    valueColor = colors.nightDiffPurple,
                    modifier = Modifier.weight(1f)
                )
                MonthlyHoursMetricTile(
                    label = "Overtime",
                    hours = summary.overtimeHours,
                    valueColor = colors.coralBright,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MonthlyHoursMetricTile(
    label: String,
    hours: Double,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = ShiftlyFormatters.formatHours(hours, compact = true),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ShiftlySans,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.azureContainer,
        border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.35f)),
        shadowElevation = 0.dp,
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
                    painter = painterResource(R.drawable.ic_lucide_split),
                    contentDescription = null,
                    tint = colors.azureInk,
                    modifier = Modifier.size(21.dp)
                )
                Column {
                    Text(
                        text = "${ShiftlyFormatters.formatCurrency(currencySymbol, pendingAmount)} owed to you",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$billCount split bill(s) awaiting payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_lucide_arrow_right),
                contentDescription = "Open Split Bill",
                tint = colors.azureInk,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun HomeTimelineRow(
    item: TimelineActivityItem,
    currencySymbol: String,
    onClick: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val isIncome = item is TimelineActivityItem.ShiftActivity || item is TimelineActivityItem.ExtraIncomeActivity
    val iconRes = when (item) {
        is TimelineActivityItem.ShiftActivity -> R.drawable.ic_lucide_clock
        is TimelineActivityItem.ExtraIncomeActivity -> R.drawable.ic_lucide_trending_up
        is TimelineActivityItem.ExpenseActivity -> R.drawable.ic_lucide_receipt_text
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isIncome) colors.mintContainer else colors.coralContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isIncome) colors.mintInk else colors.coralInk,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            when (item) {
                is TimelineActivityItem.ShiftActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.timeRange} • ${item.durationLabel}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textSecondary
                    )
                }

                is TimelineActivityItem.ExtraIncomeActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Recurring • ${item.timeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                is TimelineActivityItem.ExpenseActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.category} • ${item.timeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        when (item) {
            is TimelineActivityItem.ShiftActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showPlus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            is TimelineActivityItem.ExtraIncomeActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showPlus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            is TimelineActivityItem.ExpenseActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showMinus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.coralInk
                )
            }
        }
    }
}
