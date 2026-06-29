package com.indiewalkabout.moneymagic.feature.budgets.data.repository

import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetEntity
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod

private const val WEEKLY = "WEEKLY"
private const val MONTHLY = "MONTHLY"
private const val YEARLY = "YEARLY"
private const val CUSTOM = "CUSTOM"

fun Budget.toEntity(): BudgetEntity {
    val customPeriod = period as? BudgetPeriod.Custom
    return BudgetEntity(
        id = id,
        name = name,
        amountMinor = amountMinor,
        currency = currency,
        periodType = when (period) {
            BudgetPeriod.Weekly -> WEEKLY
            BudgetPeriod.Monthly -> MONTHLY
            BudgetPeriod.Yearly -> YEARLY
            is BudgetPeriod.Custom -> CUSTOM
        },
        customStartDate = customPeriod?.start,
        customEndDate = customPeriod?.endInclusive,
        categoryId = categoryId,
        notificationThresholdPercent = notificationThresholdPercent,
        enabled = enabled,
    )
}

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    name = name,
    amountMinor = amountMinor,
    currency = currency,
    period = when (periodType) {
        WEEKLY -> BudgetPeriod.Weekly
        MONTHLY -> BudgetPeriod.Monthly
        YEARLY -> BudgetPeriod.Yearly
        CUSTOM -> customStartDate?.let { start ->
            customEndDate?.let { end -> BudgetPeriod.Custom(start, end) }
        } ?: BudgetPeriod.Monthly
        else -> BudgetPeriod.Monthly
    },
    categoryId = categoryId,
    notificationThresholdPercent = notificationThresholdPercent,
    enabled = enabled,
)
