package com.indiewalkabout.moneymagic.feature.capture.domain.usecase

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseBillInvoiceTextUseCaseTest {
    private val parseBillInvoiceText = ParseBillInvoiceTextUseCase()

    @Test
    fun parsesItalianBillIssuerAmountDueAndDueDate() {
        val draft = parseBillInvoiceText(
            """
            Bolletta Energia
            ENERGIA VERDE SPA
            Codice cliente 123456
            Totale bolletta 118,40
            IVA 10% 8,20
            Importo da pagare € 126,60
            Scadenza 31/07/2026
            """.trimIndent(),
        )

        assertEquals("ENERGIA VERDE SPA", draft.merchant)
        assertEquals("ENERGIA VERDE SPA", draft.name)
        assertEquals("126.60", draft.amount)
        assertEquals("2026-07-31", draft.date)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.amountConfidence)
        assertEquals(ReceiptFieldConfidence.High, draft.metadata.dateConfidence)
    }

    @Test
    fun parsesEnglishInvoiceAmountDueAndDueDate() {
        val draft = parseBillInvoiceText(
            """
            INVOICE
            Cloud Utilities Ltd
            Statement date July 1, 2026
            Previous balance EUR 20.00
            Amount due EUR 87.20
            Due date July 31, 2026
            """.trimIndent(),
        )

        assertEquals("Cloud Utilities Ltd", draft.merchant)
        assertEquals("87.20", draft.amount)
        assertEquals("2026-07-31", draft.date)
    }

    @Test
    fun prefersAmountDueOverPreviousBalanceAndTaxRows() {
        val draft = parseBillInvoiceText(
            """
            Utility Provider
            Previous balance 350.00
            Tax 9.50
            Amount due 42.80
            """.trimIndent(),
        )

        assertEquals("42.80", draft.amount)
    }

    @Test
    fun exposesBillAmountAndDateCandidatesForReview() {
        val draft = parseBillInvoiceText(
            """
            Utility Provider
            Previous balance 350.00
            Amount due 42.80
            Statement date July 1, 2026
            Due date July 31, 2026
            """.trimIndent(),
        )

        assertEquals(listOf("42.80", "350.00"), draft.candidates.amounts.map { it.value })
        assertEquals(listOf("2026-07-31", "2026-07-01"), draft.candidates.dates.map { it.value })
    }

    @Test
    fun fallsBackToIssueDateWhenDueDateIsMissing() {
        val draft = parseBillInvoiceText(
            """
            FATTURA
            ACQUA SERVIZI SRL
            Data emissione 05/07/2026
            Importo da pagare 65,30
            """.trimIndent(),
        )

        assertEquals("ACQUA SERVIZI SRL", draft.merchant)
        assertEquals("2026-07-05", draft.date)
        assertEquals(ReceiptFieldConfidence.Medium, draft.metadata.dateConfidence)
    }
}
