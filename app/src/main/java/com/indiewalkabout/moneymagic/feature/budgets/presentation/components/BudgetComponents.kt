package com.indiewalkabout.moneymagic.feature.budgets.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import java.util.Locale

@Composable
fun BudgetProgressRow(
    progress: BudgetProgress,
    categories: List<Category>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "${progress.budget.name} - ${progress.budget.period.label()}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(text = progress.budget.scopeLabel(categories), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
            progress = { progress.percentUsed.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(
                R.string.spent_of,
                formatAmount(progress.spentMinor, progress.budget.currency),
                formatAmount(progress.budget.amountMinor, progress.budget.currency),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(
                R.string.budget_progress_status,
                progress.percentUsed,
                formatAmount(progress.remainingMinor, progress.budget.currency),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScopeDropdown(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = categories.firstOrNull { it.id == selectedCategoryId }?.name
        ?: stringResource(R.string.all_categories)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            enabled = enabled,
            readOnly = true,
            label = { Text(stringResource(R.string.budget_scope)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_categories)) },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                },
            )
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

@Composable
private fun Budget.scopeLabel(categories: List<Category>): String =
    categoryId?.let { selectedCategoryId ->
        categories.firstOrNull { it.id == selectedCategoryId }?.name
    } ?: stringResource(R.string.all_categories)

@Composable
private fun BudgetPeriod.label(): String =
    when (this) {
        BudgetPeriod.Weekly -> stringResource(R.string.budget_period_weekly)
        BudgetPeriod.Monthly -> stringResource(R.string.budget_period_monthly)
        BudgetPeriod.Yearly -> stringResource(R.string.budget_period_yearly)
        is BudgetPeriod.Custom -> stringResource(R.string.budget_period_custom)
    }

private fun formatAmount(amountMinor: Long, currency: String): String {
    val amount = amountMinor / 100.0
    return "%s %.2f".format(Locale.getDefault(), currency, amount)
}

@Preview(showBackground = true)
@Composable
private fun BudgetComponentsPreview() {
    val categories = listOf(Category(1, "SPESA", 0xFF00AA00, "spesa", 0, false))
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BudgetScopeDropdown(
                categories = categories,
                selectedCategoryId = 1,
                onCategorySelected = {},
                enabled = true,
            )
            BudgetProgressRow(
                progress = BudgetProgress(
                    budget = Budget(1, "Monthly groceries", 30000, "EUR", BudgetPeriod.Monthly, 1, 80, true),
                    spentMinor = 18000,
                    remainingMinor = 12000,
                    percentUsed = 60,
                    isNearTarget = false,
                    isOverBudget = false,
                ),
                categories = categories,
            )
        }
    }
}
