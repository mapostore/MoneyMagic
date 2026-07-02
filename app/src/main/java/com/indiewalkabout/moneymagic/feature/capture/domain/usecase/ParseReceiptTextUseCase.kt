package com.indiewalkabout.moneymagic.feature.capture.domain.usecase

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptParseMetadata
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

class ParseReceiptTextUseCase @Inject constructor() {
    operator fun invoke(text: String): ReceiptDraft {
        val lines = text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val merchant = lines.firstOrNull(::looksLikeMerchant).orEmpty()
        val amount = findTotalAmount(lines).orEmpty()
        val date = findDate(lines).orEmpty()
        return ReceiptDraft(
            name = merchant,
            merchant = merchant,
            amount = amount,
            date = date,
            rawText = text,
            metadata = ReceiptParseMetadata(
                merchantConfidence = if (merchant.isBlank()) {
                    ReceiptFieldConfidence.Missing
                } else {
                    ReceiptFieldConfidence.High
                },
                amountConfidence = if (amount.isBlank()) {
                    ReceiptFieldConfidence.Missing
                } else {
                    ReceiptFieldConfidence.Medium
                },
                dateConfidence = if (date.isBlank()) {
                    ReceiptFieldConfidence.Missing
                } else {
                    ReceiptFieldConfidence.High
                },
            ),
        )
    }
}

private val amountPattern = Regex("""(?<!\d)(\d{1,5}(?:[.,]\d{2}))(?!\d)""")
private val totalKeywords = listOf("total", "totale", "amount", "balance", "paid")
private val isoDatePattern = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
private val dayFirstDatePattern = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""")

private fun looksLikeMerchant(line: String): Boolean {
    val lower = line.lowercase(Locale.ROOT)
    return totalKeywords.none { lower.contains(it) } &&
        !lower.contains("date") &&
        !lower.contains("data") &&
        amountPattern.find(line) == null
}

private fun findTotalAmount(lines: List<String>): String? {
    val totalLineAmount = lines
        .asSequence()
        .filter { line -> totalKeywords.any { line.contains(it, ignoreCase = true) } }
        .flatMap { line -> amountPattern.findAll(line).map { it.groupValues[1] } }
        .mapNotNull(::parseAmount)
        .lastOrNull()

    val fallbackAmount = lines
        .asSequence()
        .flatMap { line -> amountPattern.findAll(line).map { it.groupValues[1] } }
        .mapNotNull(::parseAmount)
        .maxOrNull()

    return (totalLineAmount ?: fallbackAmount)?.toPlainAmount()
}

private fun parseAmount(amount: String): BigDecimal? =
    amount.replace(',', '.').toBigDecimalOrNull()

private fun BigDecimal.toPlainAmount(): String =
    setScale(2).toPlainString()

private fun findDate(lines: List<String>): String? {
    lines.forEach { line ->
        parseIsoDate(line)?.let { return it.toString() }
        parseDayFirstDate(line)?.let { return it.toString() }
    }
    return null
}

private fun parseIsoDate(line: String): LocalDate? {
    val match = isoDatePattern.find(line) ?: return null
    return runCatching {
        LocalDate.parse(match.value, DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrNull()
}

private fun parseDayFirstDate(line: String): LocalDate? {
    val match = dayFirstDatePattern.find(line) ?: return null
    val day = match.groupValues[1].padStart(2, '0')
    val month = match.groupValues[2].padStart(2, '0')
    val rawYear = match.groupValues[3]
    val year = if (rawYear.length == 2) "20$rawYear" else rawYear
    return try {
        LocalDate.parse("$year-$month-$day", DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }
}
