package com.example.shiftly.ui.navigation

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
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
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyMainTabTourOverlay
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlyPressEffect
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
import com.example.shiftly.ui.screens.lock.ShiftlyPostUnlockSkeletonOverlay
import com.example.shiftly.ui.screens.lock.ShiftlyScreenSkeletonOverlay
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val NavBarPillShape = ShiftlyShapes.Pill

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
    val highlightQuickAddFab: Boolean = false
)

private val MAIN_TAB_TOUR_STEPS by lazy {
    listOf(
        MainTourStep(
            title = "Home Overview",
            description = "Check your net balance, cutoff shift earnings, budget cap, and recent activity at a glance.",
            targetScreen = Screen.Home,
            highlightQuickAddFab = false
        ),
        MainTourStep(
            title = "Quick Add Modal",
            description = "Tap the floating + button above the bar to open Quick Add for shifts, expenses, allowance, split bills, recurring bills, loans, goals, and IOUs.",
            targetScreen = Screen.Home,
            highlightQuickAddFab = true
        ),
        MainTourStep(
            title = "Analytics",
            description = "Compare work earnings, spending categories, work hours, and daily patterns.",
            targetScreen = Screen.Analytics,
            highlightQuickAddFab = false
        ),
        MainTourStep(
            title = "Tools",
            description = "Open trackers and planning tools: Duties, Expenses, Split Bill, IOUs, Loans, Budgets, Goals, Recurring, Allowance, Paycheck, and Calendar.",
            targetScreen = Screen.Tools,
            highlightQuickAddFab = false
        ),
        MainTourStep(
            title = "Profile & Settings",
            description = "Tap your profile card to edit name or rate, and manage currency, budgets, app lock, theme, backups, or replay this tutorial.",
            targetScreen = Screen.Profile,
            highlightQuickAddFab = false
        )
    )
}

private fun <T> shiftlyNavSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private fun shiftlyEnter(): EnterTransition = fadeIn(tween(140))

private fun shiftlyExit(): ExitTransition = fadeOut(tween(110))

private fun shiftlyPopEnter(): EnterTransition = fadeIn(tween(140))

private fun shiftlyPopExit(): ExitTransition = fadeOut(tween(110))

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
    var showPostUnlockSkeleton by rememberSaveable { mutableStateOf(false) }
    var suppressRelockOnce by remember { mutableStateOf(false) }
    var lastBackgroundElapsedRealtime by rememberSaveable { mutableLongStateOf(-1L) }

    LaunchedEffect(showPostUnlockSkeleton) {
        if (showPostUnlockSkeleton) {
            delay(2_000L)
            showPostUnlockSkeleton = false
        }
    }

    LaunchedEffect(settings) {
        val loaded = settings ?: return@LaunchedEffect
        if (!hasInitializedLock) {
            hasInitializedLock = true
            isSessionUnlocked = !loaded.appLockEnabled
        } else if (!loaded.appLockEnabled) {
            isSessionUnlocked = true
            showPostUnlockSkeleton = false
            lastBackgroundElapsedRealtime = -1L
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(
        lifecycleOwner,
        settings?.appLockEnabled,
        settings?.appLockTimeoutSeconds
    ) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    val enabled = settings?.appLockEnabled == true
                    val timeoutSec = (settings?.appLockTimeoutSeconds ?: 0).coerceAtLeast(0)
                    val bgTimestamp = lastBackgroundElapsedRealtime
                    if (enabled && !suppressRelockOnce && bgTimestamp >= 0L) {
                        val elapsedMs = SystemClock.elapsedRealtime() - bgTimestamp
                        if (elapsedMs >= timeoutSec * 1000L) {
                            isSessionUnlocked = false
                            showPostUnlockSkeleton = false
                        }
                    }
                    lastBackgroundElapsedRealtime = -1L
                    suppressRelockOnce = false
                }
                Lifecycle.Event.ON_STOP -> {
                    val isConfigChange = (context as? Activity)?.isChangingConfigurations == true
                    val enabled = settings?.appLockEnabled == true
                    val timeoutSec = (settings?.appLockTimeoutSeconds ?: 0).coerceAtLeast(0)
                    if (enabled && !suppressRelockOnce && !isConfigChange) {
                        if (timeoutSec <= 0) {
                            isSessionUnlocked = false
                            showPostUnlockSkeleton = false
                            lastBackgroundElapsedRealtime = -1L
                        } else if (isSessionUnlocked) {
                            lastBackgroundElapsedRealtime = SystemClock.elapsedRealtime()
                        }
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
                        fadeIn(tween(160)) togetherWith fadeOut(tween(140))
                    },
                    label = "AppLockGateTransition"
                ) { isLocked ->
                    if (isLocked) {
                        AppLockGateScreen(
                            method = loadedSettings.appLockMethod,
                            pinHash = loadedSettings.appLockPinHash,
                            userName = userName.ifBlank { "Shiftly" },
                            onUnlocked = {
                                lastBackgroundElapsedRealtime = -1L
                                showPostUnlockSkeleton = true
                                isSessionUnlocked = true
                            },
                            onUpgradeLegacyPin = { rawPin ->
                                scope.launch {
                                    repository.updateAppLock(
                                        enabled = loadedSettings.appLockEnabled,
                                        method = loadedSettings.appLockMethod,
                                        newPin = rawPin,
                                        timeoutSeconds = loadedSettings.appLockTimeoutSeconds
                                    )
                                }
                            }
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
                                        onSaveSetupProfile = { name, rate, monthlyBudget, currencySymbol, cutoffSchedule ->
                                            scope.launch {
                                                repository.updateUserName(name)
                                                repository.changeCurrency(currencySymbol, null)
                                                repository.updateCutoffSchedule(
                                                    CutoffScheduleConfig.fromLegacySchedule(cutoffSchedule)
                                                )
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
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        MainDashboardShell(repository = repository)
                                        AnimatedVisibility(
                                            visible = showPostUnlockSkeleton,
                                            enter = fadeIn(tween(120)),
                                            exit = fadeOut(tween(280))
                                        ) {
                                            ShiftlyPostUnlockSkeletonOverlay()
                                        }
                                    }
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
    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var showRouteSkeleton by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        val route = currentRoute ?: return@LaunchedEffect
        if (previousRoute == null) {
            previousRoute = route
        } else if (previousRoute != route) {
            previousRoute = route
            showRouteSkeleton = true
            delay(500L)
            showRouteSkeleton = false
        }
    }

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
    val onPopBack: () -> Unit = remember(navController) { { navController.popBackStack() } }
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

    var fabBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var rootOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    val quickAddProgress by animateFloatAsState(
        targetValue = if (showQuickActionSheet) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 280f
        ),
        label = "quickAddModalProgress"
    )

    ShiftlyBlueGradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootOriginInRoot = it.positionInRoot() }
        ) {
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
                            isFabHidden = showQuickActionSheet || quickAddProgress > 0.005f,
                            onFabBoundsChanged = { fabBoundsInRoot = it },
                            onFabClick = onFabClick
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize(),
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
                AnimatedVisibility(
                    visible = showRouteSkeleton,
                    enter = fadeIn(tween(80)),
                    exit = fadeOut(tween(180))
                ) {
                    ShiftlyScreenSkeletonOverlay(
                        route = currentRoute,
                        includeBottomBar = false
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
                            activeScreen = currentStep.targetScreen,
                            highlightQuickAddFab = currentStep.highlightQuickAddFab
                        )
                    }
                )
            }
            if (showQuickActionSheet || quickAddProgress > 0.005f) {
                QuickAddPromptModalOverlay(
                    isOpen = showQuickActionSheet,
                    progress = quickAddProgress,
                    fabBoundsInRoot = fabBoundsInRoot,
                    containerOriginInRoot = rootOriginInRoot,
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
        }
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
    activeScreen: Screen,
    highlightQuickAddFab: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (highlightQuickAddFab) {
            Surface(
                shape = CircleShape,
                color = colors.azurePrimary,
                border = BorderStroke(2.dp, PureWhite.copy(alpha = 0.85f)),
                shadowElevation = 8.dp,
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
            items.forEach { screen ->
                val isSelectedTab = screen == activeScreen
                val isHighlighted = isSelectedTab && !highlightQuickAddFab
                TourBottomBarSlot(
                    screen = screen,
                    isSelectedTab = isSelectedTab,
                    highlighted = isHighlighted
                )
            }
        }
    }
}

@Composable
private fun TourBottomBarSlot(
    screen: Screen,
    isSelectedTab: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val highlightFill = if (colors.isDark) colors.cardElevated else PureWhite
    val highlightContentColor = if (colors.isDark) PureWhite else colors.azurePrimary

    when {
        highlighted -> {
            Surface(
                shape = NavBarPillShape,
                color = highlightFill,
                border = BorderStroke(1.5.dp, colors.azureAccent),
                shadowElevation = 6.dp,
                modifier = modifier
                    .defaultMinSize(minWidth = 48.dp)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ShiftlyAnimatedIcon(
                        iconRes = screen.selectedIconRes,
                        contentDescription = screen.label,
                        tint = highlightContentColor,
                        animateOnMount = true,
                        repeatIntervalMillis = 0L,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        color = highlightContentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        isSelectedTab -> {
            // Preserve the expanded pill's exact horizontal footprint when the + FAB is spotlighted above
            Box(
                modifier = modifier
                    .defaultMinSize(minWidth = 48.dp)
                    .height(48.dp)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer { alpha = 0f },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .defaultMinSize(minWidth = 48.dp)
                    .height(48.dp)
                    .padding(horizontal = 12.dp)
            )
        }
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

@Composable
private fun QuickAddPromptModalOverlay(
    isOpen: Boolean,
    progress: Float,
    fabBoundsInRoot: Rect,
    containerOriginInRoot: Offset,
    onDismiss: () -> Unit,
    onSelectAction: (ActiveQuickAddSheet) -> Unit
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val groupCardColor = colors.cardElevated
    val p = progress.coerceIn(0f, 1f)
    var cardRestingBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed backdrop that dismisses the prompt on outside tap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f * p))
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    enabled = isOpen,
                    onClick = onDismiss
                )
        )

        // Centered floating modal card (compact prompt, not full-screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .onGloballyPositioned { cardRestingBoundsInRoot = it.boundsInRoot() }
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surface,
                    shadowElevation = (18f * p).dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val s = lerp(0.90f, 1f, p)
                            scaleX = s
                            scaleY = s
                            alpha = p
                            translationY = (1f - p) * with(density) { 28.dp.toPx() }
                        }
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Add",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary
                            )
                            // Target landing spacer for the animated + -> × button at the upper-right
                            Box(modifier = Modifier.size(38.dp))
                        }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
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
                                onClick = { if (isOpen) onSelectAction(ActiveQuickAddSheet.LOG_SHIFT) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickAddPrimaryTile(
                                title = "Add Expense",
                                iconRes = R.drawable.ic_lucide_receipt_text,
                                iconTint = colors.coralBright,
                                iconContainerColor = colors.coralContainer,
                                surfaceColor = groupCardColor,
                                onClick = { if (isOpen) onSelectAction(ActiveQuickAddSheet.ADD_EXPENSE) },
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
                                onClick = { if (isOpen) onSelectAction(ActiveQuickAddSheet.ADD_ALLOWANCE) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickAddPrimaryTile(
                                title = "Split Bill",
                                iconRes = R.drawable.ic_lucide_split,
                                iconTint = colors.azureAccent,
                                iconContainerColor = colors.azureContainer,
                                surfaceColor = groupCardColor,
                                onClick = { if (isOpen) onSelectAction(ActiveQuickAddSheet.SPLIT_BILL) },
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
                                onClick = { if (isOpen) onSelectAction(ActiveQuickAddSheet.NEW_RECURRING_BILL) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickAddPrimaryTile(
                                title = "Loan",
                                iconRes = R.drawable.ic_lucide_file_text,
                                iconTint = colors.nightDiffPurple,
                                iconContainerColor = colors.nightDiffPurpleContainer,
                                surfaceColor = groupCardColor,
                                onClick = { if (isOpen) onSelectAction(ActiveQuickAddSheet.NEW_INSTALLMENT_LOAN) },
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
                                onClick = { if (isOpen) onSelectAction(ActiveQuickAddSheet.NEW_SAVINGS_GOAL) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickAddPrimaryTile(
                                title = "IOU",
                                iconRes = R.drawable.ic_lucide_circle_dollar_sign,
                                iconTint = colors.coralBright,
                                iconContainerColor = colors.coralContainer,
                                surfaceColor = groupCardColor,
                                onClick = { if (isOpen) onSelectAction(ActiveQuickAddSheet.ADD_IOU) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            }
        }

        // Animated + -> × button traveling between the FAB position and upper-right card corner
        if (!cardRestingBoundsInRoot.isEmpty || !fabBoundsInRoot.isEmpty) {
            val slotInsetPx = with(density) { 37.dp.toPx() }
            val restingTarget = if (!cardRestingBoundsInRoot.isEmpty) {
                Offset(
                    x = cardRestingBoundsInRoot.right - containerOriginInRoot.x - slotInsetPx,
                    y = cardRestingBoundsInRoot.top - containerOriginInRoot.y + slotInsetPx
                )
            } else {
                Offset.Zero
            }
            val startCenter = if (!fabBoundsInRoot.isEmpty) {
                fabBoundsInRoot.center - containerOriginInRoot
            } else {
                restingTarget
            }
            val targetCenter = if (!cardRestingBoundsInRoot.isEmpty) {
                restingTarget
            } else {
                startCenter
            }
            val currentCenter = Offset(
                x = lerp(startCenter.x, targetCenter.x, p),
                y = lerp(startCenter.y, targetCenter.y, p)
            )
            val buttonSizeDp = lerp(60.dp, 38.dp, p)
            val buttonSizePx = with(density) { buttonSizeDp.toPx() }
            val iconSizeDp = lerp(26.dp, 20.dp, p)

            Surface(
                shape = CircleShape,
                color = colors.azurePrimary,
                shadowElevation = 6.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (currentCenter.x - buttonSizePx / 2f).roundToInt(),
                            y = (currentCenter.y - buttonSizePx / 2f).roundToInt()
                        )
                    }
                    .size(buttonSizeDp)
                    .clip(CircleShape)
                    .shiftlyPressClickable(role = Role.Button) {
                        if (isOpen) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        }
                    }
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Close quick add"
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_plus,
                        contentDescription = "Close quick add",
                        tint = PureWhite,
                        animateOnMount = false,
                        repeatIntervalMillis = 0L,
                        modifier = Modifier
                            .size(iconSizeDp)
                            .graphicsLayer { rotationZ = 45f * p }
                    )
                }
            }
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
    isFabHidden: Boolean = false,
    onFabBoundsChanged: (Rect) -> Unit = {},
    onFabClick: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current

    val fabRotationState = animateFloatAsState(
        targetValue = if (isFabActive) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 280f
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
            shadowElevation = if (isFabHidden) 0.dp else 6.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .size(60.dp)
                .onGloballyPositioned { onFabBoundsChanged(it.boundsInRoot()) }
                .graphicsLayer { alpha = if (isFabHidden) 0f else 1f }
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

    val selectionProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 280f
        ),
        label = "navSelectionProgress"
    )
    val p = selectionProgress.coerceIn(0f, 1f)
    val pillColor = lerpColor(Color.Transparent, activePillBg, p)
    val contentTint = lerpColor(colors.textSecondary, activeContentColor, p)
    val horizontalPad = lerp(12.dp, 16.dp, p)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp)
            .height(48.dp)
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
            .padding(horizontal = horizontalPad),
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
                modifier = Modifier.size(22.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = p
                        translationX = (1f - p) * (-4.dp.toPx())
                        clip = true
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val currentWidth = (placeable.width * p).roundToInt()
                        layout(currentWidth, placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
            ) {
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
