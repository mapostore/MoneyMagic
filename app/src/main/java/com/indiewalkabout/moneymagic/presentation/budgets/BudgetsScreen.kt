package com.indiewalkabout.moneymagic.presentation.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.domain.model.BudgetProgress
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
            text = "Budget control",
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            label = { Text("Budget name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = viewModel::onAmountChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            label = { Text("Monthly limit") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = uiState.thresholdPercent,
            onValueChange = viewModel::onThresholdChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            label = { Text("Alert threshold percent") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                uiState.errorMessage?.let { Text(it) }
            },
        )
        Button(
            onClick = viewModel::saveBudget,
            enabled = uiState.canSave && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isSaving) "Saving" else "Save monthly budget")
        }
        Text(
            text = "Active budgets",
            style = MaterialTheme.typography.titleMedium,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.budgetProgress.isEmpty()) {
                item {
                    Text(
                        text = "No active budgets yet",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(
                    items = uiState.budgetProgress,
                    key = { progress -> progress.budget.id },
                ) { progress ->
                    BudgetProgressRow(progress = progress)
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressRow(progress: BudgetProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "${progress.budget.name} - ${progress.budget.period.label()}",
            style = MaterialTheme.typography.titleSmall,
        )
        LinearProgressIndicator(
            progress = { progress.percentUsed.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${formatAmount(progress.spentMinor, progress.budget.currency)} spent of " +
                formatAmount(progress.budget.amountMinor, progress.budget.currency),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${progress.percentUsed}% used - " +
                "${formatAmount(progress.remainingMinor, progress.budget.currency)} remaining",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun BudgetPeriod.label(): String =
    when (this) {
        BudgetPeriod.Weekly -> "Weekly"
        BudgetPeriod.Monthly -> "Monthly"
        BudgetPeriod.Yearly -> "Yearly"
        is BudgetPeriod.Custom -> "Custom"
    }

private fun formatAmount(amountMinor: Long, currency: String): String {
    val amount = amountMinor / 100.0
    return "%s %.2f".format(Locale.US, currency, amount)
}
