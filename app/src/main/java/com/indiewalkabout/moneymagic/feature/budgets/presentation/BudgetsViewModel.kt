package com.indiewalkabout.moneymagic.feature.budgets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.core.time.PeriodCalculator
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.budgets.domain.usecase.CalculateBudgetProgressUseCase
import com.indiewalkabout.moneymagic.presentation.common.calculateCurrentBudgetProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetsUiState(
    val name: String = "",
    val amount: String = "",
    val thresholdPercent: String = "80",
    val selectedCategoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val canSave: Boolean = false,
    val errorMessage: BudgetError? = null,
    val isSaving: Boolean = false,
)

enum class BudgetError {
    InvalidForm,
    SaveFailed,
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    private val calculateBudgetProgress: CalculateBudgetProgressUseCase,
    private val clock: Clock,
    private val periodCalculator: PeriodCalculator = PeriodCalculator(),
) : ViewModel() {
    private val formState = MutableStateFlow(BudgetsUiState())

    val uiState: StateFlow<BudgetsUiState> =
        combine(
            formState.asStateFlow(),
            budgetRepository.observeBudgets(),
            expenseRepository.observeExpenses(),
            categoryRepository.observeCategories(),
        ) { form, budgets, expenses, categories ->
            form.copy(
                categories = categories,
                budgetProgress = calculateCurrentBudgetProgress(
                    budgets = budgets,
                    expenses = expenses,
                    calculateBudgetProgress = calculateBudgetProgress,
                    periodCalculator = periodCalculator,
                    clock = clock,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetsUiState(),
        )

    fun onNameChanged(name: String) {
        formState.update { state -> state.copy(name = name, errorMessage = null).withSaveEligibility() }
    }

    fun onAmountChanged(amount: String) {
        formState.update { state -> state.copy(amount = amount, errorMessage = null).withSaveEligibility() }
    }

    fun onThresholdChanged(thresholdPercent: String) {
        formState.update { state ->
            state.copy(thresholdPercent = thresholdPercent, errorMessage = null).withSaveEligibility()
        }
    }

    fun onCategorySelected(categoryId: Long?) {
        formState.update { state ->
            state.copy(selectedCategoryId = categoryId, errorMessage = null).withSaveEligibility()
        }
    }

    fun saveBudget() {
        val state = formState.value
        if (state.isSaving) {
            return
        }

        val amountMinor = parseAmountMinor(state.amount)
        val thresholdPercent = state.thresholdPercent.toIntOrNull()
        val name = state.name.trim()

        if (name.isBlank() || amountMinor == null || thresholdPercent == null || thresholdPercent !in 1..100) {
            formState.update {
                it.copy(
                    canSave = false,
                    errorMessage = BudgetError.InvalidForm,
                    isSaving = false,
                )
            }
            return
        }

        formState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                budgetRepository.save(
                    Budget(
                        name = name,
                        amountMinor = amountMinor,
                        currency = "EUR",
                        period = BudgetPeriod.Monthly,
                        categoryId = state.selectedCategoryId,
                        notificationThresholdPercent = thresholdPercent,
                        enabled = true,
                    ),
                )
            }.onSuccess {
                formState.update {
                    it.copy(
                        name = "",
                        amount = "",
                        thresholdPercent = "80",
                        selectedCategoryId = null,
                        canSave = false,
                        errorMessage = null,
                        isSaving = false,
                    )
                }
            }.onFailure {
                formState.update {
                    it.copy(
                        errorMessage = BudgetError.SaveFailed,
                        isSaving = false,
                    )
                }
            }
        }
    }

    private fun BudgetsUiState.withSaveEligibility(): BudgetsUiState =
        copy(
            canSave = name.isNotBlank() &&
                parseAmountMinor(amount) != null &&
                thresholdPercent.toIntOrNull() in 1..100 &&
                !isSaving,
        )
}

private fun parseAmountMinor(amountText: String): Long? {
    val amount = amountText.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    if (amount <= BigDecimal.ZERO || amount.scale() > 2) {
        return null
    }
    return runCatching { amount.movePointRight(2).longValueExact() }.getOrNull()
}
