package com.indiewalkabout.moneymagic.feature.capture.domain.model

data class ReceiptOcrResult(
    val rawText: String,
    val blocks: List<ReceiptOcrBlock> = emptyList(),
)

data class ReceiptOcrBlock(
    val text: String,
    val lines: List<ReceiptOcrLine> = emptyList(),
)

data class ReceiptOcrLine(
    val text: String,
    val elements: List<String> = emptyList(),
)
