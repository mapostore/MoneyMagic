package com.indiewalkabout.moneymagic.feature.capture.domain.usecase

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptParseMetadata
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        val parsedDate = findDate(lines)
        val date = parsedDate?.value?.toString().orEmpty()
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
                dateConfidence = parsedDate?.confidence ?: ReceiptFieldConfidence.Missing,
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

private val nonMerchantExactLabels = listOf(
    "scontrino",
    "scontrino fiscale",
    "ricevuta",
    "ricevuta fiscale",
    "fattura",
    "documento commerciale",
    "receipt",
    "invoice",
    "tax receipt",
)

private val dateTimeLabelKeywords = listOf(
    "date",
    "data",
    "ora",
    "time",
)

private val merchantPaymentSignals = listOf(
    "pagamento",
    "payment",
    "authorization",
    "auth",
    "transazione",
    "transaction",
    "visa",
    "mastercard",
    "bancomat",
    "pos",
)

private val isoDatePattern = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
private val dayFirstDatePattern = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""")
private val longDigitSequencePattern = Regex("""\d{4,}""")
private val timePattern = Regex("""\b\d{1,2}:\d{2}\b""")
private val monthNameDatePatterns = listOf(
    Regex("""\b(\d{1,2})\s+([A-Za-zÀ-ÿ]{3,})\s+(\d{2,4})\b""", RegexOption.IGNORE_CASE),
    Regex("""\b([A-Za-zÀ-ÿ]{3,})\s+(\d{1,2}),?\s+(\d{2,4})\b""", RegexOption.IGNORE_CASE),
)

private val monthNames = mapOf(
    "gennaio" to 1, "gen" to 1, "january" to 1, "jan" to 1,
    "febbraio" to 2, "feb" to 2, "february" to 2,
    "marzo" to 3, "mar" to 3, "march" to 3,
    "aprile" to 4, "apr" to 4, "april" to 4,
    "maggio" to 5, "mag" to 5, "may" to 5,
    "giugno" to 6, "giu" to 6, "june" to 6, "jun" to 6,
    "luglio" to 7, "lug" to 7, "july" to 7, "jul" to 7,
    "agosto" to 8, "ago" to 8, "august" to 8, "aug" to 8,
    "settembre" to 9, "set" to 9, "september" to 9, "sep" to 9,
    "ottobre" to 10, "ott" to 10, "october" to 10, "oct" to 10,
    "novembre" to 11, "nov" to 11, "november" to 11,
    "dicembre" to 12, "dic" to 12, "december" to 12, "dec" to 12,
)

private data class ParsedAmount(
    val value: BigDecimal,
    val confidence: ReceiptFieldConfidence,
    val score: Int,
    val lineIndex: Int,
)

private data class ParsedDate(
    val value: LocalDate,
    val confidence: ReceiptFieldConfidence,
)

private fun looksLikeMerchant(line: String): Boolean {
    val letters = line.count(Char::isLetter)
    val digits = line.count(Char::isDigit)
    return letters >= 3 &&
        letters >= digits &&
        !isNonMerchantLabelLine(line) &&
        !matchesAnyKeyword(line, strongTotalKeywords) &&
        !isPaymentOrAdjustmentLine(line) &&
        moneyPattern.find(line) == null &&
        parseDate(line) == null
}

private fun isNonMerchantLabelLine(line: String): Boolean {
    val normalized = line.trim()
        .split(Regex("""\s+"""))
        .joinToString(" ")
        .lowercase(Locale.ROOT)
    return nonMerchantExactLabels.any { normalized == it } ||
        (
            matchesAnyKeyword(line, dateTimeLabelKeywords) &&
                (parseDate(line) != null || timePattern.containsMatchIn(line))
            )
}

private fun isPaymentOrAdjustmentLine(line: String): Boolean {
    if (!matchesAnyKeyword(line, weakAmountKeywords)) return false
    return moneyPattern.find(line) != null ||
        longDigitSequencePattern.containsMatchIn(line) ||
        matchesAnyKeyword(line, merchantPaymentSignals)
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

private fun findDate(lines: List<String>): ParsedDate? {
    lines.forEach { line ->
        parseDate(line)?.let { return it }
    }
    return null
}

private fun parseDate(line: String): ParsedDate? =
    parseIsoDate(line)
        ?: parseMonthNameDate(line)
        ?: parseNumericDate(line)

private fun parseIsoDate(line: String): ParsedDate? {
    val match = isoDatePattern.find(line) ?: return null
    return runCatching {
        ParsedDate(LocalDate.parse(match.value, DateTimeFormatter.ISO_LOCAL_DATE), ReceiptFieldConfidence.High)
    }.getOrNull()
}

private fun parseMonthNameDate(line: String): ParsedDate? {
    monthNameDatePatterns.forEachIndexed { index, pattern ->
        val match = pattern.find(line) ?: return@forEachIndexed
        val day = (
            if (index == 0) match.groupValues[1].toIntOrNull() else match.groupValues[2].toIntOrNull()
            ) ?: return@forEachIndexed
        val monthText = if (index == 0) match.groupValues[2] else match.groupValues[1]
        val yearText = match.groupValues[3]
        val month = monthNames[monthText.lowercase(Locale.ROOT)] ?: return@forEachIndexed
        val year = normalizeYear(yearText)
        return runCatching {
            ParsedDate(LocalDate.of(year, month, day), ReceiptFieldConfidence.High)
        }.getOrNull()
    }
    return null
}

private fun parseNumericDate(line: String): ParsedDate? {
    val match = dayFirstDatePattern.find(line) ?: return null
    val first = match.groupValues[1].toIntOrNull() ?: return null
    val second = match.groupValues[2].toIntOrNull() ?: return null
    val year = normalizeYear(match.groupValues[3])
    val monthFirst = matchesAnyKeyword(line, listOf("date")) && first in 1..12 && second in 1..31
    val day = if (monthFirst) second else first
    val month = if (monthFirst) first else second
    return try {
        ParsedDate(LocalDate.of(year, month, day), ReceiptFieldConfidence.High)
    } catch (_: DateTimeException) {
        null
    }
}

private fun normalizeYear(rawYear: String): Int =
    if (rawYear.length == 2) "20$rawYear".toInt() else rawYear.toInt()
