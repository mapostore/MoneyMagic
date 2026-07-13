package com.indiewalkabout.moneymagic.feature.capture.domain.model

data class ReceiptDraft(
    val name: String = "",
    val merchant: String = "",
    val amount: String = "",
    val date: String = "",
    val rawText: String = "",
    val metadata: ReceiptParseMetadata = ReceiptParseMetadata(),
    val candidates: ReceiptCandidates = ReceiptCandidates(),
)
