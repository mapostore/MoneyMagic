package com.indiewalkabout.moneymagic.feature.capture.domain.usecase

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidate
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidateField
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptCandidates
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptParseMetadata
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class ParseBillInvoiceTextUseCase @Inject constructor() {
    operator fun invoke(text: String): ReceiptDraft {
        val lines = text
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()
        val issuerCandidates = findIssuerCandidates(lines)
        val issuer = issuerCandidates.firstOrNull().orEmpty()
        val amountCandidates = findAmountCandidates(lines)
        val amount = amountCandidates.firstOrNull()
        val dateCandidates = findDateCandidates(lines)
        val date = dateCandidates.firstOrNull()
        return ReceiptDraft(
            name = issuer,
            merchant = issuer,
            amount = amount?.value?.toPlainAmount().orEmpty(),
            date = date?.value?.toString().orEmpty(),
            rawText = text,
            metadata = ReceiptParseMetadata(
                merchantConfidence = if (issuer.isBlank()) ReceiptFieldConfidence.Missing else ReceiptFieldConfidence.Medium,
                amountConfidence = amount?.confidence ?: ReceiptFieldConfidence.Missing,
                dateConfidence = date?.confidence ?: ReceiptFieldConfidence.Missing,
            ),
            candidates = ReceiptCandidates(
                merchants = issuerCandidates.take(3).map { candidate ->
                    ReceiptCandidate(
                        field = ReceiptCandidateField.Merchant,
                        value = candidate,
                        label = candidate,
                        confidence = ReceiptFieldConfidence.Medium,
                    )
                },
                amounts = amountCandidates
                    .distinctBy { it.value }
                    .take(5)
                    .map { candidate ->
                        ReceiptCandidate(
                            field = ReceiptCandidateField.Amount,
                            value = candidate.value.toPlainAmount(),
                            label = candidate.label,
                            confidence = candidate.confidence,
                        )
                    },
                dates = dateCandidates
                    .distinctBy { it.value }
                    .take(3)
                    .map { candidate ->
                        ReceiptCandidate(
                            field = ReceiptCandidateField.Date,
                            value = candidate.value.toString(),
                            label = candidate.label,
                            confidence = candidate.confidence,
                        )
                    },
            ),
        )
    }
}

private val moneyPattern = Regex(
    pattern = """(?<![\dA-Za-z])(?:€|EUR|EURO|USD|\$)?\s*(\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d{1,6}[.,]\d{2})(?![\dA-Za-z])""",
    option = RegexOption.IGNORE_CASE,
)
private val isoDatePattern = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
private val dayFirstDatePattern = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""")
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

private val headerLabels = listOf(
    "bolletta",
    "fattura",
    "invoice",
    "statement",
)

private val amountDueKeywords = listOf(
    "importo da pagare",
    "totale da pagare",
    "totale bolletta",
    "amount due",
    "balance due",
    "total due",
)

private val demotedAmountKeywords = listOf(
    "saldo precedente",
    "previous balance",
    "iva",
    "tax",
    "vat",
    "imponibile",
    "canone",
)

private val dueDateKeywords = listOf(
    "scadenza",
    "due date",
    "payment due",
)

private val issueDateKeywords = listOf(
    "data emissione",
    "data fattura",
    "statement date",
    "invoice date",
)

private data class BillParsedAmount(
    val value: BigDecimal,
    val confidence: ReceiptFieldConfidence,
    val score: Int,
    val lineIndex: Int,
    val label: String,
)

private data class BillParsedDate(
    val value: LocalDate,
    val confidence: ReceiptFieldConfidence,
    val score: Int,
    val lineIndex: Int,
    val label: String,
)

private fun findIssuerCandidates(lines: List<String>): List<String> =
    lines.filter { line ->
        line.count(Char::isLetter) >= 3 &&
            moneyPattern.find(line) == null &&
            parseDate(line) == null &&
            !matchesAnyKeyword(line, headerLabels + amountDueKeywords + dueDateKeywords + issueDateKeywords)
    }.distinct()

private fun findAmountCandidates(lines: List<String>): List<BillParsedAmount> =
    lines.flatMapIndexed { index, line ->
        moneyPattern.findAll(line)
            .mapNotNull { match -> scoredAmount(line, match.groupValues[1], index) }
    }
        .sortedWith(
            compareByDescending<BillParsedAmount> { it.score }
                .thenByDescending { it.lineIndex }
                .thenByDescending { it.value },
        )

private fun scoredAmount(line: String, rawAmount: String, lineIndex: Int): BillParsedAmount? {
    val value = parseAmount(rawAmount) ?: return null
    if (value <= BigDecimal.ZERO) return null
    val score = when {
        matchesAnyKeyword(line, amountDueKeywords) -> 100
        matchesAnyKeyword(line, demotedAmountKeywords) -> 10
        else -> 40
    }
    val confidence = when {
        score >= 100 -> ReceiptFieldConfidence.High
        score <= 10 -> ReceiptFieldConfidence.Low
        else -> ReceiptFieldConfidence.Medium
    }
    return BillParsedAmount(value, confidence, score, lineIndex, line)
}

private fun findDateCandidates(lines: List<String>): List<BillParsedDate> =
    lines.flatMapIndexed { index, line ->
        parseDate(line)?.let { parsed ->
            val score = when {
                matchesAnyKeyword(line, dueDateKeywords) -> 100
                matchesAnyKeyword(line, issueDateKeywords) -> 60
                else -> 40
            }
            listOf(
                BillParsedDate(
                    value = parsed,
                    confidence = if (score >= 100) ReceiptFieldConfidence.High else ReceiptFieldConfidence.Medium,
                    score = score,
                    lineIndex = index,
                    label = line,
                ),
            )
        } ?: emptyList()
    }
        .sortedWith(
            compareByDescending<BillParsedDate> { it.score }
                .thenByDescending { it.lineIndex },
        )

private fun parseDate(line: String): LocalDate? =
    parseIsoDate(line)
        ?: parseMonthNameDate(line)
        ?: parseNumericDate(line)

private fun parseIsoDate(line: String): LocalDate? {
    val match = isoDatePattern.find(line) ?: return null
    return runCatching { LocalDate.parse(match.value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
}

private fun parseMonthNameDate(line: String): LocalDate? {
    monthNameDatePatterns.forEachIndexed { index, pattern ->
        val match = pattern.find(line) ?: return@forEachIndexed
        val day = (
            if (index == 0) match.groupValues[1].toIntOrNull() else match.groupValues[2].toIntOrNull()
            ) ?: return@forEachIndexed
        val monthText = if (index == 0) match.groupValues[2] else match.groupValues[1]
        val month = monthNames[monthText.lowercase(Locale.ROOT)] ?: return@forEachIndexed
        val year = normalizeYear(match.groupValues[3])
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }
    return null
}

private fun parseNumericDate(line: String): LocalDate? {
    val match = dayFirstDatePattern.find(line) ?: return null
    val day = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    val year = normalizeYear(match.groupValues[3])
    return try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        null
    }
}

private fun parseAmount(amount: String): BigDecimal? {
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
    return "$integer.$cents".toBigDecimalOrNull()
}

private fun BigDecimal.toPlainAmount(): String =
    setScale(2).toPlainString()

private fun normalizeYear(rawYear: String): Int =
    if (rawYear.length == 2) "20$rawYear".toInt() else rawYear.toInt()

private fun matchesAnyKeyword(line: String, keywords: List<String>): Boolean =
    keywords.any { keyword -> keyword.toKeywordRegex().containsMatchIn(line) }

private fun String.toKeywordRegex(): Regex {
    val phrase = trim()
        .split(Regex("""\s+"""))
        .joinToString("""\s+""") { Regex.escape(it) }
    return Regex("""(?<![\p{L}\p{N}])$phrase(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)
}
