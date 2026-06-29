package com.indiewalkabout.moneymagic.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.core.time.PeriodCalculator
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetRepository
import com.indiewalkabout.moneymagic.feature.budgets.domain.usecase.CalculateBudgetProgressUseCase
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.presentation.common.calculateCurrentBudgetProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val recentExpenses: List<Expense> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val topCategories: List<CategorySpend> = emptyList(),
)

data class CategorySpend(
    val category: Category,
    val amountMinor: Long,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    expenseRepository: ExpenseRepository,
    budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    calculateBudgetProgress: CalculateBudgetProgressUseCase,
    clock: Clock,
    periodCalculator: PeriodCalculator = PeriodCalculator(),
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> =
        combine(
            expenseRepository.observeExpenses(),
            budgetRepository.observeBudgets(),
            categoryRepository.observeCategories(),
        ) { expenses, budgets, categories ->
            DashboardUiState(
                recentExpenses = expenses.sortedByDescending { it.dateTime }.take(5),
                budgetProgress = calculateCurrentBudgetProgress(
                    budgets = budgets,
                    expenses = expenses,
                    calculateBudgetProgress = calculateBudgetProgress,
                    periodCalculator = periodCalculator,
                    clock = clock,
                ).take(3),
                topCategories = expenses
                    .groupBy { it.categoryId }
                    .mapNotNull { (categoryId, categoryExpenses) ->
                        categories.firstOrNull { it.id == categoryId }?.let { category ->
                            CategorySpend(
                                category = category,
                                amountMinor = categoryExpenses.sumOf { it.amountMinor },
                            )
                        }
                    }
                    .sortedByDescending { it.amountMinor }
                    .take(3),
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardUiState(),
            )
}
