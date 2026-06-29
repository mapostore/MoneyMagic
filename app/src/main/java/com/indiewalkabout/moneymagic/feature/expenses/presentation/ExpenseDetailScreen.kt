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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod

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
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onDeleted()
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
        onNotesChanged = viewModel::onNotesChanged,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
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
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
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
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isEditable(),
                label = { Text(stringResource(R.string.expense_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
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
                OutlinedTextField(
                    value = uiState.date,
                    onValueChange = onDateChanged,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.isEditable(),
                    label = { Text(stringResource(R.string.date)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = uiState.time,
                    onValueChange = onTimeChanged,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.isEditable(),
                    label = { Text(stringResource(R.string.time)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            OutlinedTextField(
                value = uiState.merchant,
                onValueChange = onMerchantChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isEditable(),
                label = { Text(stringResource(R.string.merchant)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isEditable(),
                label = { Text(stringResource(R.string.notes)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) stringResource(R.string.saving) else stringResource(R.string.save))
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = uiState.expenseId != null && !uiState.isSaving && !uiState.isDeleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isDeleting) stringResource(R.string.deleting) else stringResource(R.string.delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selectedCategory?.name.orEmpty(),
            onValueChange = {},
            modifier = Modifier
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
private fun PaymentMethodDropdown(
    paymentMethods: List<PaymentMethod>,
    selectedPaymentMethodId: Long?,
    onPaymentMethodSelected: (Long?) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPaymentMethod = paymentMethods.firstOrNull { it.id == selectedPaymentMethodId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selectedPaymentMethod?.name.orEmpty(),
            onValueChange = {},
            modifier = Modifier
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
