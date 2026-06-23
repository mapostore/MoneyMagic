package com.indiewalkabout.moneymagic.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.domain.model.Expense
import com.indiewalkabout.moneymagic.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val recentExpenses: List<Expense> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    expenseRepository: ExpenseRepository,
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> =
        expenseRepository.observeExpenses()
            .map { expenses ->
                DashboardUiState(recentExpenses = expenses.sortedByDescending { it.dateTime }.take(5))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardUiState(),
            )
}
