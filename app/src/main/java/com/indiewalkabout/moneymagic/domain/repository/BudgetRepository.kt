package com.indiewalkabout.moneymagic.domain.repository

import com.indiewalkabout.moneymagic.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeBudgets(): Flow<List<Budget>>
    suspend fun save(budget: Budget): Long
    suspend fun delete(budgetId: Long)
}
