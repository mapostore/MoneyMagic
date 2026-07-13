package com.indiewalkabout.moneymagic.feature.capture.domain.usecase

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrResult
import java.util.Locale
import javax.inject.Inject

class ClassifyReceiptDocumentUseCase @Inject constructor() {
    operator fun invoke(ocrResult: ReceiptOcrResult): ReceiptDocumentType {
        val searchableText = ocrResult.searchableText()
        if (searchableText.isBlank()) return ReceiptDocumentType.Unknown

        val billScore = billSignals.sumOf { signal -> signal.scoreIn(searchableText) }
        val receiptScore = receiptSignals.sumOf { signal -> signal.scoreIn(searchableText) }

        return when {
            billScore >= 4 && billScore > receiptScore -> ReceiptDocumentType.BillInvoice
            receiptScore >= 4 && receiptScore >= billScore -> ReceiptDocumentType.Receipt
            else -> ReceiptDocumentType.Unknown
        }
    }
}

private data class DocumentSignal(
    val keyword: String,
    val weight: Int,
) {
    fun scoreIn(text: String): Int =
        if (keyword.toKeywordRegex().containsMatchIn(text)) weight else 0
}

private val receiptSignals = listOf(
    DocumentSignal("documento commerciale", 4),
    DocumentSignal("scontrino", 4),
    DocumentSignal("ricevuta fiscale", 4),
    DocumentSignal("receipt", 4),
    DocumentSignal("tax receipt", 4),
    DocumentSignal("totale complessivo", 2),
    DocumentSignal("grand total", 2),
    DocumentSignal("pagamento elettronico", 2),
    DocumentSignal("cashier", 2),
    DocumentSignal("cassa", 2),
)

private val billSignals = listOf(
    DocumentSignal("bolletta", 4),
    DocumentSignal("fattura", 4),
    DocumentSignal("invoice", 4),
    DocumentSignal("statement", 3),
    DocumentSignal("importo da pagare", 4),
    DocumentSignal("amount due", 4),
    DocumentSignal("balance due", 4),
    DocumentSignal("scadenza", 3),
    DocumentSignal("due date", 3),
    DocumentSignal("data emissione", 2),
    DocumentSignal("statement date", 2),
    DocumentSignal("codice bollettino", 2),
)

private fun ReceiptOcrResult.searchableText(): String {
    val structuredText = blocks
        .flatMap { block -> block.lines }
        .joinToString("\n") { line -> line.text }
    return listOf(rawText, structuredText)
        .filter(String::isNotBlank)
        .joinToString("\n")
        .lowercase(Locale.ROOT)
}

private fun String.toKeywordRegex(): Regex {
    val phrase = trim()
        .split(Regex("""\s+"""))
        .joinToString("""\s+""") { Regex.escape(it) }
    return Regex("""(?<![\p{L}\p{N}])$phrase(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)
}
