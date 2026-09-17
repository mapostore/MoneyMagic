package com.indiewalkabout.moneymagic.feature.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.dashboard.presentation.CategorySpend
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import java.time.Instant
import java.util.Locale

@Composable
fun RecentExpenseCard(expense: Expense, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
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
fun BudgetProgressCard(progress: BudgetProgress, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
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
fun CategorySpendCard(categorySpend: CategorySpend, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
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

@Preview(showBackground = true)
@Composable
private fun DashboardCardsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BudgetProgressCard(
                progress = BudgetProgress(
                    budget = Budget(
                        id = 1,
                        name = "Monthly food",
                        amountMinor = 30000,
                        currency = "EUR",
                        period = BudgetPeriod.Monthly,
                        categoryId = 1,
                        notificationThresholdPercent = 80,
                        enabled = true,
                    ),
                    spentMinor = 18000,
                    remainingMinor = 12000,
                    percentUsed = 60,
                    isNearTarget = false,
                    isOverBudget = false,
                ),
            )
            RecentExpenseCard(
                expense = Expense(
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
                ),
            )
            CategorySpendCard(
                categorySpend = CategorySpend(
                    category = Category(1, "SPESA", 0xFF00AA00, "spesa", 0, false),
                    amountMinor = 12900,
                ),
            )
        }
    }
}
