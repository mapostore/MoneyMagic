package com.indiewalkabout.moneymagic.domain.repository

interface BudgetAlertRepository {
    suspend fun wasThresholdAlertSent(budgetId: Long, periodKey: String): Boolean
    suspend fun markThresholdAlertSent(budgetId: Long, periodKey: String)
}
