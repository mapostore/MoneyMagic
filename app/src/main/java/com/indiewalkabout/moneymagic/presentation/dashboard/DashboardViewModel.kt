package com.indiewalkabout.moneymagic.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.core.time.PeriodCalculator
import com.indiewalkabout.moneymagic.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.domain.model.Expense
import com.indiewalkabout.moneymagic.domain.repository.BudgetRepository
import com.indiewalkabout.moneymagic.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.domain.usecase.CalculateBudgetProgressUseCase
import com.indiewalkabout.moneymagic.presentation.common.calculateCurrentBudgetProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val recentExpenses: List<Expense> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    expenseRepository: ExpenseRepository,
    budgetRepository: BudgetRepository,
    calculateBudgetProgress: CalculateBudgetProgressUseCase,
    clock: Clock,
    periodCalculator: PeriodCalculator = PeriodCalculator(),
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> =
        combine(
            expenseRepository.observeExpenses(),
            budgetRepository.observeBudgets(),
        ) { expenses, budgets ->
            DashboardUiState(
                recentExpenses = expenses.sortedByDescending { it.dateTime }.take(5),
                budgetProgress = calculateCurrentBudgetProgress(
                    budgets = budgets,
                    expenses = expenses,
                    calculateBudgetProgress = calculateBudgetProgress,
                    periodCalculator = periodCalculator,
                    clock = clock,
                ).take(3),
            )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardUiState(),
            )
}
