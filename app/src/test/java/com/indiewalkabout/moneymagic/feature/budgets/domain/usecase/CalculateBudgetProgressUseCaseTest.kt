package com.indiewalkabout.moneymagic.feature.budgets.domain.usecase

import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateBudgetProgressUseCaseTest {
    private val useCase = CalculateBudgetProgressUseCase()

    @Test
    fun calculatesRemainingPercentAndNearTargetState() {
        val budget = Budget(
            id = 1,
            name = "Monthly",
            amountMinor = 10000,
            currency = "EUR",
            period = BudgetPeriod.Monthly,
            categoryId = null,
            notificationThresholdPercent = 80,
            enabled = true,
        )

        val progress = useCase(budget, spentMinor = 8500)

        assertEquals(1500, progress.remainingMinor)
        assertEquals(85, progress.percentUsed)
        assertTrue(progress.isNearTarget)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun marksOverBudgetWhenSpentExceedsBudget() {
        val budget = Budget(1, "Monthly", 10000, "EUR", BudgetPeriod.Monthly, null, 80, true)

        val progress = useCase(budget, spentMinor = 12500)

        assertEquals(-2500, progress.remainingMinor)
        assertEquals(125, progress.percentUsed)
        assertTrue(progress.isOverBudget)
    }
}
