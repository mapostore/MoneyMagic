package com.indiewalkabout.moneymagic.feature.capture.domain.repository

import android.graphics.Bitmap
import android.net.Uri

interface ReceiptTextRecognizer {
    suspend fun recognize(uri: Uri): Result<String>
    suspend fun recognize(bitmap: Bitmap): Result<String>
}
