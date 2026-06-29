package com.indiewalkabout.moneymagic.feature.expenses.domain.repository

import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<Expense>>
    suspend fun save(expense: Expense): Long
    suspend fun delete(expenseId: Long)
}
