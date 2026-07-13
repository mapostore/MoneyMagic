package com.indiewalkabout.moneymagic.feature.capture.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptOcrResult

interface ReceiptTextRecognizer {
    suspend fun recognize(uri: Uri): Result<ReceiptOcrResult>
    suspend fun recognize(bitmap: Bitmap): Result<ReceiptOcrResult>
}
