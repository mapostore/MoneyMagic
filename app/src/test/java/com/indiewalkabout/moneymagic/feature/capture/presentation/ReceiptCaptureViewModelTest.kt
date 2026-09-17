package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.graphics.Bitmap
import android.net.Uri
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidateField
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrBlock
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrLine
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrResult
import com.indiewalkabout.moneymagic.feature.capture.domain.repository.ReceiptTextRecognizer
import com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ClassifyReceiptDocumentUseCase
import com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReceiptCaptureViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankOcrTextShowsNoTextErrorWithoutDraft() = runTest {
        val viewModel = ReceiptCaptureViewModel(FakeRecognizer(Result.success(ReceiptOcrResult(rawText = ""))), ParseReceiptTextUseCase())

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.rawText)
        assertNull(viewModel.uiState.value.draft)
        assertEquals(ReceiptCaptureError.NoTextFound, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun nonblankWeakOcrTextPreservesRawTextAndDraft() = runTest {
        val viewModel = ReceiptCaptureViewModel(
            FakeRecognizer(Result.success(ReceiptOcrResult(rawText = "unstructured words only"))),
            ParseReceiptTextUseCase(),
        )

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals("unstructured words only", viewModel.uiState.value.rawText)
        assertNotNull(viewModel.uiState.value.draft)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun successfulImageScanExposesDiagnostics() = runTest {
        val ocrText = """
            MARKET ONE
            Total 12.90
            Date 2026-07-13
        """.trimIndent()
        val viewModel = ReceiptCaptureViewModel(
            FakeRecognizer(
                Result.success(
                    ReceiptOcrResult(
                        rawText = ocrText,
                        blocks = listOf(
                            ReceiptOcrBlock(
                                text = ocrText,
                                lines = listOf(
                                    ReceiptOcrLine(text = "MARKET ONE", elements = listOf("MARKET", "ONE")),
                                    ReceiptOcrLine(text = "Total 12.90", elements = listOf("Total", "12.90")),
                                    ReceiptOcrLine(text = "Date 2026-07-13", elements = listOf("Date", "2026-07-13")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            ParseReceiptTextUseCase(),
        )

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals(ReceiptScanSource.Image, viewModel.uiState.value.diagnostics?.source)
        assertEquals(3, viewModel.uiState.value.diagnostics?.lineCount)
        assertEquals(1, viewModel.uiState.value.diagnostics?.blockCount)
        assertEquals(6, viewModel.uiState.value.diagnostics?.elementCount)
        assertEquals(ocrText.length, viewModel.uiState.value.diagnostics?.characterCount)
    }

    @Test
    fun successfulCameraScanExposesCameraDiagnostics() = runTest {
        val viewModel = ReceiptCaptureViewModel(
            FakeRecognizer(
                Result.success(
                    ReceiptOcrResult(
                        rawText = "Camera Market\nTotal 5.00",
                        blocks = listOf(
                            ReceiptOcrBlock(
                                text = "Camera Market\nTotal 5.00",
                                lines = listOf(
                                    ReceiptOcrLine(text = "Camera Market", elements = listOf("Camera", "Market")),
                                    ReceiptOcrLine(text = "Total 5.00", elements = listOf("Total", "5.00")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            ParseReceiptTextUseCase(),
        )

        viewModel.scanCameraUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals(ReceiptScanSource.Camera, viewModel.uiState.value.diagnostics?.source)
        assertEquals(2, viewModel.uiState.value.diagnostics?.lineCount)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun cameraScanRunsCompletionCallbackOnSuccess() = runTest {
        var completionCount = 0
        val viewModel = ReceiptCaptureViewModel(
            FakeRecognizer(Result.success(ReceiptOcrResult(rawText = "Camera Market\nTotal 5.00"))),
            ParseReceiptTextUseCase(),
        )

        viewModel.scanCameraUri(Uri.EMPTY, onScanComplete = { completionCount++ })
        advanceUntilIdle()

        assertEquals(1, completionCount)
    }

    @Test
    fun scanKeepsStructuredOcrResultForLaterReview() = runTest {
        val ocrResult = ReceiptOcrResult(
            rawText = "Structured Shop\nTotal 8.50",
            blocks = listOf(
                ReceiptOcrBlock(
                    text = "Structured Shop\nTotal 8.50",
                    lines = listOf(
                        ReceiptOcrLine(text = "Structured Shop", elements = listOf("Structured", "Shop")),
                        ReceiptOcrLine(text = "Total 8.50", elements = listOf("Total", "8.50")),
                    ),
                ),
            ),
        )
        val viewModel = ReceiptCaptureViewModel(FakeRecognizer(Result.success(ocrResult)), ParseReceiptTextUseCase())

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals(ocrResult, viewModel.uiState.value.ocrResult)
        assertEquals("Structured Shop\nTotal 8.50", viewModel.uiState.value.rawText)
    }

    @Test
    fun scanClassifiesBillInvoiceDocumentType() = runTest {
        val viewModel = ReceiptCaptureViewModel(
            recognizer = FakeRecognizer(
                Result.success(
                    ReceiptOcrResult(
                        rawText = """
                            Bolletta Energia
                            Importo da pagare 124,65
                            Scadenza 31/07/2026
                        """.trimIndent(),
                    ),
                ),
            ),
            parseReceiptText = ParseReceiptTextUseCase()::invoke,
            classifyDocument = ClassifyReceiptDocumentUseCase()::invoke,
        )

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals(ReceiptDocumentType.BillInvoice, viewModel.uiState.value.documentType)
        assertEquals(ReceiptDocumentType.BillInvoice, viewModel.uiState.value.diagnostics?.documentType)
    }

    @Test
    fun billInvoiceScanUsesBillParser() = runTest {
        val billDraft = ReceiptDraft(
            name = "Bill parser issuer",
            merchant = "Bill parser issuer",
            amount = "44.20",
            date = "2026-07-31",
            rawText = "bill parser raw",
        )
        val viewModel = ReceiptCaptureViewModel(
            recognizer = FakeRecognizer(
                Result.success(
                    ReceiptOcrResult(
                        rawText = "Amount due 44.20\nDue date 31/07/2026",
                    ),
                ),
            ),
            parseReceiptText = { ReceiptDraft(name = "receipt parser", merchant = "receipt parser") },
            parseBillInvoiceText = { billDraft },
            classifyDocument = { ReceiptDocumentType.BillInvoice },
        )

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals(billDraft, viewModel.uiState.value.draft)
    }

    @Test
    fun selectingCandidateUpdatesDraftField() = runTest {
        val viewModel = ReceiptCaptureViewModel(
            recognizer = FakeRecognizer(
                Result.success(
                    ReceiptOcrResult(
                        rawText = """
                            Shop One
                            Total 10.00
                            Cash paid 20.00
                        """.trimIndent(),
                    ),
                ),
            ),
            parseReceiptText = ParseReceiptTextUseCase(),
        )

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        viewModel.selectCandidate(ReceiptCandidateField.Amount, "20.00")

        assertEquals("20.00", viewModel.uiState.value.draft?.amount)
    }

    @Test
    fun parserExceptionPreservesRawTextInDraftWithoutError() = runTest {
        val ocrText = "receipt text that trips parser"
        val viewModel = ReceiptCaptureViewModel(
            recognizer = FakeRecognizer(Result.success(ReceiptOcrResult(rawText = ocrText))),
            parseReceiptText = { throw IllegalArgumentException("parse failed") },
        )

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals(ocrText, viewModel.uiState.value.rawText)
        assertEquals(ocrText, viewModel.uiState.value.draft?.rawText)
        assertNotNull(viewModel.uiState.value.draft)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun failedScanClearsPreviousDiagnostics() = runTest {
        val viewModel = ReceiptCaptureViewModel(
            QueueRecognizer(
                listOf(
                    Result.success(ReceiptOcrResult(rawText = "Market\nTotal 10.00")),
                    Result.failure(IllegalStateException("camera failed")),
                ),
            ),
            ParseReceiptTextUseCase(),
        )

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.diagnostics)

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.diagnostics)
        assertEquals("", viewModel.uiState.value.rawText)
        assertEquals(ReceiptCaptureError.ScanFailed, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun cameraScanRunsCompletionCallbackOnFailure() = runTest {
        var completionCount = 0
        val viewModel = ReceiptCaptureViewModel(
            FakeRecognizer(Result.failure(IllegalStateException("camera failed"))),
            ParseReceiptTextUseCase(),
        )

        viewModel.scanCameraUri(Uri.EMPTY, onScanComplete = { completionCount++ })
        advanceUntilIdle()

        assertEquals(1, completionCount)
        assertEquals(ReceiptCaptureError.ScanFailed, viewModel.uiState.value.errorMessage)
    }

    private class FakeRecognizer(
        private val result: Result<ReceiptOcrResult>,
    ) : ReceiptTextRecognizer {
        override suspend fun recognize(uri: Uri): Result<ReceiptOcrResult> = result
        override suspend fun recognize(bitmap: Bitmap): Result<ReceiptOcrResult> = result
    }

    private class QueueRecognizer(results: List<Result<ReceiptOcrResult>>) : ReceiptTextRecognizer {
        private val pendingResults = ArrayDeque(results)

        override suspend fun recognize(uri: Uri): Result<ReceiptOcrResult> = pendingResults.removeFirst()
        override suspend fun recognize(bitmap: Bitmap): Result<ReceiptOcrResult> = pendingResults.removeFirst()
    }
}
