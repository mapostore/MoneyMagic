package com.indiewalkabout.moneymagic.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.PaymentMethodRepository
import com.indiewalkabout.moneymagic.feature.settings.domain.usecase.ExportExpensesXlsxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val categoryName: String = "",
    val editingCategoryId: Long? = null,
    val paymentMethodName: String = "",
    val paymentMethodType: PaymentMethodType = PaymentMethodType.Card,
    val editingPaymentMethodId: Long? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val expenseRepository: ExpenseRepository,
    private val exportExpensesXlsx: ExportExpensesXlsxUseCase,
) : ViewModel() {
    private val formState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> =
        combine(
            formState.asStateFlow(),
            categoryRepository.observeCategories(),
            paymentMethodRepository.observePaymentMethods(),
            expenseRepository.observeExpenses(),
        ) { form, categories, paymentMethods, expenses ->
            form.copy(
                categories = categories,
                paymentMethods = paymentMethods,
                expenses = expenses,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onCategoryNameChanged(name: String) {
        formState.update { it.copy(categoryName = name) }
    }

    fun editCategory(category: Category) {
        formState.update {
            it.copy(
                categoryName = category.name,
                editingCategoryId = category.id,
            )
        }
    }

    fun clearCategoryForm() {
        formState.update { it.copy(categoryName = "", editingCategoryId = null) }
    }

    fun saveCategory() {
        val state = formState.value
        val name = state.categoryName.trim()
        if (name.isBlank()) {
            return
        }

        viewModelScope.launch {
            val existing = state.editingCategoryId?.let { id ->
                uiState.value.categories.firstOrNull { it.id == id }
            }
            categoryRepository.save(
                Category(
                    id = state.editingCategoryId ?: 0,
                    name = name,
                    color = existing?.color ?: 0xFF6B7280,
                    iconKey = existing?.iconKey ?: "custom",
                    sortOrder = existing?.sortOrder ?: uiState.value.categories.size,
                    archived = false,
                ),
            )
            clearCategoryForm()
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.archive(categoryId)
            if (formState.value.editingCategoryId == categoryId) {
                clearCategoryForm()
            }
        }
    }

    fun onPaymentMethodNameChanged(name: String) {
        formState.update { it.copy(paymentMethodName = name) }
    }

    fun onPaymentMethodTypeChanged(type: PaymentMethodType) {
        formState.update { it.copy(paymentMethodType = type) }
    }

    fun editPaymentMethod(paymentMethod: PaymentMethod) {
        formState.update {
            it.copy(
                paymentMethodName = paymentMethod.name,
                paymentMethodType = paymentMethod.type,
                editingPaymentMethodId = paymentMethod.id,
            )
        }
    }

    fun clearPaymentMethodForm() {
        formState.update {
            it.copy(
                paymentMethodName = "",
                paymentMethodType = PaymentMethodType.Card,
                editingPaymentMethodId = null,
            )
        }
    }

    fun savePaymentMethod() {
        val state = formState.value
        val name = state.paymentMethodName.trim()
        if (name.isBlank()) {
            return
        }

        viewModelScope.launch {
            paymentMethodRepository.save(
                PaymentMethod(
                    id = state.editingPaymentMethodId ?: 0,
                    name = name,
                    type = state.paymentMethodType,
                    archived = false,
                ),
            )
            clearPaymentMethodForm()
        }
    }

    fun deletePaymentMethod(paymentMethodId: Long) {
        viewModelScope.launch {
            paymentMethodRepository.archive(paymentMethodId)
            if (formState.value.editingPaymentMethodId == paymentMethodId) {
                clearPaymentMethodForm()
            }
        }
    }

    fun createExpenseExport(): ByteArray = exportExpensesXlsx(uiState.value.expenses)
}
