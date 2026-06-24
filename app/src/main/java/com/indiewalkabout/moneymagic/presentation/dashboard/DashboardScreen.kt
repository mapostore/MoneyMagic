package com.indiewalkabout.moneymagic.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.domain.model.Expense
import java.util.Locale

@Composable
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
        )
        Button(
            onClick = onAddExpenseClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add expense")
        }
        Text(
            text = "Budget progress",
            style = MaterialTheme.typography.titleMedium,
        )
        if (uiState.budgetProgress.isEmpty()) {
            Text(
                text = "No active budgets",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            uiState.budgetProgress.forEach { progress ->
                BudgetProgressText(progress = progress)
            }
        }
        Text(
            text = "Recent expenses",
            style = MaterialTheme.typography.titleMedium,
        )
        if (uiState.recentExpenses.isEmpty()) {
            Text(
                text = "No recent expenses",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            uiState.recentExpenses.forEach { expense ->
                RecentExpenseText(expense = expense)
            }
        }
        Text(
            text = "Top categories",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun RecentExpenseText(expense: Expense) {
    Text(
        text = "${expense.merchant.ifBlank { "Expense" }} - ${formatAmount(expense.amountMinor, expense.currency)}",
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun formatAmount(amountMinor: Long, currency: String): String {
    val amount = amountMinor / 100.0
    return "%s %.2f".format(Locale.US, currency, amount)
}

@Composable
private fun BudgetProgressText(progress: BudgetProgress) {
    Text(
        text = "${progress.budget.name}: ${progress.percentUsed}% used, " +
            "${formatAmount(progress.remainingMinor, progress.budget.currency)} remaining",
        style = MaterialTheme.typography.bodyMedium,
    )
}
