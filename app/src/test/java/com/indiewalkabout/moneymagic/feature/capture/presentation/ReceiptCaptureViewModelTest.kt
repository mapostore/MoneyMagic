package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.graphics.Bitmap
import android.net.Uri
import com.indiewalkabout.moneymagic.feature.capture.domain.repository.ReceiptTextRecognizer
import com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReceiptCaptureViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankOcrTextShowsNoTextErrorWithoutDraft() = runTest {
        val viewModel = ReceiptCaptureViewModel(FakeRecognizer(Result.success("")), ParseReceiptTextUseCase())

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.rawText)
        assertNull(viewModel.uiState.value.draft)
        assertEquals(ReceiptCaptureError.NoTextFound, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun nonblankWeakOcrTextPreservesRawTextAndDraft() = runTest {
        val viewModel = ReceiptCaptureViewModel(FakeRecognizer(Result.success("unstructured words only")), ParseReceiptTextUseCase())

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals("unstructured words only", viewModel.uiState.value.rawText)
        assertNotNull(viewModel.uiState.value.draft)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private class FakeRecognizer(
        private val result: Result<String>,
    ) : ReceiptTextRecognizer {
        override suspend fun recognize(uri: Uri): Result<String> = result
        override suspend fun recognize(bitmap: Bitmap): Result<String> = result
    }
}
