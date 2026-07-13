package com.indiewalkabout.moneymagic.feature.capture.domain.usecase

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrBlock
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrLine
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifyReceiptDocumentUseCaseTest {
    private val classifyDocument = ClassifyReceiptDocumentUseCase()

    @Test
    fun classifiesRetailReceiptFromCommercialDocumentSignals() {
        val result = classifyDocument(
            ReceiptOcrResult(
                rawText = """
                    DOCUMENTO COMMERCIALE
                    CONAD CITY
                    Data 13/07/2026
                    Totale complessivo 18,40
                    Pagamento elettronico
                """.trimIndent(),
            ),
        )

        assertEquals(ReceiptDocumentType.Receipt, result)
    }

    @Test
    fun classifiesItalianBillFromDueAmountAndDueDateSignals() {
        val result = classifyDocument(
            ReceiptOcrResult(
                rawText = """
                    Bolletta Energia
                    Importo da pagare 124,65
                    Scadenza 31/07/2026
                    Codice bollettino 123456
                """.trimIndent(),
            ),
        )

        assertEquals(ReceiptDocumentType.BillInvoice, result)
    }

    @Test
    fun classifiesEnglishInvoiceFromInvoiceAndAmountDueSignals() {
        val result = classifyDocument(
            ReceiptOcrResult(
                rawText = """
                    INVOICE
                    Statement date July 1, 2026
                    Amount due EUR 87.20
                    Due date July 31, 2026
                """.trimIndent(),
            ),
        )

        assertEquals(ReceiptDocumentType.BillInvoice, result)
    }

    @Test
    fun returnsUnknownForSparseUnclearText() {
        val result = classifyDocument(ReceiptOcrResult(rawText = "hello reference 12345"))

        assertEquals(ReceiptDocumentType.Unknown, result)
    }

    @Test
    fun usesStructuredLinesWhenRawTextIsSparse() {
        val result = classifyDocument(
            ReceiptOcrResult(
                rawText = "",
                blocks = listOf(
                    ReceiptOcrBlock(
                        text = "Bill block",
                        lines = listOf(
                            ReceiptOcrLine(text = "Amount due 45.00", elements = listOf("Amount", "due", "45.00")),
                            ReceiptOcrLine(text = "Due date 31/07/2026", elements = listOf("Due", "date", "31/07/2026")),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(ReceiptDocumentType.BillInvoice, result)
    }
}
