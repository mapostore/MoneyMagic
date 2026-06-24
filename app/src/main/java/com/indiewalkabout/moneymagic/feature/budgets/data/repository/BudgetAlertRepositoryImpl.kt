package com.indiewalkabout.moneymagic.feature.budgets.data.repository

import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetAlertDao
import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetAlertEntity
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetAlertRepository
import javax.inject.Inject

class BudgetAlertRepositoryImpl @Inject constructor(
    private val budgetAlertDao: BudgetAlertDao,
) : BudgetAlertRepository {
    override suspend fun wasThresholdAlertSent(budgetId: Long, periodKey: String): Boolean =
        budgetAlertDao.wasThresholdAlertSent(budgetId, periodKey)

    override suspend fun markThresholdAlertSent(budgetId: Long, periodKey: String) {
        budgetAlertDao.insert(BudgetAlertEntity(budgetId, periodKey))
    }
}
