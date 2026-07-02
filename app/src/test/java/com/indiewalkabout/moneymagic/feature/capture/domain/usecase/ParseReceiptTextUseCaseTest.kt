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
}
