package com.indiewalkabout.moneymagic.feature.capture.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.indiewalkabout.moneymagic.feature.capture.domain.repository.ReceiptTextRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class LocalReceiptTextRecognizer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReceiptTextRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(uri: Uri): Result<String> =
        runCatching {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image).await().text
        }

    override suspend fun recognize(bitmap: Bitmap): Result<String> =
        runCatching {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image).await().text
        }
}

private suspend fun Task<Text>.await(): Text =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { text -> continuation.resume(text) }
        addOnFailureListener { error -> continuation.resumeWith(Result.failure(error)) }
    }
