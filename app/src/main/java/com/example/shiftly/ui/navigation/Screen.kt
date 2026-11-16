package com.example.shiftly.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Welcome : Screen(
        route = "welcome",
        label = "Welcome",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Home : Screen(
        route = "home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Analytics : Screen(
        route = "analytics",
        label = "Analytics",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    data object Tools : Screen(
        route = "tools",
        label = "Tools",
        selectedIcon = Icons.Filled.GridView,
        unselectedIcon = Icons.Outlined.GridView
    )

    data object Duties : Screen(
        route = "duties",
        label = "Duties",
        selectedIcon = Icons.Filled.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    )

    data object Expense : Screen(
        route = "expense",
        label = "Expense",
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong
    )

    data object SplitBill : Screen(
        route = "split_bill",
        label = "Split Bill",
        selectedIcon = Icons.AutoMirrored.Filled.CallSplit,
        unselectedIcon = Icons.AutoMirrored.Outlined.CallSplit
    )

    data object Profile : Screen(
        route = "profile",
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val bottomNavItems = listOf(Home, Analytics, Tools, Profile)
    }
}
