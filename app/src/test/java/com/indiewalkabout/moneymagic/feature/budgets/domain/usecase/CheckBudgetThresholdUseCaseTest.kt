package com.indiewalkabout.moneymagic.feature.budgets.domain.usecase

import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetAlertRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckBudgetThresholdUseCaseTest {
    @Test
    fun emitsAlertOnlyOncePerBudgetPeriod() = runTest {
        val repository = FakeBudgetAlertRepository()
        val useCase = CheckBudgetThresholdUseCase(repository)
        val budget = Budget(1, "Monthly", 10000, "EUR", BudgetPeriod.Monthly, null, 80, true)
        val progress = BudgetProgress(budget, 8500, 1500, 85, true, false)

        assertTrue(useCase(progress, periodKey = "2026-06"))
        assertFalse(useCase(progress, periodKey = "2026-06"))
    }

    @Test
    fun doesNotEmitAlertWhenBudgetIsDisabled() = runTest {
        val repository = FakeBudgetAlertRepository()
        val useCase = CheckBudgetThresholdUseCase(repository)
        val budget = Budget(1, "Monthly", 10000, "EUR", BudgetPeriod.Monthly, null, 80, false)
        val progress = BudgetProgress(budget, 8500, 1500, 85, true, false)

        assertFalse(useCase(progress, periodKey = "2026-06"))
        assertFalse(repository.wasThresholdAlertSent(1, "2026-06"))
    }

    @Test
    fun doesNotEmitAlertBeforeNearTarget() = runTest {
        val repository = FakeBudgetAlertRepository()
        val useCase = CheckBudgetThresholdUseCase(repository)
        val budget = Budget(1, "Monthly", 10000, "EUR", BudgetPeriod.Monthly, null, 80, true)
        val progress = BudgetProgress(budget, 5000, 5000, 50, false, false)

        assertFalse(useCase(progress, periodKey = "2026-06"))
        assertFalse(repository.wasThresholdAlertSent(1, "2026-06"))
    }
}

private class FakeBudgetAlertRepository : BudgetAlertRepository {
    private val sent = mutableSetOf<Pair<Long, String>>()

    override suspend fun wasThresholdAlertSent(budgetId: Long, periodKey: String): Boolean =
        sent.contains(budgetId to periodKey)

    override suspend fun markThresholdAlertSent(budgetId: Long, periodKey: String) {
        sent += budgetId to periodKey
    }
}
