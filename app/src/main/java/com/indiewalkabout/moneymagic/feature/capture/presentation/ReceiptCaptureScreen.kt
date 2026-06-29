package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft

@Composable
fun ReceiptCaptureScreen(
    onBack: () -> Unit,
    onReviewExpense: (ReceiptDraft) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptCaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::scanUri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let(viewModel::scanBitmap)
    }

    ReceiptCaptureContent(
        uiState = uiState,
        onPickImage = { imageLauncher.launch("image/*") },
        onTakePicture = { cameraLauncher.launch(null) },
        onReviewExpense = onReviewExpense,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ReceiptCaptureContent(
    uiState: ReceiptCaptureUiState,
    onPickImage: () -> Unit,
    onTakePicture: () -> Unit,
    onReviewExpense: (ReceiptDraft) -> Unit,
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
                    text = stringResource(R.string.receipt_capture_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextButton(onClick = onBack, enabled = !uiState.isScanning) {
                    Text(stringResource(R.string.cancel))
                }
            }
            Button(
                onClick = onPickImage,
                enabled = !uiState.isScanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.select_receipt_image))
            }
            OutlinedButton(
                onClick = onTakePicture,
                enabled = !uiState.isScanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scan_with_camera))
            }
            if (uiState.isScanning) {
                Text(
                    text = stringResource(R.string.scanning_receipt),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            uiState.errorMessage?.let { error ->
                Text(
                    text = error.label(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            uiState.draft?.let { draft ->
                ReceiptDraftCard(
                    draft = draft,
                    onReviewExpense = { onReviewExpense(draft) },
                )
            }
            if (uiState.rawText.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.recognized_text),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(text = uiState.rawText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptDraftCard(
    draft: ReceiptDraft,
    onReviewExpense: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.receipt_draft),
                style = MaterialTheme.typography.titleMedium,
            )
            DraftLine(label = stringResource(R.string.merchant), value = draft.merchant)
            DraftLine(label = stringResource(R.string.amount), value = draft.amount)
            DraftLine(label = stringResource(R.string.date), value = draft.date)
            Button(
                onClick = onReviewExpense,
                enabled = draft.amount.isNotBlank() || draft.merchant.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.review_expense))
            }
        }
    }
}

@Composable
private fun DraftLine(label: String, value: String) {
    if (value.isNotBlank()) {
        Text(text = "$label: $value", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReceiptCaptureError.label(): String =
    when (this) {
        ReceiptCaptureError.ScanFailed -> stringResource(R.string.receipt_scan_failed)
        ReceiptCaptureError.NoTextFound -> stringResource(R.string.receipt_no_text_found)
    }
