package com.indiewalkabout.moneymagic.feature.budgets.domain.usecase

import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import javax.inject.Inject
import kotlin.math.roundToInt

class CalculateBudgetProgressUseCase @Inject constructor() {
    operator fun invoke(budget: Budget, spentMinor: Long): BudgetProgress {
        val percentUsed = if (budget.amountMinor <= 0) {
            0
        } else {
            ((spentMinor.toDouble() / budget.amountMinor.toDouble()) * 100).roundToInt()
        }

        return BudgetProgress(
            budget = budget,
            spentMinor = spentMinor,
            remainingMinor = budget.amountMinor - spentMinor,
            percentUsed = percentUsed,
            isNearTarget = percentUsed >= budget.notificationThresholdPercent,
            isOverBudget = spentMinor > budget.amountMinor,
        )
    }
}
