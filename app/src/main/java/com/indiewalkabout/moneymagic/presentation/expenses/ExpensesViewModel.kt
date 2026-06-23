package com.indiewalkabout.moneymagic.presentation.expenses

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

data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    expenseRepository: ExpenseRepository,
) : ViewModel() {
    val uiState: StateFlow<ExpensesUiState> =
        expenseRepository.observeExpenses()
            .map { expenses -> ExpensesUiState(expenses = expenses) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExpensesUiState(),
            )
}
