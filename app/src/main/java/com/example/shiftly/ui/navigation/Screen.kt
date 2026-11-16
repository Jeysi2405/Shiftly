package com.example.shiftly.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
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
        selectedIcon = Icons.Rounded.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    )

    data object Expense : Screen(
        route = "expense",
        label = "Expense",
        selectedIcon = Icons.Rounded.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    )

    data object SplitBill : Screen(
        route = "split_bill",
        label = "Split Bill",
        selectedIcon = Icons.Rounded.CallSplit,
        unselectedIcon = Icons.Outlined.CallSplit
    )

    data object Profile : Screen(
        route = "profile",
        label = "Profile",
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val bottomNavItems = listOf(Duties, Expense, SplitBill, Profile)
    }
}