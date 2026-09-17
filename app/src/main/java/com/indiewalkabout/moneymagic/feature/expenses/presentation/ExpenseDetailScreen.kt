package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExpenseDetailScreen(
    expenseId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(expenseId) {
        viewModel.load(expenseId)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ExpenseDetailEvent.Saved -> onSaved()
                ExpenseDetailEvent.Deleted -> onDeleted()
            }
        }
    }

    ExpenseDetailContent(
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
        onDeleteRequest = viewModel::requestDeleteConfirmation,
        onDeleteConfirm = viewModel::confirmDelete,
        onDeleteDismiss = viewModel::dismissDeleteConfirmation,
        onMissingFieldsDismiss = viewModel::dismissMissingFieldsDialog,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ExpenseDetailContent(
    uiState: ExpenseDetailUiState,
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
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onMissingFieldsDismiss: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text(stringResource(R.string.remove_expense_title)) },
            text = { Text(stringResource(R.string.remove_expense_message)) },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirm,
                    enabled = !uiState.isDeleting,
                ) {
                    Text(stringResource(R.string.remove_expense))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteDismiss,
                    enabled = !uiState.isDeleting,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (uiState.showMissingFieldsDialog) {
        AlertDialog(
            onDismissRequest = onMissingFieldsDismiss,
            title = { Text(stringResource(R.string.missing_expense_fields_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.missing_expense_fields_message))
                    uiState.missingFieldErrors.forEach { error ->
                        Text("- ${error.label()}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onMissingFieldsDismiss) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!uiState.missingExpense) {
                ExpenseDetailActions(
                    uiState = uiState,
                    onSave = onSave,
                    onDelete = onDeleteRequest,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.expense_detail_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextButton(
                    onClick = onBack,
                    enabled = !uiState.isSaving && !uiState.isDeleting,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }

            if (uiState.missingExpense) {
                Text(
                    text = stringResource(R.string.expense_missing),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                enabled = uiState.isEditable(),
                label = { Text(stringResource(R.string.expense_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = onAmountChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                enabled = uiState.isEditable(),
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
                enabled = uiState.isEditable(),
            )
            PaymentMethodDropdown(
                paymentMethods = uiState.paymentMethods,
                selectedPaymentMethodId = uiState.paymentMethodId,
                onPaymentMethodSelected = onPaymentMethodSelected,
                enabled = uiState.isEditable(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExpenseDateField(
                    value = uiState.date,
                    onValueChange = onDateChanged,
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewOnFocus(),
                    enabled = uiState.isEditable(),
                )
                OutlinedTextField(
                    value = uiState.time,
                    onValueChange = onTimeChanged,
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewOnFocus(),
                    enabled = uiState.isEditable(),
                    label = { Text(stringResource(R.string.time)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            OutlinedTextField(
                value = uiState.merchant,
                onValueChange = onMerchantChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                enabled = uiState.isEditable(),
                label = { Text(stringResource(R.string.merchant)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                enabled = uiState.isEditable(),
                label = { Text(stringResource(R.string.description)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            ExpandableOcrField(
                notes = uiState.notes,
                onNotesChanged = onNotesChanged,
                enabled = uiState.isEditable(),
                fieldModifier = Modifier.bringIntoViewOnFocus(),
                initiallyExpanded = true,
            )
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun Modifier.bringIntoViewOnFocus(): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                scope.launch {
                    bringIntoViewRequester.bringIntoView()
                    delay(650)
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
}

@Composable
private fun ExpenseDetailActions(
    uiState: ExpenseDetailUiState,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding(),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onSave,
                enabled = uiState.isEditable(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (uiState.isSaving) stringResource(R.string.saving) else stringResource(R.string.save))
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = uiState.expenseId != null && !uiState.isSaving && !uiState.isDeleting,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (uiState.isDeleting) stringResource(R.string.deleting) else stringResource(R.string.delete))
            }
        }
    }
}

private fun ExpenseDetailUiState.isEditable(): Boolean =
    !isLoading && !missingExpense && !isSaving && !isDeleting

@Composable
private fun ExpenseDetailError.label(): String =
    when (this) {
        ExpenseDetailError.EnterAmount -> stringResource(R.string.enter_amount)
        ExpenseDetailError.AmountMustBePositive -> stringResource(R.string.amount_must_be_positive)
        ExpenseDetailError.MissingCategory -> stringResource(R.string.choose_category)
        ExpenseDetailError.InvalidAmount -> stringResource(R.string.enter_valid_amount)
        ExpenseDetailError.InvalidDateTime -> stringResource(R.string.enter_valid_date_time)
        ExpenseDetailError.SaveFailed -> stringResource(R.string.unable_save_expense)
        ExpenseDetailError.DeleteFailed -> stringResource(R.string.unable_delete_expense)
    }

@Preview(showBackground = true)
@Composable
private fun ExpenseDetailScreenPreview() {
    MaterialTheme {
        ExpenseDetailContent(
            uiState = ExpenseDetailUiState(
                expenseId = 42,
                isLoading = false,
                name = "Fuel stop",
                amount = "45.67",
                categoryId = 1,
                paymentMethodId = 7,
                date = "2026-06-24",
                time = "14:30",
                merchant = "Q8",
                description = "BENZINA 2026-06-24 14:30",
                notes = "OCR text",
                categories = listOf(Category(1, "BENZINA", 0xFF00AA00, "benzina", 0, false)),
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
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onMissingFieldsDismiss = {},
            onBack = {},
        )
    }
}
