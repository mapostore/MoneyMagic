package com.indiewalkabout.moneymagic.feature.capture.domain.usecase

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseReceiptTextUseCaseTest {
    private val parseReceiptText = ParseReceiptTextUseCase()

    @Test
    fun parsesMerchantDateAndTotalFromEnglishReceipt() {
        val draft = parseReceiptText(
            """
            Fresh Market
            Via Roma 12
            Date 2026-06-24
            Apples 3.20
            Bread 2.50
            TOTAL EUR 12.34
            """.trimIndent(),
        )

        assertEquals("Fresh Market", draft.merchant)
        assertEquals("Fresh Market", draft.name)
        assertEquals("12.34", draft.amount)
        assertEquals("2026-06-24", draft.date)
    }

    @Test
    fun parsesItalianReceiptWithCommaAmountAndDayFirstDate() {
        val draft = parseReceiptText(
            """
            Alimentari Centro
            Scontrino fiscale
            Data 24/06/2026
            Subtotale 9,90
            Totale 15,75
            """.trimIndent(),
        )

        assertEquals("Alimentari Centro", draft.merchant)
        assertEquals("15.75", draft.amount)
        assertEquals("2026-06-24", draft.date)
    }

    @Test
    fun fallsBackToLargestAmountWhenNoTotalLabelExists() {
        val draft = parseReceiptText(
            """
            Corner Cafe
            Espresso 1.20
            Sandwich 6.40
            Tax 0.30
            """.trimIndent(),
        )

        assertEquals("Corner Cafe", draft.merchant)
        assertEquals("6.40", draft.amount)
    }

    @Test
    fun marksRiskyFallbackAmountAsMediumConfidence() {
        val draft = parseReceiptText(
            """
            Corner Cafe
            Espresso 1.20
            Sandwich 6.40
            Tax 0.30
            """.trimIndent(),
        )

        assertEquals("6.40", draft.amount)
        assertEquals(ReceiptFieldConfidence.Medium, draft.metadata.amountConfidence)
    }

    @Test
    fun parsesItalianTotalWithoutChoosingIvaOrImponibile() {
        val draft = parseReceiptText(
            """
            CONAD CITY
            DOCUMENTO COMMERCIALE
            Data 24/06/2026 Ora 18:42
            Imponibile 14,34
            IVA 10% 1,43
            Totale complessivo 15,77
            Pagamento elettronico 15,77
            """.trimIndent(),
        )

        assertEquals("CONAD CITY", draft.merchant)
        assertEquals("15.77", draft.amount)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.amountConfidence)
    }

    @Test
    fun ignoresCardAuthorizationAndTransactionNumbers() {
        val draft = parseReceiptText(
            """
            Payment Receipt
            CARD VISA **** 1234
            AUTH 987654
            TRANSACTION 000123456789
            AMOUNT PAID EUR 42.80
            """.trimIndent(),
        )

        assertEquals("42.80", draft.amount)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.amountConfidence)
    }

    @Test
    fun parsesItalianAndEnglishThousandSeparators() {
        val italian = parseReceiptText("Bolletta Energia\nImporto da pagare € 1.234,56")
        val english = parseReceiptText("Utility Bill\nAmount due EUR 1,234.56")

        assertEquals("1234.56", italian.amount)
        assertEquals("1234.56", english.amount)
    }

    @Test
    fun keepsItalianSubtotalConfidenceLow() {
        val draft = parseReceiptText("Subtotale 9,90")

        assertEquals("9.90", draft.amount)
        assertEquals(ReceiptFieldConfidence.Low, draft.metadata.amountConfidence)
    }

    @Test
    fun keepsEnglishSubtotalConfidenceLow() {
        val draft = parseReceiptText("Subtotal 9.90")

        assertEquals("9.90", draft.amount)
        assertEquals(ReceiptFieldConfidence.Low, draft.metadata.amountConfidence)
    }
}
