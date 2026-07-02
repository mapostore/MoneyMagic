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
        val parsedAmount = findTotalAmount(lines)
        val amount = parsedAmount?.value?.toPlainAmount().orEmpty()
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
                amountConfidence = parsedAmount?.confidence ?: ReceiptFieldConfidence.Missing,
                dateConfidence = if (date.isBlank()) {
                    ReceiptFieldConfidence.Missing
                } else {
                    ReceiptFieldConfidence.High
                },
            ),
        )
    }
}

private val moneyPattern = Regex(
    pattern = """(?<![\dA-Za-z])(?:€|EUR|EURO|USD|\$)?\s*(\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d{1,6}[.,]\d{2})(?![\dA-Za-z])""",
    option = RegexOption.IGNORE_CASE,
)
private val percentPattern = Regex("""\b\d{1,2}(?:[.,]\d{1,2})?\s*%""")

private val strongTotalKeywords = listOf(
    "totale complessivo",
    "importo totale",
    "importo pagato",
    "importo da pagare",
    "da pagare",
    "totale",
    "pagato",
    "saldo",
    "grand total",
    "amount due",
    "amount paid",
    "balance due",
    "total",
    "paid",
)

private val weakAmountKeywords = listOf(
    "subtotale",
    "imponibile",
    "iva",
    "resto",
    "sconto",
    "contanti",
    "carta",
    "bancomat",
    "pos",
    "transazione",
    "subtotal",
    "tax",
    "vat",
    "change",
    "cash",
    "card",
    "authorization",
    "auth",
    "transaction",
)

private val isoDatePattern = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
private val dayFirstDatePattern = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""")

private data class ParsedAmount(
    val value: BigDecimal,
    val confidence: ReceiptFieldConfidence,
    val score: Int,
    val lineIndex: Int,
)

private fun looksLikeMerchant(line: String): Boolean {
    val lower = line.lowercase(Locale.ROOT)
    return !matchesAnyKeyword(line, strongTotalKeywords) &&
        !lower.contains("date") &&
        !lower.contains("data") &&
        moneyPattern.find(line) == null
}

private fun findTotalAmount(lines: List<String>): ParsedAmount? {
    val candidates = lines.flatMapIndexed { index, line -> amountCandidates(line, index) }
    return candidates
        .sortedWith(
            compareByDescending<ParsedAmount> { it.score }
                .thenByDescending { it.lineIndex }
                .thenByDescending { it.value },
        )
        .firstOrNull()
}

private fun amountCandidates(line: String, lineIndex: Int): List<ParsedAmount> {
    if (percentPattern.containsMatchIn(line)) {
        val percentRanges = percentPattern.findAll(line).map { it.range }.toList()
        return moneyPattern.findAll(line)
            .filterNot { match -> percentRanges.any { match.range.first >= it.first && match.range.last <= it.last } }
            .mapNotNull { match -> scoredAmount(line, match.groupValues[1], lineIndex) }
            .toList()
    }
    return moneyPattern.findAll(line)
        .mapNotNull { match -> scoredAmount(line, match.groupValues[1], lineIndex) }
        .toList()
}

private fun scoredAmount(line: String, rawAmount: String, lineIndex: Int): ParsedAmount? {
    val value = parseAmount(rawAmount) ?: return null
    if (value <= BigDecimal.ZERO) return null
    val strong = matchesAnyKeyword(line, strongTotalKeywords)
    val weak = matchesAnyKeyword(line, weakAmountKeywords)
    val score = when {
        strong -> 100
        weak -> 10
        else -> 40
    }
    val confidence = when {
        strong -> ReceiptFieldConfidence.High
        weak -> ReceiptFieldConfidence.Low
        else -> ReceiptFieldConfidence.Medium
    }
    return ParsedAmount(value = value, confidence = confidence, score = score, lineIndex = lineIndex)
}

private fun matchesAnyKeyword(line: String, keywords: List<String>): Boolean =
    keywords.any { keyword -> keyword.toKeywordRegex().containsMatchIn(line) }

private fun String.toKeywordRegex(): Regex {
    val phrase = trim()
        .split(Regex("""\s+"""))
        .joinToString("""\s+""") { Regex.escape(it) }
    return Regex("""(?<![\p{L}\p{N}])$phrase(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)
}

private fun parseAmount(amount: String): BigDecimal? {
    val normalized = normalizeAmount(amount) ?: return null
    return normalized.toBigDecimalOrNull()
}

private fun normalizeAmount(amount: String): String? {
    val compact = amount.filter { it.isDigit() || it == ',' || it == '.' }
    if (compact.isBlank()) return null
    val lastComma = compact.lastIndexOf(',')
    val lastDot = compact.lastIndexOf('.')
    val decimalSeparator = when {
        lastComma == -1 -> '.'
        lastDot == -1 -> ','
        lastComma > lastDot -> ','
        else -> '.'
    }
    val integer = compact.substringBeforeLast(decimalSeparator, missingDelimiterValue = compact)
        .filter(Char::isDigit)
    val cents = compact.substringAfterLast(decimalSeparator, missingDelimiterValue = "")
        .filter(Char::isDigit)
    if (integer.isBlank() || cents.length != 2) return null
    return "$integer.$cents"
}

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
