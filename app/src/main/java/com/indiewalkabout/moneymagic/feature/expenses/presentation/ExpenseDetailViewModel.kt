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
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseDetailUiState(
    val expenseId: Long? = null,
    val isLoading: Boolean = true,
    val missingExpense: Boolean = false,
    val name: String = "",
    val amount: String = "",
    val categoryId: Long? = null,
    val paymentMethodId: Long? = null,
    val date: String = "",
    val time: String = "",
    val merchant: String = "",
    val description: String = "",
    val notes: String = "",
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val canSave: Boolean = false,
    val errorMessage: ExpenseDetailError? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showMissingFieldsDialog: Boolean = false,
    val missingFieldErrors: List<ExpenseDetailError> = emptyList(),
    val isDeleted: Boolean = false,
)

enum class ExpenseDetailError {
    EnterAmount,
    AmountMustBePositive,
    MissingCategory,
    InvalidAmount,
    InvalidDateTime,
    SaveFailed,
    DeleteFailed,
}

enum class ExpenseDetailEvent {
    Saved,
    Deleted,
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val validateExpense: ValidateExpenseUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val expenseId = MutableStateFlow<Long?>(null)
    private val _uiState = MutableStateFlow(ExpenseDetailUiState())
    private val _events = MutableSharedFlow<ExpenseDetailEvent>()
    val uiState: StateFlow<ExpenseDetailUiState> = _uiState.asStateFlow()
    val events: SharedFlow<ExpenseDetailEvent> = _events.asSharedFlow()
    private var loadedExpense: Expense? = null
    private var descriptionEditedByUser = false
    private var categorySelectedByUser = false

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { state ->
                    state
                        .copy(categories = categories)
                        .withSuggestedCategoryIfNeeded()
                        .withSaveEligibility()
                }
            }
        }
        viewModelScope.launch {
            paymentMethodRepository.observePaymentMethods().collect { paymentMethods ->
                _uiState.update { state -> state.copy(paymentMethods = paymentMethods) }
            }
        }
        viewModelScope.launch {
            expenseId
                .flatMapLatest { id -> id?.let(expenseRepository::observeExpense) ?: flowOf(null) }
                .collect { expense ->
                    if (expense == null) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = expenseId.value == null,
                                missingExpense = expenseId.value != null,
                            )
                        }
                    } else if (loadedExpense?.id != expense.id || !_uiState.value.isSaving) {
                        loadedExpense = expense
                        _uiState.update { state ->
                            expense
                                .toUiState(
                                    categories = state.categories,
                                    paymentMethods = state.paymentMethods,
                                    shouldAutoDescription = !descriptionEditedByUser,
                                )
                                .copy(
                                    errorMessage = state.errorMessage,
                                    isSaved = state.isSaved,
                                    isDeleting = state.isDeleting,
                                    showDeleteConfirmation = state.showDeleteConfirmation,
                                    showMissingFieldsDialog = state.showMissingFieldsDialog,
                                    missingFieldErrors = state.missingFieldErrors,
                                    isDeleted = state.isDeleted,
                                )
                                .withSaveEligibility()
                        }
                    }
                }
        }
    }

    fun load(expenseId: Long) {
        if (this.expenseId.value == expenseId) {
            return
        }
        val currentState = _uiState.value
        this.expenseId.value = expenseId
        loadedExpense = null
        descriptionEditedByUser = false
        categorySelectedByUser = false
        _uiState.update {
            ExpenseDetailUiState(
                expenseId = expenseId,
                categories = currentState.categories,
                paymentMethods = currentState.paymentMethods,
            )
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(name = name, isSaved = false, errorMessage = null)
                .withSuggestedCategoryIfNeeded()
                .withSaveEligibility()
        }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(amount = amount, isSaved = false, errorMessage = null).withSaveEligibility() }
    }

    fun onCategorySelected(categoryId: Long?) {
        categorySelectedByUser = true
        _uiState.update { it.copy(categoryId = categoryId, isSaved = false, errorMessage = null).withSaveEligibility() }
    }

    fun onPaymentMethodSelected(paymentMethodId: Long?) {
        _uiState.update { it.copy(paymentMethodId = paymentMethodId, isSaved = false, errorMessage = null) }
    }

    fun onDateChanged(date: String) {
        _uiState.update { it.copy(date = date, isSaved = false, errorMessage = null).withSaveEligibility() }
    }

    fun onTimeChanged(time: String) {
        _uiState.update { it.copy(time = time, isSaved = false, errorMessage = null).withSaveEligibility() }
    }

    fun onMerchantChanged(merchant: String) {
        _uiState.update {
            it.copy(merchant = merchant, isSaved = false, errorMessage = null)
                .withSuggestedCategoryIfNeeded()
        }
    }

    fun onDescriptionChanged(description: String) {
        descriptionEditedByUser = true
        _uiState.update {
            it.copy(description = description, isSaved = false, errorMessage = null)
                .withSuggestedCategoryIfNeeded()
        }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update {
            it.copy(notes = notes, isSaved = false, errorMessage = null)
                .withSuggestedCategoryIfNeeded()
        }
    }

    fun save() {
        val original = loadedExpense ?: return
        val state = uiState.value
        if (state.isSaving || state.isDeleting) {
            return
        }

        val validation = validateExpense(state.amount, state.categoryId)
        val dateTime = state.toInstantOrNull()
        if (validation.errors.isNotEmpty() || validation.amountMinor == null || dateTime == null) {
            val fieldErrors = validation.errors.map { it.toError() } +
                listOfNotNull(ExpenseDetailError.InvalidDateTime.takeIf { dateTime == null })
            _uiState.update {
                it.copy(
                    canSave = false,
                    errorMessage = fieldErrors.firstOrNull(),
                    showMissingFieldsDialog = true,
                    missingFieldErrors = fieldErrors.distinct(),
                    isSaved = false,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessage = null,
                showMissingFieldsDialog = false,
                missingFieldErrors = emptyList(),
                isSaved = false,
            )
        }
        viewModelScope.launch {
            runCatching {
                expenseRepository.save(
                    original.copy(
                        name = state.name.trim(),
                        amountMinor = validation.amountMinor,
                        dateTime = dateTime,
                        categoryId = requireNotNull(state.categoryId),
                        merchant = state.merchant.trim(),
                        paymentMethodId = state.paymentMethodId,
                        description = state.description.trim(),
                        notes = state.notes.trim(),
                        updatedAt = Instant.now(clock),
                    ),
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, isSaved = true, errorMessage = null) }
                _events.emit(ExpenseDetailEvent.Saved)
            }.onFailure {
                _uiState.update {
                    it.copy(isSaving = false, isSaved = false, errorMessage = ExpenseDetailError.SaveFailed)
                }
            }
        }
    }

    fun requestDeleteConfirmation() {
        val state = uiState.value
        if (state.expenseId == null || state.isDeleting || state.isSaving) {
            return
        }
        _uiState.update { it.copy(showDeleteConfirmation = true, errorMessage = null) }
    }

    fun dismissDeleteConfirmation() {
        if (uiState.value.isDeleting) {
            return
        }
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun dismissMissingFieldsDialog() {
        _uiState.update { it.copy(showMissingFieldsDialog = false) }
    }

    fun confirmDelete() {
        val id = uiState.value.expenseId ?: return
        if (uiState.value.isDeleting || uiState.value.isSaving) {
            return
        }
        _uiState.update { it.copy(isDeleting = true, showDeleteConfirmation = false, errorMessage = null) }
        viewModelScope.launch {
            runCatching { expenseRepository.delete(id) }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isDeleted = true, errorMessage = null) }
                    _events.emit(ExpenseDetailEvent.Deleted)
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isDeleting = false, isDeleted = false, errorMessage = ExpenseDetailError.DeleteFailed)
                    }
                }
        }
    }

    private fun ExpenseDetailUiState.withSaveEligibility(): ExpenseDetailUiState {
        val validation = validateExpense(amount, categoryId)
        return copy(
            canSave = !isLoading &&
                !missingExpense &&
                validation.errors.isEmpty() &&
                toInstantOrNull() != null &&
                !isSaving &&
                !isDeleting,
        )
    }

    private fun ExpenseDetailUiState.withSuggestedCategoryIfNeeded(): ExpenseDetailUiState {
        if (categories.isEmpty() || categorySelectedByUser || categoryId != null && categories.any { it.id == categoryId }) {
            return this
        }
        val suggestedCategoryId = suggestCategoryId(
            categories = categories,
            text = listOf(name, merchant, description, notes).joinToString(" "),
        ) ?: return this
        return copy(categoryId = suggestedCategoryId)
    }
}

private fun suggestCategoryId(categories: List<Category>, text: String): Long? {
    val normalizedText = text.normalizedCategoryText()
    if (normalizedText.isBlank()) return null

    return categories
        .mapNotNull { category ->
            val normalizedName = category.name.normalizedCategoryText()
            if (normalizedName.isBlank()) {
                null
            } else {
                val score = when {
                    normalizedText.contains(normalizedName) -> 100 + normalizedName.length
                    normalizedName.split(' ').filter { it.length >= 4 }.any { normalizedText.contains(it) } -> 50
                    else -> 0
                }
                score.takeIf { it > 0 }?.let { it to category.id }
            }
        }
        .maxByOrNull { it.first }
        ?.second
}

private fun String.normalizedCategoryText(): String =
    lowercase(Locale.ROOT)
        .replace('_', ' ')
        .replace(Regex("[^a-z0-9àèéìòù]+"), " ")
        .trim()

private fun Expense.toUiState(
    categories: List<Category>,
    paymentMethods: List<PaymentMethod>,
    shouldAutoDescription: Boolean,
): ExpenseDetailUiState {
    val zonedDateTime = dateTime.atZone(ZoneId.systemDefault())
    val displayDate = zonedDateTime.toLocalDate().toString()
    val displayTime = zonedDateTime.toLocalTime().withSecond(0).withNano(0).toString()
    val categoryName = categories.firstOrNull { it.id == categoryId }?.name.orEmpty()
    return ExpenseDetailUiState(
        expenseId = id,
        isLoading = false,
        missingExpense = false,
        name = name,
        amount = "%.2f".format(Locale.US, amountMinor / 100.0),
        categoryId = categoryId,
        paymentMethodId = paymentMethodId,
        date = displayDate,
        time = displayTime,
        merchant = merchant,
        description = description.ifBlank {
            if (shouldAutoDescription) {
                buildAutoDescription(categoryName = categoryName, date = displayDate, time = displayTime)
            } else {
                ""
            }
        },
        notes = notes,
        categories = categories,
        paymentMethods = paymentMethods,
    )
}

private fun buildAutoDescription(categoryName: String, date: String, time: String): String =
    listOf(categoryName, date, time)
        .filter(String::isNotBlank)
        .joinToString(separator = " ", prefix = "  ")

private fun ExpenseDetailUiState.toInstantOrNull(): Instant? =
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

private fun ExpenseValidationError.toError(): ExpenseDetailError =
    when (this) {
        ExpenseValidationError.EmptyAmount -> ExpenseDetailError.EnterAmount
        ExpenseValidationError.AmountMustBePositive -> ExpenseDetailError.AmountMustBePositive
        ExpenseValidationError.MissingCategory -> ExpenseDetailError.MissingCategory
        ExpenseValidationError.InvalidAmount -> ExpenseDetailError.InvalidAmount
    }
