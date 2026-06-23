package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.ExpenseDao
import com.indiewalkabout.moneymagic.domain.model.Expense
import com.indiewalkabout.moneymagic.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
) : ExpenseRepository {
    override fun observeExpenses(): Flow<List<Expense>> =
        expenseDao.observeExpenses().map { expenses -> expenses.map { it.toDomain() } }

    override suspend fun save(expense: Expense): Long = expenseDao.upsert(expense.toEntity())

    override suspend fun delete(expenseId: Long) {
        expenseDao.delete(expenseId)
    }
}
