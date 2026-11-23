package com.example.shiftly.ui.screens.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.ui.components.ShiftlyConfettiOverlay
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyDialogShell
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlyProgressBar
import com.example.shiftly.ui.components.ShiftlyScreenTutorialSheet
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.SavingsGoalMath
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private fun filterSingleDecimal(raw: String, current: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    return if (filtered.count { it == '.' } <= 1) filtered else current
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingsGoalSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onCreateGoal: (title: String, targetAmount: Double, targetDateEpochDay: Long) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }

    var titleInput by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val formattedTargetDate = remember(targetDate, dateFormatter) {
        targetDate.format(dateFormatter)
    }

    val parsedTarget = remember(targetInput) { targetInput.toDoubleOrNull() ?: 0.0 }
    val isValid = remember(titleInput, parsedTarget) {
        titleInput.isNotBlank() && parsedTarget.isFinite() && parsedTarget > 0.0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { ShiftlySheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShiftlySheetHeader(title = "New Savings Goal")

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Goal Name
                    GoalFormInsetRow(
                        label = "Goal Name",
                        value = titleInput,
                        onValueChange = {
                            titleInput = it
                            errorText = null
                        },
                        placeholder = "e.g. Laptop"
                    )

                    ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                    // Row 2: Target Amount
                    GoalFormInsetRow(
                        label = "Target Amount",
                        value = targetInput,
                        onValueChange = {
                            targetInput = filterSingleDecimal(it, targetInput)
                            errorText = null
                        },
                        placeholder = "0.00",
                        prefix = currencySymbol,
                        isNumeric = true
                    )

                    ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                    // Row 3: Target Date
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .shiftlyPressClickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Date",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = colors.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_calendar,
                                contentDescription = null,
                                tint = colors.azureInk,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = formattedTargetDate,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ).tabular(),
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.coralTextOnSurface
                )
            }

            val submitGoal: () -> Unit = {
                val cleanTitle = titleInput.trim()
                val parsed = targetInput.toDoubleOrNull()
                when {
                    cleanTitle.isEmpty() -> errorText = "Please enter a goal name."
                    parsed == null || !parsed.isFinite() || parsed <= 0.0 -> {
                        errorText = "Please enter a target amount above 0."
                    }
                    else -> {
                        onCreateGoal(cleanTitle, parsed, targetDate.toEpochDay())
                    }
                }
            }

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = submitGoal,
                cancelLabel = "Cancel",
                actionLabel = "Save",
                actionEnabled = isValid
            )
        }
    }

    if (showDatePicker) {
        val initialMillis = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        ShiftlyDialogShell(
            title = "Target Date",
            onDismiss = { showDatePicker = false },
            actionLabel = "Done",
            onAction = {
                pickerState.selectedDateMillis?.let { millis ->
                    targetDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                }
                showDatePicker = false
            },
            horizontalOuterPadding = 12.dp,
            contentHorizontalPadding = 8.dp
        ) {
            DatePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = colors.surface,
                    titleContentColor = colors.textPrimary,
                    headlineContentColor = colors.textPrimary,
                    weekdayContentColor = colors.textSecondary,
                    subheadContentColor = colors.textSecondary,
                    yearContentColor = colors.textPrimary,
                    currentYearContentColor = colors.azureAccent,
                    selectedYearContentColor = colors.onAzurePrimary,
                    selectedYearContainerColor = colors.azurePrimary,
                    dayContentColor = colors.textPrimary,
                    disabledDayContentColor = colors.textSecondary.copy(alpha = 0.45f),
                    selectedDayContentColor = colors.onAzurePrimary,
                    selectedDayContainerColor = colors.azurePrimary,
                    todayContentColor = colors.azureAccent,
                    todayDateBorderColor = colors.azureAccent
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalDetailSheet(
    goals: List<SavingsGoalWithDeposits>,
    initialGoalId: Long?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSelectActiveGoal: (Long) -> Unit,
    onOpenAddNewGoal: () -> Unit,
    onAddDeposit: (goal: SavingsGoalEntity, amount: Double, note: String) -> Unit,
    onDeleteDeposit: (SavingsDepositEntity) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val dateTimeFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", locale) }
    val zone = remember { ZoneId.systemDefault() }

    var selectedGoalId by remember {
        mutableStateOf(
            initialGoalId?.takeIf { id -> goals.any { it.goal.id == id } }
                ?: goals.firstOrNull { it.goal.isActive }?.goal?.id
                ?: goals.firstOrNull()?.goal?.id
        )
    }
    LaunchedEffect(goals) {
        val currentId = selectedGoalId
        if (goals.isEmpty()) {
            selectedGoalId = null
        } else if (currentId == null || goals.none { it.goal.id == currentId }) {
            selectedGoalId = initialGoalId?.takeIf { id -> goals.any { it.goal.id == id } }
                ?: goals.firstOrNull { it.goal.isActive }?.goal?.id
                ?: goals.firstOrNull()?.goal?.id
        }
    }
    val safeIndex = remember(goals, selectedGoalId) {
        goals.indexOfFirst { it.goal.id == selectedGoalId }.let { if (it >= 0) it else 0 }
    }
    val selectedItem = goals.getOrNull(safeIndex)

    var depositAmountInput by remember { mutableStateOf("") }
    var depositNoteInput by remember { mutableStateOf("") }
    var depositError by remember { mutableStateOf<String?>(null) }
    var pendingDeleteGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var pendingDeleteDeposit by remember { mutableStateOf<SavingsDepositEntity?>(null) }
    var showTutorialSheet by remember { mutableStateOf(false) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { ShiftlySheetDragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShiftlySheetHeader(
                title = "Savings Goals",
                onHelpClick = { showTutorialSheet = true }
            )

            if (selectedItem == null) {
                ShiftlyEmptyState(
                    iconRes = R.drawable.ic_lucide_piggy_bank,
                    title = "No savings goal set yet",
                    subtitle = null,
                    actionText = "Add Goal",
                    onAction = onOpenAddNewGoal,
                    iconBackgroundColor = colors.mintBright,
                    containerColor = colors.cardElevated
                )
                ShiftlySheetCloseButton(
                    label = "Close",
                    onClick = onDismiss
                )
            } else {
                // Goal Navigation Controls (when multiple goals exist)
                if (goals.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var prevTapCount by remember { mutableIntStateOf(0) }
                        var nextTapCount by remember { mutableIntStateOf(0) }
                        IconButton(
                            onClick = {
                                prevTapCount++
                                val prev = if (safeIndex > 0) safeIndex - 1 else goals.lastIndex
                                val targetId = goals[prev].goal.id
                                selectedGoalId = targetId
                                onSelectActiveGoal(targetId)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_chevron_left,
                                contentDescription = "Previous goal",
                                tint = colors.textPrimary,
                                triggerKey = prevTapCount,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Goal ${safeIndex + 1} of ${goals.size}",
                            style = MaterialTheme.typography.labelLarge.tabular(),
                            color = colors.textSecondary
                        )

                        IconButton(
                            onClick = {
                                nextTapCount++
                                val next = if (safeIndex < goals.lastIndex) safeIndex + 1 else 0
                                val targetId = goals[next].goal.id
                                selectedGoalId = targetId
                                onSelectActiveGoal(targetId)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_chevron_right,
                                contentDescription = "Next goal",
                                tint = colors.textPrimary,
                                triggerKey = nextTapCount,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goals.forEach { entry ->
                            val isSelected = entry.goal.id == selectedItem.goal.id
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .shiftlyPressClickable {
                                        selectedGoalId = entry.goal.id
                                        onSelectActiveGoal(entry.goal.id)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) colors.azurePrimary else colors.cardElevated,
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        text = entry.goal.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                                        color = if (isSelected) PureWhite else colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val goal = selectedItem.goal
                val progress = remember(selectedItem) { SavingsGoalMath.computeProgress(selectedItem) }
                val targetDateStr = remember(goal.targetDateEpochDay, dateFormatter) {
                    LocalDate.ofEpochDay(goal.targetDateEpochDay).format(dateFormatter)
                }
                val pct = remember(progress.progressFraction) {
                    (progress.progressFraction * 100f).roundToInt()
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
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
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${ShiftlyFormatters.formatCurrency(currencySymbol, goal.targetAmount)} • $targetDateStr",
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = colors.textSecondary
                                )
                            }
                            var deleteGoalTapCount by remember { mutableIntStateOf(0) }
                            IconButton(
                                onClick = {
                                    deleteGoalTapCount++
                                    pendingDeleteGoal = goal
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                ShiftlyAnimatedIcon(
                                    iconRes = R.drawable.ic_lucide_trash_2,
                                    contentDescription = "Delete goal",
                                    tint = colors.coralInk,
                                    triggerKey = deleteGoalTapCount,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progress",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = if (progress.isCompleted) "100%" else "$pct%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ).tabular(),
                                    color = colors.mintTextOnSurface
                                )
                            }
                            ShiftlyProgressBar(
                                progress = progress.progressFraction,
                                color = colors.mintBright,
                                trackColor = colors.surface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            GoalMetricColumn(
                                label = "Current",
                                value = ShiftlyFormatters.formatCurrency(currencySymbol, progress.savedAmount),
                                valueColor = colors.mintTextOnSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )
                            ShiftlyVerticalHairlineDivider(modifier = Modifier.fillMaxHeight())
                            GoalMetricColumn(
                                label = "Remaining",
                                value = ShiftlyFormatters.formatCurrency(currencySymbol, progress.leftToSave),
                                valueColor = colors.textPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.mintContainer,
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        progress.isCompleted -> "Goal reached!"
                                        progress.isPastDue -> "Target date passed • ${ShiftlyFormatters.formatCurrency(currencySymbol, progress.leftToSave)} left"
                                        else -> "Save ${ShiftlyFormatters.formatCurrency(currencySymbol, progress.neededPerDay)}/day"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontSize = 14.sp
                                    ).tabular(),
                                    color = colors.mintInk
                                )
                                Text(
                                    text = if (progress.isCompleted) "Completed" else "${progress.daysRemaining}d left",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = colors.mintInk
                                )
                            }
                        }
                    }
                }

                // Add Deposit Form
                Text(
                    text = "Add Deposit",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        GoalFormInsetRow(
                            label = "Amount ($currencySymbol)",
                            value = depositAmountInput,
                            onValueChange = {
                                depositAmountInput = filterSingleDecimal(it, depositAmountInput)
                                depositError = null
                            },
                            placeholder = "0.00",
                            prefix = currencySymbol,
                            isNumeric = true
                        )

                        ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                        GoalFormInsetRow(
                            label = "Note (optional)",
                            value = depositNoteInput,
                            onValueChange = { depositNoteInput = it },
                            placeholder = "Note"
                        )
                    }
                }
                if (!depositError.isNullOrBlank()) {
                    Text(
                        text = depositError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.coralTextOnSurface
                    )
                }

                ShiftlyPrimaryPillButton(
                    text = "Add Deposit",
                    onClick = {
                        val amt = depositAmountInput.toDoubleOrNull()
                        if (amt == null || !amt.isFinite() || amt <= 0.0) {
                            depositError = "Enter a valid deposit amount above 0."
                        } else {
                            if (!progress.isCompleted && (progress.savedAmount + amt) >= goal.targetAmount) {
                                confettiTrigger++
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onAddDeposit(goal, amt, depositNoteInput)
                            depositAmountInput = ""
                            depositNoteInput = ""
                            depositError = null
                        }
                    }
                )

                ShiftlySecondaryTextButton(
                    text = "Add Goal",
                    onClick = onOpenAddNewGoal,
                    leadingIcon = {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_plus,
                            contentDescription = null,
                            tint = colors.azureAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Deposit Transaction History
                Text(
                    text = "Deposits (${selectedItem.deposits.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp
                    ).tabular(),
                    color = colors.textPrimary
                )

                val sortedDepositsWithLabels = remember(selectedItem.deposits, zone, dateTimeFormatter) {
                    selectedItem.deposits
                        .sortedByDescending { it.timestampMillis }
                        .map { dep ->
                            val dtText = Instant.ofEpochMilli(dep.timestampMillis)
                                .atZone(zone)
                                .format(dateTimeFormatter)
                            dep to dtText
                        }
                }
                if (sortedDepositsWithLabels.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardElevated,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            sortedDepositsWithLabels.forEachIndexed { idx, (dep, dtText) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ShiftlyFormatters.formatCurrency(currencySymbol, dep.amount, showPlus = true),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ).tabular(),
                                            color = colors.mintTextOnSurface
                                        )
                                        Text(
                                            text = if (dep.note.isNotBlank()) "$dtText • ${dep.note}" else dtText,
                                            style = MaterialTheme.typography.bodySmall.tabular(),
                                            color = colors.textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    var deleteDepositTapCount by remember { mutableIntStateOf(0) }
                                    IconButton(
                                        onClick = {
                                            deleteDepositTapCount++
                                            pendingDeleteDeposit = dep
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        ShiftlyAnimatedIcon(
                                            iconRes = R.drawable.ic_lucide_trash_2,
                                            contentDescription = "Delete deposit",
                                            tint = colors.coralInk,
                                            triggerKey = deleteDepositTapCount,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (idx < sortedDepositsWithLabels.lastIndex) {
                                    ShiftlyHairlineDivider(startInset = 14.dp, endInset = 14.dp)
                                }
                            }
                        }
                    }
                }

                ShiftlySheetCloseButton(
                    label = "Done",
                    onClick = onDismiss
                )
            }
        }
            ShiftlyConfettiOverlay(
                triggerCount = confettiTrigger,
                modifier = Modifier.matchParentSize()
            )
        }
    }

    if (showTutorialSheet) {
        ShiftlyScreenTutorialSheet(
            title = "Savings Goals Guide",
            subtitle = null,
            steps = listOf(
                "Create & switch goals" to "Set a target amount and date, or switch goals above.",
                "Record deposits" to "Enter an amount to update your progress and daily pace.",
                "Deposit history" to "Review or remove past deposits anytime."
            ),
            onDismiss = { showTutorialSheet = false }
        )
    }

    pendingDeleteGoal?.let { goalToDelete ->
        ShiftlyDeleteConfirmDialog(
            title = "Delete Savings Goal?",
            message = "Delete \"${goalToDelete.title}\"?",
            onDismiss = { pendingDeleteGoal = null },
            onConfirm = {
                onDeleteGoal(goalToDelete)
                pendingDeleteGoal = null
            }
        )
    }

    pendingDeleteDeposit?.let { depositToDelete ->
        ShiftlyDeleteConfirmDialog(
            title = "Delete Deposit?",
            message = "Delete ${ShiftlyFormatters.formatCurrency(currencySymbol, depositToDelete.amount)} deposit?",
            onDismiss = { pendingDeleteDeposit = null },
            onConfirm = {
                onDeleteDeposit(depositToDelete)
                pendingDeleteDeposit = null
            }
        )
    }
}

@Composable
private fun GoalMetricColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = valueColor
        )
    }
}

@Composable
private fun GoalFormInsetRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    prefix: String? = null,
    isNumeric: Boolean = false
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = colors.textPrimary,
            maxLines = 1
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (prefix != null && value.isNotEmpty()) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isNumeric) KeyboardType.Decimal else KeyboardType.Text
                ),
                cursorBrush = SolidColor(colors.azureAccent),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End
                ).let { if (isNumeric) it.tabular() else it },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    color = colors.textSecondary
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.width(150.dp)
            )
        }
    }
}
