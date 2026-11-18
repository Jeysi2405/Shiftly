package com.example.shiftly.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shiftly.R
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.ProfilePhotoHelper
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.screens.lock.canUseBiometrics
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.workers.ShiftlyDailyWorker
import java.io.File
import java.time.LocalDate

private enum class ActiveSettingsSheet {
    ACCOUNT,
    CURRENCY,
    NOTIFICATIONS,
    THEME,
    BUDGETS,
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
    var showSavingsGoalDetailSheet by remember { mutableStateOf(false) }
    var showAddSavingsGoalSheet by remember { mutableStateOf(false) }
    var pendingFileRestoreMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val profilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val savedPath = ProfilePhotoHelper.saveProfilePhotoFromUri(context, uri)
        if (savedPath != null) {
            viewModel.updateProfilePhotoPath(savedPath)
            onShowMessage("Profile photo updated")
        } else {
            onShowMessage("Could not load selected photo")
        }
    }

    val launchProfilePhotoPicker: () -> Unit = {
        suppressNextRelock()
        profilePhotoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

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
        viewModel.exportBackupZip { result ->
            result.fold(
                onSuccess = { zipFile ->
                    suppressNextRelock()
                    val launched = shareExportFile(
                        context = context,
                        file = zipFile,
                        mimeType = "application/zip",
                        chooserTitle = "Export Full Shiftly Backup (.zip)"
                    )
                    onShowMessage(
                        if (launched) "Full backup archive ready to share"
                        else "No app available to share this file"
                    )
                },
                onFailure = { err ->
                    onShowMessage("Export failed: ${err.message ?: "storage error"}")
                }
            )
        }
    }

    val triggerJsonShare: () -> Unit = {
        viewModel.exportBackup { result ->
            result.fold(
                onSuccess = { backupJson ->
                    suppressNextRelock()
                    val launched = runCatching {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Shiftly Backup JSON")
                            putExtra(Intent.EXTRA_TEXT, backupJson)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export Shiftly Backup JSON"))
                    }.isSuccess
                    if (!launched) {
                        onShowMessage("No app available to share this backup")
                    }
                },
                onFailure = { err ->
                    onShowMessage("Export failed: ${err.message ?: "storage error"}")
                }
            )
        }
    }

    val triggerCsvShare: () -> Unit = {
        viewModel.exportCsv { result ->
            result.fold(
                onSuccess = { csvFile ->
                    suppressNextRelock()
                    val launched = shareExportFile(
                        context = context,
                        file = csvFile,
                        mimeType = "text/csv",
                        chooserTitle = "Export Shiftly CSV"
                    )
                    onShowMessage(
                        if (launched) "CSV report ready to share"
                        else "No app available to share this file"
                    )
                },
                onFailure = { err ->
                    onShowMessage("Export failed: ${err.message ?: "storage error"}")
                }
            )
        }
    }

    val triggerPdfShare: () -> Unit = {
        viewModel.exportPdf { result ->
            result.fold(
                onSuccess = { pdfFile ->
                    suppressNextRelock()
                    val launched = shareExportFile(
                        context = context,
                        file = pdfFile,
                        mimeType = "application/pdf",
                        chooserTitle = "Export Shiftly PDF Statement"
                    )
                    onShowMessage(
                        if (launched) "PDF statement ready to share"
                        else "No app available to share this file"
                    )
                },
                onFailure = { err ->
                    onShowMessage("Export failed: ${err.message ?: "storage error"}")
                }
            )
        }
    }

    ProfileScreenContent(
        state = state,
        onAvatarClick = launchProfilePhotoPicker,
        onSaveWageRates = { hourlyRate, minWagePerDay ->
            viewModel.updateWageRates(hourlyRate, minWagePerDay)
        },
        onShowMessage = onShowMessage,
        onOpenSheet = { activeSheet = it },
        onOpenSavingsGoals = {
            if (state.savingsGoals.isEmpty()) {
                showAddSavingsGoalSheet = true
            } else {
                showSavingsGoalDetailSheet = true
            }
        },
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

    if (showSavingsGoalDetailSheet) {
        val activeGoalId = (state.savingsGoals.firstOrNull { it.goal.isActive }
            ?: state.savingsGoals.firstOrNull())?.goal?.id
        SavingsGoalDetailSheet(
            goals = state.savingsGoals,
            initialGoalId = activeGoalId,
            currencySymbol = state.currencySymbol,
            onDismiss = { showSavingsGoalDetailSheet = false },
            onSelectActiveGoal = { goalId -> viewModel.setActiveSavingsGoal(goalId) },
            onOpenAddNewGoal = {
                showSavingsGoalDetailSheet = false
                showAddSavingsGoalSheet = true
            },
            onAddDeposit = { goal, amount, note ->
                viewModel.addSavingsDeposit(goal, amount, note)
                onShowMessage("Saved ${ShiftlyFormatters.formatCurrency(state.currencySymbol, amount)} & logged expense")
            },
            onDeleteDeposit = { dep ->
                viewModel.deleteSavingsDeposit(dep)
                onShowMessage("Savings deposit removed")
            },
            onDeleteGoal = { goal ->
                viewModel.deleteSavingsGoal(goal)
                onShowMessage("Savings goal deleted")
            }
        )
    }

    if (showAddSavingsGoalSheet) {
        AddSavingsGoalSheet(
            currencySymbol = state.currencySymbol,
            onDismiss = { showAddSavingsGoalSheet = false },
            onCreateGoal = { title, targetAmount, targetDateEpochDay ->
                viewModel.createSavingsGoal(title, targetAmount, targetDateEpochDay)
                showAddSavingsGoalSheet = false
                onShowMessage("Savings goal \"$title\" added")
            }
        )
    }

    when (activeSheet) {
        ActiveSettingsSheet.ACCOUNT -> {
            SettingsSheetContainer(
                title = "Account Profile",
                onDismiss = { activeSheet = null }
            ) {
                AccountEditSheetContent(
                    currentName = state.userName,
                    profilePhotoPath = state.profilePhotoPath,
                    hourlyRate = state.defaultHourlyRate,
                    currencySymbol = state.currencySymbol,
                    onChangePhoto = launchProfilePhotoPicker,
                    onRemovePhoto = {
                        viewModel.updateProfilePhotoPath(null)
                        onShowMessage("Profile photo removed")
                    },
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

        ActiveSettingsSheet.NOTIFICATIONS -> {
            SettingsSheetContainer(
                title = "Notifications",
                onDismiss = { activeSheet = null }
            ) {
                NotificationsSheetContent(
                    notificationsEnabled = state.settings.notificationsEnabled,
                    billReminderEnabled = state.settings.billReminderEnabled,
                    shiftReminderEnabled = state.settings.shiftReminderEnabled,
                    savingsReminderEnabled = state.settings.savingsReminderEnabled,
                    onSave = { enabled, bills, shifts, savings ->
                        if (enabled && (bills || shifts || savings)) {
                            ensureNotificationPermissionThen {
                                viewModel.updateNotificationPreferences(enabled, bills, shifts, savings)
                                activeSheet = null
                                onShowMessage("Notification preferences saved")
                            }
                        } else {
                            viewModel.updateNotificationPreferences(enabled, bills, shifts, savings)
                            activeSheet = null
                            onShowMessage("Notification preferences saved")
                        }
                    }
                )
            }
        }

        ActiveSettingsSheet.THEME -> {
            SettingsSheetContainer(
                title = "App Theme",
                onDismiss = { activeSheet = null }
            ) {
                ThemeSelectorSheetContent(
                    currentThemeMode = state.settings.themeMode,
                    onSave = { mode ->
                        viewModel.updateThemeMode(mode)
                        activeSheet = null
                        onShowMessage(
                            "Theme set to ${
                                when (mode.uppercase()) {
                                    "LIGHT" -> "Light Mode"
                                    "DARK" -> "Dark Mode"
                                    else -> "System Default"
                                }
                            }"
                        )
                    }
                )
            }
        }

        ActiveSettingsSheet.BUDGETS -> {
            SettingsSheetContainer(
                title = "Monthly & Category Budgets",
                showTopClose = false,
                onDismiss = { activeSheet = null }
            ) {
                BudgetsEditorSheetContent(
                    currencySymbol = state.currencySymbol,
                    currentMonthlyBudget = state.monthlyBudget,
                    currentCategoryBudgets = state.settings.categoryBudgets,
                    managedCategories = state.settings.managedCategories,
                    onDismiss = { activeSheet = null },
                    onSave = { monthly, categoryList ->
                        viewModel.updateBudgets(monthly, categoryList)
                        activeSheet = null
                        onShowMessage("Budgets saved")
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
    onAvatarClick: () -> Unit,
    onSaveWageRates: (hourlyRate: Double, minimumWagePerDay: Double) -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenSheet: (ActiveSettingsSheet) -> Unit,
    onOpenSavingsGoals: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val colors = ShiftlyColors.current
    val currency = state.currencySymbol
    val hasPhoto = !state.profilePhotoPath.isNullOrBlank() && File(state.profilePhotoPath).exists()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(onClick = onAvatarClick)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                            .border(1.5.dp, colors.azureAccent.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasPhoto && state.profilePhotoPath != null) {
                            AsyncImage(
                                model = File(state.profilePhotoPath),
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_user),
                                contentDescription = "Add profile photo",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(colors.azureAccent)
                            .border(1.5.dp, colors.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_camera),
                            contentDescription = null,
                            tint = colors.onAzurePrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                ShiftlyScreenHeader(
                    title = "Profile & Settings",
                    modifier = Modifier.weight(1f)
                )
            }
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
                ProfileWageRateCard(
                    defaultHourlyRate = state.defaultHourlyRate,
                    minimumWagePerDay = state.minimumWagePerDay,
                    currencySymbol = currency,
                    onSaveRates = onSaveWageRates,
                    onShowMessage = onShowMessage
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 520.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 0.dp
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
                                .background(colors.border)
                        )
                    }

                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    SettingsGroupCard(groupTitle = "ACCOUNT & PREFERENCES") {
                        SettingsChevronRow(
                            iconRes = R.drawable.ic_lucide_user,
                            title = "Account",
                            onClick = { onOpenSheet(ActiveSettingsSheet.ACCOUNT) }
                        )
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            iconRes = R.drawable.ic_lucide_circle_dollar_sign,
                            title = "Currency",
                            trailingBadge = currency,
                            iconBg = colors.mintContainer,
                            iconTint = colors.mintInk,
                            onClick = { onOpenSheet(ActiveSettingsSheet.CURRENCY) }
                        )
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(start = 66.dp))
                        SettingsSwitchRow(
                            iconRes = R.drawable.ic_lucide_bell,
                            title = "Notifications",
                            checked = state.settings.notificationsEnabled,
                            onCheckedChange = onToggleNotifications,
                            onRowClick = { onOpenSheet(ActiveSettingsSheet.NOTIFICATIONS) }
                        )
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            iconRes = R.drawable.ic_lucide_palette,
                            title = "App Theme",
                            subtitle = when (state.settings.themeMode.uppercase()) {
                                "LIGHT" -> "Light Mode"
                                "DARK" -> "Dark Mode"
                                else -> "System Default"
                            },
                            iconBg = colors.coralContainer,
                            iconTint = colors.coralInk,
                            onClick = { onOpenSheet(ActiveSettingsSheet.THEME) }
                        )
                    }

                    SettingsGroupCard(groupTitle = "CATEGORIES & PREFERENCES") {
                        SettingsChevronRow(
                            iconRes = R.drawable.ic_lucide_tags,
                            title = "Manage Categories",
                            onClick = { onOpenSheet(ActiveSettingsSheet.CATEGORIES) }
                        )
                    }

                    SettingsGroupCard(groupTitle = "SECURITY & DATA EXPORT") {
                        SettingsSwitchRow(
                            iconRes = R.drawable.ic_lucide_fingerprint,
                            title = "App Lock",
                            checked = state.settings.appLockEnabled,
                            onCheckedChange = onToggleAppLock,
                            onRowClick = { onOpenSheet(ActiveSettingsSheet.APP_LOCK) },
                            iconBg = colors.azureContainer,
                            iconTint = colors.azureInk
                        )
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(start = 66.dp))
                        SettingsChevronRow(
                            iconRes = R.drawable.ic_lucide_cloud_upload,
                            title = "Data Export & Restore",
                            trailingBadge = "ZIP • PDF",
                            onClick = { onOpenSheet(ActiveSettingsSheet.BACKUP_EXPORT) }
                        )
                    }
                }
            }
        }
    }
}

private fun shareExportFile(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: String
): Boolean {
    return runCatching {
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
    }.isSuccess
}

// =============================================================================
// High-Fidelity Previews
// =============================================================================

private fun previewProfileState(): ProfileUiState = ProfileUiState(
    userName = "James",
    defaultHourlyRate = 96.38,
    minimumWagePerDay = 645.0,
    currencySymbol = "₱",
    monthlyBudget = 3000.0,
    savingsBuffer = 0.0,
    totalShifts = 18,
    totalHoursWorked = 82.5,
    totalEarnings = 8420.0,
    totalExpenses = 3190.0,
    savingsGoals = listOf(
        SavingsGoalWithDeposits(
            goal = SavingsGoalEntity(
                id = 1L,
                title = "Laptop Fund",
                targetAmount = 5000.0,
                targetDateEpochDay = LocalDate.of(2026, 12, 15).toEpochDay(),
                isActive = true
            ),
            deposits = emptyList()
        )
    ),
    settings = ShiftlySettingsSnapshot(
        notificationsEnabled = true,
        payCycleSchedule = "15th & 30th",
        paydayReminderEnabled = true,
        shiftReminderEnabled = true,
        billReminderEnabled = true,
        savingsReminderEnabled = true,
        savingsGoalTitle = "Laptop Fund",
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
            ManagedCategorySetting("Savings", true),
            ManagedCategorySetting("Other", true)
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "3. Profile Snapshot & Wage")
@Composable
private fun ProfileSnapshotAndBackupPreview() {
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                ProfileScreenContent(
                    state = previewProfileState(),
                    onAvatarClick = {},
                    onSaveWageRates = { _, _ -> },
                    onShowMessage = {},
                    onOpenSheet = {},
                    onOpenSavingsGoals = {},
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
                    onAvatarClick = {},
                    onSaveWageRates = { _, _ -> },
                    onShowMessage = {},
                    onOpenSheet = {},
                    onOpenSavingsGoals = {},
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
