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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.budgets.presentation.components.BudgetProgressRow
import com.indiewalkabout.moneymagic.feature.budgets.presentation.components.BudgetScopeDropdown
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BudgetsContent(
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onThresholdChanged = viewModel::onThresholdChanged,
        onSaveBudget = viewModel::saveBudget,
        modifier = modifier,
    )
}

@Composable
private fun BudgetsContent(
    uiState: BudgetsUiState,
    onNameChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onThresholdChanged: (String) -> Unit,
    onSaveBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            label = { Text(stringResource(R.string.budget_name)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = onAmountChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            label = { Text(stringResource(R.string.monthly_limit)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        BudgetScopeDropdown(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = onCategorySelected,
            enabled = !uiState.isSaving,
        )
        OutlinedTextField(
            value = uiState.thresholdPercent,
            onValueChange = onThresholdChanged,
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
            onClick = onSaveBudget,
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

@Preview(showBackground = true)
@Composable
private fun BudgetsScreenPreview() {
    val categories = listOf(Category(1, "SPESA", 0xFF00AA00, "spesa", 0, false))
    MaterialTheme {
        BudgetsContent(
            uiState = BudgetsUiState(
                name = "Monthly groceries",
                amount = "300.00",
                selectedCategoryId = 1,
                categories = categories,
                budgetProgress = listOf(
                    BudgetProgress(
                        budget = Budget(1, "Monthly groceries", 30000, "EUR", BudgetPeriod.Monthly, 1, 80, true),
                        spentMinor = 18000,
                        remainingMinor = 12000,
                        percentUsed = 60,
                        isNearTarget = false,
                        isOverBudget = false,
                    ),
                ),
                canSave = true,
            ),
            onNameChanged = {},
            onAmountChanged = {},
            onCategorySelected = {},
            onThresholdChanged = {},
            onSaveBudget = {},
        )
    }
}

@Composable
private fun BudgetError.label(): String =
    when (this) {
        BudgetError.InvalidForm -> stringResource(R.string.budget_invalid_form)
        BudgetError.SaveFailed -> stringResource(R.string.unable_save_budget)
    }
