package com.indiewalkabout.moneymagic.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            CategorySection(
                uiState = uiState,
                onNameChanged = viewModel::onCategoryNameChanged,
                onSave = viewModel::saveCategory,
                onClear = viewModel::clearCategoryForm,
                onEdit = viewModel::editCategory,
                onDelete = { category -> viewModel.deleteCategory(category.id) },
            )
        }
        item {
            PaymentMethodSection(
                uiState = uiState,
                onNameChanged = viewModel::onPaymentMethodNameChanged,
                onTypeChanged = viewModel::onPaymentMethodTypeChanged,
                onSave = viewModel::savePaymentMethod,
                onClear = viewModel::clearPaymentMethodForm,
                onEdit = viewModel::editPaymentMethod,
                onDelete = { paymentMethod -> viewModel.deletePaymentMethod(paymentMethod.id) },
            )
        }
    }
}

@Composable
private fun CategorySection(
    uiState: SettingsUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(R.string.categories), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.categoryName,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.category_name)) },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = uiState.categoryName.isNotBlank()) {
                Text(stringResource(if (uiState.editingCategoryId == null) R.string.add else R.string.update))
            }
            OutlinedButton(onClick = onClear) {
                Text(stringResource(R.string.clear))
            }
        }
        uiState.categories.forEach { category ->
            CategoryCard(category = category, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(onClick = { onEdit(category) }) {
                Text(stringResource(R.string.edit))
            }
            OutlinedButton(onClick = { onDelete(category) }) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun PaymentMethodSection(
    uiState: SettingsUiState,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (PaymentMethodType) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onEdit: (PaymentMethod) -> Unit,
    onDelete: (PaymentMethod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(R.string.payment_methods), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.paymentMethodName,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.payment_method_name)) },
            singleLine = true,
        )
        PaymentMethodTypeDropdown(
            selectedType = uiState.paymentMethodType,
            onTypeChanged = onTypeChanged,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = uiState.paymentMethodName.isNotBlank()) {
                Text(stringResource(if (uiState.editingPaymentMethodId == null) R.string.add else R.string.update))
            }
            OutlinedButton(onClick = onClear) {
                Text(stringResource(R.string.clear))
            }
        }
        uiState.paymentMethods.forEach { paymentMethod ->
            PaymentMethodCard(paymentMethod = paymentMethod, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodTypeDropdown(
    selectedType: PaymentMethodType,
    onTypeChanged: (PaymentMethodType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedType.label(),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(stringResource(R.string.payment_method_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PaymentMethodType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label()) },
                    onClick = {
                        onTypeChanged(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    paymentMethod: PaymentMethod,
    onEdit: (PaymentMethod) -> Unit,
    onDelete: (PaymentMethod) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = paymentMethod.name, style = MaterialTheme.typography.titleSmall)
                Text(text = paymentMethod.type.label(), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { onEdit(paymentMethod) }) {
                Text(stringResource(R.string.edit))
            }
            OutlinedButton(onClick = { onDelete(paymentMethod) }) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun PaymentMethodType.label(): String =
    stringResource(
        when (this) {
            PaymentMethodType.Cash -> R.string.cash
            PaymentMethodType.Card -> R.string.card
            PaymentMethodType.Bank -> R.string.bank
            PaymentMethodType.Other -> R.string.other
        },
    )
