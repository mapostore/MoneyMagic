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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import java.io.File

@Composable
fun SettingsScreen(
    onCategoriesClick: () -> Unit,
    onPaymentMethodsClick: () -> Unit,
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
        if (uri != null && bytes != null) context.writeBytes(uri, bytes)
    }

    SettingsContent(
        uiState = uiState,
        onCategoriesClick = onCategoriesClick,
        onPaymentMethodsClick = onPaymentMethodsClick,
        onExport = {
            pendingExport = viewModel.createExpenseExport()
            exportLauncher.launch("moneymagic-expenses.xlsx")
        },
        onShare = { context.shareExpenseExport(viewModel.createExpenseExport()) },
        onShowDeleteAllConfirmation = viewModel::showDeleteAllConfirmation,
        onDeleteAllConfirmationChanged = viewModel::onDeleteAllConfirmationChanged,
        onDismissDeleteAllConfirmation = viewModel::dismissDeleteAllConfirmation,
        onDeleteAllExpenses = viewModel::deleteAllExpenses,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onCategoriesClick: () -> Unit,
    onPaymentMethodsClick: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onShowDeleteAllConfirmation: () -> Unit,
    onDeleteAllConfirmationChanged: (String) -> Unit,
    onDismissDeleteAllConfirmation: () -> Unit,
    onDeleteAllExpenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            ManagementSection(onCategoriesClick, onPaymentMethodsClick)
        }
        item {
            ExportSection(uiState, onExport, onShare)
        }
        item {
            DeleteHistorySection(
                hasExpenses = uiState.expenses.isNotEmpty(),
                onDeleteClick = onShowDeleteAllConfirmation,
            )
        }
    }

    if (uiState.showDeleteAllConfirmation) {
        DeleteAllExpensesDialog(
            confirmationText = uiState.deleteAllConfirmationText,
            canDelete = uiState.canDeleteAllExpenses,
            isDeleting = uiState.isDeletingAllExpenses,
            deleteFailed = uiState.deleteAllExpensesFailed,
            onConfirmationChanged = onDeleteAllConfirmationChanged,
            onDismiss = onDismissDeleteAllConfirmation,
            onConfirm = onDeleteAllExpenses,
        )
    }
}

@Composable
private fun ManagementSection(
    onCategoriesClick: () -> Unit,
    onPaymentMethodsClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.manage_spending_data), style = MaterialTheme.typography.titleMedium)
        SettingsNavigationCard(stringResource(R.string.categories), onCategoriesClick)
        SettingsNavigationCard(stringResource(R.string.payment_methods), onPaymentMethodsClick)
    }
}

@Composable
private fun SettingsNavigationCard(title: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            },
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
        Text(stringResource(R.string.export_data), style = MaterialTheme.typography.titleMedium)
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
            Text(stringResource(R.string.export_expenses_excel_empty), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DeleteHistorySection(hasExpenses: Boolean, onDeleteClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.expense_history_controls), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = onDeleteClick,
            enabled = hasExpenses,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.delete_all_expenses))
        }
    }
}

@Composable
private fun DeleteAllExpensesDialog(
    confirmationText: String,
    canDelete: Boolean,
    isDeleting: Boolean,
    deleteFailed: Boolean,
    onConfirmationChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(stringResource(R.string.delete_all_expenses_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.delete_all_expenses_message))
                Text(stringResource(R.string.type_yes_to_confirm))
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = onConfirmationChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.confirmation)) },
                    singleLine = true,
                    enabled = !isDeleting,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                )
                if (deleteFailed) {
                    Text(
                        text = stringResource(R.string.delete_all_expenses_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(if (isDeleting) R.string.deleting else R.string.delete_all_expenses))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsContent(
            uiState = SettingsUiState(),
            onCategoriesClick = {},
            onPaymentMethodsClick = {},
            onExport = {},
            onShare = {},
            onShowDeleteAllConfirmation = {},
            onDeleteAllConfirmationChanged = {},
            onDismissDeleteAllConfirmation = {},
            onDeleteAllExpenses = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteAllExpensesDialogPreview() {
    MaterialTheme {
        DeleteAllExpensesDialog(
            confirmationText = "yes",
            canDelete = true,
            isDeleting = false,
            deleteFailed = false,
            onConfirmationChanged = {},
            onDismiss = {},
            onConfirm = {},
        )
    }
}

private fun Context.writeBytes(uri: Uri, bytes: ByteArray) {
    contentResolver.openOutputStream(uri)?.use { output -> output.write(bytes) }
}

private fun Context.shareExpenseExport(bytes: ByteArray) {
    val exportDir = File(cacheDir, "exports").apply { mkdirs() }
    val exportFile = File(exportDir, "moneymagic-expenses.xlsx").apply { writeBytes(bytes) }
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
