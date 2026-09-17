package com.indiewalkabout.moneymagic.feature.capture.presentation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptCaptureFileStoreTest {
    @Test
    fun deleteReceiptCaptureFileRemovesExistingFile() {
        val file = File.createTempFile("receipt-capture-test-", ".jpg")
        assertTrue(file.exists())

        val deleted = deleteReceiptCaptureFile(file.path)

        assertTrue(deleted)
        assertFalse(file.exists())
    }

    @Test
    fun deleteReceiptCaptureFileIgnoresMissingPath() {
        assertFalse(deleteReceiptCaptureFile(null))
    }
}
