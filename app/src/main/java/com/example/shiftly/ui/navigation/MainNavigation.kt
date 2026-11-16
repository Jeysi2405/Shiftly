package com.example.shiftly.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.screens.duties.DutiesScreen
import com.example.shiftly.ui.screens.expense.ExpenseScreen
import com.example.shiftly.ui.screens.profile.ProfileScreen
import com.example.shiftly.ui.screens.splitbill.SplitBillScreen
import com.example.shiftly.ui.theme.BorderSubtle
import com.example.shiftly.ui.theme.DeepBlueContainer
import com.example.shiftly.ui.theme.ElectricBlue
import com.example.shiftly.ui.theme.ObsidianBackground
import com.example.shiftly.ui.theme.SlateSurface
import com.example.shiftly.ui.theme.SlateSurfaceElevated
import com.example.shiftly.ui.theme.SoftBlue
import com.example.shiftly.ui.theme.TextPrimary
import com.example.shiftly.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val repository = remember { ShiftlyRepository(context.applicationContext) }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SlateSurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shadowElevation = 6.dp
                ) {
                    Snackbar(
                        snackbarData = data,
                        containerColor = SlateSurfaceElevated,
                        contentColor = TextPrimary,
                        actionColor = SoftBlue
                    )
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                NavigationBar(
                    containerColor = SlateSurface,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                indicatorColor = DeepBlueContainer,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Duties.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Duties.route) { DutiesScreen(repository = repository, onShowMessage = showMessage) }
            composable(Screen.Expense.route) { ExpenseScreen(repository = repository, onShowMessage = showMessage) }
            composable(Screen.SplitBill.route) { SplitBillScreen(repository = repository, onShowMessage = showMessage) }
            composable(Screen.Profile.route) { ProfileScreen(repository = repository, onShowMessage = showMessage) }
        }
    }
}