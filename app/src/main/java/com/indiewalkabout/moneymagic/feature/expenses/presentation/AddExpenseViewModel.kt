package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.usecase.ExpenseValidationError
import com.indiewalkabout.moneymagic.feature.expenses.domain.usecase.ValidateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddExpenseUiState(
    val amount: String = "",
    val categoryId: Long? = null,
    val merchant: String = "",
    val notes: String = "",
    val canSave: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val validateExpense: ValidateExpenseUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    fun onAmountChanged(amount: String) {
        _uiState.update { state ->
            state.copy(amount = amount, isSaved = false).withSaveEligibility()
        }
    }

    fun onCategorySelected(categoryId: Long?) {
        _uiState.update { state ->
            state.copy(categoryId = categoryId, isSaved = false).withSaveEligibility()
        }
    }

    fun onMerchantChanged(merchant: String) {
        _uiState.update { state ->
            state.copy(merchant = merchant, isSaved = false)
        }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { state ->
            state.copy(notes = notes, isSaved = false)
        }
    }

    fun save() {
        val state = uiState.value
        if (state.isSaving) {
            return
        }

        val validation = validateExpense(state.amount, state.categoryId)
        if (validation.errors.isNotEmpty() || validation.amountMinor == null) {
            _uiState.update {
                it.copy(
                    canSave = false,
                    errorMessage = validation.errors.firstOrNull()?.toMessage(),
                    isSaving = false,
                    isSaved = false,
                )
            }
            return
        }

        _uiState.update {
            it.copy(errorMessage = null, isSaving = true, isSaved = false)
        }

        viewModelScope.launch {
            val now = Instant.now()
            runCatching {
                expenseRepository.save(
                    Expense(
                        amountMinor = validation.amountMinor,
                        currency = "EUR",
                        dateTime = now,
                        categoryId = requireNotNull(state.categoryId),
                        merchant = state.merchant.trim(),
                        paymentMethodId = null,
                        notes = state.notes.trim(),
                        tags = emptyList(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(isSaving = false, isSaved = true, errorMessage = null)
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSaved = false,
                        errorMessage = "Unable to save expense. Please try again.",
                    )
                }
            }
        }
    }

    private fun AddExpenseUiState.withSaveEligibility(): AddExpenseUiState {
        val validation = validateExpense(amount, categoryId)
        return copy(
            canSave = validation.errors.isEmpty() && !isSaving,
            errorMessage = null,
        )
    }
}

private fun ExpenseValidationError.toMessage(): String =
    when (this) {
        ExpenseValidationError.EmptyAmount -> "Enter an amount."
        ExpenseValidationError.AmountMustBePositive -> "Amount must be greater than zero."
        ExpenseValidationError.MissingCategory -> "Choose a category."
        ExpenseValidationError.InvalidAmount -> "Enter a valid amount."
    }
