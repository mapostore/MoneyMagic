package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidate
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidateField
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidates
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
import com.indiewalkabout.moneymagic.feature.capture.presentation.components.ReceiptDraftCard
import com.indiewalkabout.moneymagic.feature.capture.presentation.components.ReceiptScanDiagnosticsCard
import com.indiewalkabout.moneymagic.feature.capture.presentation.components.RecognizedTextCard

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
                RecognizedTextCard(rawText = uiState.rawText)
            }
        }
    }
}

@Composable
private fun ReceiptCaptureError.label(): String =
    when (this) {
        ReceiptCaptureError.ScanFailed -> stringResource(R.string.receipt_scan_failed)
        ReceiptCaptureError.NoTextFound -> stringResource(R.string.receipt_no_text_found)
    }

@Preview(showBackground = true)
@Composable
private fun ReceiptCaptureScreenPreview() {
    MaterialTheme {
        ReceiptCaptureContent(
            uiState = ReceiptCaptureUiState(
                draft = ReceiptDraft(
                    merchant = "Fresh Market",
                    amount = "24.90",
                    date = "2026-06-24",
                    rawText = "Fresh Market\nTOTAL 24.90",
                    candidates = ReceiptCandidates(
                        merchants = listOf(
                            ReceiptCandidate(
                                ReceiptCandidateField.Merchant,
                                "Fresh Market",
                                "merchant",
                                ReceiptFieldConfidence.High,
                            ),
                        ),
                        amounts = listOf(
                            ReceiptCandidate(ReceiptCandidateField.Amount, "24.90", "total", ReceiptFieldConfidence.High),
                            ReceiptCandidate(ReceiptCandidateField.Amount, "4.90", "line", ReceiptFieldConfidence.Low),
                        ),
                        dates = listOf(
                            ReceiptCandidate(ReceiptCandidateField.Date, "2026-06-24", "date", ReceiptFieldConfidence.High),
                        ),
                    ),
                ),
                rawText = "Fresh Market\nTOTAL 24.90",
                diagnostics = ReceiptScanDiagnostics(
                    source = ReceiptScanSource.Image,
                    documentType = ReceiptDocumentType.Receipt,
                    lineCount = 8,
                    blockCount = 2,
                    elementCount = 24,
                    characterCount = 180,
                ),
            ),
            onPickImage = {},
            onTakePicture = {},
            onSelectCandidate = { _, _ -> },
            onShareRawText = {},
            onReviewExpense = {},
            onBack = {},
        )
    }
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
