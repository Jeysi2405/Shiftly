package com.example.shiftly.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Duties : Screen(
        route = "duties",
        label = "Duties",
        selectedIcon = Icons.Outlined.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    )

    data object Expense : Screen(
        route = "expense",
        label = "Expense",
        selectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong
    )

    data object SplitBill : Screen(
        route = "split_bill",
        label = "Split Bill",
        selectedIcon = Icons.AutoMirrored.Outlined.CallSplit,
        unselectedIcon = Icons.AutoMirrored.Outlined.CallSplit
    )

    data object Profile : Screen(
        route = "profile",
        label = "Profile",
        selectedIcon = Icons.Outlined.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val bottomNavItems = listOf(Duties, Expense, SplitBill, Profile)
    }
}
