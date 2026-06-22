package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.BudgetDao
import com.indiewalkabout.moneymagic.domain.model.Budget
import com.indiewalkabout.moneymagic.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
) : BudgetRepository {
    override fun observeBudgets(): Flow<List<Budget>> =
        budgetDao.observeBudgets().map { budgets -> budgets.map { it.toDomain() } }

    override suspend fun save(budget: Budget): Long = budgetDao.upsert(budget.toEntity())

    override suspend fun delete(budgetId: Long) {
        budgetDao.delete(budgetId)
    }
}
