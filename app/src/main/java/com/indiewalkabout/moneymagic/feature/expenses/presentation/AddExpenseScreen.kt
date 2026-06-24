package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val ExpenseCategories = listOf(
    1L to "Food",
    2L to "Transport",
    3L to "Home",
    4L to "Other",
)

@Composable
fun AddExpenseScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    AddExpenseContent(
        uiState = uiState,
        onAmountChanged = viewModel::onAmountChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onMerchantChanged = viewModel::onMerchantChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onSave = viewModel::save,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun AddExpenseContent(
    uiState: AddExpenseUiState,
    onAmountChanged: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Add expense",
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextButton(
                    onClick = onBack,
                    enabled = !uiState.isSaving,
                ) {
                    Text("Cancel")
                }
            }
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                isError = uiState.errorMessage != null && !uiState.canSave,
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    uiState.errorMessage?.let { errorMessage ->
                        Text(errorMessage)
                    }
                },
            )
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { selectableGroup() },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExpenseCategories.forEach { (id, label) ->
                    val selected = uiState.categoryId == id
                    if (selected) {
                        Button(
                            onClick = { onCategorySelected(id) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(min = 72.dp)
                                .semantics {
                                    role = Role.RadioButton
                                    this.selected = true
                                },
                        ) {
                            Text(label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onCategorySelected(id) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(min = 72.dp)
                                .semantics {
                                    role = Role.RadioButton
                                    this.selected = false
                                },
                        ) {
                            Text(label)
                        }
                    }
                }
            }
            OutlinedTextField(
                value = uiState.merchant,
                onValueChange = onMerchantChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                label = { Text("Merchant") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                label = { Text("Notes") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Button(
                onClick = onSave,
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "Saving" else "Save")
            }
        }
    }
}
