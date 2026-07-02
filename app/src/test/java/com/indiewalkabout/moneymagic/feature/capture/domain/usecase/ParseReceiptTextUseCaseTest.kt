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
        assertEquals(ReceiptFieldConfidence.High, italian.metadata.amountConfidence)
        assertEquals("1234.56", english.amount)
        assertEquals(ReceiptFieldConfidence.High, english.metadata.amountConfidence)
    }

    @Test
    fun keepsEnglishCardPaidTotalConfidenceHigh() {
        val draft = parseReceiptText("AMOUNT PAID BY CARD EUR 42.80")

        assertEquals("42.80", draft.amount)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.amountConfidence)
    }

    @Test
    fun keepsItalianCardPaidTotalConfidenceHigh() {
        val draft = parseReceiptText("Totale pagato carta 42,80")

        assertEquals("42.80", draft.amount)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.amountConfidence)
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

    @Test
    fun keepsMerchantNamesThatContainBroadPaymentOrTaxWords() {
        val cashConverters = parseReceiptText("Cash Converters\nTotal 12.50")
        val cardFactory = parseReceiptText("Card Factory\nTotal 8.90")
        val taxAssist = parseReceiptText("TaxAssist\nTotal 20.00")

        assertEquals("Cash Converters", cashConverters.merchant)
        assertEquals("Card Factory", cardFactory.merchant)
        assertEquals("TaxAssist", taxAssist.merchant)
    }

    @Test
    fun keepsMerchantNamesThatContainLabelWords() {
        val timeMarket = parseReceiptText("Time Market\nTotal 12.00")
        val dateCafe = parseReceiptText("Date Cafe\nTotal 8.00")
        val receiptBank = parseReceiptText("Receipt Bank\nTotal 20.00")

        assertEquals("Time Market", timeMarket.merchant)
        assertEquals("Date Cafe", dateCafe.merchant)
        assertEquals("Receipt Bank", receiptBank.merchant)
    }

    @Test
    fun parsesItalianMonthNameDateAndSkipsFiscalHeader() {
        val draft = parseReceiptText(
            """
            DOCUMENTO COMMERCIALE
            ALIMENTARI ROSSI SRL
            Via Torino 8
            24 giugno 2026
            Totale 18,20
            """.trimIndent(),
        )

        assertEquals("ALIMENTARI ROSSI SRL", draft.merchant)
        assertEquals("2026-06-24", draft.date)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.dateConfidence)
    }

    @Test
    fun parsesEnglishMonthNameDate() {
        val draft = parseReceiptText(
            """
            North Market
            Tax Receipt
            June 24, 2026
            Grand Total $32.10
            """.trimIndent(),
        )

        assertEquals("North Market", draft.merchant)
        assertEquals("2026-06-24", draft.date)
    }

    @Test
    fun parsesEnglishMonthFirstDateWhenDateLabelIsEnglish() {
        val draft = parseReceiptText(
            """
            City Pharmacy
            Date 06/24/2026
            Total 9.99
            """.trimIndent(),
        )

        assertEquals("2026-06-24", draft.date)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.dateConfidence)
    }

    @Test
    fun parsesAmbiguousEnglishMonthFirstDateWhenDateLabelIsEnglish() {
        val draft = parseReceiptText(
            """
            City Pharmacy
            Date 06/07/2026
            Total 9.99
            """.trimIndent(),
        )

        assertEquals("2026-06-07", draft.date)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.dateConfidence)
    }
}
