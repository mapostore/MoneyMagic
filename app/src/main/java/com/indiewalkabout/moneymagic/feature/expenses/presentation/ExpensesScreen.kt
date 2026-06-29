package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExpensesScreen(
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Long) -> Unit,
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
            onSelected = viewModel::selectPeriodFilter,
        )
        SortDropdown(
            selected = uiState.selectedSortOption,
            onSelected = viewModel::selectSortOption,
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
                    ExpenseCard(item = item, onClick = { onExpenseClick(item.expense.id) })
                }
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    item: ExpenseListItem,
    onClick: () -> Unit,
) {
    val expense = item.expense
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}")
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = expense.name.ifBlank { expense.merchant.ifBlank { stringResource(R.string.add_expense) } },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = formatAmount(expense.amountMinor, expense.currency),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = buildString {
                    append(formatDate(expense))
                    if (item.categoryName.isNotBlank()) {
                        append(" - ")
                        append(item.categoryName)
                    }
                    item.paymentMethodName?.let {
                        append(" - ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (expense.merchant.isNotBlank()) {
                Text(text = expense.merchant, style = MaterialTheme.typography.bodySmall)
            }
            if (expense.notes.isNotBlank()) {
                Text(text = expense.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDropdown(
    selected: ExpensePeriodFilter,
    onSelected: (ExpensePeriodFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(stringResource(R.string.select_period)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ExpensePeriodFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label()) },
                    onClick = {
                        onSelected(filter)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(
    selected: ExpenseSortOption,
    onSelected: (ExpenseSortOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(stringResource(R.string.select_sort)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ExpenseSortOption.entries.forEach { sortOption ->
                DropdownMenuItem(
                    text = { Text(sortOption.label()) },
                    onClick = {
                        onSelected(sortOption)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun formatAmount(amountMinor: Long, currency: String): String {
    val amount = amountMinor / 100.0
    return "%s %.2f".format(Locale.getDefault(), currency, amount)
}

private fun formatDate(expense: Expense): String =
    DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(expense.dateTime)

@Composable
private fun ExpensePeriodFilter.label(): String =
    stringResource(
        when (this) {
            ExpensePeriodFilter.ALL -> R.string.period_all
            ExpensePeriodFilter.DAY -> R.string.period_day
            ExpensePeriodFilter.WEEK -> R.string.period_week
            ExpensePeriodFilter.MONTH -> R.string.period_month
            ExpensePeriodFilter.YEAR -> R.string.period_year
        },
    )

@Composable
private fun ExpenseSortOption.label(): String =
    stringResource(
        when (this) {
            ExpenseSortOption.NEWEST -> R.string.sort_newest
            ExpenseSortOption.HIGHEST_AMOUNT -> R.string.sort_most_expensive
            ExpenseSortOption.LOWEST_AMOUNT -> R.string.sort_least_expensive
        },
    )
