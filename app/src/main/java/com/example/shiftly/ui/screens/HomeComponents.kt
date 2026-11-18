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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
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
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.screens.expense.CategorySpendSummary
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.AzureDarkContainer
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralBright
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralSoftContainer
import com.example.shiftly.ui.theme.CoralTextOnBlue
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCardElevated
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.MintTextOnBlue
import com.example.shiftly.ui.theme.NightDiffPurple
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
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
                color = TextLightSecondary
            )
            Text(
                text = today.format(numericDateFormatter),
                style = MaterialTheme.typography.headlineLarge.tabular(),
                color = TextLightPrimary,
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
                        .background(DarkCard)
                        .semantics { contentDescription = notificationDescription }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = TextLightPrimary
                    )
                }
                if (pendingAlertsCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CoralBright)
                    )
                }
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AzureDarkContainer)
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
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Open Profile and Settings",
                        tint = AzureAccent
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
    val greeting = remember(greetingHour) { greetingForHour(greetingHour) }
    val display = userName.trim().ifEmpty { "Shiftly" }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = ShiftlySerif),
            color = TextLightSecondary
        )
        Text(
            text = display,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = ShiftlySans,
                fontWeight = FontWeight.Bold
            ),
            color = TextLightPrimary,
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
                        contentPadding = PaddingValues(16.dp),
                        showBorder = false
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Balance",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                                    color = TextLightSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currency, state.totalBalance),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = ShiftlySans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ).tabular(),
                                    color = TextLightPrimary
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
                        contentPadding = PaddingValues(16.dp),
                        showBorder = false
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
                                        color = TextLightSecondary
                                    )
                                    Text(
                                        text = cutoff.window.label,
                                        style = MaterialTheme.typography.labelSmall.tabular(),
                                        color = MintTextOnBlue
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
                                    color = MintTextOnBlue
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
                            .background(if (active) AzureAccent else DarkBorder)
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
                            color = TextLightSecondary
                        )
                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                            tint = MintTextOnBlue,
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
                        color = TextLightPrimary
                    )
                    Text(
                        text = "Set a target amount and date goal to track daily savings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary,
                        maxLines = 2
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = AzurePrimaryButton,
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
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Add Savings Goal",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                            color = PureWhite
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
                            color = TextLightSecondary,
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
                                        .background(DarkCard)
                                        .border(1.dp, DarkBorder, CircleShape)
                                        .clickable {
                                            val prev = if (safeIdx > 0) safeIdx - 1 else goals.lastIndex
                                            goalIndex = prev
                                            onSelectActiveGoal(goals[prev].goalWithDeposits.goal.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronLeft,
                                        contentDescription = "Previous goal",
                                        tint = TextLightPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "${safeIdx + 1}/${goals.size}",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = TextLightSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(DarkCard)
                                        .border(1.dp, DarkBorder, CircleShape)
                                        .clickable {
                                            val next = if (safeIdx < goals.lastIndex) safeIdx + 1 else 0
                                            goalIndex = next
                                            onSelectActiveGoal(goals[next].goalWithDeposits.goal.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = "Next goal",
                                        tint = TextLightPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Goal • $targetDateLabel",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = MintTextOnBlue
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
                        color = MintTextOnBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        color = MintBright,
                        trackColor = DarkBorder,
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
                            color = DarkCard,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { onOpenGoalDetail(goal.id) }
                        ) {
                            Text(
                                text = "View Goal",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                                color = TextLightPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AzurePrimaryButton,
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
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Add Goal",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                                    color = PureWhite
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DarkCard
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isIncome) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                contentDescription = null,
                tint = if (isIncome) MintTextOnBlue else CoralTextOnBlue,
                modifier = Modifier.size(17.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                    color = TextLightSecondary
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
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DarkCard
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
                color = TextLightSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = ShiftlySans,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = TextLightPrimary,
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
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DarkCard,
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
                        color = TextLightSecondary
                    )
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotalSpent),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = TextLightPrimary
                    )
                }

                if (topCategories.isEmpty()) {
                    Text(
                        text = "No expenses logged in $monthName yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary
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
                                                .background(CoralSoftContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = categoryIcon(item.category),
                                                contentDescription = null,
                                                tint = CoralInkOnWhite,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextLightPrimary
                                        )
                                    }
                                    Text(
                                        text = "$pct% • ${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = ShiftlySans,
                                            fontWeight = FontWeight.Bold
                                        ).tabular(),
                                        color = TextLightPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { item.fraction.coerceIn(0.05f, 1f) },
                                    color = CoralBright,
                                    trackColor = DarkCardElevated,
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
 * Full-month hours indicator card placed below Top Spending (Regular/Total = White, Night Diff = Purple, Overtime = Red).
 */
@Composable
fun MonthlyHoursIndicatorCard(
    monthName: String,
    summary: MonthlyHoursSummary
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DarkCard,
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
                    color = TextLightSecondary
                )
                Text(
                    text = "Full month",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                    color = TextLightSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonthlyHoursMetricTile(
                    label = "Total Hours",
                    hours = summary.totalHours,
                    valueColor = PureWhite,
                    modifier = Modifier.weight(1f)
                )
                MonthlyHoursMetricTile(
                    label = "Night Diff",
                    hours = summary.nightDiffHours,
                    valueColor = NightDiffPurple,
                    modifier = Modifier.weight(1f)
                )
                MonthlyHoursMetricTile(
                    label = "Overtime",
                    hours = summary.overtimeHours,
                    valueColor = CoralBright,
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface
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
                color = TextLightSecondary,
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AzureSoftContainer,
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
                        text = "${ShiftlyFormatters.formatCurrency(currencySymbol, pendingAmount)} owed to you",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = TextLightPrimary
                    )
                    Text(
                        text = "$billCount split bill(s) awaiting payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary
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
    currencySymbol: String,
    onClick: () -> Unit = {}
) {
    val isIncome = item is TimelineActivityItem.ShiftActivity || item is TimelineActivityItem.ExtraIncomeActivity
    val iconVector = when (item) {
        is TimelineActivityItem.ShiftActivity -> Icons.Outlined.Schedule
        is TimelineActivityItem.ExtraIncomeActivity -> Icons.AutoMirrored.Outlined.TrendingUp
        is TimelineActivityItem.ExpenseActivity -> Icons.AutoMirrored.Outlined.ReceiptLong
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
                .background(if (isIncome) MintSoftContainer else CoralSoftContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = if (isIncome) MintInkOnWhite else CoralInkOnWhite,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            when (item) {
                is TimelineActivityItem.ShiftActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextLightPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.timeRange} • ${item.durationLabel}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = TextLightSecondary
                    )
                }

                is TimelineActivityItem.ExtraIncomeActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextLightPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Recurring • ${item.timeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary
                    )
                }

                is TimelineActivityItem.ExpenseActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextLightPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.category} • ${item.timeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary
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
                    color = MintInkOnWhite
                )
            }

            is TimelineActivityItem.ExtraIncomeActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showPlus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = MintInkOnWhite
                )
            }

            is TimelineActivityItem.ExpenseActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showMinus = true),
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
