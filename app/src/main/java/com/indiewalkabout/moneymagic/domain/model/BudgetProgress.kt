package com.indiewalkabout.moneymagic.domain.model

data class BudgetProgress(
    val budget: Budget,
    val spentMinor: Long,
    val remainingMinor: Long,
    val percentUsed: Int,
    val isNearTarget: Boolean,
    val isOverBudget: Boolean,
)
