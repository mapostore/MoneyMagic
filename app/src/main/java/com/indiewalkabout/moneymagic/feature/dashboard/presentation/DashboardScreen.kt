package com.indiewalkabout.moneymagic.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.dashboard.presentation.components.BudgetProgressCard
import com.indiewalkabout.moneymagic.feature.dashboard.presentation.components.CategorySpendCard
import com.indiewalkabout.moneymagic.feature.dashboard.presentation.components.RecentExpenseCard
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import java.time.Instant

@Composable
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        uiState = uiState,
        onAddExpenseClick = onAddExpenseClick,
        onScanReceiptClick = onScanReceiptClick,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onAddExpenseClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddExpenseClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.add_expense))
                }
                Button(
                    onClick = onScanReceiptClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.scan_receipt))
                }
            }
        }
        item { SectionTitle(text = stringResource(R.string.budget_progress)) }
        if (uiState.budgetProgress.isEmpty()) {
            item { Text(text = stringResource(R.string.no_active_budgets), style = MaterialTheme.typography.bodyMedium) }
        } else {
            uiState.budgetProgress.forEach { progress ->
                item { BudgetProgressCard(progress = progress) }
            }
        }
        item { SectionTitle(text = stringResource(R.string.recent_expenses)) }
        if (uiState.recentExpenses.isEmpty()) {
            item { Text(text = stringResource(R.string.no_recent_expenses), style = MaterialTheme.typography.bodyMedium) }
        } else {
            uiState.recentExpenses.forEach { expense ->
                item { RecentExpenseCard(expense = expense) }
            }
        }
        item { SectionTitle(text = stringResource(R.string.top_categories)) }
        if (uiState.topCategories.isEmpty()) {
            item { Text(text = stringResource(R.string.no_category_spending), style = MaterialTheme.typography.bodyMedium) }
        } else {
            uiState.topCategories.forEach { categorySpend ->
                item { CategorySpendCard(categorySpend = categorySpend) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardContent(
            uiState = DashboardUiState(
                recentExpenses = listOf(sampleDashboardExpense()),
                budgetProgress = listOf(sampleDashboardBudgetProgress()),
                topCategories = listOf(
                    CategorySpend(
                        category = Category(1, "SPESA", 0xFF00AA00, "spesa", 0, false),
                        amountMinor = 12900,
                    ),
                ),
            ),
            onAddExpenseClick = {},
            onScanReceiptClick = {},
        )
    }
}

private fun sampleDashboardExpense(): Expense =
    Expense(
        id = 1,
        name = "Grocery run",
        amountMinor = 4288,
        currency = "EUR",
        dateTime = Instant.parse("2026-06-24T12:30:00Z"),
        categoryId = 1,
        merchant = "Market",
        paymentMethodId = 1,
        description = "SPESA 2026-06-24 14:30",
        notes = "",
        tags = emptyList(),
        createdAt = Instant.parse("2026-06-24T12:30:00Z"),
        updatedAt = Instant.parse("2026-06-24T12:30:00Z"),
    )

private fun sampleDashboardBudgetProgress(): BudgetProgress =
    BudgetProgress(
        budget = Budget(1, "Monthly groceries", 30000, "EUR", BudgetPeriod.Monthly, 1, 80, true),
        spentMinor = 18000,
        remainingMinor = 12000,
        percentUsed = 60,
        isNearTarget = false,
        isOverBudget = false,
    )
