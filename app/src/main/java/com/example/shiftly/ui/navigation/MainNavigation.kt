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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyMainTabTourOverlay
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.components.shiftlySheetScrimColor
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
import com.example.shiftly.ui.screens.splitbill.NewSplitBillBottomSheet
import com.example.shiftly.ui.screens.splitbill.SplitBillScreen
import com.example.shiftly.ui.screens.splitbill.SplitBillViewModel
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.RecurringEngine
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

private data class MainTourStep(
    val title: String,
    val description: String,
    val targetScreen: Screen,
    val navSlotIndex: Int
)

private val MAIN_TAB_TOUR_STEPS by lazy {
    listOf(
        MainTourStep(
            title = "Your Calm Home Overview",
            description = "See your net balance, total earnings, and spending at a glance. Tap the eye icon anytime to hide amounts in public.",
            targetScreen = Screen.Home,
            navSlotIndex = 0
        ),
        MainTourStep(
            title = "Quick Add in Seconds",
            description = "Tap the + button from anywhere to quickly log a work shift, record an expense, or split a bill.",
            targetScreen = Screen.Home,
            navSlotIndex = 2
        ),
        MainTourStep(
            title = "Spending & Earning Trends",
            description = "View visual charts of your income vs. expenses, category breakdowns, and your daily safe-to-spend guide.",
            targetScreen = Screen.Analytics,
            navSlotIndex = 1
        ),
        MainTourStep(
            title = "All Your Trackers in One Place",
            description = "Open Duties & Shifts, Expenses, Split Bill, IOUs, Budgets, Savings Goals, Recurring Bills, Allowance, Paycheck Estimator, and Calendar.",
            targetScreen = Screen.Tools,
            navSlotIndex = 3
        ),
        MainTourStep(
            title = "Profile, Security & Backups",
            description = "Update your hourly rate, lock the app with PIN or biometrics, switch currency or theme, and back up your data offline.",
            targetScreen = Screen.Profile,
            navSlotIndex = 4
        )
    )
}

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
                                onSaveSetupProfile = { name, rate, monthlyBudget, profilePhotoPath ->
                                    scope.launch {
                                        repository.updateUserName(name)
                                        repository.updateDefaultHourlyRate(rate)
                                        repository.updateMonthlyBudget(monthlyBudget)
                                        if (!profilePhotoPath.isNullOrBlank()) {
                                            repository.updateProfilePhotoPath(profilePhotoPath)
                                        }
                                    }
                                },
                                onGetStartedComplete = { _, _, _, _ ->
                                    scope.launch {
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

    val hasSeenAppTutorial by repository.hasSeenAppTutorial
        .collectAsStateWithLifecycle(initialValue = null)
    var tourStep by rememberSaveable { mutableIntStateOf(0) }

    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showQuickLogShiftSheet by remember { mutableStateOf(false) }
    var quickEditingShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var showQuickAddExpenseSheet by remember { mutableStateOf(false) }
    var showQuickNewSplitSheet by remember { mutableStateOf(false) }
    var quickSplitTrigger by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(hasSeenAppTutorial, tourStep, isSubScreen) {
        if (hasSeenAppTutorial == false && !isSubScreen) {
            val safeIndex = tourStep.coerceIn(0, MAIN_TAB_TOUR_STEPS.lastIndex)
            if (safeIndex != tourStep) {
                tourStep = safeIndex
            }
            val target = MAIN_TAB_TOUR_STEPS[safeIndex].targetScreen
            if (currentRoute != target.route) {
                navigateToTab(target)
            }
        }
    }

    val completeTour: () -> Unit = remember(scope, repository, navigateToTab) {
        {
            scope.launch {
                repository.setHasSeenAppTutorial(true)
            }
            tourStep = 0
            navigateToTab(Screen.Home)
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                                    iconSize = 20.dp
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
                            onShowMessage = showMessage,
                            onReplayTutorial = {
                                tourStep = 0
                                navigateToTab(Screen.Home)
                                scope.launch {
                                    repository.setHasSeenAppTutorial(false)
                                }
                            }
                        )
                    }
                }
            }

            if (hasSeenAppTutorial == false && !isSubScreen) {
                val safeIndex = tourStep.coerceIn(0, MAIN_TAB_TOUR_STEPS.lastIndex)
                val currentStep = MAIN_TAB_TOUR_STEPS[safeIndex]
                ShiftlyMainTabTourOverlay(
                    stepIndex = safeIndex,
                    totalSteps = MAIN_TAB_TOUR_STEPS.size,
                    title = currentStep.title,
                    description = currentStep.description,
                    onBack = {
                        if (safeIndex > 0) {
                            tourStep = safeIndex - 1
                        }
                    },
                    onNextOrFinish = {
                        if (safeIndex < MAIN_TAB_TOUR_STEPS.lastIndex) {
                            tourStep = safeIndex + 1
                        } else {
                            completeTour()
                        }
                    },
                    onSkip = completeTour,
                    highlightedBottomBar = {
                        TourHighlightedBottomBar(
                            items = Screen.bottomNavItems,
                            highlightedSlotIndex = currentStep.navSlotIndex
                        )
                    }
                )
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
}

@Composable
private fun TourHighlightedBottomBar(
    items: List<Screen>,
    highlightedSlotIndex: Int,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val leftItems = remember(items) { items.take(2) }
    val rightItems = remember(items) { items.drop(2) }
    val highlightFill = if (colors.isDark) Color(0xFF1C1C1E) else PureWhite

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leftItems.forEachIndexed { idx, screen ->
                val isHighlighted = idx == highlightedSlotIndex
                TourBottomBarSlot(
                    highlighted = isHighlighted,
                    highlightFill = highlightFill,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isHighlighted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(screen.selectedIconRes),
                                contentDescription = screen.label,
                                tint = colors.azureAccent,
                                modifier = Modifier.size(25.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.azureAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            val isPlusHighlighted = highlightedSlotIndex == 2
            TourBottomBarSlot(
                highlighted = isPlusHighlighted,
                highlightFill = highlightFill,
                modifier = Modifier.weight(1f)
            ) {
                if (isPlusHighlighted) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_plus),
                        contentDescription = "Quick add action",
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            rightItems.forEachIndexed { idx, screen ->
                val slotIndex = idx + 3
                val isHighlighted = slotIndex == highlightedSlotIndex
                TourBottomBarSlot(
                    highlighted = isHighlighted,
                    highlightFill = highlightFill,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isHighlighted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(screen.selectedIconRes),
                                contentDescription = screen.label,
                                tint = colors.azureAccent,
                                modifier = Modifier.size(25.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.azureAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TourBottomBarSlot(
    highlighted: Boolean,
    highlightFill: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (highlighted) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = highlightFill,
            border = BorderStroke(1.dp, Color(0xFF0A84FF).copy(alpha = 0.75f)),
            shadowElevation = 0.dp,
            modifier = modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .height(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    } else {
        Box(
            modifier = modifier.heightIn(min = 58.dp),
            contentAlignment = Alignment.Center
        ) {}
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddActionSheet(
    onDismiss: () -> Unit,
    onLogShift: () -> Unit,
    onAddExpense: () -> Unit,
    onSplitBill: () -> Unit
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
            ShiftlySheetHeader(title = "Quick Add")

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (colors.isDark) Color(0xFF2C2C2E) else colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    QuickAddGroupedRow(
                        title = "Log Duty Shift",
                        iconRes = R.drawable.ic_lucide_clock,
                        iconTint = Color(0xFF30D158),
                        onClick = onLogShift
                    )
                    ShiftlyHairlineDivider(startInset = 50.dp)
                    QuickAddGroupedRow(
                        title = "Add Expense",
                        iconRes = R.drawable.ic_lucide_receipt_text,
                        iconTint = Color(0xFFFF453A),
                        onClick = onAddExpense
                    )
                    ShiftlyHairlineDivider(startInset = 50.dp)
                    QuickAddGroupedRow(
                        title = "Split a Bill",
                        iconRes = R.drawable.ic_lucide_split,
                        iconTint = Color(0xFF0A84FF),
                        onClick = onSplitBill
                    )
                }
            }

            ShiftlySheetCloseButton(
                label = "Cancel",
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun QuickAddGroupedRow(
    title: String,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_lucide_chevron_right),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Full-width iOS-style docked bottom navigation bar with solid opaque surface and hairline top border:
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
                if (colors.isDark) Color(0xFF161618) else Color(0xFFFFFFFF)
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
