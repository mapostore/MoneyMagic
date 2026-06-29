package com.indiewalkabout.moneymagic.feature.budgets.domain.usecase

import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetAlertRepository
import javax.inject.Inject

class CheckBudgetThresholdUseCase @Inject constructor(
    private val alertRepository: BudgetAlertRepository,
) {
    suspend operator fun invoke(progress: BudgetProgress, periodKey: String): Boolean {
        if (!progress.budget.enabled || !progress.isNearTarget) {
            return false
        }

        if (alertRepository.wasThresholdAlertSent(progress.budget.id, periodKey)) {
            return false
        }

        alertRepository.markThresholdAlertSent(progress.budget.id, periodKey)
        return true
    }
}
