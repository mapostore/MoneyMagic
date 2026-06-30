package com.indiewalkabout.moneymagic.feature.settings.domain.usecase

import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.zip.ZipInputStream
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportExpensesXlsxUseCaseTest {
    private val exportExpenses = ExportExpensesXlsxUseCase()

    @Test
    fun exportsNegativeAmountBlankColumnsAndItalianDate() {
        val bytes = exportExpenses(
            listOf(
                testExpense(
                    amountMinor = 1234,
                    dateTime = Instant.parse("2026-06-01T10:00:00Z"),
                ),
            ),
        )

        val sheet = bytes.entryText("xl/worksheets/sheet1.xml")

        assertTrue(sheet.contains("2026 - giugno"))
        assertTrue(sheet.contains("-12,34"))
        assertTrue(sheet.contains("lun 01 giugno 2026"))
        assertTrue(sheet.contains("""r="B2""""))
        assertTrue(sheet.contains("""r="C2""""))
    }

    @Test
    fun groupsExpensesByMonthAndYearNewestGroupFirst() {
        val bytes = exportExpenses(
            listOf(
                testExpense(
                    amountMinor = 1000,
                    dateTime = Instant.parse("2026-05-15T10:00:00Z"),
                ),
                testExpense(
                    amountMinor = 2000,
                    dateTime = Instant.parse("2026-06-01T10:00:00Z"),
                ),
                testExpense(
                    amountMinor = 3000,
                    dateTime = Instant.parse("2025-12-01T10:00:00Z"),
                ),
            ),
        )

        val sheet = bytes.entryText("xl/worksheets/sheet1.xml")

        assertTrue(sheet.indexOf("2026 - giugno") < sheet.indexOf("2026 - maggio"))
        assertTrue(sheet.indexOf("2026 - maggio") < sheet.indexOf("2025 - dicembre"))
    }

    private fun testExpense(amountMinor: Long, dateTime: Instant): Expense =
        Expense(
            id = amountMinor,
            name = "Expense",
            amountMinor = amountMinor,
            currency = "EUR",
            dateTime = dateTime,
            categoryId = 1,
            merchant = "Merchant",
            paymentMethodId = null,
            notes = "",
            tags = emptyList(),
            createdAt = dateTime,
            updatedAt = dateTime,
        )
}

private fun ByteArray.entryText(name: String): String {
    val zip = ZipInputStream(ByteArrayInputStream(this))
    while (true) {
        val entry = zip.nextEntry ?: error("Missing zip entry $name")
        if (entry.name == name) {
            return zip.bufferedReader().readText()
        }
    }
}
