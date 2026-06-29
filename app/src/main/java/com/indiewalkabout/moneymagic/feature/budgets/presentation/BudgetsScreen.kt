package com.indiewalkabout.moneymagic.feature.budgets.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import java.util.Locale

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.budget_control),
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            label = { Text(stringResource(R.string.budget_name)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = viewModel::onAmountChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            label = { Text(stringResource(R.string.monthly_limit)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        BudgetScopeDropdown(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = viewModel::onCategorySelected,
            enabled = !uiState.isSaving,
        )
        OutlinedTextField(
            value = uiState.thresholdPercent,
            onValueChange = viewModel::onThresholdChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            label = { Text(stringResource(R.string.alert_threshold_percent)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                uiState.errorMessage?.let { Text(it.label()) }
            },
        )
        Button(
            onClick = viewModel::saveBudget,
            enabled = uiState.canSave && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (uiState.isSaving) {
                    stringResource(R.string.saving)
                } else {
                    stringResource(R.string.save_monthly_budget)
                },
            )
        }
        Text(
            text = stringResource(R.string.active_budgets),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.budgetProgress.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_active_budgets_yet),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(
                    items = uiState.budgetProgress,
                    key = { progress -> progress.budget.id },
                ) { progress ->
                    BudgetProgressRow(progress = progress, categories = uiState.categories)
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressRow(
    progress: BudgetProgress,
    categories: List<Category>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "${progress.budget.name} - ${progress.budget.period.label()}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = progress.budget.scopeLabel(categories),
            style = MaterialTheme.typography.bodySmall,
        )
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
private fun BudgetScopeDropdown(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = categories.firstOrNull { it.id == selectedCategoryId }?.name
        ?: stringResource(R.string.all_categories)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
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
private fun Budget.scopeLabel(
    categories: List<Category>,
): String =
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

@Composable
private fun BudgetError.label(): String =
    when (this) {
        BudgetError.InvalidForm -> stringResource(R.string.budget_invalid_form)
        BudgetError.SaveFailed -> stringResource(R.string.unable_save_budget)
    }

private fun formatAmount(amountMinor: Long, currency: String): String {
    val amount = amountMinor / 100.0
    return "%s %.2f".format(Locale.getDefault(), currency, amount)
}
