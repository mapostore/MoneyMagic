package com.indiewalkabout.moneymagic.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import java.util.Locale

@Composable
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

@Composable
private fun RecentExpenseCard(expense: Expense) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = expense.name.ifBlank { expense.merchant.ifBlank { stringResource(R.string.add_expense) } },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = formatAmount(expense.amountMinor, expense.currency),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BudgetProgressCard(progress: BudgetProgress) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = progress.budget.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(
                    R.string.budget_progress_status,
                    progress.percentUsed,
                    formatAmount(progress.remainingMinor, progress.budget.currency),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CategorySpendCard(categorySpend: CategorySpend) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = categorySpend.category.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = formatAmount(categorySpend.amountMinor, "EUR"),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatAmount(amountMinor: Long, currency: String): String {
    val amount = amountMinor / 100.0
    return "%s %.2f".format(Locale.getDefault(), currency, amount)
}
