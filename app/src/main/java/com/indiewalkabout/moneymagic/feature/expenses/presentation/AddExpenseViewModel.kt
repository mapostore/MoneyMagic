package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.PaymentMethodRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.usecase.ExpenseValidationError
import com.indiewalkabout.moneymagic.feature.expenses.domain.usecase.ValidateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddExpenseUiState(
    val name: String = "",
    val amount: String = "",
    val categoryId: Long? = null,
    val paymentMethodId: Long? = null,
    val date: String = "",
    val time: String = "",
    val merchant: String = "",
    val notes: String = "",
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val canSave: Boolean = false,
    val errorMessage: AddExpenseError? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

data class ExpenseDraftInput(
    val name: String? = null,
    val amount: String? = null,
    val date: String? = null,
    val merchant: String? = null,
    val notes: String? = null,
)

enum class AddExpenseError {
    EnterAmount,
    AmountMustBePositive,
    MissingCategory,
    InvalidAmount,
    InvalidDateTime,
    SaveFailed,
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val validateExpense: ValidateExpenseUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    init {
        val now = LocalDate.now(clock)
        val currentTime = LocalTime.now(clock).withSecond(0).withNano(0)
        _uiState.update {
            it.copy(date = now.toString(), time = currentTime.toString())
        }
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { state ->
                    state.copy(categories = categories).withSaveEligibility()
                }
            }
        }
        viewModelScope.launch {
            paymentMethodRepository.observePaymentMethods().collect { paymentMethods ->
                _uiState.update { state ->
                    state.copy(paymentMethods = paymentMethods)
                }
            }
        }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { state ->
            state.copy(amount = amount, isSaved = false).withSaveEligibility()
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { state ->
            state.copy(name = name, isSaved = false)
        }
    }

    fun onCategorySelected(categoryId: Long?) {
        _uiState.update { state ->
            state.copy(categoryId = categoryId, isSaved = false).withSaveEligibility()
        }
    }

    fun onPaymentMethodSelected(paymentMethodId: Long?) {
        _uiState.update { state ->
            state.copy(paymentMethodId = paymentMethodId, isSaved = false)
        }
    }

    fun onDateChanged(date: String) {
        _uiState.update { state ->
            state.copy(date = date, isSaved = false, errorMessage = null).withSaveEligibility()
        }
    }

    fun onTimeChanged(time: String) {
        _uiState.update { state ->
            state.copy(time = time, isSaved = false, errorMessage = null).withSaveEligibility()
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

    fun applyDraft(draft: ExpenseDraftInput) {
        _uiState.update { state ->
            state.copy(
                name = draft.name?.takeIf { it.isNotBlank() } ?: state.name,
                amount = draft.amount?.takeIf { it.isNotBlank() } ?: state.amount,
                date = draft.date?.takeIf { it.isNotBlank() } ?: state.date,
                merchant = draft.merchant?.takeIf { it.isNotBlank() } ?: state.merchant,
                notes = draft.notes?.takeIf { it.isNotBlank() } ?: state.notes,
                isSaved = false,
                errorMessage = null,
            ).withSaveEligibility()
        }
    }

    fun save() {
        val state = uiState.value
        if (state.isSaving) {
            return
        }

        val validation = validateExpense(state.amount, state.categoryId)
        val dateTime = state.toInstantOrNull()
        if (validation.errors.isNotEmpty() || validation.amountMinor == null || dateTime == null) {
            _uiState.update {
                it.copy(
                    canSave = false,
                    errorMessage = validation.errors.firstOrNull()?.toError() ?: AddExpenseError.InvalidDateTime,
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
            val now = Instant.now(clock)
            runCatching {
                expenseRepository.save(
                    Expense(
                        name = state.name.trim(),
                        amountMinor = validation.amountMinor,
                        currency = "EUR",
                        dateTime = dateTime,
                        categoryId = requireNotNull(state.categoryId),
                        merchant = state.merchant.trim(),
                        paymentMethodId = state.paymentMethodId,
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
                        errorMessage = AddExpenseError.SaveFailed,
                    )
                }
            }
        }
    }

    private fun AddExpenseUiState.withSaveEligibility(): AddExpenseUiState {
        val validation = validateExpense(amount, categoryId)
        return copy(
            canSave = validation.errors.isEmpty() && toInstantOrNull() != null && !isSaving,
            errorMessage = null,
        )
    }
}

private fun AddExpenseUiState.toInstantOrNull(): Instant? =
    try {
        val parsedDate = LocalDate.parse(date)
        val parsedTime = LocalTime.parse(time)
        parsedDate
            .atTime(parsedTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    } catch (_: DateTimeParseException) {
        null
    }

private fun ExpenseValidationError.toError(): AddExpenseError =
    when (this) {
        ExpenseValidationError.EmptyAmount -> AddExpenseError.EnterAmount
        ExpenseValidationError.AmountMustBePositive -> AddExpenseError.AmountMustBePositive
        ExpenseValidationError.MissingCategory -> AddExpenseError.MissingCategory
        ExpenseValidationError.InvalidAmount -> AddExpenseError.InvalidAmount
    }
