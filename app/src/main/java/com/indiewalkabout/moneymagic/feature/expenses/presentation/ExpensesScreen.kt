package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.presentation.components.ExpenseCard
import com.indiewalkabout.moneymagic.feature.expenses.presentation.components.PeriodDropdown
import com.indiewalkabout.moneymagic.feature.expenses.presentation.components.SortDropdown
import java.time.Instant

@Composable
fun ExpensesScreen(
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingRemoveItem by remember { mutableStateOf<ExpenseListItem?>(null) }

    pendingRemoveItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRemoveItem = null },
            title = { Text(stringResource(R.string.remove_expense_title)) },
            text = { Text(stringResource(R.string.remove_expense_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(item.expense.id)
                        pendingRemoveItem = null
                    },
                ) {
                    Text(stringResource(R.string.remove_expense))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveItem = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    ExpensesContent(
        uiState = uiState,
        onAddExpenseClick = onAddExpenseClick,
        onExpenseClick = onExpenseClick,
        onPeriodSelected = viewModel::selectPeriodFilter,
        onSortSelected = viewModel::selectSortOption,
        onRemoveClick = { pendingRemoveItem = it },
        modifier = modifier,
    )
}

@Composable
private fun ExpensesContent(
    uiState: ExpensesUiState,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    onPeriodSelected: (ExpensePeriodFilter) -> Unit,
    onSortSelected: (ExpenseSortOption) -> Unit,
    onRemoveClick: (ExpenseListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.expense_history),
            style = MaterialTheme.typography.headlineMedium,
        )
        Button(
            onClick = onAddExpenseClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_expense))
        }
        PeriodDropdown(
            selected = uiState.selectedPeriodFilter,
            onSelected = onPeriodSelected,
        )
        SortDropdown(
            selected = uiState.selectedSortOption,
            onSelected = onSortSelected,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.expenses.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_expenses_yet),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(
                    items = uiState.expenses,
                    key = { item -> item.expense.id },
                ) { item ->
                    ExpenseCard(
                        item = item,
                        onClick = { onExpenseClick(item.expense.id) },
                        onRemoveClick = { onRemoveClick(item) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpensesScreenPreview() {
    MaterialTheme {
        ExpensesContent(
            uiState = ExpensesUiState(
                expenses = listOf(
                    ExpenseListItem(
                        expense = Expense(
                            id = 1,
                            name = "Fuel stop",
                            amountMinor = 4567,
                            currency = "EUR",
                            dateTime = Instant.parse("2026-06-24T12:30:00Z"),
                            categoryId = 1,
                            merchant = "Q8",
                            paymentMethodId = 1,
                            description = "BENZINA 2026-06-24 14:30",
                            notes = "OCR text",
                            tags = emptyList(),
                            createdAt = Instant.parse("2026-06-24T12:30:00Z"),
                            updatedAt = Instant.parse("2026-06-24T12:30:00Z"),
                        ),
                        categoryName = "BENZINA",
                        paymentMethodName = "Card",
                    ),
                ),
            ),
            onAddExpenseClick = {},
            onExpenseClick = {},
            onPeriodSelected = {},
            onSortSelected = {},
            onRemoveClick = {},
        )
    }
}
