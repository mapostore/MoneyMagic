package com.indiewalkabout.moneymagic.feature.settings.domain.usecase

import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.Month
import java.time.ZoneId
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class ExportExpensesXlsxUseCase @Inject constructor() {
    operator fun invoke(expenses: List<Expense>): ByteArray {
        val rows = expenses
            .sortedByDescending { it.dateTime }
            .groupBy { expense ->
                val date = expense.dateTime.atZone(ZoneId.systemDefault()).toLocalDate()
                date.year to date.month
            }
            .flatMap { (yearMonth, monthExpenses) ->
                val (year, month) = yearMonth
                listOf(ExportRow.Header("$year - ${month.italianName()}")) +
                    monthExpenses.map(ExportRow::ExpenseLine)
            }

        return createXlsx(rows)
    }
}

private sealed interface ExportRow {
    data class Header(val text: String) : ExportRow
    data class ExpenseLine(val expense: Expense) : ExportRow
}

private fun createXlsx(rows: List<ExportRow>): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        zip.writeEntry("[Content_Types].xml", contentTypesXml)
        zip.writeEntry("_rels/.rels", relsXml)
        zip.writeEntry("xl/workbook.xml", workbookXml)
        zip.writeEntry("xl/_rels/workbook.xml.rels", workbookRelsXml)
        zip.writeEntry("xl/worksheets/sheet1.xml", sheetXml(rows))
    }
    return output.toByteArray()
}

private fun ZipOutputStream.writeEntry(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun sheetXml(rows: List<ExportRow>): String =
    buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("<sheetData>")
        rows.forEachIndexed { index, row ->
            val rowNumber = index + 1
            append("""<row r="$rowNumber">""")
            when (row) {
                is ExportRow.Header -> {
                    append(inlineStringCell("A", rowNumber, row.text))
                }
                is ExportRow.ExpenseLine -> {
                    val expense = row.expense
                    append(inlineStringCell("A", rowNumber, expense.amountMinor.negativeAmount()))
                    append(inlineStringCell("B", rowNumber, ""))
                    append(inlineStringCell("C", rowNumber, ""))
                    append(inlineStringCell("D", rowNumber, expense.italianDate()))
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

private fun inlineStringCell(column: String, row: Int, value: String): String =
    """<c r="$column$row" t="inlineStr"><is><t>${value.xmlEscaped()}</t></is></c>"""

private fun Long.negativeAmount(): String {
    val euros = this / 100
    val cents = kotlin.math.abs(this % 100)
    return "-%d,%02d".format(Locale.ROOT, euros, cents)
}

private fun Expense.italianDate(): String {
    val date = dateTime.atZone(ZoneId.systemDefault()).toLocalDate()
    return "${date.dayOfWeek.italianShortName()} ${date.dayOfMonth.toString().padStart(2, '0')} " +
        "${date.month.italianName()} ${date.year}"
}

private fun DayOfWeek.italianShortName(): String =
    when (this) {
        DayOfWeek.MONDAY -> "lun"
        DayOfWeek.TUESDAY -> "mar"
        DayOfWeek.WEDNESDAY -> "mer"
        DayOfWeek.THURSDAY -> "gio"
        DayOfWeek.FRIDAY -> "ven"
        DayOfWeek.SATURDAY -> "sab"
        DayOfWeek.SUNDAY -> "dom"
    }

private fun Month.italianName(): String =
    when (this) {
        Month.JANUARY -> "gennaio"
        Month.FEBRUARY -> "febbraio"
        Month.MARCH -> "marzo"
        Month.APRIL -> "aprile"
        Month.MAY -> "maggio"
        Month.JUNE -> "giugno"
        Month.JULY -> "luglio"
        Month.AUGUST -> "agosto"
        Month.SEPTEMBER -> "settembre"
        Month.OCTOBER -> "ottobre"
        Month.NOVEMBER -> "novembre"
        Month.DECEMBER -> "dicembre"
    }

private fun String.xmlEscaped(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private val contentTypesXml = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
        <Default Extension="xml" ContentType="application/xml"/>
        <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
        <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    </Types>
""".trimIndent()

private val relsXml = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
    </Relationships>
""".trimIndent()

private val workbookXml = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
        <sheets>
            <sheet name="Expenses" sheetId="1" r:id="rId1"/>
        </sheets>
    </workbook>
""".trimIndent()

private val workbookRelsXml = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    </Relationships>
""".trimIndent()
