package com.indiewalkabout.moneymagic.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.budgets.presentation.BudgetsScreen
import com.indiewalkabout.moneymagic.feature.dashboard.presentation.DashboardScreen
import com.indiewalkabout.moneymagic.feature.expenses.presentation.AddExpenseScreen
import com.indiewalkabout.moneymagic.feature.expenses.presentation.ExpensesScreen
import com.indiewalkabout.moneymagic.feature.settings.presentation.SettingsScreen
import kotlinx.serialization.Serializable

@Composable
fun MoneyMagicNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(DashboardRoute)
    val destinations = remember { MainDestination.entries }
    val currentRoute = backStack.last()
    val selectedDestination = destinations.firstOrNull { it.route == currentRoute }

    BackHandler(enabled = backStack.size == 1) {
        // Keep the current top-level route visible instead of exiting the app shell.
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (selectedDestination != null) {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = selectedDestination == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                backStack.clear()
                                backStack.add(destination.route)
                            },
                            modifier = Modifier.testTag(destination.testTag),
                            icon = {},
                            label = {
                                Text(text = stringResource(destination.labelResId))
                            },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            entryProvider = entryProvider {
                entry<DashboardRoute> {
                    DashboardScreen(onAddExpenseClick = { backStack.add(AddExpenseRoute) })
                }
                entry<ExpensesRoute> {
                    ExpensesScreen(onAddExpenseClick = { backStack.add(AddExpenseRoute) })
                }
                entry<AddExpenseRoute> {
                    AddExpenseScreen(
                        onSaved = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                    )
                }
                entry<BudgetsRoute> { BudgetsScreen() }
                entry<SettingsRoute> { SettingsScreen() }
            },
        )
    }
}

private enum class MainDestination(
    val labelResId: Int,
    val route: MoneyMagicRoute,
    val testTag: String,
) {
    Dashboard(R.string.bottom_nav_dashboard, DashboardRoute, "bottom_nav_dashboard"),
    Expenses(R.string.bottom_nav_expenses, ExpensesRoute, "bottom_nav_expenses"),
    Budgets(R.string.bottom_nav_budgets, BudgetsRoute, "bottom_nav_budgets"),
    Settings(R.string.bottom_nav_settings, SettingsRoute, "bottom_nav_settings"),
}

internal sealed interface MoneyMagicRoute : NavKey

@Serializable
internal data object DashboardRoute : MoneyMagicRoute

@Serializable
internal data object ExpensesRoute : MoneyMagicRoute

@Serializable
internal data object AddExpenseRoute : MoneyMagicRoute

@Serializable
internal data object BudgetsRoute : MoneyMagicRoute

@Serializable
internal data object SettingsRoute : MoneyMagicRoute
