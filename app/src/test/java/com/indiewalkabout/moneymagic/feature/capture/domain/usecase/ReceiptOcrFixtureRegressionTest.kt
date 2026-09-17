package com.indiewalkabout.moneymagic.feature.capture.domain.usecase

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptOcrFixtureRegressionTest {
    private val classifyDocument = ClassifyReceiptDocumentUseCase()
    private val parseReceiptText = ParseReceiptTextUseCase()
    private val parseBillInvoiceText = ParseBillInvoiceTextUseCase()

    @Test
    fun parsesRealOcrFixtures() {
        fixtureNames.forEach { fixtureName ->
            val rawText = readFixtureText(fixtureName)
            val expected = readExpectedValues(fixtureName)
            val expectedType = ReceiptDocumentType.valueOf(expected.required("documentType"))
            val actualType = classifyDocument(ReceiptOcrResult(rawText = rawText))
            val draft = when (actualType) {
                ReceiptDocumentType.BillInvoice -> parseBillInvoiceText(rawText)
                ReceiptDocumentType.Receipt,
                ReceiptDocumentType.Unknown,
                -> parseReceiptText(rawText)
            }

            assertEquals("$fixtureName documentType", expectedType, actualType)
            assertEquals("$fixtureName merchant", expected.required("merchant"), draft.merchant)
            assertEquals("$fixtureName amount", expected.required("amount"), draft.amount)
            assertEquals("$fixtureName date", expected.required("date"), draft.date)
        }
    }

    private fun readFixtureText(fixtureName: String): String =
        requireNotNull(javaClass.getResource("/ocr-fixtures/$fixtureName.txt")) {
            "Missing OCR fixture text for $fixtureName"
        }.readText().trim()

    private fun readExpectedValues(fixtureName: String): Map<String, String> =
        requireNotNull(javaClass.getResource("/ocr-fixtures/$fixtureName.expected.txt")) {
            "Missing OCR fixture expectations for $fixtureName"
        }.readText()
            .lineSequence()
            .map(String::trim)
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { line ->
                val key = line.substringBefore("=")
                val value = line.substringAfter("=", missingDelimiterValue = "")
                key to value
            }

    private fun Map<String, String>.required(key: String): String =
        requireNotNull(getValue(key)) { "Missing expected value $key" }

    companion object {
        private val fixtureNames = listOf(
            "it_energy_bill_ocr",
            "en_cloud_invoice_ocr",
            "it_market_receipt_ocr",
        )
    }
}
