package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.PaymentMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExpensePeriodFilter {
    ALL,
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

enum class ExpenseSortOption {
    NEWEST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT,
}

data class ExpensesUiState(
    val expenses: List<ExpenseListItem> = emptyList(),
    val selectedPeriodFilter: ExpensePeriodFilter = ExpensePeriodFilter.ALL,
    val selectedSortOption: ExpenseSortOption = ExpenseSortOption.NEWEST,
)

data class ExpenseListItem(
    val expense: Expense,
    val categoryName: String,
    val paymentMethodName: String?,
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    paymentMethodRepository: PaymentMethodRepository,
    private val clock: Clock,
) : ViewModel() {
    private val selectedPeriodFilter = MutableStateFlow(ExpensePeriodFilter.ALL)
    private val selectedSortOption = MutableStateFlow(ExpenseSortOption.NEWEST)

    val uiState: StateFlow<ExpensesUiState> =
        combine(
            expenseRepository.observeExpenses(),
            categoryRepository.observeCategories(),
            paymentMethodRepository.observePaymentMethods(),
            selectedPeriodFilter,
            selectedSortOption,
        ) { expenses, categories, paymentMethods, periodFilter, sortOption ->
            ExpensesUiState(
                expenses = expenses
                    .filterByPeriod(periodFilter, clock)
                    .sortedBy(sortOption)
                    .toListItems(categories, paymentMethods),
                selectedPeriodFilter = periodFilter,
                selectedSortOption = sortOption,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExpensesUiState(),
            )

    fun selectPeriodFilter(filter: ExpensePeriodFilter) {
        selectedPeriodFilter.update { filter }
    }

    fun selectSortOption(sortOption: ExpenseSortOption) {
        selectedSortOption.update { sortOption }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseRepository.delete(expenseId)
        }
    }
}

private fun List<Expense>.filterByPeriod(
    periodFilter: ExpensePeriodFilter,
    clock: Clock,
): List<Expense> {
    if (periodFilter == ExpensePeriodFilter.ALL) {
        return this
    }

    val anchor = LocalDate.now(clock)
    return filter { expense ->
        val date = expense.dateTime.atZone(clock.zoneOrDefault()).toLocalDate()
        when (periodFilter) {
            ExpensePeriodFilter.ALL -> true
            ExpensePeriodFilter.DAY -> date == anchor
            ExpensePeriodFilter.WEEK -> date in rangeFor(BudgetPeriod.Weekly, anchor)
            ExpensePeriodFilter.MONTH -> date in rangeFor(BudgetPeriod.Monthly, anchor)
            ExpensePeriodFilter.YEAR -> date in rangeFor(BudgetPeriod.Yearly, anchor)
        }
    }
}

private fun List<Expense>.sortedBy(sortOption: ExpenseSortOption): List<Expense> =
    when (sortOption) {
        ExpenseSortOption.NEWEST -> sortedByDescending { it.dateTime }
        ExpenseSortOption.HIGHEST_AMOUNT -> sortedByDescending { it.amountMinor }
        ExpenseSortOption.LOWEST_AMOUNT -> sortedBy { it.amountMinor }
    }

private fun List<Expense>.toListItems(
    categories: List<Category>,
    paymentMethods: List<PaymentMethod>,
): List<ExpenseListItem> {
    val categoriesById = categories.associateBy { it.id }
    val paymentMethodsById = paymentMethods.associateBy { it.id }
    return map { expense ->
        ExpenseListItem(
            expense = expense,
            categoryName = categoriesById[expense.categoryId]?.name.orEmpty(),
            paymentMethodName = expense.paymentMethodId?.let { paymentMethodsById[it]?.name },
        )
    }
}

private fun rangeFor(period: BudgetPeriod, anchor: LocalDate): ClosedRange<LocalDate> =
    when (period) {
        BudgetPeriod.Weekly -> {
            val start = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
            start..start.plusDays(6)
        }
        BudgetPeriod.Monthly -> anchor.withDayOfMonth(1)..anchor.withDayOfMonth(anchor.lengthOfMonth())
        BudgetPeriod.Yearly -> anchor.withDayOfYear(1)..anchor.withDayOfYear(anchor.lengthOfYear())
        is BudgetPeriod.Custom -> period.start..period.endInclusive
    }

private fun Clock.zoneOrDefault(): ZoneId = zone ?: ZoneId.systemDefault()
