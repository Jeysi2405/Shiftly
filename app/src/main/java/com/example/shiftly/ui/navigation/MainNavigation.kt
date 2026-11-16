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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.shiftly.ui.screens.WelcomeScreen
import com.example.shiftly.ui.screens.duties.DutiesScreen
import com.example.shiftly.ui.screens.expense.ExpenseScreen
import com.example.shiftly.ui.screens.lock.AppLockGateScreen
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.screens.profile.ProfileScreen
import com.example.shiftly.ui.screens.splitbill.SplitBillScreen
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.DarkBackground
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.MintBright
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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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

    ShiftlyBlueGradientBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 86.dp)
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
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true ||
                            (screen == Screen.Home && currentDestination?.route == Screen.Analytics.route)
                    },
                    onSelect = ::navigateToTab
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
                        onNavigateToAnalytics = {
                            navController.navigate(Screen.Analytics.route) {
                                launchSingleTop = true
                            }
                        },
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
                composable(Screen.Duties.route) {
                    DutiesScreen(repository = repository, onShowMessage = showMessage)
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
}

/**
 * Floating dark-charcoal pill bottom navigation bar matching app container colors (DarkSurface + DarkBorder).
 */
@Composable
fun FloatingPillBottomBar(
    items: List<Screen>,
    isSelected: (Screen) -> Boolean,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
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
                items.forEach { screen ->
                    val selected = isSelected(screen)
                    val indicatorBg by animateColorAsState(
                        targetValue = if (selected) MintBright.copy(alpha = 0.18f) else Color.Transparent,
                        label = "navIndicatorBg"
                    )
                    val indicatorBorder by animateColorAsState(
                        targetValue = if (selected) MintBright.copy(alpha = 0.45f) else Color.Transparent,
                        label = "navIndicatorBorder"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) MintBright else TextLightSecondary,
                        label = "navIconTint"
                    )

                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(screen) }
                            )
                            .semantics {
                                this.selected = selected
                                contentDescription = screen.label
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(indicatorBg)
                                .border(1.dp, indicatorBorder, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            }
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
