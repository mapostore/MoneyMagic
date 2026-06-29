package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
import com.indiewalkabout.moneymagic.feature.capture.domain.repository.ReceiptTextRecognizer
import com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptCaptureUiState(
    val isScanning: Boolean = false,
    val draft: ReceiptDraft? = null,
    val rawText: String = "",
    val errorMessage: ReceiptCaptureError? = null,
)

enum class ReceiptCaptureError {
    ScanFailed,
    NoTextFound,
}

@HiltViewModel
class ReceiptCaptureViewModel @Inject constructor(
    private val recognizer: ReceiptTextRecognizer,
    private val parseReceiptText: ParseReceiptTextUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiptCaptureUiState())
    val uiState: StateFlow<ReceiptCaptureUiState> = _uiState.asStateFlow()

    fun scanUri(uri: Uri) {
        scan { recognizer.recognize(uri) }
    }

    fun scanBitmap(bitmap: Bitmap) {
        scan { recognizer.recognize(bitmap) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun scan(recognize: suspend () -> Result<String>) {
        if (uiState.value.isScanning) {
            return
        }
        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
        viewModelScope.launch {
            recognize()
                .onSuccess { text ->
                    val draft = parseReceiptText(text)
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            draft = draft.takeIf { parsed -> parsed.rawText.isNotBlank() },
                            rawText = text,
                            errorMessage = if (text.isBlank()) ReceiptCaptureError.NoTextFound else null,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isScanning = false, errorMessage = ReceiptCaptureError.ScanFailed)
                    }
                }
        }
    }
}
