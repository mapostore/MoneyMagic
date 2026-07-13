package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidate
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidateField
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType

@Composable
fun ReceiptCaptureScreen(
    onBack: () -> Unit,
    onReviewExpense: (ReceiptDraft) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptCaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::scanUri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = pendingCameraUri?.let(Uri::parse)
        val path = pendingCameraPath
        pendingCameraUri = null
        pendingCameraPath = null
        if (captured && uri != null) {
            viewModel.scanCameraUri(uri, onScanComplete = { deleteReceiptCaptureFile(path) })
        } else if (!captured && path != null) {
            deleteReceiptCaptureFile(path)
        }
    }

    ReceiptCaptureContent(
        uiState = uiState,
        onPickImage = { imageLauncher.launch("image/*") },
        onTakePicture = {
            val target = context.createReceiptCaptureTarget()
            pendingCameraUri = target.uri.toString()
            pendingCameraPath = target.path
            cameraLauncher.launch(target.uri)
        },
        onSelectCandidate = viewModel::selectCandidate,
        onShareRawText = { rawText -> context.shareReceiptOcrText(rawText) },
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
    onSelectCandidate: (ReceiptCandidateField, String) -> Unit,
    onShareRawText: (String) -> Unit,
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
                    onSelectCandidate = onSelectCandidate,
                    onReviewExpense = { onReviewExpense(draft) },
                )
            }
            if (uiState.rawText.isNotBlank()) {
                uiState.diagnostics?.let { diagnostics ->
                    ReceiptScanDiagnosticsCard(
                        diagnostics = diagnostics,
                        rawText = uiState.rawText,
                        onShareRawText = onShareRawText,
                    )
                }
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
private fun ReceiptScanDiagnosticsCard(
    diagnostics: ReceiptScanDiagnostics,
    rawText: String,
    onShareRawText: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.scan_diagnostics),
                style = MaterialTheme.typography.titleSmall,
            )
            DraftLine(label = stringResource(R.string.scan_source), value = diagnostics.source.label())
            DraftLine(
                label = stringResource(R.string.document_type),
                value = diagnostics.documentType.label(),
            )
            DraftLine(
                label = stringResource(R.string.scan_blocks),
                value = diagnostics.blockCount.toString(),
            )
            DraftLine(
                label = stringResource(R.string.scan_lines),
                value = diagnostics.lineCount.toString(),
            )
            DraftLine(
                label = stringResource(R.string.scan_elements),
                value = diagnostics.elementCount.toString(),
            )
            DraftLine(
                label = stringResource(R.string.scan_characters),
                value = diagnostics.characterCount.toString(),
            )
            OutlinedButton(
                onClick = { onShareRawText(rawText) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.share_ocr_text))
            }
        }
    }
}

@Composable
private fun ReceiptDraftCard(
    draft: ReceiptDraft,
    onSelectCandidate: (ReceiptCandidateField, String) -> Unit,
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
            CandidateGroup(
                title = stringResource(R.string.merchant_candidates),
                candidates = draft.candidates.merchants,
                selectedValue = draft.merchant,
                onSelectCandidate = onSelectCandidate,
            )
            CandidateGroup(
                title = stringResource(R.string.amount_candidates),
                candidates = draft.candidates.amounts,
                selectedValue = draft.amount,
                onSelectCandidate = onSelectCandidate,
            )
            CandidateGroup(
                title = stringResource(R.string.date_candidates),
                candidates = draft.candidates.dates,
                selectedValue = draft.date,
                onSelectCandidate = onSelectCandidate,
            )
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
private fun CandidateGroup(
    title: String,
    candidates: List<ReceiptCandidate>,
    selectedValue: String,
    onSelectCandidate: (ReceiptCandidateField, String) -> Unit,
) {
    val visibleCandidates = candidates
        .distinctBy { it.value }
        .filter { it.value.isNotBlank() }
    if (visibleCandidates.size <= 1) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            visibleCandidates.forEach { candidate ->
                OutlinedButton(
                    onClick = { onSelectCandidate(candidate.field, candidate.value) },
                    enabled = candidate.value != selectedValue,
                ) {
                    Text(candidate.value)
                }
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

@Composable
private fun ReceiptScanSource.label(): String =
    when (this) {
        ReceiptScanSource.Image -> stringResource(R.string.scan_source_image)
        ReceiptScanSource.Camera -> stringResource(R.string.scan_source_camera)
    }

@Composable
private fun ReceiptDocumentType.label(): String =
    when (this) {
        ReceiptDocumentType.Receipt -> stringResource(R.string.document_type_receipt)
        ReceiptDocumentType.BillInvoice -> stringResource(R.string.document_type_bill_invoice)
        ReceiptDocumentType.Unknown -> stringResource(R.string.document_type_unknown)
    }

private fun Context.shareReceiptOcrText(rawText: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, rawText)
    runCatching {
        startActivity(Intent.createChooser(intent, getString(R.string.share_ocr_text)))
    }.onFailure { error ->
        if (error !is ActivityNotFoundException) throw error
    }
}
