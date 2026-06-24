package com.indiewalkabout.moneymagic.presentation.common

import com.indiewalkabout.moneymagic.core.time.PeriodCalculator
import com.indiewalkabout.moneymagic.domain.model.Budget
import com.indiewalkabout.moneymagic.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.domain.model.Expense
import com.indiewalkabout.moneymagic.domain.usecase.CalculateBudgetProgressUseCase
import java.time.Clock
import java.time.LocalDate

internal fun calculateCurrentBudgetProgress(
    budgets: List<Budget>,
    expenses: List<Expense>,
    calculateBudgetProgress: CalculateBudgetProgressUseCase,
    periodCalculator: PeriodCalculator,
    clock: Clock,
): List<BudgetProgress> {
    val anchor = LocalDate.now(clock)
    return budgets
        .filter { it.enabled }
        .map { budget ->
            val range = periodCalculator.rangeFor(budget.period, anchor)
            val spentMinor = expenses
                .asSequence()
                .filter { expense ->
                    val date = expense.dateTime.atZone(clock.zone).toLocalDate()
                    date >= range.start && date <= range.endInclusive
                }
                .filter { expense -> budget.categoryId == null || expense.categoryId == budget.categoryId }
                .sumOf { expense -> expense.amountMinor }
            calculateBudgetProgress(budget, spentMinor)
        }
}
