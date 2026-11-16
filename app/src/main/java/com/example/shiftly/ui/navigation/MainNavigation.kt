package com.example.shiftly.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import com.example.shiftly.utils.ShiftlyFormatters
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.input.KeyboardType
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.TextLightMuted
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.screens.AnalyticsScreen
import com.example.shiftly.ui.screens.HomeScreen
import com.example.shiftly.ui.screens.ToolsScreen
import com.example.shiftly.ui.screens.WelcomeScreen
import com.example.shiftly.ui.screens.duties.DutiesScreen
import com.example.shiftly.ui.screens.duties.DutiesViewModel
import com.example.shiftly.ui.screens.duties.ManualShiftBottomSheet
import com.example.shiftly.ui.screens.expense.AddExpenseBottomSheet
import com.example.shiftly.ui.screens.expense.ExpenseScreen
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.screens.lock.AppLockGateScreen
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.screens.profile.ProfileScreen
import com.example.shiftly.ui.screens.splitbill.SplitBillScreen
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralSoftContainer
import com.example.shiftly.ui.theme.DarkBackground
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.workers.ShiftlyDailyWorker
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val repository = remember { ShiftlyRepository(context.applicationContext) }
    val hasCompletedOnboarding by repository.hasCompletedOnboarding
        .collectAsStateWithLifecycle(initialValue = null)
    val settings by repository.settingsSnapshot
        .collectAsStateWithLifecycle(initialValue = null)
    val userName by repository.userName
        .collectAsStateWithLifecycle(initialValue = "")
    val scope = rememberCoroutineScope()

    var hasInitializedLock by rememberSaveable { mutableStateOf(false) }
    var isSessionUnlocked by rememberSaveable { mutableStateOf(false) }
    var suppressRelockOnce by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        val loaded = settings ?: return@LaunchedEffect
        if (!hasInitializedLock) {
            hasInitializedLock = true
            isSessionUnlocked = !loaded.appLockEnabled
        } else if (!loaded.appLockEnabled) {
            isSessionUnlocked = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, settings?.appLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    suppressRelockOnce = false
                }
                Lifecycle.Event.ON_STOP -> {
                    if ((settings?.appLockEnabled == true) && !suppressRelockOnce) {
                        isSessionUnlocked = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val loadedSettings = settings
    val isReady = hasCompletedOnboarding != null && loadedSettings != null
    val shouldShowLockGate = isReady && loadedSettings.appLockEnabled && !isSessionUnlocked

    CompositionLocalProvider(
        LocalSuppressNextRelock provides { suppressRelockOnce = true },
    ) {
        if (!isReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            )
        } else if (shouldShowLockGate) {
            AppLockGateScreen(
                method = loadedSettings.appLockMethod,
                pinHash = loadedSettings.appLockPinHash,
                userName = userName.ifBlank { "Shiftly" },
                onUnlocked = { isSessionUnlocked = true }
            )
        } else {
            AnimatedContent(
                targetState = hasCompletedOnboarding,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingToMainTransition",
            ) { completed ->
                when (completed) {
                    null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground)
                        )
                    }

                    false -> {
                        WelcomeScreen(
                            onGetStartedComplete = { name, rate, monthlyBudget, profilePhotoPath ->
                                scope.launch {
                                    repository.updateUserName(name)
                                    repository.updateDefaultHourlyRate(rate)
                                    repository.updateMonthlyBudget(monthlyBudget)
                                    if (!profilePhotoPath.isNullOrBlank()) {
                                        repository.updateProfilePhotoPath(profilePhotoPath)
                                    }
                                    repository.setOnboardingCompleted(completed = true)
                                }
                            }
                        )
                    }

                    true -> {
                        MainDashboardShell(repository = repository)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainDashboardShell(
    repository: ShiftlyRepository
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val dutiesViewModel: DutiesViewModel = viewModel(factory = DutiesViewModel.Factory(repository))
    val dutiesState by dutiesViewModel.uiState.collectAsStateWithLifecycle()
    val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory(repository))
    val expenseState by expenseViewModel.uiState.collectAsStateWithLifecycle()

    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showQuickLogShiftSheet by remember { mutableStateOf(false) }
    var showQuickAddExpenseSheet by remember { mutableStateOf(false) }
    var showQuickAddGoalSheet by remember { mutableStateOf(false) }
    var showQuickDepositSheet by remember { mutableStateOf(false) }

    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")
    val savingsGoals by repository.allSavingsGoals.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        repository.processDueRecurringEntries()
        ShiftlyDailyWorker.scheduleDailyMaintenance(context)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    fun navigateToTab(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun onBottomBarSelect(screen: Screen) {
        val currentRoute = currentDestination?.route
        if (screen == Screen.Tools && (
                currentRoute == Screen.Duties.route ||
                currentRoute == Screen.Expense.route ||
                currentRoute == Screen.SplitBill.route
            )
        ) {
            val popped = navController.popBackStack(Screen.Tools.route, inclusive = false)
            if (!popped) {
                navigateToTab(Screen.Tools)
            }
        } else {
            navigateToTab(screen)
        }
    }

    ShiftlyBlueGradientBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 92.dp)
                ) { data ->
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(50),
                        color = DarkCard,
                        border = BorderStroke(1.dp, DarkBorder),
                        shadowElevation = 8.dp
                    ) {
                        Snackbar(
                            snackbarData = data,
                            containerColor = DarkCard,
                            contentColor = TextLightPrimary,
                            actionColor = AzureAccent
                        )
                    }
                }
            },
            bottomBar = {
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { screen ->
                        val currentRoute = currentDestination?.route
                        when (screen) {
                            Screen.Tools -> currentRoute == Screen.Tools.route ||
                                currentRoute == Screen.Duties.route ||
                                currentRoute == Screen.Expense.route ||
                                currentRoute == Screen.SplitBill.route
                            else -> currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        }
                    },
                    onSelect = ::onBottomBarSelect,
                    onFabClick = { showQuickActionSheet = true }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        repository = repository,
                        onNavigateToDuties = { navigateToTab(Screen.Duties) },
                        onNavigateToExpense = { navigateToTab(Screen.Expense) },
                        onNavigateToSplitBill = { navigateToTab(Screen.SplitBill) },
                        onNavigateToProfile = { navigateToTab(Screen.Profile) },
                        onNavigateToAnalytics = { navigateToTab(Screen.Analytics) },
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen(
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onNavigateToSplitBill = { navigateToTab(Screen.SplitBill) },
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Tools.route) {
                    ToolsScreen(
                        repository = repository,
                        onOpenDuties = {
                            navController.navigate(Screen.Duties.route) { launchSingleTop = true }
                        },
                        onOpenExpense = {
                            navController.navigate(Screen.Expense.route) { launchSingleTop = true }
                        },
                        onOpenSplitBill = {
                            navController.navigate(Screen.SplitBill.route) { launchSingleTop = true }
                        },
                        onQuickLogShift = { showQuickLogShiftSheet = true },
                        onQuickAddExpense = { showQuickAddExpenseSheet = true },
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Duties.route) {
                    DutiesScreen(
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Expense.route) {
                    ExpenseScreen(repository = repository, onShowMessage = showMessage)
                }
                composable(Screen.SplitBill.route) {
                    SplitBillScreen(repository = repository, onShowMessage = showMessage)
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        repository = repository,
                        onShowMessage = showMessage
                    )
                }
            }
        }
    }

    if (showQuickActionSheet) {
        QuickAddActionSheet(
            onDismiss = { showQuickActionSheet = false },
            onLogShift = {
                showQuickActionSheet = false
                showQuickLogShiftSheet = true
            },
            onAddExpense = {
                showQuickActionSheet = false
                showQuickAddExpenseSheet = true
            },
            onSplitBill = {
                showQuickActionSheet = false
                navigateToTab(Screen.SplitBill)
            },
            onAddSavingsGoal = {
                showQuickActionSheet = false
                showQuickAddGoalSheet = true
            },
            onAddDeposit = {
                showQuickActionSheet = false
                showQuickDepositSheet = true
            }
        )
    }

    if (showQuickLogShiftSheet) {
        ManualShiftBottomSheet(
            selectedDate = dutiesState.selectedDutyDate,
            hourlyRate = dutiesState.defaultHourlyRate,
            currencySymbol = dutiesState.currencySymbol,
            presets = dutiesState.shiftTimePresets,
            title = "Log Shift",
            confirmButtonText = "Save Shift",
            onSavePreset = { sH, sM, eH, eM ->
                dutiesViewModel.saveShiftTimePreset(sH, sM, eH, eM)
                showMessage("Preset saved")
            },
            onDeletePreset = { presetId ->
                dutiesViewModel.deleteShiftTimePreset(presetId)
            },
            onDismiss = { showQuickLogShiftSheet = false },
            onSave = { date, sH, sM, eH, eM, note, dayType ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                dutiesViewModel.logManualShift(
                    date = date,
                    startHour = sH,
                    startMinute = sM,
                    endHour = eH,
                    endMinute = eM,
                    overtimeNote = note,
                    dayType = dayType
                )
                showQuickLogShiftSheet = false
                showMessage("Shift logged")
            }
        )
    }

    if (showQuickAddExpenseSheet) {
        AddExpenseBottomSheet(
            currencySymbol = expenseState.currencySymbol,
            availableCategories = expenseState.availableCategories,
            onDismiss = { showQuickAddExpenseSheet = false },
            onSaveExpense = { title, amount, category, date, receiptPath ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                expenseViewModel.addExpense(title, amount, category, date, receiptPath)
                showMessage("Expense saved")
            }
        )
    }

    if (showQuickAddGoalSheet) {
        AddSavingsGoalSheet(
            currencySymbol = currencySymbol,
            onDismiss = { showQuickAddGoalSheet = false },
            onCreateGoal = { title, targetAmount, targetDateEpochDay ->
                scope.launch {
                    repository.createSavingsGoal(title, targetAmount, targetDateEpochDay)
                    showQuickAddGoalSheet = false
                    showMessage("Savings goal \"$title\" added")
                }
            }
        )
    }

    if (showQuickDepositSheet) {
        QuickDepositPromptSheet(
            currencySymbol = currencySymbol,
            savingsGoals = savingsGoals,
            onDismiss = { showQuickDepositSheet = false },
            onSaveDeposit = { goal, amount, note ->
                scope.launch {
                    repository.addSavingsDeposit(goal = goal, amount = amount, note = note)
                    showQuickDepositSheet = false
                    showMessage("Saved ${ShiftlyFormatters.formatCurrency(currencySymbol, amount)} deposit for ${goal.title}")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddActionSheet(
    onDismiss: () -> Unit,
    onLogShift: () -> Unit,
    onAddExpense: () -> Unit,
    onSplitBill: () -> Unit,
    onAddSavingsGoal: () -> Unit,
    onAddDeposit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextLightPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextLightSecondary
                    )
                }
            }

            QuickAddOptionRow(
                title = "Log Shift",
                icon = Icons.Outlined.Schedule,
                iconBg = MintSoftContainer,
                iconTint = MintInkOnWhite,
                onClick = onLogShift
            )
            QuickAddOptionRow(
                title = "Add Expense",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                iconBg = CoralSoftContainer,
                iconTint = CoralInkOnWhite,
                onClick = onAddExpense
            )
            QuickAddOptionRow(
                title = "Split Bill",
                icon = Icons.AutoMirrored.Outlined.CallSplit,
                iconBg = AzureSoftContainer,
                iconTint = AzureInkOnWhite,
                onClick = onSplitBill
            )
            QuickAddOptionRow(
                title = "Add Savings Goal",
                icon = Icons.Outlined.Savings,
                iconBg = MintSoftContainer,
                iconTint = MintInkOnWhite,
                onClick = onAddSavingsGoal
            )
            QuickAddOptionRow(
                title = "Add Deposit",
                icon = Icons.Outlined.AccountBalanceWallet,
                iconBg = AzureSoftContainer,
                iconTint = AzureInkOnWhite,
                onClick = onAddDeposit
            )
        }
    }
}

@Composable
private fun QuickAddOptionRow(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextLightPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextLightSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickDepositPromptSheet(
    currencySymbol: String,
    savingsGoals: List<SavingsGoalWithDeposits>,
    onDismiss: () -> Unit,
    onSaveDeposit: (goal: SavingsGoalEntity, amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedGoal by remember(savingsGoals) {
        mutableStateOf((savingsGoals.firstOrNull { it.goal.isActive } ?: savingsGoals.firstOrNull())?.goal)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Deposit",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextLightPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextLightSecondary
                    )
                }
            }

            if (selectedGoal == null) {
                Text(
                    text = "No active savings goal found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLightSecondary
                )
            } else {
                Text(
                    text = "Target Goal: ${selectedGoal?.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = ShiftlySerif),
                    color = TextLightPrimary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Deposit Amount ($currencySymbol)", fontFamily = ShiftlySerif) },
                    placeholder = { Text("0.00", color = TextLightMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzureAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = AzureAccent,
                        unfocusedLabelColor = TextLightSecondary,
                        focusedTextColor = TextLightPrimary,
                        unfocusedTextColor = TextLightPrimary,
                        cursorColor = AzureAccent,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Deposit Note (optional)", fontFamily = ShiftlySerif) },
                    placeholder = { Text("Add note", color = TextLightMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzureAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = AzureAccent,
                        unfocusedLabelColor = TextLightSecondary,
                        focusedTextColor = TextLightPrimary,
                        unfocusedTextColor = TextLightPrimary,
                        cursorColor = AzureAccent,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                ShiftlyPrimaryPillButton(
                    text = "Save Deposit",
                    enabled = parsedAmount > 0.0,
                    onClick = {
                        selectedGoal?.let { goal ->
                            onSaveDeposit(goal, parsedAmount, noteText.trim())
                        }
                    }
                )
            }
        }
    }
}

/**
 * Floating dark-charcoal pill bottom navigation bar:
 * Home | Analytics | Round (+) FAB | Tools | Profile, with labels.
 */
@Composable
fun FloatingPillBottomBar(
    items: List<Screen>,
    isSelected: (Screen) -> Boolean,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    onFabClick: () -> Unit = {}
) {
    val pillShape = RoundedCornerShape(50)
    val leftItems = items.take(2)
    val rightItems = items.drop(2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = pillShape,
                    ambientColor = InkBlack.copy(alpha = 0.55f),
                    spotColor = InkBlack.copy(alpha = 0.65f)
                ),
            shape = pillShape,
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEach { screen ->
                    BottomNavItemSlot(
                        screen = screen,
                        selected = isSelected(screen),
                        onClick = { onSelect(screen) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Center Round "+" FAB
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AzurePrimaryButton,
                        border = BorderStroke(1.dp, AzureAccent.copy(alpha = 0.65f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onFabClick)
                            .semantics {
                                contentDescription = "Quick add action"
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                rightItems.forEach { screen ->
                    BottomNavItemSlot(
                        screen = screen,
                        selected = isSelected(screen),
                        onClick = { onSelect(screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItemSlot(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentTint by animateColorAsState(
        targetValue = if (selected) PureWhite else TextLightSecondary,
        label = "navContentTint"
    )

    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = screen.label
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = null,
                tint = contentTint,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = screen.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = ShiftlySerif,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "Floating Pill Bottom Bar")
@Composable
private fun FloatingPillBottomBarPreview() {
    ShiftlyTheme {
        Box(
            modifier = Modifier
                .background(DarkBackground)
                .padding(vertical = 8.dp)
        ) {
            FloatingPillBottomBar(
                items = Screen.bottomNavItems,
                isSelected = { it == Screen.Home },
                onSelect = {}
            )
        }
    }
}
