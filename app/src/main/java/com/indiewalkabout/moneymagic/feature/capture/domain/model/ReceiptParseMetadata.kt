package com.indiewalkabout.moneymagic.feature.capture.domain.model

data class ReceiptParseMetadata(
    val merchantConfidence: ReceiptFieldConfidence = ReceiptFieldConfidence.Missing,
    val amountConfidence: ReceiptFieldConfidence = ReceiptFieldConfidence.Missing,
    val dateConfidence: ReceiptFieldConfidence = ReceiptFieldConfidence.Missing,
)
