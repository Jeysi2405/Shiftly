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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.example.shiftly.ui.screens.profile.ProfileScreen
import com.example.shiftly.ui.screens.splitbill.SplitBillScreen
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.DarkBackground
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.GlassNavBottom
import com.example.shiftly.ui.theme.GlassNavTop
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(
    onLightBackgroundChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { ShiftlyRepository(context.applicationContext) }
    val hasCompletedOnboarding by repository.hasCompletedOnboarding
        .collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()

    LaunchedEffect(hasCompletedOnboarding) {
        if (hasCompletedOnboarding != null) {
            onLightBackgroundChanged(false)
        }
    }

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
                    onGetStartedComplete = { name, rate, seedSample ->
                        scope.launch {
                            repository.updateUserName(name)
                            repository.updateDefaultHourlyRate(rate)
                            if (seedSample) {
                                repository.seedSampleStudentData()
                            }
                            repository.setOnboardingCompleted(completed = true)
                        }
                    },
                    onExistingAccount = {
                        scope.launch {
                            repository.setOnboardingCompleted(completed = true)
                        }
                    },
                )
            }

            true -> {
                MainDashboardShell(repository = repository)
            }
        }
    }
}

@Composable
private fun MainDashboardShell(
    repository: ShiftlyRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
 * Floating dark-charcoal glassmorphism pill bottom navigation bar with 5 icons and a crisp mint indicator.
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = pillShape,
                    ambientColor = InkBlack.copy(alpha = 0.55f),
                    spotColor = InkBlack.copy(alpha = 0.65f)
                )
                .clip(pillShape)
        ) {
            // Frosted dark charcoal glass backdrop layer (blurred softly without affecting icons)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(GlassNavTop, GlassNavBottom)
                        )
                    )
                    .blur(12.dp)
            )
            // Specular highlight sheen + thin low-opacity light border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PureWhite.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PureWhite.copy(alpha = 0.17f),
                                PureWhite.copy(alpha = 0.07f)
                            )
                        ),
                        shape = pillShape
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
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
                                onClick = { onSelect(screen) }
                            ),
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
                                contentDescription = screen.label,
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

@Preview(showBackground = false, widthDp = 393, name = "Floating Pill Bottom Bar")
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
