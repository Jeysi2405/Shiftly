package com.example.shiftly.ui.navigation

import androidx.annotation.DrawableRes
import com.example.shiftly.R

sealed class Screen(
    val route: String,
    val label: String,
    @DrawableRes val selectedIconRes: Int,
    @DrawableRes val unselectedIconRes: Int
) {
    data object Welcome : Screen(
        route = "welcome",
        label = "Welcome",
        selectedIconRes = R.drawable.ic_lucide_house,
        unselectedIconRes = R.drawable.ic_lucide_house
    )

    data object Home : Screen(
        route = "home",
        label = "Home",
        selectedIconRes = R.drawable.ic_lucide_house,
        unselectedIconRes = R.drawable.ic_lucide_house
    )

    data object Analytics : Screen(
        route = "analytics",
        label = "Analytics",
        selectedIconRes = R.drawable.ic_lucide_chart_column,
        unselectedIconRes = R.drawable.ic_lucide_chart_column
    )

    data object Tools : Screen(
        route = "tools",
        label = "Tools",
        selectedIconRes = R.drawable.ic_lucide_layout_grid,
        unselectedIconRes = R.drawable.ic_lucide_layout_grid
    )

    data object Duties : Screen(
        route = "duties",
        label = "Duties",
        selectedIconRes = R.drawable.ic_lucide_clock,
        unselectedIconRes = R.drawable.ic_lucide_clock
    )

    data object Expense : Screen(
        route = "expense",
        label = "Expense",
        selectedIconRes = R.drawable.ic_lucide_receipt_text,
        unselectedIconRes = R.drawable.ic_lucide_receipt_text
    )

    data object SplitBill : Screen(
        route = "split_bill",
        label = "Split Bill",
        selectedIconRes = R.drawable.ic_lucide_split,
        unselectedIconRes = R.drawable.ic_lucide_split
    )

    data object RecurringBills : Screen(
        route = "recurring_bills",
        label = "Recurring Bills",
        selectedIconRes = R.drawable.ic_lucide_repeat,
        unselectedIconRes = R.drawable.ic_lucide_repeat
    )

    data object Allowance : Screen(
        route = "allowance",
        label = "Allowance",
        selectedIconRes = R.drawable.ic_lucide_wallet,
        unselectedIconRes = R.drawable.ic_lucide_wallet
    )

    data object Profile : Screen(
        route = "profile",
        label = "Profile",
        selectedIconRes = R.drawable.ic_lucide_user,
        unselectedIconRes = R.drawable.ic_lucide_user
    )

    companion object {
        val bottomNavItems = listOf(Home, Analytics, Tools, Profile)
    }
}
