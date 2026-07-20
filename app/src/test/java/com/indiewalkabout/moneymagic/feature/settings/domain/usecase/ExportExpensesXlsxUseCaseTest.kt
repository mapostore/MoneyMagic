package com.indiewalkabout.moneymagic.feature.settings.domain.usecase

import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.zip.ZipInputStream
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportExpensesXlsxUseCaseTest {
    private val exportExpenses = ExportExpensesXlsxUseCase()

    @Test
    fun exportsNegativeAmountDescriptionCategoryAndItalianDate() {
        val bytes = exportExpenses(
            listOf(
                testExpense(
                    amountMinor = 1234,
                    dateTime = Instant.parse("2026-06-01T10:00:00Z"),
                ),
            ),
            listOf(testCategory()),
        )

        val sheet = bytes.entryText("xl/worksheets/sheet1.xml")
        val styles = bytes.entryText("xl/styles.xml")

        assertTrue(sheet.contains("2026 - giugno"))
        assertTrue(sheet.contains("-12,34"))
        assertTrue(sheet.contains("Lunch with team"))
        assertTrue(sheet.contains("SPESA"))
        assertTrue(sheet.contains("lun 01 giugno 2026"))
        assertTrue(sheet.contains("""r="A2" s="1""""))
        assertTrue(sheet.contains("""r="B2" s="1""""))
        assertTrue(sheet.contains("""r="C2" s="1""""))
        assertTrue(sheet.contains("""r="D2" s="2""""))
        assertTrue(styles.contains("""<sz val="10"/>"""))
        assertTrue(styles.contains("""<name val="Arial"/>"""))
        assertTrue(styles.contains("""<left style="thin"><color rgb="FF000000"/></left>"""))
        assertTrue(styles.contains("""<right style="thin"><color rgb="FF000000"/></right>"""))
        assertTrue(styles.contains("""<alignment horizontal="left"/>"""))
        assertTrue(styles.contains("""<alignment horizontal="right"/>"""))
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
            listOf(testCategory()),
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
            description = "Lunch with team",
            notes = "",
            tags = emptyList(),
            createdAt = dateTime,
            updatedAt = dateTime,
        )

    private fun testCategory(): Category =
        Category(
            id = 1,
            name = "SPESA",
            color = 0xFF2563EB,
            iconKey = "spesa",
            sortOrder = 0,
            archived = false,
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
