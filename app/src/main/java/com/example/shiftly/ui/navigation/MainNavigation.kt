package com.example.shiftly.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
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
import androidx.compose.ui.graphics.SolidColor
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.theme.tabular
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
import com.example.shiftly.ui.screens.recurring.AddEditRecurringBillBottomSheet
import com.example.shiftly.ui.screens.recurring.RecurringBillsScreen
import com.example.shiftly.ui.screens.recurring.RecurringBillsViewModel
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.splitbill.NewSplitBillBottomSheet
import com.example.shiftly.ui.screens.splitbill.SplitBillScreen
import com.example.shiftly.ui.screens.splitbill.SplitBillViewModel
import com.example.shiftly.ui.theme.InterFontFamily
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
private val SheetTopRoundedShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val DepositFieldShape = RoundedCornerShape(14.dp)

private val TabRouteOrder by lazy {
    listOf(Screen.Home.route, Screen.Analytics.route, Screen.Tools.route, Screen.Profile.route)
}

private fun <T> shiftlyNavSpring() = spring<T>(
    dampingRatio = 0.8f,
    stiffness = Spring.StiffnessMediumLow
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = TabRouteOrder.indexOf(initialState.destination.route)
    val to = TabRouteOrder.indexOf(targetState.destination.route)
    return if (from >= 0 && to >= 0 && from != to) (if (to > from) 1 else -1) else 0
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.shiftlyEnter(): EnterTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideInHorizontally(shiftlyNavSpring<IntOffset>()) { it * dir } +
            fadeIn(shiftlyNavSpring<Float>())
    } else {
        slideInHorizontally(shiftlyNavSpring<IntOffset>()) { it }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.shiftlyExit(): ExitTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideOutHorizontally(shiftlyNavSpring<IntOffset>()) { -it * dir / 3 } +
            fadeOut(shiftlyNavSpring<Float>())
    } else {
        slideOutHorizontally(shiftlyNavSpring<IntOffset>()) { -it / 3 } +
            fadeOut(shiftlyNavSpring<Float>())
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.shiftlyPopEnter(): EnterTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideInHorizontally(shiftlyNavSpring<IntOffset>()) { it * dir } +
            fadeIn(shiftlyNavSpring<Float>())
    } else {
        slideInHorizontally(shiftlyNavSpring<IntOffset>()) { -it / 3 } +
            fadeIn(shiftlyNavSpring<Float>())
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.shiftlyPopExit(): ExitTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideOutHorizontally(shiftlyNavSpring<IntOffset>()) { -it * dir / 3 } +
            fadeOut(shiftlyNavSpring<Float>())
    } else {
        slideOutHorizontally(shiftlyNavSpring<IntOffset>()) { it }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val repository = remember { ShiftlyRepository.getInstance(context) }
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
                    transitionSpec = {
                        fadeIn(shiftlyNavSpring<Float>()) togetherWith fadeOut(shiftlyNavSpring<Float>())
                    },
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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isSubScreen = currentRoute == Screen.Duties.route ||
        currentRoute == Screen.Expense.route ||
        currentRoute == Screen.SplitBill.route ||
        currentRoute == Screen.RecurringBills.route ||
        currentRoute == Screen.Allowance.route

    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showQuickLogShiftSheet by remember { mutableStateOf(false) }
    var quickEditingShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var showQuickAddExpenseSheet by remember { mutableStateOf(false) }
    var showQuickNewSplitSheet by remember { mutableStateOf(false) }
    var quickSplitTrigger by remember { mutableIntStateOf(0) }
    var showQuickAddGoalSheet by remember { mutableStateOf(false) }
    var showQuickDepositSheet by remember { mutableStateOf(false) }
    var showQuickAddRecurringBillSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ShiftlyDailyWorker.scheduleDailyMaintenance(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch(Dispatchers.IO) {
                    repository.processDueRecurringEntries()
                    repository.processDueCutoffAllowances()
                    repository.syncInAppNotifications()
                    val bills = repository.allRecurringBills.first()
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

    val navigateToTab: (Screen) -> Unit = remember(navController) {
        { screen: Screen ->
            navController.navigate(screen.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val onBottomBarSelect: (Screen) -> Unit = remember(navController, currentRoute, navigateToTab) {
        { screen: Screen ->
            if (screen == Screen.Tools && (
                    currentRoute == Screen.Duties.route ||
                    currentRoute == Screen.Expense.route ||
                    currentRoute == Screen.SplitBill.route ||
                    currentRoute == Screen.RecurringBills.route ||
                    currentRoute == Screen.Allowance.route
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
    }

    val isNavItemSelected: (Screen) -> Boolean = remember(currentDestination) {
        { screen: Screen ->
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        }
    }
    val onFabClick = remember { { showQuickActionSheet = true } }

    val onNavigateToDuties = remember(navigateToTab) { { navigateToTab(Screen.Duties) } }
    val onNavigateToExpense = remember(navigateToTab) { { navigateToTab(Screen.Expense) } }
    val onNavigateToSplitBill = remember(navigateToTab) { { navigateToTab(Screen.SplitBill) } }
    val onNavigateToProfile = remember(navigateToTab) { { navigateToTab(Screen.Profile) } }
    val onNavigateToAnalytics = remember(navigateToTab) { { navigateToTab(Screen.Analytics) } }
    val onPopBack: () -> Unit = remember(navController) { { navController.popBackStack(); Unit } }
    val onOpenDuties = remember(navController) {
        { navController.navigate(Screen.Duties.route) { launchSingleTop = true } }
    }
    val onOpenExpense = remember(navController) {
        { navController.navigate(Screen.Expense.route) { launchSingleTop = true } }
    }
    val onOpenSplitBill = remember(navController) {
        { navController.navigate(Screen.SplitBill.route) { launchSingleTop = true } }
    }
    val onOpenRecurringBills = remember(navController) {
        { navController.navigate(Screen.RecurringBills.route) { launchSingleTop = true } }
    }
    val onOpenAllowance = remember(navController) {
        { navController.navigate(Screen.Allowance.route) { launchSingleTop = true } }
    }
    val onQuickLogShift = remember { { showQuickLogShiftSheet = true } }
    val onQuickAddExpense = remember { { showQuickAddExpenseSheet = true } }

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
                        shape = NavBarPillShape,
                        color = colors.cardElevated,
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
                            ShiftlyIosIconTile(
                                iconRes = R.drawable.ic_lucide_circle_check,
                                backgroundColor = colors.mintBright,
                                size = 24.dp,
                                cornerRadius = 7.dp,
                                iconSize = 14.dp
                            )
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            data.visuals.actionLabel?.let { actionLabel ->
                                val actionInteraction = remember { MutableInteractionSource() }
                                TextButton(
                                    onClick = { data.performAction() },
                                    interactionSource = actionInteraction,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.shiftlyPressEffect(actionInteraction)
                                ) {
                                    Text(
                                        text = actionLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = InterFontFamily,
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
                    enter = slideInVertically(shiftlyNavSpring<IntOffset>()) { it } +
                        fadeIn(shiftlyNavSpring<Float>()),
                    exit = slideOutVertically(shiftlyNavSpring<IntOffset>()) { it } +
                        fadeOut(shiftlyNavSpring<Float>())
                ) {
                    FloatingPillBottomBar(
                        items = Screen.bottomNavItems,
                        isSelected = isNavItemSelected,
                        onSelect = onBottomBarSelect,
                        isFabActive = showQuickActionSheet,
                        onFabClick = onFabClick
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
                enterTransition = { shiftlyEnter() },
                exitTransition = { shiftlyExit() },
                popEnterTransition = { shiftlyPopEnter() },
                popExitTransition = { shiftlyPopExit() }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        repository = repository,
                        onNavigateToDuties = onNavigateToDuties,
                        onNavigateToExpense = onNavigateToExpense,
                        onNavigateToSplitBill = onNavigateToSplitBill,
                        onNavigateToRecurringBills = onOpenRecurringBills,
                        onNavigateToAllowance = onOpenAllowance,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen(
                        repository = repository,
                        onBack = onPopBack,
                        onNavigateToSplitBill = onNavigateToSplitBill,
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Tools.route) {
                    ToolsScreen(
                        repository = repository,
                        onOpenDuties = onOpenDuties,
                        onOpenExpense = onOpenExpense,
                        onOpenSplitBill = onOpenSplitBill,
                        onOpenRecurringBills = onOpenRecurringBills,
                        onOpenAllowance = onOpenAllowance,
                        onQuickLogShift = onQuickLogShift,
                        onQuickAddExpense = onQuickAddExpense,
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Duties.route) {
                    DutiesScreen(
                        repository = repository,
                        onBack = onPopBack,
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Expense.route) {
                    ExpenseScreen(
                        repository = repository,
                        onBack = onPopBack,
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.SplitBill.route) {
                    SplitBillScreen(
                        repository = repository,
                        onBack = onPopBack,
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.RecurringBills.route) {
                    RecurringBillsScreen(
                        repository = repository,
                        onBack = onPopBack,
                        onShowMessage = showMessage
                    )
                }
                composable(Screen.Allowance.route) {
                    AllowanceScreen(
                        repository = repository,
                        onBack = onPopBack,
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
            },
            onAddRecurringBill = {
                showQuickActionSheet = false
                showQuickAddRecurringBillSheet = true
            }
        )
    }

    if (showQuickLogShiftSheet || quickEditingShift != null) {
        QuickShiftSheetsHost(
            repository = repository,
            showLogSheet = showQuickLogShiftSheet,
            editingShift = quickEditingShift,
            onDismissLog = { showQuickLogShiftSheet = false },
            onSwitchToEdit = { existing ->
                showQuickLogShiftSheet = false
                quickEditingShift = existing
            },
            onDismissEdit = { quickEditingShift = null },
            onShowMessage = showMessage
        )
    }

    if (showQuickAddExpenseSheet) {
        QuickExpenseSheetHost(
            repository = repository,
            onDismiss = { showQuickAddExpenseSheet = false },
            onShowMessage = showMessage
        )
    }

    if (showQuickNewSplitSheet) {
        QuickSplitSheetHost(
            repository = repository,
            prefillTrigger = quickSplitTrigger,
            onDismiss = { showQuickNewSplitSheet = false },
            onShowMessage = showMessage
        )
    }

    if (showQuickAddGoalSheet) {
        QuickAddGoalSheetHost(
            repository = repository,
            onDismiss = { showQuickAddGoalSheet = false },
            onShowMessage = showMessage
        )
    }

    if (showQuickDepositSheet) {
        QuickDepositSheetHost(
            repository = repository,
            onDismiss = { showQuickDepositSheet = false },
            onShowMessage = showMessage
        )
    }

    if (showQuickAddRecurringBillSheet) {
        QuickAddRecurringBillSheetHost(
            repository = repository,
            onDismiss = { showQuickAddRecurringBillSheet = false },
            onShowMessage = showMessage
        )
    }
}

@Composable
private fun QuickShiftSheetsHost(
    repository: ShiftlyRepository,
    showLogSheet: Boolean,
    editingShift: ShiftEntity?,
    onDismissLog: () -> Unit,
    onSwitchToEdit: (ShiftEntity) -> Unit,
    onDismissEdit: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val dutiesViewModel: DutiesViewModel = viewModel(factory = DutiesViewModel.Factory(repository))
    val dutiesState by dutiesViewModel.uiState.collectAsStateWithLifecycle()

    if (showLogSheet) {
        ManualShiftBottomSheet(
            selectedDate = dutiesState.selectedDutyDate,
            hourlyRate = dutiesState.defaultHourlyRate,
            currencySymbol = dutiesState.currencySymbol,
            presets = dutiesState.shiftTimePresets,
            title = "Log Shift",
            confirmButtonText = "Save Shift",
            blockedEpochDays = dutiesState.loggedEpochDays,
            onSwitchToEdit = { duplicateDate ->
                dutiesState.shiftsByEpochDay[duplicateDate.toEpochDay()]?.let(onSwitchToEdit)
            },
            onSavePreset = { sH, sM, eH, eM ->
                dutiesViewModel.saveShiftTimePreset(sH, sM, eH, eM)
                onShowMessage("Preset saved")
            },
            onDeletePreset = { presetId ->
                dutiesViewModel.deleteShiftTimePreset(presetId)
            },
            onDismiss = onDismissLog,
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
                            onDismissLog()
                            onShowMessage("Shift logged")
                        } else {
                            onShowMessage("A shift is already logged for this date")
                        }
                    }
                )
            }
        )
    }

    editingShift?.let { targetShift ->
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
            onDismiss = onDismissEdit,
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
                            onDismissEdit()
                            onShowMessage("Shift updated")
                        } else {
                            onShowMessage("A shift is already logged for that date")
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun QuickExpenseSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory(repository))
    val expenseState by expenseViewModel.uiState.collectAsStateWithLifecycle()

    AddExpenseBottomSheet(
        currencySymbol = expenseState.currencySymbol,
        availableCategories = expenseState.availableCategories,
        onDismiss = onDismiss,
        onSaveExpense = { title, amount, category, date, receiptPath ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            expenseViewModel.addExpense(title, amount, category, date, receiptPath)
            onShowMessage("Expense saved")
        }
    )
}

@Composable
private fun QuickSplitSheetHost(
    repository: ShiftlyRepository,
    prefillTrigger: Int,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val splitBillViewModel: SplitBillViewModel = viewModel(factory = SplitBillViewModel.Factory(repository))
    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")

    NewSplitBillBottomSheet(
        currencySymbol = currencySymbol,
        prefillTrigger = prefillTrigger,
        onDismiss = onDismiss,
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
            onDismiss()
            onShowMessage(
                if (alsoLogAsExpense) "Split bill saved & share logged to Expenses"
                else "Split bill saved as pending"
            )
        }
    )
}

@Composable
private fun QuickAddGoalSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")

    AddSavingsGoalSheet(
        currencySymbol = currencySymbol,
        onDismiss = onDismiss,
        onCreateGoal = { title, targetAmount, targetDateEpochDay ->
            scope.launch {
                repository.createSavingsGoal(title, targetAmount, targetDateEpochDay)
                onDismiss()
                onShowMessage("Savings goal \"$title\" added")
            }
        }
    )
}

@Composable
private fun QuickDepositSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")
    val savingsGoals by repository.allSavingsGoals.collectAsStateWithLifecycle(initialValue = emptyList())

    QuickDepositPromptSheet(
        currencySymbol = currencySymbol,
        savingsGoals = savingsGoals,
        onDismiss = onDismiss,
        onSaveDeposit = { goal, amount, note ->
            scope.launch {
                repository.addSavingsDeposit(goal = goal, amount = amount, note = note)
                onDismiss()
                onShowMessage("Saved ${ShiftlyFormatters.formatCurrency(currencySymbol, amount)} deposit for ${goal.title}")
            }
        }
    )
}

@Composable
private fun QuickAddRecurringBillSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val recurringViewModel: RecurringBillsViewModel = viewModel(factory = RecurringBillsViewModel.Factory(repository))
    val recurringState by recurringViewModel.uiState.collectAsStateWithLifecycle()

    AddEditRecurringBillBottomSheet(
        currencySymbol = recurringState.currencySymbol,
        availableCategories = recurringState.availableCategories,
        editingBill = null,
        onDismiss = onDismiss,
        onSave = { title, amount, category, frequency, dueDate, dueHour, dueMinute, autoLog ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            recurringViewModel.saveBill(
                existing = null,
                title = title,
                amount = amount,
                category = category,
                frequency = frequency,
                dueDate = dueDate,
                dueHour = dueHour,
                dueMinute = dueMinute,
                isEnabled = autoLog,
                onResult = { posted ->
                    onDismiss()
                    if (posted > 0) {
                        onShowMessage("Recurring bill \"$title\" saved & logged to Expenses")
                    } else {
                        onShowMessage("Recurring bill \"$title\" saved")
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddActionSheet(
    onDismiss: () -> Unit,
    onLogShift: () -> Unit,
    onAddExpense: () -> Unit,
    onSplitBill: () -> Unit,
    onAddSavingsGoal: () -> Unit,
    onAddDeposit: () -> Unit,
    onAddRecurringBill: () -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        shape = SheetTopRoundedShape,
        dragHandle = { ShiftlySheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                val cancelInteraction = remember { MutableInteractionSource() }
                TextButton(
                    onClick = onDismiss,
                    interactionSource = cancelInteraction,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .heightIn(min = 44.dp)
                        .shiftlyPressEffect(cancelInteraction)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal
                        ),
                        color = colors.azureAccent,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAddOptionBox(
                    title = "Shift",
                    iconRes = R.drawable.ic_lucide_clock,
                    iconBg = colors.mintBright,
                    onClick = onLogShift,
                    modifier = Modifier.weight(1f)
                )
                QuickAddOptionBox(
                    title = "Expense",
                    iconRes = R.drawable.ic_lucide_receipt_text,
                    iconBg = colors.coralBright,
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f)
                )
                QuickAddOptionBox(
                    title = "Split",
                    iconRes = R.drawable.ic_lucide_split,
                    iconBg = colors.azurePrimary,
                    onClick = onSplitBill,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAddOptionBox(
                    title = "Savings",
                    iconRes = R.drawable.ic_lucide_piggy_bank,
                    iconBg = colors.iosOrange,
                    onClick = onAddSavingsGoal,
                    modifier = Modifier.weight(1f)
                )
                QuickAddOptionBox(
                    title = "Deposit",
                    iconRes = R.drawable.ic_lucide_wallet,
                    iconBg = colors.nightDiffPurple,
                    onClick = onAddDeposit,
                    modifier = Modifier.weight(1f)
                )
                QuickAddOptionBox(
                    title = "Recurring",
                    iconRes = R.drawable.ic_lucide_repeat,
                    iconBg = colors.iosOrange,
                    onClick = onAddRecurringBill,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .shiftlyPressClickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShiftlyIosIconTile(
            iconRes = iconRes,
            backgroundColor = iconBg,
            size = 56.dp,
            cornerRadius = 14.dp,
            iconSize = 26.dp
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            ),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    val parsedAmount = remember(amountText) { amountText.toDoubleOrNull() ?: 0.0 }
    val submitDeposit: () -> Unit = {
        if (parsedAmount > 0.0) {
            selectedGoal?.let { goal ->
                onSaveDeposit(goal, parsedAmount, noteText.trim())
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        shape = SheetTopRoundedShape,
        dragHandle = { ShiftlySheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShiftlySheetHeader(
                title = "Add Deposit",
                onCancel = onDismiss,
                actionLabel = "Save",
                actionEnabled = selectedGoal != null && parsedAmount > 0.0,
                onAction = submitDeposit
            )

            if (selectedGoal == null) {
                Text(
                    text = "No active savings goal found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                ShiftlySecondaryTextButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Row 1: Target Goal
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Goal",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = colors.textPrimary
                            )
                            Text(
                                text = selectedGoal?.title.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.azureAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        ShiftlyHairlineDivider(startInset = 16.dp)

                        // Row 2: Amount
                        QuickDepositInsetRow(
                            label = "Amount ($currencySymbol)",
                            value = amountText,
                            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                            placeholder = "0.00",
                            prefix = currencySymbol,
                            isNumeric = true
                        )
                        ShiftlyHairlineDivider(startInset = 16.dp)

                        // Row 3: Note
                        QuickDepositInsetRow(
                            label = "Note (optional)",
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = "Note"
                        )
                    }
                }

                ShiftlySheetBottomButtons(
                    onCancel = onDismiss,
                    onAction = submitDeposit,
                    actionLabel = "Save",
                    actionEnabled = parsedAmount > 0.0
                )
            }
        }
    }
}

@Composable
private fun QuickDepositInsetRow(
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

/**
 * Full-width iOS-style docked bottom navigation bar with translucent dark surface and hairline top border:
 * Home | Analytics | Quick Add (+) | Tools | Profile.
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
    val haptics = LocalHapticFeedback.current
    val leftItems = remember(items) { items.take(2) }
    val rightItems = remember(items) { items.drop(2) }

    val fabRotationState = animateFloatAsState(
        targetValue = if (isFabActive) 45f else 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fabRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (colors.isDark) Color(0xEB1C1C1E) else colors.surface.copy(alpha = 0.94f)
            )
    ) {
        ShiftlyHairlineDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp)
                .selectableGroup()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leftItems.forEach { screen ->
                val onClick = remember(screen, onSelect) { { onSelect(screen) } }
                BottomNavItemSlot(
                    screen = screen,
                    selected = isSelected(screen),
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
            }

            // Center Quick Add Button (Icon Only, Larger, Brighter)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 58.dp)
                    .shiftlyPressClickable(role = Role.Button) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFabClick()
                    }
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Quick add action"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_plus),
                    contentDescription = "Quick add action",
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { rotationZ = fabRotationState.value }
                )
            }

            rightItems.forEach { screen ->
                val onClick = remember(screen, onSelect) { { onSelect(screen) } }
                BottomNavItemSlot(
                    screen = screen,
                    selected = isSelected(screen),
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
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
    val haptics = LocalHapticFeedback.current
    val contentTint by animateColorAsState(
        targetValue = if (selected) colors.azureAccent else colors.textSecondary,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "navContentTint"
    )

    Box(
        modifier = modifier
            .heightIn(min = 58.dp)
            .shiftlyPressClickable(role = Role.Tab) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
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
                modifier = Modifier.size(25.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = screen.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "Docked iOS Bottom Bar")
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
