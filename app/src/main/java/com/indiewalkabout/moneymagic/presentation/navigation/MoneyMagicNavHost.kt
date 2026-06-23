package com.indiewalkabout.moneymagic.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.indiewalkabout.moneymagic.presentation.budgets.BudgetsScreen
import com.indiewalkabout.moneymagic.presentation.dashboard.DashboardScreen
import com.indiewalkabout.moneymagic.presentation.expenses.ExpensesScreen
import com.indiewalkabout.moneymagic.presentation.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Composable
fun MoneyMagicNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(DashboardRoute)
    var selectedDestination by remember { mutableStateOf<MainDestination>(MainDestination.Dashboard) }
    val destinations = remember { MainDestination.entries }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = selectedDestination == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            selectedDestination = destination
                            backStack.clear()
                            backStack.add(destination.route)
                        },
                        icon = {},
                        label = {
                            Text(
                                text = destination.label,
                                modifier = if (selected) {
                                    Modifier.clearAndSetSemantics {}
                                } else {
                                    Modifier
                                },
                            )
                        },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = {},
            entryProvider = entryProvider {
                entry<DashboardRoute> { DashboardScreen() }
                entry<ExpensesRoute> { ExpensesScreen() }
                entry<BudgetsRoute> { BudgetsScreen() }
                entry<SettingsRoute> { SettingsScreen() }
            },
        )
    }
}

private enum class MainDestination(
    val label: String,
    val route: MoneyMagicRoute,
) {
    Dashboard("Dashboard", DashboardRoute),
    Expenses("Expenses", ExpensesRoute),
    Budgets("Budgets", BudgetsRoute),
    Settings("Settings", SettingsRoute),
}

private sealed interface MoneyMagicRoute : NavKey

@Serializable
private data object DashboardRoute : MoneyMagicRoute

@Serializable
private data object ExpensesRoute : MoneyMagicRoute

@Serializable
private data object BudgetsRoute : MoneyMagicRoute

@Serializable
private data object SettingsRoute : MoneyMagicRoute
