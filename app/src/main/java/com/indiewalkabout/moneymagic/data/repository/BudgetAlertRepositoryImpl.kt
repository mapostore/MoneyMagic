package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.BudgetAlertDao
import com.indiewalkabout.moneymagic.data.local.BudgetAlertEntity
import com.indiewalkabout.moneymagic.domain.repository.BudgetAlertRepository

class BudgetAlertRepositoryImpl(
    private val budgetAlertDao: BudgetAlertDao,
) : BudgetAlertRepository {
    override suspend fun wasThresholdAlertSent(budgetId: Long, periodKey: String): Boolean =
        budgetAlertDao.wasThresholdAlertSent(budgetId, periodKey)

    override suspend fun markThresholdAlertSent(budgetId: Long, periodKey: String) {
        budgetAlertDao.insert(BudgetAlertEntity(budgetId, periodKey))
    }
}
