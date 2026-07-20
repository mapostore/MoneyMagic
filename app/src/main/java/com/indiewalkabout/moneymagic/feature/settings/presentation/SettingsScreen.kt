package com.indiewalkabout.moneymagic.feature.settings.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.FileProvider
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType
import com.indiewalkabout.moneymagic.feature.settings.presentation.components.CategoryCard
import com.indiewalkabout.moneymagic.feature.settings.presentation.components.PaymentMethodCard
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ),
    ) { uri ->
        val bytes = pendingExport
        pendingExport = null
        if (uri != null && bytes != null) {
            context.writeBytes(uri, bytes)
        }
    }

    SettingsContent(
        uiState = uiState,
        onExport = {
            pendingExport = viewModel.createExpenseExport()
            exportLauncher.launch("moneymagic-expenses.xlsx")
        },
        onShare = {
            context.shareExpenseExport(viewModel.createExpenseExport())
        },
        onCategoryNameChanged = viewModel::onCategoryNameChanged,
        onSaveCategory = viewModel::saveCategory,
        onClearCategory = viewModel::clearCategoryForm,
        onEditCategory = viewModel::editCategory,
        onDeleteCategory = { category -> viewModel.deleteCategory(category.id) },
        onPaymentMethodNameChanged = viewModel::onPaymentMethodNameChanged,
        onPaymentMethodTypeChanged = viewModel::onPaymentMethodTypeChanged,
        onSavePaymentMethod = viewModel::savePaymentMethod,
        onClearPaymentMethod = viewModel::clearPaymentMethodForm,
        onEditPaymentMethod = viewModel::editPaymentMethod,
        onDeletePaymentMethod = { paymentMethod -> viewModel.deletePaymentMethod(paymentMethod.id) },
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onCategoryNameChanged: (String) -> Unit,
    onSaveCategory: () -> Unit,
    onClearCategory: () -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onPaymentMethodNameChanged: (String) -> Unit,
    onPaymentMethodTypeChanged: (PaymentMethodType) -> Unit,
    onSavePaymentMethod: () -> Unit,
    onClearPaymentMethod: () -> Unit,
    onEditPaymentMethod: (PaymentMethod) -> Unit,
    onDeletePaymentMethod: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            ExportSection(
                uiState = uiState,
                onExport = onExport,
                onShare = onShare,
            )
        }
        item {
            CategorySection(
                uiState = uiState,
                onNameChanged = onCategoryNameChanged,
                onSave = onSaveCategory,
                onClear = onClearCategory,
                onEdit = onEditCategory,
                onDelete = onDeleteCategory,
            )
        }
        item {
            PaymentMethodSection(
                uiState = uiState,
                onNameChanged = onPaymentMethodNameChanged,
                onTypeChanged = onPaymentMethodTypeChanged,
                onSave = onSavePaymentMethod,
                onClear = onClearPaymentMethod,
                onEdit = onEditPaymentMethod,
                onDelete = onDeletePaymentMethod,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsContent(
            uiState = SettingsUiState(
                categories = listOf(Category(1, "BENZINA", 0xFF00AA00, "benzina", 0, false)),
                paymentMethods = listOf(PaymentMethod(7, "Card", PaymentMethodType.Card, false)),
                categoryName = "SPESA",
                paymentMethodName = "Cash",
                paymentMethodType = PaymentMethodType.Cash,
            ),
            onExport = {},
            onShare = {},
            onCategoryNameChanged = {},
            onSaveCategory = {},
            onClearCategory = {},
            onEditCategory = {},
            onDeleteCategory = {},
            onPaymentMethodNameChanged = {},
            onPaymentMethodTypeChanged = {},
            onSavePaymentMethod = {},
            onClearPaymentMethod = {},
            onEditPaymentMethod = {},
            onDeletePaymentMethod = {},
        )
    }
}

@Composable
private fun ExportSection(
    uiState: SettingsUiState,
    onExport: () -> Unit,
    onShare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(R.string.export_data), style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = onExport,
            enabled = uiState.expenses.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.export_expenses_excel))
        }
        OutlinedButton(
            onClick = onShare,
            enabled = uiState.expenses.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.share_expenses_excel))
        }
        if (uiState.expenses.isEmpty()) {
            Text(
                text = stringResource(R.string.export_expenses_excel_empty),
                style = MaterialTheme.typography.bodySmall,
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
private fun PaymentMethodType.label(): String =
    stringResource(
        when (this) {
            PaymentMethodType.Cash -> R.string.cash
            PaymentMethodType.Card -> R.string.card
            PaymentMethodType.Bank -> R.string.bank
            PaymentMethodType.Other -> R.string.other
        },
    )

private fun Context.writeBytes(uri: Uri, bytes: ByteArray) {
    contentResolver.openOutputStream(uri)?.use { output ->
        output.write(bytes)
    }
}

private fun Context.shareExpenseExport(bytes: ByteArray) {
    val exportDir = File(cacheDir, "exports").apply { mkdirs() }
    val exportFile = File(exportDir, "moneymagic-expenses.xlsx").apply {
        writeBytes(bytes)
    }
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", exportFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        startActivity(Intent.createChooser(intent, getString(R.string.share_expenses_excel_title)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.share_expenses_excel_unavailable, Toast.LENGTH_SHORT).show()
    }
}
