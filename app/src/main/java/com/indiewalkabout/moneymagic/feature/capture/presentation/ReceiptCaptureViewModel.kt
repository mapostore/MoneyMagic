package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidateField
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrResult
import com.indiewalkabout.moneymagic.feature.capture.domain.repository.ReceiptTextRecognizer
import com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ClassifyReceiptDocumentUseCase
import com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseBillInvoiceTextUseCase
import com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptCaptureUiState(
    val isScanning: Boolean = false,
    val draft: ReceiptDraft? = null,
    val rawText: String = "",
    val ocrResult: ReceiptOcrResult? = null,
    val documentType: ReceiptDocumentType = ReceiptDocumentType.Unknown,
    val diagnostics: ReceiptScanDiagnostics? = null,
    val errorMessage: ReceiptCaptureError? = null,
)

enum class ReceiptCaptureError {
    ScanFailed,
    NoTextFound,
}

@HiltViewModel
class ReceiptCaptureViewModel : ViewModel {
    private val recognizer: ReceiptTextRecognizer
    private val parseReceiptText: (String) -> ReceiptDraft
    private val parseBillInvoiceText: (String) -> ReceiptDraft
    private val classifyDocument: (ReceiptOcrResult) -> ReceiptDocumentType

    @Inject
    constructor(
        recognizer: ReceiptTextRecognizer,
        parseReceiptText: ParseReceiptTextUseCase,
        parseBillInvoiceText: ParseBillInvoiceTextUseCase,
        classifyDocument: ClassifyReceiptDocumentUseCase,
    ) : this(
        recognizer = recognizer,
        parseReceiptText = parseReceiptText::invoke,
        parseBillInvoiceText = parseBillInvoiceText::invoke,
        classifyDocument = classifyDocument::invoke,
    )

    internal constructor(
        recognizer: ReceiptTextRecognizer,
        parseReceiptText: ParseReceiptTextUseCase,
    ) : this(
        recognizer = recognizer,
        parseReceiptText = parseReceiptText::invoke,
        parseBillInvoiceText = parseReceiptText::invoke,
    )

    internal constructor(
        recognizer: ReceiptTextRecognizer,
        parseReceiptText: (String) -> ReceiptDraft,
        parseBillInvoiceText: (String) -> ReceiptDraft = parseReceiptText,
        classifyDocument: (ReceiptOcrResult) -> ReceiptDocumentType = { ReceiptDocumentType.Unknown },
    ) {
        this.recognizer = recognizer
        this.parseReceiptText = parseReceiptText
        this.parseBillInvoiceText = parseBillInvoiceText
        this.classifyDocument = classifyDocument
    }

    private val _uiState = MutableStateFlow(ReceiptCaptureUiState())
    val uiState: StateFlow<ReceiptCaptureUiState> = _uiState.asStateFlow()

    fun scanUri(uri: Uri) {
        scan(source = ReceiptScanSource.Image, onScanComplete = {}) { recognizer.recognize(uri) }
    }

    fun scanCameraUri(uri: Uri, onScanComplete: () -> Unit = {}) {
        scan(source = ReceiptScanSource.Camera, onScanComplete = onScanComplete) { recognizer.recognize(uri) }
    }

    fun scanBitmap(bitmap: Bitmap) {
        scan(source = ReceiptScanSource.Camera, onScanComplete = {}) { recognizer.recognize(bitmap) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun selectCandidate(field: ReceiptCandidateField, value: String) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            state.copy(
                draft = when (field) {
                    ReceiptCandidateField.Merchant -> draft.copy(name = value, merchant = value)
                    ReceiptCandidateField.Amount -> draft.copy(amount = value)
                    ReceiptCandidateField.Date -> draft.copy(date = value)
                },
            )
        }
    }

    private fun scan(
        source: ReceiptScanSource,
        onScanComplete: () -> Unit,
        recognize: suspend () -> Result<ReceiptOcrResult>,
    ) {
        if (uiState.value.isScanning) {
            return
        }
        _uiState.update {
            it.copy(
                isScanning = true,
                draft = null,
                rawText = "",
                ocrResult = null,
                documentType = ReceiptDocumentType.Unknown,
                diagnostics = null,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            recognize()
                .onSuccess { ocrResult ->
                    val text = ocrResult.rawText
                    val documentType = ocrResult
                        .takeIf { parsed -> parsed.rawText.isNotBlank() || parsed.blocks.isNotEmpty() }
                        ?.let(classifyDocument)
                        ?: ReceiptDocumentType.Unknown
                    val draft = try {
                        when (documentType) {
                            ReceiptDocumentType.BillInvoice -> parseBillInvoiceText(text)
                            ReceiptDocumentType.Receipt,
                            ReceiptDocumentType.Unknown,
                            -> parseReceiptText(text)
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        ReceiptDraft(rawText = text)
                    }
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            draft = draft.takeIf { parsed -> parsed.rawText.isNotBlank() },
                            rawText = text,
                            ocrResult = ocrResult.takeIf { parsed -> parsed.rawText.isNotBlank() },
                            documentType = documentType,
                            diagnostics = ocrResult.takeIf { parsed -> parsed.rawText.isNotBlank() }?.toDiagnostics(
                                source = source,
                                documentType = documentType,
                            ),
                            errorMessage = if (text.isBlank()) ReceiptCaptureError.NoTextFound else null,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            draft = null,
                            rawText = "",
                            ocrResult = null,
                            documentType = ReceiptDocumentType.Unknown,
                            diagnostics = null,
                            errorMessage = ReceiptCaptureError.ScanFailed,
                        )
                    }
                }
                .also {
                    onScanComplete()
                }
        }
    }
}

private fun ReceiptOcrResult.toDiagnostics(
    source: ReceiptScanSource,
    documentType: ReceiptDocumentType,
): ReceiptScanDiagnostics =
    ReceiptScanDiagnostics(
        source = source,
        documentType = documentType,
        lineCount = structuredLineCount.takeIf { it > 0 } ?: rawText.lineSequence().map(String::trim).count(String::isNotBlank),
        blockCount = blocks.size,
        elementCount = blocks.sumOf { block -> block.lines.sumOf { line -> line.elements.size } },
        characterCount = rawText.length,
    )

private val ReceiptOcrResult.structuredLineCount: Int
    get() = blocks.sumOf { block -> block.lines.count { line -> line.text.isNotBlank() } }
