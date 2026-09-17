package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType
import com.indiewalkabout.moneymagic.feature.expenses.presentation.components.CategoryDropdown
import com.indiewalkabout.moneymagic.feature.expenses.presentation.components.ExpandableOcrField
import com.indiewalkabout.moneymagic.feature.expenses.presentation.components.ExpenseDateField
import com.indiewalkabout.moneymagic.feature.expenses.presentation.components.PaymentMethodDropdown

@Composable
fun AddExpenseScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    initialDraft: ExpenseDraftInput? = null,
    modifier: Modifier = Modifier,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialDraft) {
        initialDraft?.let(viewModel::applyDraft)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    AddExpenseContent(
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
        onDateChanged = viewModel::onDateChanged,
        onTimeChanged = viewModel::onTimeChanged,
        onMerchantChanged = viewModel::onMerchantChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onSave = viewModel::save,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun AddExpenseContent(
    uiState: AddExpenseUiState,
    onNameChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onPaymentMethodSelected: (Long?) -> Unit,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
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
                    text = stringResource(R.string.add_expense_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextButton(
                    onClick = onBack,
                    enabled = !uiState.isSaving,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                label = { Text(stringResource(R.string.expense_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                isError = uiState.errorMessage != null && !uiState.canSave,
                label = { Text(stringResource(R.string.amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    uiState.errorMessage?.let { errorMessage ->
                        Text(errorMessage.label())
                    }
                },
            )
            CategoryDropdown(
                categories = uiState.categories,
                selectedCategoryId = uiState.categoryId,
                onCategorySelected = onCategorySelected,
                enabled = !uiState.isSaving,
            )
            PaymentMethodDropdown(
                paymentMethods = uiState.paymentMethods,
                selectedPaymentMethodId = uiState.paymentMethodId,
                onPaymentMethodSelected = onPaymentMethodSelected,
                enabled = !uiState.isSaving,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExpenseDateField(
                    value = uiState.date,
                    onValueChange = onDateChanged,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving,
                )
                OutlinedTextField(
                    value = uiState.time,
                    onValueChange = onTimeChanged,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving,
                    label = { Text(stringResource(R.string.time)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            }
            OutlinedTextField(
                value = uiState.merchant,
                onValueChange = onMerchantChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                label = { Text(stringResource(R.string.merchant)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                label = { Text(stringResource(R.string.description)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            ExpandableOcrField(
                notes = uiState.notes,
                onNotesChanged = onNotesChanged,
                enabled = !uiState.isSaving,
            )
            Button(
                onClick = onSave,
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) stringResource(R.string.saving) else stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun AddExpenseError.label(): String =
    when (this) {
        AddExpenseError.EnterAmount -> stringResource(R.string.enter_amount)
        AddExpenseError.AmountMustBePositive -> stringResource(R.string.amount_must_be_positive)
        AddExpenseError.MissingCategory -> stringResource(R.string.choose_category)
        AddExpenseError.InvalidAmount -> stringResource(R.string.enter_valid_amount)
        AddExpenseError.InvalidDateTime -> stringResource(R.string.enter_valid_date_time)
        AddExpenseError.SaveFailed -> stringResource(R.string.unable_save_expense)
    }

@Preview(showBackground = true)
@Composable
private fun AddExpenseScreenPreview() {
    MaterialTheme {
        AddExpenseContent(
            uiState = AddExpenseUiState(
                name = "Fresh Market",
                amount = "24.90",
                categoryId = 1,
                paymentMethodId = 7,
                date = "2026-06-24",
                time = "14:30",
                merchant = "Fresh Market",
                description = "SPESA 2026-06-24 14:30",
                notes = "OCR text",
                categories = listOf(Category(1, "SPESA", 0xFF00AA00, "spesa", 0, false)),
                paymentMethods = listOf(PaymentMethod(7, "Card", PaymentMethodType.Card, false)),
                canSave = true,
            ),
            onNameChanged = {},
            onAmountChanged = {},
            onCategorySelected = {},
            onPaymentMethodSelected = {},
            onDateChanged = {},
            onTimeChanged = {},
            onMerchantChanged = {},
            onDescriptionChanged = {},
            onNotesChanged = {},
            onSave = {},
            onBack = {},
        )
    }
}
