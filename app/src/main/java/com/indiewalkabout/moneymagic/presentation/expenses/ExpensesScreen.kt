package com.indiewalkabout.moneymagic.presentation.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.domain.model.Expense
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExpensesScreen(
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Expense history",
            style = MaterialTheme.typography.headlineMedium,
        )
        Button(
            onClick = onAddExpenseClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add expense")
        }
        Text(
            text = "Filter by period",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExpensePeriodFilter.entries.forEach { filter ->
                FilterButton(
                    label = filter.label(),
                    selected = uiState.selectedPeriodFilter == filter,
                    onClick = { viewModel.selectPeriodFilter(filter) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = "Sort by newest",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExpenseSortOption.entries.forEach { sortOption ->
                FilterButton(
                    label = sortOption.label(),
                    selected = uiState.selectedSortOption == sortOption,
                    onClick = { viewModel.selectSortOption(sortOption) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.expenses.isEmpty()) {
                item {
                    Text(
                        text = "No expenses yet",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(
                    items = uiState.expenses,
                    key = { expense -> expense.id },
                ) { expense ->
                    ExpenseRow(expense = expense)
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = expense.merchant.ifBlank { "Expense" },
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "${formatDate(expense)} - ${formatAmount(expense.amountMinor, expense.currency)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (expense.notes.isNotBlank()) {
            Text(
                text = expense.notes,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatAmount(amountMinor: Long, currency: String): String {
    val amount = amountMinor / 100.0
    return "%s %.2f".format(Locale.US, currency, amount)
}

private fun formatDate(expense: Expense): String =
    DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US)
        .withZone(ZoneId.systemDefault())
        .format(expense.dateTime)

@Composable
private fun FilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

private fun ExpensePeriodFilter.label(): String =
    when (this) {
        ExpensePeriodFilter.ALL -> "All"
        ExpensePeriodFilter.DAY -> "Day"
        ExpensePeriodFilter.WEEK -> "Week"
        ExpensePeriodFilter.MONTH -> "Month"
        ExpensePeriodFilter.YEAR -> "Year"
    }

private fun ExpenseSortOption.label(): String =
    when (this) {
        ExpenseSortOption.NEWEST -> "Newest"
        ExpenseSortOption.HIGHEST_AMOUNT -> "Most"
        ExpenseSortOption.LOWEST_AMOUNT -> "Least"
    }
