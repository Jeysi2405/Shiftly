package com.example.shiftly.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyMainTabTourOverlay
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.screens.AddIouBottomSheet
import com.example.shiftly.ui.screens.AnalyticsScreen
import com.example.shiftly.ui.screens.HomeScreen
import com.example.shiftly.ui.screens.ToolsScreen
import com.example.shiftly.ui.screens.ToolsViewModel
import com.example.shiftly.ui.screens.WelcomeScreen
import com.example.shiftly.ui.screens.allowance.AddAllowanceBottomSheet
import com.example.shiftly.ui.screens.allowance.AllowanceScreen
import com.example.shiftly.ui.screens.allowance.AllowanceViewModel
import com.example.shiftly.ui.screens.duties.DutiesScreen
import com.example.shiftly.ui.screens.duties.DutiesViewModel
import com.example.shiftly.ui.screens.duties.ManualShiftBottomSheet
import com.example.shiftly.ui.screens.expense.AddExpenseBottomSheet
import com.example.shiftly.ui.screens.expense.ExpenseScreen
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.screens.loans.AddEditInstallmentLoanSheet
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
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
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

private val NavBarPillShape = ShiftlyShapes.Pill
private val SheetTopRoundedShape = ShiftlyShapes.Sheet

private enum class ActiveQuickAddSheet {
    LOG_SHIFT,
    ADD_EXPENSE,
    ADD_ALLOWANCE,
    SPLIT_BILL,
    NEW_RECURRING_BILL,
    NEW_INSTALLMENT_LOAN,
    NEW_SAVINGS_GOAL,
    ADD_IOU
}

private data class MainTourStep(
    val title: String,
    val description: String,
    val targetScreen: Screen,
    val navSlotIndex: Int
)

private val MAIN_TAB_TOUR_STEPS by lazy {
    listOf(
        MainTourStep(
            title = "Home Overview",
            description = "Balance, earnings, and spending at a glance.",
            targetScreen = Screen.Home,
            navSlotIndex = 0
        ),
        MainTourStep(
            title = "Quick Add",
            description = "Tap + to log a shift, expense, or split bill.",
            targetScreen = Screen.Home,
            navSlotIndex = 2
        ),
        MainTourStep(
            title = "Analytics",
            description = "Track income, expenses, and spending trends.",
            targetScreen = Screen.Analytics,
            navSlotIndex = 1
        ),
        MainTourStep(
            title = "Tools",
            description = "Access shifts, expenses, bills, loans, and goals.",
            targetScreen = Screen.Tools,
            navSlotIndex = 3
        ),
        MainTourStep(
            title = "Profile & Settings",
            description = "Manage rates, app lock, theme, and backups.",
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
        fadeIn(tween(150))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.shiftlyExit(): ExitTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideOutHorizontally(shiftlyNavSpring<IntOffset>()) { -it * dir / 3 } +
            fadeOut(shiftlyNavSpring<Float>())
    } else {
        fadeOut(tween(120))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.shiftlyPopEnter(): EnterTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideInHorizontally(shiftlyNavSpring<IntOffset>()) { it * dir } +
            fadeIn(shiftlyNavSpring<Float>())
    } else {
        fadeIn(tween(150))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.shiftlyPopExit(): ExitTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideOutHorizontally(shiftlyNavSpring<IntOffset>()) { -it * dir / 3 } +
            fadeOut(shiftlyNavSpring<Float>())
    } else {
        fadeOut(tween(120))
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
            } else {
                AnimatedContent(
                    targetState = shouldShowLockGate,
                    transitionSpec = {
                        if (!targetState) {
                            (fadeIn(tween(260)) + scaleIn(
                                initialScale = 0.96f,
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )) togetherWith (fadeOut(tween(200)) + scaleOut(
                                targetScale = 1.04f,
                                animationSpec = tween(200)
                            ))
                        } else {
                            fadeIn(tween(120)) togetherWith fadeOut(tween(100))
                        }
                    },
                    label = "AppLockGateTransition"
                ) { isLocked ->
                    if (isLocked) {
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
                                        onSaveSetupProfile = { name, rate, monthlyBudget ->
                                            scope.launch {
                                                repository.updateUserName(name)
                                                repository.updateDefaultHourlyRate(rate)
                                                repository.updateMonthlyBudget(monthlyBudget)
                                            }
                                        },
                                        onGetStartedComplete = { _, _, _ ->
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
    var activeQuickAddSheet by remember { mutableStateOf<ActiveQuickAddSheet?>(null) }
    var quickEditingShift by remember { mutableStateOf<ShiftEntity?>(null) }
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

    val showUndoMessage: (String, () -> Unit, () -> Unit) -> Unit = remember(scope, snackbarHostState) {
        { message: String, onUndo: () -> Unit, onDismissed: () -> Unit ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onUndo()
                } else {
                    onDismissed()
                }
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
                                    iconSize = 20.dp,
                                    triggerKey = data.visuals.message,
                                    animateOnMount = true
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
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .shiftlyPressEffect(actionInteraction)
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
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
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
                    }
                    composable(Screen.Analytics.route) {
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                            AnalyticsScreen(
                                repository = repository,
                                onBack = onPopBack,
                                onNavigateToSplitBill = onNavigateToSplitBill,
                                onShowMessage = showMessage
                            )
                        }
                    }
                    composable(Screen.Tools.route) {
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                            ToolsScreen(
                                repository = repository,
                                onOpenDuties = onOpenDuties,
                                onOpenExpense = onOpenExpense,
                                onOpenSplitBill = onOpenSplitBill,
                                onOpenRecurringBills = onOpenRecurringBills,
                                onOpenAllowance = onOpenAllowance,
                                onShowMessage = showMessage
                            )
                        }
                    }
                    composable(Screen.Duties.route) {
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                            DutiesScreen(
                                repository = repository,
                                onBack = onPopBack,
                                onShowMessage = showMessage
                            )
                        }
                    }
                    composable(Screen.Expense.route) {
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                            ExpenseScreen(
                                repository = repository,
                                onBack = onPopBack,
                                onShowMessage = showMessage,
                                onShowUndoMessage = showUndoMessage
                            )
                        }
                    }
                    composable(Screen.SplitBill.route) {
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                            SplitBillScreen(
                                repository = repository,
                                onBack = onPopBack,
                                onShowMessage = showMessage
                            )
                        }
                    }
                    composable(Screen.RecurringBills.route) {
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                            RecurringBillsScreen(
                                repository = repository,
                                onBack = onPopBack,
                                onShowMessage = showMessage,
                                onShowUndoMessage = showUndoMessage
                            )
                        }
                    }
                    composable(Screen.Allowance.route) {
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                            AllowanceScreen(
                                repository = repository,
                                onBack = onPopBack,
                                onShowMessage = showMessage
                            )
                        }
                    }
                    composable(Screen.Profile.route) {
                        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
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
            onSelectAction = { action ->
                showQuickActionSheet = false
                if (action == ActiveQuickAddSheet.SPLIT_BILL) {
                    quickSplitTrigger++
                }
                activeQuickAddSheet = action
            }
        )
    }

    if (activeQuickAddSheet == ActiveQuickAddSheet.LOG_SHIFT || quickEditingShift != null) {
        QuickShiftSheetsHost(
            repository = repository,
            showLogSheet = activeQuickAddSheet == ActiveQuickAddSheet.LOG_SHIFT,
            editingShift = quickEditingShift,
            onDismissLog = {
                if (activeQuickAddSheet == ActiveQuickAddSheet.LOG_SHIFT) {
                    activeQuickAddSheet = null
                }
            },
            onSwitchToEdit = { existing ->
                activeQuickAddSheet = null
                quickEditingShift = existing
            },
            onDismissEdit = { quickEditingShift = null },
            onShowMessage = showMessage
        )
    }

    when (activeQuickAddSheet) {
        ActiveQuickAddSheet.ADD_EXPENSE -> {
            QuickExpenseSheetHost(
                repository = repository,
                onDismiss = { activeQuickAddSheet = null },
                onShowMessage = showMessage
            )
        }

        ActiveQuickAddSheet.ADD_ALLOWANCE -> {
            QuickAllowanceSheetHost(
                repository = repository,
                onDismiss = { activeQuickAddSheet = null },
                onShowMessage = showMessage
            )
        }

        ActiveQuickAddSheet.SPLIT_BILL -> {
            QuickSplitSheetHost(
                repository = repository,
                prefillTrigger = quickSplitTrigger,
                onDismiss = { activeQuickAddSheet = null },
                onShowMessage = showMessage
            )
        }

        ActiveQuickAddSheet.NEW_RECURRING_BILL -> {
            QuickRecurringBillSheetHost(
                repository = repository,
                onDismiss = { activeQuickAddSheet = null },
                onShowMessage = showMessage
            )
        }

        ActiveQuickAddSheet.NEW_INSTALLMENT_LOAN -> {
            QuickInstallmentLoanSheetHost(
                repository = repository,
                onDismiss = { activeQuickAddSheet = null },
                onShowMessage = showMessage
            )
        }

        ActiveQuickAddSheet.NEW_SAVINGS_GOAL -> {
            QuickSavingsGoalSheetHost(
                repository = repository,
                onDismiss = { activeQuickAddSheet = null },
                onShowMessage = showMessage
            )
        }

        ActiveQuickAddSheet.ADD_IOU -> {
            QuickIouSheetHost(
                repository = repository,
                onDismiss = { activeQuickAddSheet = null },
                onShowMessage = showMessage
            )
        }

        ActiveQuickAddSheet.LOG_SHIFT, null -> Unit
    }
}

@Composable
private fun TourHighlightedBottomBar(
    items: List<Screen>,
    highlightedSlotIndex: Int,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val highlightFill = if (colors.isDark) colors.card else PureWhite

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        val isPlusHighlighted = highlightedSlotIndex == 2
        if (isPlusHighlighted) {
            Surface(
                shape = CircleShape,
                color = colors.azurePrimary,
                border = BorderStroke(1.5.dp, colors.azureAccent.copy(alpha = 0.75f)),
                shadowElevation = 6.dp,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_plus,
                        contentDescription = "Quick add action",
                        tint = PureWhite,
                        animateOnMount = true,
                        repeatIntervalMillis = 0L,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(60.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { idx, screen ->
                val slotIndex = if (idx < 2) idx else idx + 1
                val isHighlighted = slotIndex == highlightedSlotIndex
                TourBottomBarSlot(
                    highlighted = isHighlighted,
                    highlightFill = highlightFill,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isHighlighted) {
                        ShiftlyAnimatedIcon(
                            iconRes = screen.selectedIconRes,
                            contentDescription = screen.label,
                            tint = colors.azureAccent,
                            animateOnMount = true,
                            repeatIntervalMillis = 0L,
                            modifier = Modifier.size(22.dp)
                        )
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
    val colors = ShiftlyColors.current
    if (highlighted) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = highlightFill,
            border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.75f)),
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
private fun QuickAllowanceSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val allowanceViewModel: AllowanceViewModel = viewModel(factory = AllowanceViewModel.Factory(repository))
    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")

    AddAllowanceBottomSheet(
        currencySymbol = currencySymbol,
        onDismiss = onDismiss,
        onSave = { title, amount, source, date ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            allowanceViewModel.addManualAllowance(title, amount, source, date)
            onDismiss()
            onShowMessage("Allowance added")
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
private fun QuickRecurringBillSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val recurringViewModel: RecurringBillsViewModel = viewModel(factory = RecurringBillsViewModel.Factory(repository))
    val recurringState by recurringViewModel.uiState.collectAsStateWithLifecycle()

    AddEditRecurringBillBottomSheet(
        currencySymbol = recurringState.currencySymbol,
        availableCategories = recurringState.availableCategories,
        editingBill = null,
        onDismiss = onDismiss,
        onSave = { title, amount, category, frequency, dueDate, dueHour, dueMinute, isEnabled ->
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
                isEnabled = isEnabled
            ) { posted ->
                scope.launch(Dispatchers.IO) {
                    val bills = repository.allRecurringBills.first()
                    ShiftlyDailyWorker.scheduleNextRecurringCheck(
                        context = context,
                        nextDue = RecurringEngine.earliestNextDueDateTime(bills)
                    )
                }
                onDismiss()
                onShowMessage(
                    if (posted > 0) "Saved & posted $posted due bill(s) to Expenses"
                    else "Recurring bill added"
                )
            }
        }
    )
}

@Composable
private fun QuickInstallmentLoanSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val toolsViewModel: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repository))
    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")

    AddEditInstallmentLoanSheet(
        currencySymbol = currencySymbol,
        editingLoan = null,
        onDismiss = onDismiss,
        onSave = { loan ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            toolsViewModel.addOrUpdateInstallmentLoan(loan)
            onDismiss()
            onShowMessage("Loan contract saved")
        }
    )
}

@Composable
private fun QuickSavingsGoalSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val toolsViewModel: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repository))
    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")

    AddSavingsGoalSheet(
        currencySymbol = currencySymbol,
        onDismiss = onDismiss,
        onCreateGoal = { title, targetAmount, targetDateEpochDay ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            toolsViewModel.createSavingsGoal(title, targetAmount, targetDateEpochDay)
            onDismiss()
            onShowMessage("Savings goal \"$title\" added")
        }
    )
}

@Composable
private fun QuickIouSheetHost(
    repository: ShiftlyRepository,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val toolsViewModel: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repository))
    val currencySymbol by repository.currencySymbol.collectAsStateWithLifecycle(initialValue = "₱")

    AddIouBottomSheet(
        currencySymbol = currencySymbol,
        onDismiss = onDismiss,
        onSave = { personName, amount, isLent, note ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            toolsViewModel.addIouEntry(
                personName = personName,
                amount = amount,
                isLent = isLent,
                note = note
            )
            onDismiss()
            onShowMessage("IOU entry added")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddActionSheet(
    onDismiss: () -> Unit,
    onSelectAction: (ActiveQuickAddSheet) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetContainerColor = colors.surface
    val groupCardColor = colors.cardElevated

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAddPrimaryTile(
                        title = "Log Shift",
                        iconRes = R.drawable.ic_lucide_clock,
                        iconTint = colors.mintBright,
                        iconContainerColor = colors.mintContainer,
                        surfaceColor = groupCardColor,
                        onClick = { onSelectAction(ActiveQuickAddSheet.LOG_SHIFT) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAddPrimaryTile(
                        title = "Add Expense",
                        iconRes = R.drawable.ic_lucide_receipt_text,
                        iconTint = colors.coralBright,
                        iconContainerColor = colors.coralContainer,
                        surfaceColor = groupCardColor,
                        onClick = { onSelectAction(ActiveQuickAddSheet.ADD_EXPENSE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAddPrimaryTile(
                        title = "Allowance",
                        iconRes = R.drawable.ic_lucide_wallet,
                        iconTint = colors.azureAccent,
                        iconContainerColor = colors.azureContainer,
                        surfaceColor = groupCardColor,
                        onClick = { onSelectAction(ActiveQuickAddSheet.ADD_ALLOWANCE) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAddPrimaryTile(
                        title = "Split Bill",
                        iconRes = R.drawable.ic_lucide_split,
                        iconTint = colors.azureAccent,
                        iconContainerColor = colors.azureContainer,
                        surfaceColor = groupCardColor,
                        onClick = { onSelectAction(ActiveQuickAddSheet.SPLIT_BILL) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAddPrimaryTile(
                        title = "Recurring",
                        iconRes = R.drawable.ic_lucide_repeat,
                        iconTint = colors.iosOrange,
                        iconContainerColor = colors.orangeContainer,
                        surfaceColor = groupCardColor,
                        onClick = { onSelectAction(ActiveQuickAddSheet.NEW_RECURRING_BILL) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAddPrimaryTile(
                        title = "Loan",
                        iconRes = R.drawable.ic_lucide_file_text,
                        iconTint = colors.nightDiffPurple,
                        iconContainerColor = colors.nightDiffPurpleContainer,
                        surfaceColor = groupCardColor,
                        onClick = { onSelectAction(ActiveQuickAddSheet.NEW_INSTALLMENT_LOAN) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAddPrimaryTile(
                        title = "Goal",
                        iconRes = R.drawable.ic_lucide_piggy_bank,
                        iconTint = colors.mintBright,
                        iconContainerColor = colors.mintContainer,
                        surfaceColor = groupCardColor,
                        onClick = { onSelectAction(ActiveQuickAddSheet.NEW_SAVINGS_GOAL) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAddPrimaryTile(
                        title = "IOU",
                        iconRes = R.drawable.ic_lucide_circle_dollar_sign,
                        iconTint = colors.coralBright,
                        iconContainerColor = colors.coralContainer,
                        surfaceColor = groupCardColor,
                        onClick = { onSelectAction(ActiveQuickAddSheet.ADD_IOU) },
                        modifier = Modifier.weight(1f)
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
private fun QuickAddPrimaryTile(
    title: String,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    iconContainerColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    var tapCount by remember { mutableIntStateOf(0) }
    Surface(
        shape = ShiftlyShapes.Card,
        color = surfaceColor,
        shadowElevation = 0.dp,
        modifier = modifier
            .clip(ShiftlyShapes.Card)
            .shiftlyPressClickable(role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                tapCount++
                onClick()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                ShiftlyAnimatedIcon(
                    iconRes = iconRes,
                    contentDescription = null,
                    tint = iconTint,
                    triggerKey = tapCount,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Full-width floating pill bottom navigation bar (4 tabs with expanding active pill) paired with a
 * detached circular Quick Add (+) button floating at its upper-right corner.
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
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Detached Quick Add Circle Button floating at upper-right above the nav bar
        Surface(
            shape = CircleShape,
            color = colors.azurePrimary,
            shadowElevation = 6.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .shiftlyPressClickable(role = Role.Button) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFabClick()
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Quick add action"
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                ShiftlyAnimatedIcon(
                    iconRes = R.drawable.ic_lucide_plus,
                    contentDescription = "Quick add action",
                    tint = PureWhite,
                    triggerKey = isFabActive,
                    animateOnMount = false,
                    repeatIntervalMillis = 0L,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { rotationZ = fabRotationState.value }
                )
            }
        }

        // Full-width floating pill bottom navigation bar
        Surface(
            shape = NavBarPillShape,
            color = colors.navBarSurface,
            shadowElevation = 6.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val onClick = remember(screen, onSelect) { { onSelect(screen) } }
                    BottomNavItemSlot(
                        screen = screen,
                        selected = isSelected(screen),
                        onClick = onClick
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
    val haptics = LocalHapticFeedback.current
    val activePillBg = if (colors.isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        colors.azurePrimary.copy(alpha = 0.12f)
    }
    val activeContentColor = if (colors.isDark) PureWhite else colors.azurePrimary

    val pillColor by animateColorAsState(
        targetValue = if (selected) activePillBg else Color.Transparent,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "navPillColor"
    )
    val contentTint by animateColorAsState(
        targetValue = if (selected) activeContentColor else colors.textSecondary,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "navContentTint"
    )
    var wasSelected by remember { mutableStateOf(selected) }
    val iconBounce = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected && !wasSelected) {
            iconBounce.snapTo(1.16f)
            iconBounce.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.48f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        wasSelected = selected
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp)
            .height(48.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(NavBarPillShape)
            .background(pillColor)
            .shiftlyPressClickable(role = Role.Tab) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = screen.label
            }
            .padding(horizontal = if (selected) 16.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ShiftlyAnimatedIcon(
                iconRes = if (selected) screen.selectedIconRes else screen.unselectedIconRes,
                contentDescription = null,
                tint = contentTint,
                triggerKey = if (selected) screen.route else null,
                animateOnMount = false,
                repeatIntervalMillis = 0L,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconBounce.value
                        scaleY = iconBounce.value
                    }
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(180)) + expandHorizontally(
                    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(100)) + shrinkHorizontally(
                    spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        color = contentTint,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
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
