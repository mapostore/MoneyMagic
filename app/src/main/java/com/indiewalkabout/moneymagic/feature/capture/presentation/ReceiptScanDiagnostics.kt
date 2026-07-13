package com.indiewalkabout.moneymagic.feature.capture.presentation

import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDocumentType

data class ReceiptScanDiagnostics(
    val source: ReceiptScanSource,
    val documentType: ReceiptDocumentType,
    val lineCount: Int,
    val blockCount: Int,
    val elementCount: Int,
    val characterCount: Int,
)

enum class ReceiptScanSource {
    Image,
    Camera,
}
