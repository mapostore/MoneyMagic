package com.indiewalkabout.moneymagic.feature.budgets.domain.model

data class Budget(
    val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val period: BudgetPeriod,
    val categoryId: Long?,
    val notificationThresholdPercent: Int,
    val enabled: Boolean,
)
