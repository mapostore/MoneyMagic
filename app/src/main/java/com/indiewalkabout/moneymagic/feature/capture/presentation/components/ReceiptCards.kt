package com.indiewalkabout.moneymagic.feature.capture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidate
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidateField
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidates
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
import com.indiewalkabout.moneymagic.feature.capture.presentation.ReceiptScanDiagnostics
import com.indiewalkabout.moneymagic.feature.capture.presentation.ReceiptScanSource

@Composable
fun ReceiptScanDiagnosticsCard(
    diagnostics: ReceiptScanDiagnostics,
    rawText: String,
    onShareRawText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.scan_diagnostics),
                style = MaterialTheme.typography.titleSmall,
            )
            DraftLine(label = stringResource(R.string.scan_source), value = diagnostics.source.label())
            DraftLine(label = stringResource(R.string.document_type), value = diagnostics.documentType.label())
            DraftLine(label = stringResource(R.string.scan_blocks), value = diagnostics.blockCount.toString())
            DraftLine(label = stringResource(R.string.scan_lines), value = diagnostics.lineCount.toString())
            DraftLine(label = stringResource(R.string.scan_elements), value = diagnostics.elementCount.toString())
            DraftLine(label = stringResource(R.string.scan_characters), value = diagnostics.characterCount.toString())
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
fun ReceiptDraftCard(
    draft: ReceiptDraft,
    onSelectCandidate: (ReceiptCandidateField, String) -> Unit,
    onReviewExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = stringResource(R.string.receipt_draft), style = MaterialTheme.typography.titleMedium)
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
fun RecognizedTextCard(rawText: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = stringResource(R.string.recognized_text), style = MaterialTheme.typography.titleSmall)
            Text(text = rawText, style = MaterialTheme.typography.bodySmall)
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
    val visibleCandidates = candidates.distinctBy { it.value }.filter { it.value.isNotBlank() }
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

@Preview(showBackground = true)
@Composable
private fun ReceiptCardsPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReceiptDraftCard(
                draft = ReceiptDraft(
                    merchant = "Fresh Market",
                    amount = "24.90",
                    date = "2026-06-24",
                    rawText = "Fresh Market\nTOTAL 24.90",
                    candidates = ReceiptCandidates(
                        merchants = listOf(
                            ReceiptCandidate(
                                field = ReceiptCandidateField.Merchant,
                                value = "Fresh Market",
                                label = "merchant line",
                                confidence = ReceiptFieldConfidence.High,
                            ),
                        ),
                        amounts = listOf(
                            ReceiptCandidate(ReceiptCandidateField.Amount, "24.90", "total", ReceiptFieldConfidence.High),
                            ReceiptCandidate(ReceiptCandidateField.Amount, "4.90", "line item", ReceiptFieldConfidence.Low),
                        ),
                        dates = listOf(
                            ReceiptCandidate(ReceiptCandidateField.Date, "2026-06-24", "receipt date", ReceiptFieldConfidence.High),
                        ),
                    ),
                ),
                onSelectCandidate = { _, _ -> },
                onReviewExpense = {},
            )
            ReceiptScanDiagnosticsCard(
                diagnostics = ReceiptScanDiagnostics(
                    source = ReceiptScanSource.Image,
                    documentType = ReceiptDocumentType.Receipt,
                    lineCount = 8,
                    blockCount = 2,
                    elementCount = 24,
                    characterCount = 180,
                ),
                rawText = "Fresh Market\nTOTAL 24.90",
                onShareRawText = {},
            )
        }
    }
}
