package com.indiewalkabout.moneymagic.feature.expenses.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType
import com.indiewalkabout.moneymagic.feature.expenses.presentation.ExpensePeriodFilter
import com.indiewalkabout.moneymagic.feature.expenses.presentation.ExpenseSortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selectedCategory?.name.orEmpty(),
            onValueChange = {},
            modifier = modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            enabled = enabled,
            readOnly = true,
            label = { Text(stringResource(R.string.select_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodDropdown(
    paymentMethods: List<PaymentMethod>,
    selectedPaymentMethodId: Long?,
    onPaymentMethodSelected: (Long?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPaymentMethod = paymentMethods.firstOrNull { it.id == selectedPaymentMethodId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selectedPaymentMethod?.name.orEmpty(),
            onValueChange = {},
            modifier = modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            enabled = enabled,
            readOnly = true,
            label = { Text(stringResource(R.string.select_payment_method)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            paymentMethods.forEach { paymentMethod ->
                DropdownMenuItem(
                    text = { Text(paymentMethod.name) },
                    onClick = {
                        onPaymentMethodSelected(paymentMethod.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDropdown(
    selected: ExpensePeriodFilter,
    onSelected: (ExpensePeriodFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            modifier = modifier
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
fun SortDropdown(
    selected: ExpenseSortOption,
    onSelected: (ExpenseSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            modifier = modifier
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

@Preview(showBackground = true)
@Composable
private fun CategoryDropdownPreview() {
    MaterialTheme {
        CategoryDropdown(
            categories = listOf(sampleCategory(1, "SPESA"), sampleCategory(2, "BENZINA")),
            selectedCategoryId = 1,
            onCategorySelected = {},
            enabled = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentMethodDropdownPreview() {
    MaterialTheme {
        PaymentMethodDropdown(
            paymentMethods = listOf(PaymentMethod(id = 1, name = "Card", type = PaymentMethodType.Card, archived = false)),
            selectedPaymentMethodId = 1,
            onPaymentMethodSelected = {},
            enabled = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PeriodDropdownPreview() {
    MaterialTheme {
        PeriodDropdown(selected = ExpensePeriodFilter.MONTH, onSelected = {})
    }
}

private fun sampleCategory(id: Long, name: String): Category =
    Category(id = id, name = name, color = 0xFF00AA00, iconKey = name.lowercase(), sortOrder = id.toInt(), archived = false)
