package com.example.shiftly.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.shiftly.R
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.screens.AnalyticsScreen
import com.example.shiftly.ui.screens.HomeScreen
import com.example.shiftly.ui.screens.ToolsScreen
import com.example.shiftly.ui.screens.WelcomeScreen
import com.example.shiftly.ui.screens.allowance.AllowanceScreen
import com.example.shiftly.ui.screens.duties.DutiesScreen
import com.example.shiftly.ui.screens.duties.DutiesViewModel
import com.example.shiftly.ui.screens.duties.ManualShiftBottomSheet
import com.example.shiftly.ui.screens.expense.AddExpenseBottomSheet
import com.example.shiftly.ui.screens.expense.ExpenseScreen
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.screens.lock.AppLockGateScreen
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.screens.profile.ProfileScreen
import com.example.shiftly.ui.screens.recurring.RecurringBillsScreen
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.splitbill.NewSplitBillBottomSheet
import com.example.shiftly.ui.screens.splitbill.SplitBillScreen
import com.example.shiftly.ui.screens.splitbill.SplitBillViewModel
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.RecurringEngine
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.workers.ShiftlyDailyWorker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val NavBarPillShape = RoundedCornerShape(50)

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
        ShiftlyTheme(themeMode = loadedSettings?.themeMode ?: "SYSTEM") {
            val colors = ShiftlyColors.current
            if (!isReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
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
                                    .background(colors.background)
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
}

@Composable
private fun MainDashboardShell(
    repository: ShiftlyRepository
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isSubScreen = currentRoute == Screen.Duties.route ||
        currentRoute == Screen.Expense.route ||
        currentRoute == Screen.SplitBill.route ||
        currentRoute == Screen.RecurringBills.route ||
        currentRoute == Screen.Allowance.route

    val dutiesViewModel: DutiesViewModel = viewModel(factory = DutiesViewModel.Factory(repository))
    val dutiesState by dutiesViewModel.uiState.collectAsStateWithLifecycle()
    val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory(repository))
    val expenseState by expenseViewModel.uiState.collectAsStateWithLifecycle()
    val splitBillViewModel: SplitBillViewModel = viewModel(factory = SplitBillViewModel.Factory(repository))

    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showQuickLogShiftSheet by remember { mutableStateOf(false) }
    var quickEditingShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var showQuickAddExpenseSheet by remember { mutableStateOf(false) }
    var showQuickNewSplitSheet by remember { mutableStateOf(false) }
    var quickSplitTrigger by remember { mutableIntStateOf(0) }
    var showQuickAddGoalSheet by remember { mutableStateOf(false) }
    var showQuickDepositSheet by remember { mutableStateOf(false) }

    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")
    val savingsGoals by repository.allSavingsGoals.collectAsStateWithLifecycle(initialValue = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        repository.processDueRecurringEntries()
        repository.processDueCutoffAllowances()
        ShiftlyDailyWorker.scheduleDailyMaintenance(context)
        val bills = withContext(Dispatchers.IO) { repository.allRecurringBills.first() }
        ShiftlyDailyWorker.scheduleNextRecurringCheck(
            context = context,
            nextDue = RecurringEngine.earliestNextDueDateTime(bills)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    repository.processDueRecurringEntries()
                    repository.processDueCutoffAllowances()
                    val bills = withContext(Dispatchers.IO) { repository.allRecurringBills.first() }
                    ShiftlyDailyWorker.scheduleNextRecurringCheck(
                        context = context,
                        nextDue = RecurringEngine.earliestNextDueDateTime(bills)
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val showMessage: (String) -> Unit = remember(scope, snackbarHostState) {
        { message: String ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
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
        val route = currentDestination?.route
        if (screen == Screen.Tools && (
                route == Screen.Duties.route ||
                route == Screen.Expense.route ||
                route == Screen.SplitBill.route ||
                route == Screen.RecurringBills.route ||
                route == Screen.Allowance.route
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
                    modifier = Modifier
                        .then(if (isSubScreen) Modifier.navigationBarsPadding() else Modifier)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) { data ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier.widthIn(max = 420.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                start = 14.dp,
                                end = if (data.visuals.actionLabel != null) 10.dp else 18.dp,
                                top = 11.dp,
                                bottom = 11.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(colors.mintContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_circle_check),
                                    contentDescription = null,
                                    tint = colors.mintInk,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            data.visuals.actionLabel?.let { actionLabel ->
                                TextButton(
                                    onClick = { data.performAction() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = actionLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = colors.azureAccent
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !isSubScreen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    FloatingPillBottomBar(
                        items = Screen.bottomNavItems,
                        isSelected = { screen ->
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        },
                        onSelect = ::onBottomBarSelect,
                        isFabActive = showQuickActionSheet,
                        onFabClick = { showQuickActionSheet = true }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(180)) }
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
                        onOpenRecurringBills = {
                            navController.navigate(Screen.RecurringBills.route) { launchSingleTop = true }
                        },
                        onOpenAllowance = {
                            navController.navigate(Screen.Allowance.route) { launchSingleTop = true }
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
                    ExpenseScreen(
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.SplitBill.route) {
                    SplitBillScreen(
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.RecurringBills.route) {
                    RecurringBillsScreen(
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Allowance.route) {
                    AllowanceScreen(
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onShowMessage = showMessage
                    )
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
                quickSplitTrigger++
                showQuickNewSplitSheet = true
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
            blockedEpochDays = dutiesState.loggedEpochDays,
            onSwitchToEdit = { duplicateDate ->
                dutiesState.shiftsByEpochDay[duplicateDate.toEpochDay()]?.let { existing ->
                    showQuickLogShiftSheet = false
                    quickEditingShift = existing
                }
            },
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
                    dayType = dayType,
                    onResult = { saved ->
                        if (saved) {
                            showQuickLogShiftSheet = false
                            showMessage("Shift logged")
                        } else {
                            showMessage("A shift is already logged for this date")
                        }
                    }
                )
            }
        )
    }

    quickEditingShift?.let { targetShift ->
        val zone = remember { ZoneId.systemDefault() }
        val startLocalTime = remember(targetShift.id, targetShift.timeInMillis) {
            Instant.ofEpochMilli(targetShift.timeInMillis).atZone(zone).toLocalTime()
        }
        val endLocalTime = remember(targetShift.id, targetShift.timeOutMillis) {
            Instant.ofEpochMilli(targetShift.timeOutMillis).atZone(zone).toLocalTime()
        }
        val initialDayType = remember(targetShift.id, targetShift.dayType) {
            runCatching { DayType.valueOf(targetShift.dayType) }.getOrNull() ?: DayType.ORDINARY
        }

        ManualShiftBottomSheet(
            selectedDate = LocalDate.ofEpochDay(targetShift.dateEpochDay),
            hourlyRate = targetShift.lockedHourlyRate,
            currencySymbol = dutiesState.currencySymbol,
            presets = dutiesState.shiftTimePresets,
            title = "Edit Shift",
            confirmButtonText = "Update Shift",
            initialStartHour24 = startLocalTime.hour,
            initialStartMinute = startLocalTime.minute,
            initialEndHour24 = endLocalTime.hour,
            initialEndMinute = endLocalTime.minute,
            initialNote = targetShift.overtimeNote,
            initialDayType = initialDayType,
            showPresets = false,
            blockedEpochDays = dutiesState.loggedEpochDays - targetShift.dateEpochDay,
            onDismiss = { quickEditingShift = null },
            onSave = { date, sH, sM, eH, eM, note, dayType ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                dutiesViewModel.editShift(
                    shift = targetShift,
                    date = date,
                    startHour = sH,
                    startMinute = sM,
                    endHour = eH,
                    endMinute = eM,
                    dayType = dayType,
                    overtimeNote = note,
                    onResult = { updated ->
                        if (updated) {
                            quickEditingShift = null
                            showMessage("Shift updated")
                        } else {
                            showMessage("A shift is already logged for that date")
                        }
                    }
                )
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

    if (showQuickNewSplitSheet) {
        NewSplitBillBottomSheet(
            currencySymbol = currencySymbol,
            prefillTrigger = quickSplitTrigger,
            onDismiss = { showQuickNewSplitSheet = false },
            onSaveSplitBill = { title, totalAmount, personCount, names, paidStates, alsoLogAsExpense ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                splitBillViewModel.createSplitBill(
                    rawTitle = title,
                    totalAmount = totalAmount,
                    personCount = personCount,
                    participantNames = names,
                    participantPaidStates = paidStates,
                    alsoLogMyShareAsExpense = alsoLogAsExpense
                )
                showQuickNewSplitSheet = false
                showMessage(
                    if (alsoLogAsExpense) "Split bill saved & share logged to Expenses"
                    else "Split bill saved as pending"
                )
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
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        tonalElevation = 0.dp,
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
                    color = colors.textPrimary
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                        color = colors.textSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAddOptionBox(
                    title = "Shift",
                    iconRes = R.drawable.ic_lucide_clock,
                    iconBg = colors.mintContainer,
                    iconTint = colors.mintInk,
                    onClick = onLogShift,
                    modifier = Modifier.weight(1f)
                )
                QuickAddOptionBox(
                    title = "Expense",
                    iconRes = R.drawable.ic_lucide_receipt_text,
                    iconBg = colors.coralContainer,
                    iconTint = colors.coralInk,
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f)
                )
                QuickAddOptionBox(
                    title = "Split",
                    iconRes = R.drawable.ic_lucide_split,
                    iconBg = colors.azureContainer,
                    iconTint = colors.azureInk,
                    onClick = onSplitBill,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAddOptionBox(
                    title = "Savings",
                    iconRes = R.drawable.ic_lucide_piggy_bank,
                    iconBg = colors.mintContainer,
                    iconTint = colors.mintInk,
                    onClick = onAddSavingsGoal,
                    modifier = Modifier.weight(1f)
                )
                QuickAddOptionBox(
                    title = "Deposit",
                    iconRes = R.drawable.ic_lucide_wallet,
                    iconBg = colors.azureContainer,
                    iconTint = colors.azureInk,
                    onClick = onAddDeposit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickAddOptionBox(
    title: String,
    @DrawableRes iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    val colors = ShiftlyColors.current
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedGoal by remember(savingsGoals) {
        mutableStateOf((savingsGoals.firstOrNull { it.goal.isActive } ?: savingsGoals.firstOrNull())?.goal)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        tonalElevation = 0.dp,
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
                    color = colors.textPrimary
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                        color = colors.textSecondary
                    )
                }
            }

            if (selectedGoal == null) {
                Text(
                    text = "No active savings goal found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            } else {
                Text(
                    text = "Target Goal: ${selectedGoal?.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlusJakartaSans),
                    color = colors.textPrimary
                )

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.azureAccent,
                    unfocusedBorderColor = colors.border,
                    focusedLabelColor = colors.azureAccent,
                    unfocusedLabelColor = colors.textSecondary,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.azureAccent,
                    focusedContainerColor = colors.card,
                    unfocusedContainerColor = colors.card
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Deposit Amount ($currencySymbol)", fontFamily = PlusJakartaSans) },
                    placeholder = {
                        Text(
                            text = "0.00",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                            color = colors.textMuted
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Deposit Note (optional)", fontFamily = PlusJakartaSans) },
                    placeholder = {
                        Text(
                            text = "Add note",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                            color = colors.textMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                val parsedAmount = remember(amountText) { amountText.toDoubleOrNull() ?: 0.0 }
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
 * Floating pill bottom navigation bar:
 * Home | Analytics | Round (+) FAB | Tools | Profile, with labels.
 */
@Composable
fun FloatingPillBottomBar(
    items: List<Screen>,
    isSelected: (Screen) -> Boolean,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    isFabActive: Boolean = false,
    onFabClick: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val leftItems = remember(items) { items.take(2) }
    val rightItems = remember(items) { items.drop(2) }

    val fabRotation by animateFloatAsState(
        targetValue = if (isFabActive) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabRotation"
    )

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
                .height(68.dp),
            shape = NavBarPillShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 0.dp
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
                        color = colors.azurePrimary,
                        border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.65f)),
                        shadowElevation = 0.dp,
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
                                painter = painterResource(R.drawable.ic_lucide_plus),
                                contentDescription = null,
                                tint = colors.onAzurePrimary,
                                modifier = Modifier
                                    .size(26.dp)
                                    .graphicsLayer { rotationZ = fabRotation }
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
    val colors = ShiftlyColors.current
    val contentTint by animateColorAsState(
        targetValue = if (selected) colors.textPrimary else colors.textSecondary,
        label = "navContentTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navIconScale"
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
                painter = painterResource(
                    if (selected) screen.selectedIconRes else screen.unselectedIconRes
                ),
                contentDescription = null,
                tint = contentTint,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = screen.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PlusJakartaSans,
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
                .background(ShiftlyColors.current.background)
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
