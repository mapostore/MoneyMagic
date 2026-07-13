package com.indiewalkabout.moneymagic.feature.capture.domain.model

data class ReceiptCandidates(
    val merchants: List<ReceiptCandidate> = emptyList(),
    val amounts: List<ReceiptCandidate> = emptyList(),
    val dates: List<ReceiptCandidate> = emptyList(),
)

data class ReceiptCandidate(
    val field: ReceiptCandidateField,
    val value: String,
    val label: String,
    val confidence: ReceiptFieldConfidence,
)

enum class ReceiptCandidateField {
    Merchant,
    Amount,
    Date,
}
