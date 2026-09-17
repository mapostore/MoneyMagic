package com.indiewalkabout.moneymagic.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType
import com.indiewalkabout.moneymagic.feature.settings.presentation.components.PaymentMethodCard

@Composable
fun PaymentMethodManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentMethodManagementContent(
        uiState = uiState,
        onBack = onBack,
        onNameChanged = viewModel::onPaymentMethodNameChanged,
        onTypeChanged = viewModel::onPaymentMethodTypeChanged,
        onSave = viewModel::savePaymentMethod,
        onClear = viewModel::clearPaymentMethodForm,
        onEdit = viewModel::editPaymentMethod,
        onDelete = { paymentMethod -> viewModel.deletePaymentMethod(paymentMethod.id) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodManagementContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (PaymentMethodType) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onEdit: (PaymentMethod) -> Unit,
    onDelete: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payment_methods)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.paymentMethodName,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.payment_method_name)) },
                    singleLine = true,
                )
            }
            item {
                PaymentMethodTypeDropdown(uiState.paymentMethodType, onTypeChanged)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, enabled = uiState.paymentMethodName.isNotBlank()) {
                        Text(stringResource(if (uiState.editingPaymentMethodId == null) R.string.add else R.string.update))
                    }
                    OutlinedButton(onClick = onClear) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }
            items(uiState.paymentMethods, key = { it.id }) { paymentMethod ->
                PaymentMethodCard(paymentMethod, onEdit, onDelete)
            }
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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
private fun PaymentMethodType.label(): String = stringResource(
    when (this) {
        PaymentMethodType.Cash -> R.string.cash
        PaymentMethodType.Card -> R.string.card
        PaymentMethodType.Bank -> R.string.bank
        PaymentMethodType.Other -> R.string.other
    },
)

@Preview(showBackground = true)
@Composable
private fun PaymentMethodManagementScreenPreview() {
    MaterialTheme {
        PaymentMethodManagementContent(
            uiState = SettingsUiState(
                paymentMethods = listOf(PaymentMethod(7, "Card", PaymentMethodType.Card, false)),
                paymentMethodName = "Cash",
                paymentMethodType = PaymentMethodType.Cash,
            ),
            onBack = {},
            onNameChanged = {},
            onTypeChanged = {},
            onSave = {},
            onClear = {},
            onEdit = {},
            onDelete = {},
        )
    }
}
