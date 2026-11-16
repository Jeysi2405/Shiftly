package com.example.shiftly.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.screens.lock.canUseBiometrics
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralSoftContainer
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.workers.ShiftlyDailyWorker
import java.io.File

private enum class ActiveSettingsSheet {
    ACCOUNT,
    CURRENCY,
    PAY_CYCLE,
    SHIFT_RULES,
    BUDGETS,
    SAVINGS_GOAL,
    CATEGORIES,
    RECURRING,
    APP_LOCK,
    BACKUP_EXPORT
}

@Composable
fun ProfileScreen(
    repository: ShiftlyRepository,
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val suppressNextRelock = LocalSuppressNextRelock.current
    var activeSheet by remember { mutableStateOf<ActiveSettingsSheet?>(null) }
    var pendingFileRestoreMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingNotificationAction
        pendingNotificationAction = null
        if (granted) {
            action?.invoke()
            ShiftlyDailyWorker.triggerImmediateCheck(context)
        } else {
            viewModel.updateNotificationsEnabled(false)
            onShowMessage("Notification permission denied — enable in Android Settings")
        }
    }

    fun ensureNotificationPermissionThen(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
            ShiftlyDailyWorker.triggerImmediateCheck(context)
        } else {
            pendingNotificationAction = onGranted
            suppressNextRelock()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val backupFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            onShowMessage("Could not read selected backup file")
            return@rememberLauncherForActivityResult
        }
        viewModel.restoreBackupBytes(bytes, pendingFileRestoreMode) { result ->
            activeSheet = null
            result.fold(
                onSuccess = { msg -> onShowMessage(msg) },
                onFailure = { err -> onShowMessage("Restore failed: ${err.message ?: "invalid backup"}") }
            )
        }
    }

    val triggerZipShare: () -> Unit = {
        viewModel.exportBackupZip { zipFile ->
            suppressNextRelock()
            shareExportFile(
                context = context,
                file = zipFile,
                mimeType = "application/zip",
                chooserTitle = "Export Full Shiftly Backup (.zip)"
            )
            onShowMessage("Full backup archive ready to share")
        }
    }

    val triggerJsonShare: () -> Unit = {
        viewModel.exportBackup { backupJson ->
            suppressNextRelock()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Shiftly Backup JSON")
                putExtra(Intent.EXTRA_TEXT, backupJson)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export Shiftly Backup JSON"))
        }
    }

    val triggerCsvShare: () -> Unit = {
        viewModel.exportCsv { csvFile ->
            suppressNextRelock()
            shareExportFile(
                context = context,
                file = csvFile,
                mimeType = "text/csv",
                chooserTitle = "Export Shiftly CSV"
            )
            onShowMessage("CSV report ready to share")
        }
    }

    val triggerPdfShare: () -> Unit = {
        viewModel.exportPdf { pdfFile ->
            suppressNextRelock()
            shareExportFile(
                context = context,
                file = pdfFile,
                mimeType = "application/pdf",
                chooserTitle = "Export Shiftly PDF Statement"
            )
            onShowMessage("PDF statement ready to share")
        }
    }

    ProfileScreenContent(
        state = state,
        onExportBackup = triggerZipShare,
        onOpenRestore = { activeSheet = ActiveSettingsSheet.BACKUP_EXPORT },
        onExportCsv = triggerCsvShare,
        onExportPdf = triggerPdfShare,
        onOpenSheet = { activeSheet = it },
        onToggleNotifications = { enabled ->
            if (enabled) {
                ensureNotificationPermissionThen {
                    viewModel.updateNotificationsEnabled(true)
                    onShowMessage("Notifications enabled")
                }
            } else {
                viewModel.updateNotificationsEnabled(false)
                onShowMessage("Notifications paused")
            }
        },
        onToggleAppLock = { enabled ->
            val method = state.settings.appLockMethod
            val needsPinSetup = enabled && method != "Fingerprint Only" && !state.settings.hasAppLockPin
            val needsBiometricReview = enabled && method == "Fingerprint Only" && !canUseBiometrics(context)
            if (needsPinSetup || needsBiometricReview) {
                activeSheet = ActiveSettingsSheet.APP_LOCK
                onShowMessage(
                    if (needsPinSetup) "Set a 4-digit PIN to enable App Lock"
                    else "Review lock method—no biometrics detected"
                )
            } else {
                viewModel.updateAppLock(
                    enabled = enabled,
                    method = method,
                    newPin = null
                )
                onShowMessage(if (enabled) "App lock active ($method)" else "App lock turned off")
            }
        }
    )

    when (activeSheet) {
        ActiveSettingsSheet.ACCOUNT -> {
            SettingsSheetContainer(
                title = "Account Profile",
                onDismiss = { activeSheet = null }
            ) {
                AccountEditSheetContent(
                    currentName = state.userName,
                    hourlyRate = state.defaultHourlyRate,
                    currencySymbol = state.currencySymbol,
                    onSave = { newName ->
                        viewModel.updateUserName(newName)
                        activeSheet = null
                        onShowMessage("Profile updated")
                    }
                )
            }
        }

        ActiveSettingsSheet.CURRENCY -> {
            SettingsSheetContainer(
                title = "Currency",
                onDismiss = { activeSheet = null }
            ) {
                CurrencyPickerSheetContent(
                    currentCurrency = state.currencySymbol,
                    onApplyCurrency = { symbol, conversionRate ->
                        viewModel.changeCurrency(symbol, conversionRate)
                        activeSheet = null
                        onShowMessage(
                            if (conversionRate != null) "Converted amounts to $symbol"
                            else "Currency changed to $symbol"
                        )
                    }
                )
            }
        }

        ActiveSettingsSheet.PAY_CYCLE -> {
            SettingsSheetContainer(
                title = "Pay Cycle & Reminders",
                onDismiss = { activeSheet = null }
            ) {
                PayCycleSheetContent(
                    currentSchedule = state.settings.payCycleSchedule,
                    currentPaydayReminder = state.settings.paydayReminderEnabled,
                    currentShiftReminder = state.settings.shiftReminderEnabled,
                    onSave = { schedule, paydayRem, shiftRem ->
                        if ((paydayRem || shiftRem) && state.settings.notificationsEnabled) {
                            ensureNotificationPermissionThen {
                                viewModel.updatePayCycle(schedule, paydayRem, shiftRem)
                                activeSheet = null
                                onShowMessage("Pay cycle saved ($schedule)")
                            }
                        } else {
                            viewModel.updatePayCycle(schedule, paydayRem, shiftRem)
                            activeSheet = null
                            onShowMessage("Pay cycle saved ($schedule)")
                        }
                    }
                )
            }
        }

        ActiveSettingsSheet.SHIFT_RULES -> {
            SettingsSheetContainer(
                title = "Night Diff & Overtime Rules",
                onDismiss = { activeSheet = null }
            ) {
                ShiftRulesSheetContent(
                    currentNdStartHour = state.settings.nightDiffStartHour,
                    currentNdMultiplier = state.settings.nightDiffMultiplier,
                    currentOtMultiplier = state.settings.overtimeMultiplier,
                    onSave = { ndStart, ndMult, otMult ->
                        viewModel.updateShiftRules(ndStart, ndMult, otMult)
                        activeSheet = null
                        onShowMessage("Shift wage rules updated")
                    }
                )
            }
        }

        ActiveSettingsSheet.BUDGETS -> {
            SettingsSheetContainer(
                title = "Monthly & Category Budgets",
                onDismiss = { activeSheet = null }
            ) {
                BudgetsEditorSheetContent(
                    currencySymbol = state.currencySymbol,
                    currentMonthlyBudget = state.monthlyBudget,
                    currentCategoryBudgets = state.settings.categoryBudgets,
                    managedCategories = state.settings.managedCategories,
                    onSave = { monthly, categoryList ->
                        viewModel.updateBudgets(monthly, categoryList)
                        activeSheet = null
                        onShowMessage("Budgets saved")
                    }
                )
            }
        }

        ActiveSettingsSheet.SAVINGS_GOAL -> {
            SettingsSheetContainer(
                title = "Savings Goal Setup",
                onDismiss = { activeSheet = null }
            ) {
                SavingsGoalSheetContent(
                    currencySymbol = state.currencySymbol,
                    currentTitle = state.settings.savingsGoalTitle,
                    currentTarget = state.settings.savingsGoalTarget,
                    currentBuffer = state.savingsBuffer,
                    onSave = { title, target, buffer ->
                        viewModel.updateSavingsGoal(title, target, buffer)
                        activeSheet = null
                        onShowMessage("Savings goal updated")
                    }
                )
            }
        }

        ActiveSettingsSheet.CATEGORIES -> {
            SettingsSheetContainer(
                title = "Manage Categories",
                onDismiss = { activeSheet = null }
            ) {
                ManageCategoriesSheetContent(
                    currentCategories = state.settings.managedCategories,
                    onSave = { updated ->
                        viewModel.updateManagedCategories(updated)
                        activeSheet = null
                        onShowMessage("Categories updated")
                    }
                )
            }
        }

        ActiveSettingsSheet.RECURRING -> {
            val visibleExpenseCategories = state.settings.managedCategories
                .filter {
                    it.isVisible &&
                        it.name.isNotBlank() &&
                        !it.name.equals("Split Bill", ignoreCase = true)
                }
                .map { it.name.trim() }
                .ifEmpty { ExpenseViewModel.EXPENSE_CATEGORIES.filter { it != "Split Bill" } }

            SettingsSheetContainer(
                title = "Recurring Entries",
                onDismiss = { activeSheet = null }
            ) {
                RecurringEntriesSheetContent(
                    currencySymbol = state.currencySymbol,
                    currentEntries = state.settings.recurringEntries,
                    availableCategories = visibleExpenseCategories,
                    onSave = { updated ->
                        viewModel.updateRecurringEntries(updated) { posted ->
                            activeSheet = null
                            onShowMessage(
                                if (posted > 0) "Saved & posted $posted recurring entry(s)"
                                else "Recurring entries saved"
                            )
                        }
                    }
                )
            }
        }

        ActiveSettingsSheet.APP_LOCK -> {
            SettingsSheetContainer(
                title = "App Lock Security",
                onDismiss = { activeSheet = null }
            ) {
                AppLockSheetContent(
                    currentEnabled = state.settings.appLockEnabled,
                    currentMethod = state.settings.appLockMethod,
                    hasSavedPin = state.settings.hasAppLockPin,
                    onSave = { enabled, method, newPin ->
                        viewModel.updateAppLock(enabled, method, newPin)
                        activeSheet = null
                        onShowMessage(if (enabled) "App lock saved ($method)" else "App lock disabled")
                    }
                )
            }
        }

        ActiveSettingsSheet.BACKUP_EXPORT -> {
            SettingsSheetContainer(
                title = "Backup, Restore & Export",
                onDismiss = { activeSheet = null }
            ) {
                BackupAndExportSheetContent(
                    onExportZip = {
                        activeSheet = null
                        triggerZipShare()
                    },
                    onExportJson = {
                        activeSheet = null
                        triggerJsonShare()
                    },
                    onExportCsv = {
                        activeSheet = null
                        triggerCsvShare()
                    },
                    onExportPdf = {
                        activeSheet = null
                        triggerPdfShare()
                    },
                    onPickBackupFile = { mode ->
                        pendingFileRestoreMode = mode
                        suppressNextRelock()
                        backupFilePicker.launch(arrayOf("application/zip", "application/json", "text/plain", "*/*"))
                    },
                    onRestoreJson = { rawJson, mode ->
                        viewModel.restoreBackup(rawJson, mode) { result ->
                            activeSheet = null
                            result.fold(
                                onSuccess = { msg -> onShowMessage(msg) },
                                onFailure = { err -> onShowMessage("Restore failed: ${err.message ?: "invalid JSON"}") }
                            )
                        }
                    }
                )
            }
        }

        null -> Unit
    }
}

@Composable
private fun ProfileScreenContent(
    state: ProfileUiState,
    onExportBackup: () -> Unit,
    onOpenRestore: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onOpenSheet: (ActiveSettingsSheet) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val currency = state.currencySymbol
    val ndHourFormatted = formatRuleHour(state.settings.nightDiffStartHour)
    val ndBonusPct = ((state.settings.nightDiffMultiplier - 1.0) * 100).toInt()
    val visibleCategoriesCount = state.settings.managedCategories.count { it.isVisible }
    val activeRecurringCount = state.settings.recurringEntries.count { it.isEnabled }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShiftlyScreenHeader(
                title = "Profile & Settings",
                subtitle = "${state.userName} • Student tracker",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ProfileSnapshotGlassCard(
                    currencySymbol = currency,
                    totalShifts = state.totalShifts,
                    totalHoursWorked = state.totalHoursWorked,
                    totalEarnings = state.totalEarnings,
                    totalExpenses = state.totalExpenses
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ProfileBackupGlassCard(
                    onExportBackup = onExportBackup,
                    onOpenRestore = onOpenRestore,
                    onExportCsv = onExportCsv,
                    onExportPdf = onExportPdf
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 520.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = DarkSurface,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .padding(bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(DarkBorder)
                        )
                    }

                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextLightPrimary
                    )

                    SettingsGroupCard(groupTitle = "ACCOUNT & PREFERENCES") {
                        SettingsChevronRow(
                            icon = Icons.Outlined.Person,
                            title = "Account",
                            subtitle = "${state.userName} • Rate $currency${"%.2f".format(state.defaultHourlyRate)}/hr",
                            onClick = { onOpenSheet(ActiveSettingsSheet.ACCOUNT) }
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            icon = Icons.Outlined.CurrencyExchange,
                            title = "Currency",
                            subtitle = "Active display & input currency",
                            trailingBadge = currency,
                            iconBg = MintSoftContainer,
                            iconTint = MintInkOnWhite,
                            onClick = { onOpenSheet(ActiveSettingsSheet.CURRENCY) }
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(start = 66.dp))
                        SettingsSwitchRow(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            subtitle = if (state.settings.notificationsEnabled) {
                                "Shift reminders & payday alerts active"
                            } else {
                                "All reminders muted"
                            },
                            checked = state.settings.notificationsEnabled,
                            onCheckedChange = onToggleNotifications,
                            onRowClick = { onOpenSheet(ActiveSettingsSheet.PAY_CYCLE) }
                        )
                    }

                    SettingsGroupCard(groupTitle = "PAY, RULES & BUDGETS") {
                        SettingsChevronRow(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "Pay Cycle",
                            subtitle = buildPayCycleSubtitle(state.settings),
                            trailingBadge = state.settings.payCycleSchedule,
                            onClick = { onOpenSheet(ActiveSettingsSheet.PAY_CYCLE) }
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            icon = Icons.Outlined.NightlightRound,
                            title = "Night Diff & Overtime Rules",
                            subtitle = "Starts $ndHourFormatted • ND +$ndBonusPct% • OT ${"%.2f".format(state.settings.overtimeMultiplier)}x",
                            iconBg = MintSoftContainer,
                            iconTint = MintInkOnWhite,
                            onClick = { onOpenSheet(ActiveSettingsSheet.SHIFT_RULES) }
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            title = "Monthly & Category Budgets",
                            subtitle = "Monthly cap $currency${"%,.0f".format(state.monthlyBudget)} • ${state.settings.categoryBudgets.size} limits",
                            iconBg = CoralSoftContainer,
                            iconTint = CoralInkOnWhite,
                            onClick = { onOpenSheet(ActiveSettingsSheet.BUDGETS) }
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            icon = Icons.Outlined.Savings,
                            title = "Savings Goals Setup",
                            subtitle = "${state.settings.savingsGoalTitle} • Target $currency${"%,.0f".format(state.settings.savingsGoalTarget)}",
                            iconBg = MintSoftContainer,
                            iconTint = MintInkOnWhite,
                            onClick = { onOpenSheet(ActiveSettingsSheet.SAVINGS_GOAL) }
                        )
                    }

                    SettingsGroupCard(groupTitle = "CATEGORIES & RECURRING") {
                        SettingsChevronRow(
                            icon = Icons.Outlined.Category,
                            title = "Manage Categories",
                            subtitle = "Add, edit, or hide ($visibleCategoriesCount visible)",
                            onClick = { onOpenSheet(ActiveSettingsSheet.CATEGORIES) }
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            icon = Icons.Outlined.Repeat,
                            title = "Recurring Entries",
                            subtitle = if (activeRecurringCount > 0) "$activeRecurringCount active rules" else "Tap to add weekly or monthly rules",
                            iconBg = MintSoftContainer,
                            iconTint = MintInkOnWhite,
                            onClick = { onOpenSheet(ActiveSettingsSheet.RECURRING) }
                        )
                    }

                    SettingsGroupCard(groupTitle = "SECURITY & DATA EXPORT") {
                        SettingsSwitchRow(
                            icon = Icons.Outlined.Fingerprint,
                            title = "App Lock",
                            subtitle = if (state.settings.appLockEnabled) {
                                "${state.settings.appLockMethod} enabled"
                            } else {
                                "Protect balances with PIN or fingerprint"
                            },
                            checked = state.settings.appLockEnabled,
                            onCheckedChange = onToggleAppLock,
                            onRowClick = { onOpenSheet(ActiveSettingsSheet.APP_LOCK) },
                            iconBg = AzureSoftContainer,
                            iconTint = AzureInkOnWhite
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            icon = Icons.Outlined.Backup,
                            title = "Data Export & Restore",
                            subtitle = "ZIP + receipts, JSON backup, CSV sheet & PDF",
                            trailingBadge = "ZIP • PDF",
                            onClick = { onOpenSheet(ActiveSettingsSheet.BACKUP_EXPORT) }
                        )
                    }
                }
            }
        }
    }
}

private fun formatRuleHour(hour24: Int): String {
    val normalized = ((hour24 % 24) + 24) % 24
    return when {
        normalized == 0 -> "12 AM"
        normalized < 12 -> "$normalized AM"
        normalized == 12 -> "12 PM"
        else -> "${normalized - 12} PM"
    }
}

private fun buildPayCycleSubtitle(settings: ShiftlySettingsSnapshot): String {
    val active = buildList {
        if (settings.paydayReminderEnabled) add("Payday")
        if (settings.shiftReminderEnabled) add("Shift alerts")
    }
    return if (active.isEmpty()) "Reminders off" else active.joinToString(" & ") + " active"
}

private fun shareExportFile(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: String
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

// =============================================================================
// High-Fidelity Previews (Snapshot + Backup, and Grouped Settings List)
// =============================================================================

private fun previewProfileState(): ProfileUiState = ProfileUiState(
    userName = "Maya",
    defaultHourlyRate = 96.38,
    currencySymbol = "₱",
    monthlyBudget = 3000.0,
    savingsBuffer = 1000.0,
    totalShifts = 18,
    totalHoursWorked = 82.5,
    totalEarnings = 8420.0,
    totalExpenses = 3190.0,
    settings = ShiftlySettingsSnapshot(
        notificationsEnabled = true,
        payCycleSchedule = "15th & 30th",
        paydayReminderEnabled = true,
        shiftReminderEnabled = true,
        nightDiffStartHour = 22,
        nightDiffMultiplier = 1.10,
        overtimeMultiplier = 1.25,
        savingsGoalTitle = "Tuition & Laptop Fund",
        savingsGoalTarget = 5000.0,
        categoryBudgets = listOf(
            CategoryBudgetSetting("Food", 1400.0),
            CategoryBudgetSetting("Transport", 600.0),
            CategoryBudgetSetting("Supplies", 550.0),
            CategoryBudgetSetting("Bills", 450.0)
        ),
        managedCategories = listOf(
            ManagedCategorySetting("Food", true),
            ManagedCategorySetting("Transport", true),
            ManagedCategorySetting("Bills", true),
            ManagedCategorySetting("Supplies", true),
            ManagedCategorySetting("Health", true),
            ManagedCategorySetting("Misc", true)
        ),
        recurringEntries = listOf(
            RecurringEntrySetting("r1", "Weekly Allowance", 1500.0, "Weekly", isIncome = true, isEnabled = true),
            RecurringEntrySetting("r2", "Campus Wi-Fi & Cloud", 199.0, "Monthly", isIncome = false, isEnabled = true, category = "Bills")
        ),
        appLockEnabled = true,
        appLockMethod = "PIN & Fingerprint",
        hasAppLockPin = true,
        appLockPinHash = ""
    )
)

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "3. Profile Snapshot & Backup")
@Composable
private fun ProfileSnapshotAndBackupPreview() {
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                ProfileScreenContent(
                    state = previewProfileState(),
                    onExportBackup = {},
                    onOpenRestore = {},
                    onExportCsv = {},
                    onExportPdf = {},
                    onOpenSheet = {},
                    onToggleNotifications = {},
                    onToggleAppLock = {}
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Profile },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "4. Profile Settings List")
@Composable
private fun ProfileSettingsListPreview() {
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                ProfileScreenContent(
                    state = previewProfileState(),
                    onExportBackup = {},
                    onOpenRestore = {},
                    onExportCsv = {},
                    onExportPdf = {},
                    onOpenSheet = {},
                    onToggleNotifications = {},
                    onToggleAppLock = {},
                    listState = rememberLazyListState(initialFirstVisibleItemIndex = 3)
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Profile },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
