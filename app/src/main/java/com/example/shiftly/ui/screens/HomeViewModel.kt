package com.example.shiftly.ui.screens

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.InAppNotificationItem
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.screens.expense.CategorySpendSummary
import com.example.shiftly.utils.CutoffCalculator
import com.example.shiftly.utils.CutoffShiftSummary
import com.example.shiftly.utils.CutoffWindow
import com.example.shiftly.utils.SavingsGoalMath
import com.example.shiftly.utils.SavingsGoalProgress
import com.example.shiftly.utils.SplitBillMath
import com.example.shiftly.utils.WageCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun greetingForHour(hourOfDay: Int): String {
    val normalized = ((hourOfDay % 24) + 24) % 24
    return when (normalized) {
        in 5..11 -> "Good morning,"
        in 12..17 -> "Good afternoon,"
        else -> "Good evening,"
    }
}

data class SavingsGoalCardItem(
    val goalWithDeposits: SavingsGoalWithDeposits,
    val progress: SavingsGoalProgress,
    val targetDate: LocalDate
)

@Immutable
data class MonthlyHoursSummary(
    val totalHours: Double = 0.0,
    val nightDiffHours: Double = 0.0,
    val overtimeHours: Double = 0.0
)

@Immutable
sealed class TimelineActivityItem {
    abstract val sortEpochDay: Long
    abstract val sortTimestamp: Long

    @Immutable
    data class ShiftActivity(
        val id: Long,
        val title: String,
        val timeRange: String,
        val durationLabel: String,
        val amount: Double,
        val epochDay: Long,
        val timestamp: Long
    ) : TimelineActivityItem() {
        override val sortEpochDay: Long get() = epochDay
        override val sortTimestamp: Long get() = timestamp
    }

    @Immutable
    data class ExpenseActivity(
        val id: Long,
        val title: String,
        val category: String,
        val timeLabel: String,
        val amount: Double,
        val epochDay: Long,
        val timestamp: Long
    ) : TimelineActivityItem() {
        override val sortEpochDay: Long get() = epochDay
        override val sortTimestamp: Long get() = timestamp
    }

    @Immutable
    data class ExtraIncomeActivity(
        val id: Long,
        val title: String,
        val timeLabel: String,
        val amount: Double,
        val epochDay: Long,
        val timestamp: Long,
        val subtitleTag: String = "Recurring"
    ) : TimelineActivityItem() {
        override val sortEpochDay: Long get() = epochDay
        override val sortTimestamp: Long get() = timestamp
    }
}

@Immutable
data class UpcomingBillBannerInfo(
    val billId: Long,
    val title: String,
    val amount: Double,
    val dueEpochDay: Long,
    val dueDateLabel: String
)

@Immutable
data class PaydayCutoffBannerInfo(
    val cutoffDayOfMonth: Int,
    val daysRemaining: Int,
    val estimatedEarnings: Double
)

@Immutable
data class HomeUiState(
    val userName: String = "",
    val currencySymbol: String = "₱",
    val hourlyRate: Double = 0.0,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val mtdIncome: Double = 0.0,
    val mtdExpenses: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val categoryBudgets: List<CategoryBudgetSetting> = emptyList(),
    val managedCategories: List<ManagedCategorySetting> = emptyList(),
    val pendingSplitsOwed: Double = 0.0,
    val pendingSplitBillCount: Int = 0,
    val notifications: List<InAppNotificationItem> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val pendingHeadsUpNotification: InAppNotificationItem? = null,
    val isBalanceHidden: Boolean = false,
    val upcomingBillBanner: UpcomingBillBannerInfo? = null,
    val paydayCutoffBanner: PaydayCutoffBannerInfo? = null,
    val cutoffSummary: CutoffShiftSummary = CutoffShiftSummary(
        window = CutoffWindow(
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            isFirstCutoff = true,
            label = ""
        ),
        totalEarnings = 0.0,
        shiftDaysCount = 0,
        totalHours = 0.0
    ),
    val savingsGoals: List<SavingsGoalCardItem> = emptyList(),
    val currentMonthName: String = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
    val monthlyTopCategories: List<CategorySpendSummary> = emptyList(),
    val monthlyHoursSummary: MonthlyHoursSummary = MonthlyHoursSummary(),
    val timelineItems: List<TimelineActivityItem> = emptyList(),
    val activeTimer: ActiveTimerState = ActiveTimerState()
)

class HomeViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private class CachedHomeFormatters(
        val locale: Locale,
        val timeFormatter: DateTimeFormatter,
        val monthDayFormatter: DateTimeFormatter
    )

    @Volatile
    private var cachedFormatters: CachedHomeFormatters? = null

    private fun formattersFor(locale: Locale): CachedHomeFormatters {
        val existing = cachedFormatters
        if (existing != null && existing.locale == locale) return existing
        return CachedHomeFormatters(
            locale = locale,
            timeFormatter = DateTimeFormatter.ofPattern("h:mm a", locale),
            monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", locale)
        ).also { cachedFormatters = it }
    }

    private data class CorePrefs(
        val userName: String,
        val currency: String,
        val rate: Double,
        val budget: Double,
        val activeTimer: ActiveTimerState
    )

    private val identityPrefsFlow = combine(
        repository.userName,
        repository.currencySymbol
    ) { userName, currency ->
        userName to currency
    }

    private val financePrefsFlow = combine(
        repository.defaultHourlyRate,
        repository.monthlyBudget,
        repository.activeTimerState
    ) { rate, budget, activeTimer ->
        Triple(rate, budget, activeTimer)
    }

    private val corePrefsFlow = combine(identityPrefsFlow, financePrefsFlow) { id, fin ->
        CorePrefs(
            userName = id.first,
            currency = id.second,
            rate = fin.first,
            budget = fin.second,
            activeTimer = fin.third
        )
    }

    private val settingsAndInboxFlow = combine(
        repository.settingsSnapshot,
        repository.notificationInbox
    ) { settings, inbox ->
        settings to inbox
    }

    val uiState: StateFlow<HomeUiState> = combine(
        corePrefsFlow,
        repository.allShifts,
        repository.allExtraIncomes,
        repository.allExpenses,
        repository.allSplitBills,
        repository.allSavingsGoals,
        repository.allAllowanceEntries,
        settingsAndInboxFlow,
        repository.allRecurringBills,
        repository.balancePrivacyHidden
    ) { values ->
        val prefs = values[0] as CorePrefs
        @Suppress("UNCHECKED_CAST")
        val shifts = values[1] as List<ShiftEntity>
        @Suppress("UNCHECKED_CAST")
        val extraIncomes = values[2] as List<ExtraIncomeEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[3] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val splitBills = values[4] as List<SplitBillWithParticipants>
        @Suppress("UNCHECKED_CAST")
        val rawGoals = values[5] as List<SavingsGoalWithDeposits>
        @Suppress("UNCHECKED_CAST")
        val allowanceEntries = values[6] as List<AllowanceEntryEntity>
        @Suppress("UNCHECKED_CAST")
        val settingsAndInbox = values[7] as Pair<ShiftlySettingsSnapshot, List<InAppNotificationItem>>
        @Suppress("UNCHECKED_CAST")
        val recurringBills = values[8] as List<RecurringBillEntity>
        val isBalanceHidden = values[9] as Boolean
        val settings = settingsAndInbox.first
        val inboxItems = settingsAndInbox.second

        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.from(today)
        val monthStartEpoch = currentMonth.atDay(1).toEpochDay()
        val monthEndEpoch = currentMonth.atEndOfMonth().toEpochDay()

        // Exclude auto-credited Duties cutoff entries (cutoffKey != null) so shifts are never double-counted
        val manualAllowances = allowanceEntries.filter { it.cutoffKey.isNullOrBlank() }

        val totalIncome = shifts.sumOf { it.totalEarned } +
            extraIncomes.sumOf { it.amount } +
            manualAllowances.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }
        val totalBalance = totalIncome - totalExpenses

        val monthShifts = shifts.filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
        val mtdIncome = monthShifts.sumOf { it.totalEarned } +
            extraIncomes
                .filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
                .sumOf { it.amount } +
            manualAllowances
                .filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
                .sumOf { it.amount }
        val mtdExpensesList = expenses.filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
        val mtdExpenses = mtdExpensesList.sumOf { it.amount }

        var monthNdMins = 0L
        var monthOtMins = 0L
        monthShifts.forEach { shift ->
            val details = WageCalculator.calculateTotalEarned(
                startMillis = shift.timeInMillis,
                endMillis = shift.timeOutMillis,
                hourlyRate = shift.lockedHourlyRate,
                dayTypeStr = shift.dayType
            )
            monthNdMins += details.nightDiffMinutes
            monthOtMins += details.overtimeMinutes
        }
        val monthlyHoursSummary = MonthlyHoursSummary(
            totalHours = monthShifts.sumOf { it.durationMinutes } / 60.0,
            nightDiffHours = monthNdMins / 60.0,
            overtimeHours = monthOtMins / 60.0
        )

        val splitSummary = SplitBillMath.summarizeFriendsOwed(splitBills)
        val pendingSplitsOwed = splitSummary.pendingFriendsAmount
        val pendingSplitBillCount = splitSummary.pendingBillCount

        val cutoffSummary = CutoffCalculator.summarizeCutoffShifts(
            shifts = shifts,
            referenceDate = today,
            config = settings.cutoffScheduleConfig()
        )

        val formatters = formattersFor(Locale.getDefault())
        val timeFormatter = formatters.timeFormatter
        val monthDayFormatter = formatters.monthDayFormatter

        // Upcoming Recurring Bill smart banner
        val dueSoonBill = recurringBills
            .filter { it.isEnabled && it.dueEpochDay >= today.toEpochDay() && it.dueEpochDay <= today.toEpochDay() + 7 }
            .minByOrNull { it.dueEpochDay }
        val upcomingBillBanner = dueSoonBill?.let { bill ->
            val dueDays = (bill.dueEpochDay - today.toEpochDay()).toInt()
            val dueLabel = when (dueDays) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> LocalDate.ofEpochDay(bill.dueEpochDay).format(monthDayFormatter)
            }
            UpcomingBillBannerInfo(
                billId = bill.id,
                title = bill.title,
                amount = bill.amount,
                dueEpochDay = bill.dueEpochDay,
                dueDateLabel = dueLabel
            )
        }

        // Payday / Cutoff Countdown smart banner
        val cutoffEndDate = cutoffSummary.window.endDate
        val daysUntilCutoff = (cutoffEndDate.toEpochDay() - today.toEpochDay()).toInt()
        val paydayCutoffBanner = if (daysUntilCutoff in 0..5) {
            PaydayCutoffBannerInfo(
                cutoffDayOfMonth = cutoffEndDate.dayOfMonth,
                daysRemaining = daysUntilCutoff,
                estimatedEarnings = cutoffSummary.totalEarnings
            )
        } else {
            null
        }

        val savingsGoalItems = rawGoals.map { goalWithDeps ->
            SavingsGoalCardItem(
                goalWithDeposits = goalWithDeps,
                progress = SavingsGoalMath.computeProgress(goalWithDeps, today),
                targetDate = LocalDate.ofEpochDay(goalWithDeps.goal.targetDateEpochDay)
            )
        }

        val mtdDenom = mtdExpenses.takeIf { it > 0.0 } ?: 1.0
        val monthlyTopCategories = mtdExpensesList
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategorySpendSummary(
                    category = cat,
                    totalAmount = sum,
                    fraction = (sum / mtdDenom).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.totalAmount }

        val shiftItems = shifts.take(15).map { shift ->
            val startStr = Instant.ofEpochMilli(shift.timeInMillis).atZone(zone).toLocalTime().format(timeFormatter)
            val endStr = Instant.ofEpochMilli(shift.timeOutMillis).atZone(zone).toLocalTime().format(timeFormatter)
            val hrs = shift.durationMinutes / 60
            val mins = shift.durationMinutes % 60
            val dur = if (hrs > 0 && mins > 0) "${hrs}h ${mins}m" else if (hrs > 0) "${hrs}h" else "${mins}m"
            TimelineActivityItem.ShiftActivity(
                id = shift.id,
                title = shift.overtimeNote.ifBlank { "Duty Shift" },
                timeRange = "$startStr – $endStr",
                durationLabel = dur,
                amount = shift.totalEarned,
                epochDay = shift.dateEpochDay,
                timestamp = shift.timeInMillis
            )
        }

        val extraItems = extraIncomes.take(10).map { inc ->
            val dateLabel = LocalDate.ofEpochDay(inc.dateEpochDay).format(monthDayFormatter)
            TimelineActivityItem.ExtraIncomeActivity(
                id = inc.id,
                title = inc.title,
                timeLabel = dateLabel,
                amount = inc.amount,
                epochDay = inc.dateEpochDay,
                timestamp = inc.createdAtMillis.takeIf { it > 0L } ?: (inc.dateEpochDay * 86_400_000L + inc.id),
                subtitleTag = "Recurring"
            )
        }

        val manualAllowanceItems = manualAllowances.take(10).map { entry ->
            val dateLabel = LocalDate.ofEpochDay(entry.dateEpochDay).format(monthDayFormatter)
            TimelineActivityItem.ExtraIncomeActivity(
                id = entry.id,
                title = entry.title,
                timeLabel = dateLabel,
                amount = entry.amount,
                epochDay = entry.dateEpochDay,
                timestamp = entry.createdAtMillis.takeIf { it > 0L } ?: (entry.dateEpochDay * 86_400_000L + entry.id),
                subtitleTag = "Allowance"
            )
        }

        val expenseItems = expenses.take(15).map { exp ->
            val dateLabel = LocalDate.ofEpochDay(exp.dateEpochDay).format(monthDayFormatter)
            TimelineActivityItem.ExpenseActivity(
                id = exp.id,
                title = exp.title,
                category = exp.category,
                timeLabel = dateLabel,
                amount = exp.amount,
                epochDay = exp.dateEpochDay,
                timestamp = exp.createdAtMillis.takeIf { it > 0L } ?: (exp.dateEpochDay * 86_400_000L + exp.id)
            )
        }

        val combinedTimeline = (shiftItems + extraItems + manualAllowanceItems + expenseItems)
            .sortedWith(compareByDescending<TimelineActivityItem> { it.sortEpochDay }.thenByDescending { it.sortTimestamp })

        val unreadCount = inboxItems.count { !it.isRead }
        val pendingToast = if (settings.notificationsEnabled) {
            inboxItems.firstOrNull { !it.isRead && !it.hasShownToast }
        } else {
            null
        }

        HomeUiState(
            userName = prefs.userName,
            currencySymbol = prefs.currency,
            hourlyRate = prefs.rate,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            mtdIncome = mtdIncome,
            mtdExpenses = mtdExpenses,
            monthlyBudget = prefs.budget,
            categoryBudgets = settings.categoryBudgets,
            managedCategories = settings.managedCategories,
            pendingSplitsOwed = pendingSplitsOwed,
            pendingSplitBillCount = pendingSplitBillCount,
            notifications = inboxItems,
            unreadNotificationCount = unreadCount,
            pendingHeadsUpNotification = pendingToast,
            isBalanceHidden = isBalanceHidden,
            upcomingBillBanner = upcomingBillBanner,
            paydayCutoffBanner = paydayCutoffBanner,
            cutoffSummary = cutoffSummary,
            savingsGoals = savingsGoalItems,
            currentMonthName = today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            monthlyTopCategories = monthlyTopCategories,
            monthlyHoursSummary = monthlyHoursSummary,
            timelineItems = combinedTimeline,
            activeTimer = prefs.activeTimer
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun toggleBalancePrivacy() {
        viewModelScope.launch {
            repository.setBalancePrivacyHidden(!uiState.value.isBalanceHidden)
        }
    }

    fun updateBudgets(monthlyBudget: Double, categoryBudgets: List<CategoryBudgetSetting>) {
        viewModelScope.launch {
            repository.updateMonthlyBudget(monthlyBudget)
            repository.updateCategoryBudgets(categoryBudgets)
        }
    }

    fun updateManagedCategories(items: List<ManagedCategorySetting>) {
        viewModelScope.launch {
            repository.updateManagedCategories(items)
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markNotificationToastShown(id: String) {
        viewModelScope.launch {
            repository.markNotificationToastShown(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearNotificationInbox()
        }
    }

    fun createSavingsGoal(title: String, targetAmount: Double, targetDateEpochDay: Long) {
        viewModelScope.launch {
            repository.createSavingsGoal(title, targetAmount, targetDateEpochDay)
        }
    }

    fun setActiveSavingsGoal(goalId: Long) {
        viewModelScope.launch {
            repository.setActiveSavingsGoal(goalId)
        }
    }

    fun addSavingsDeposit(goal: SavingsGoalEntity, amount: Double, note: String) {
        viewModelScope.launch {
            repository.addSavingsDeposit(goal = goal, amount = amount, note = note)
        }
    }

    fun deleteSavingsDeposit(deposit: SavingsDepositEntity) {
        viewModelScope.launch {
            repository.deleteSavingsDeposit(deposit)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
