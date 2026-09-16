package com.indiewalkabout.moneymagic.feature.expenses.data.repository

import com.indiewalkabout.moneymagic.feature.expenses.data.local.ExpenseDao
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
) : ExpenseRepository {
    override fun observeExpenses(): Flow<List<Expense>> =
        expenseDao.observeExpenses().map { expenses -> expenses.map { it.toDomain() } }

    override fun observeExpense(expenseId: Long): Flow<Expense?> =
        expenseDao.observeExpense(expenseId).map { expense -> expense?.toDomain() }

    override suspend fun save(expense: Expense): Long = expenseDao.upsert(expense.toEntity())

    override suspend fun delete(expenseId: Long) {
        expenseDao.delete(expenseId)
    }

    override suspend fun deleteAll() {
        expenseDao.deleteAll()
    }
}
