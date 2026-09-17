package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class ReceiptCaptureTarget(
    val uri: Uri,
    val path: String,
)

fun Context.createReceiptCaptureTarget(): ReceiptCaptureTarget {
    val imageDirectory = File(cacheDir, "receipt_images").apply { mkdirs() }
    val imageFile = File.createTempFile("receipt-", ".jpg", imageDirectory)
    return ReceiptCaptureTarget(
        uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile,
        ),
        path = imageFile.path,
    )
}

fun deleteReceiptCaptureFile(path: String?): Boolean =
    path?.let { File(it).delete() } ?: false
