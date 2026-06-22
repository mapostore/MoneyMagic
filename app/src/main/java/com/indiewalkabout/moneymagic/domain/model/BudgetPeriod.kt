package com.indiewalkabout.moneymagic.domain.model

sealed interface BudgetPeriod {
    data object Weekly : BudgetPeriod
    data object Monthly : BudgetPeriod
    data object Yearly : BudgetPeriod
    data class Custom(val start: java.time.LocalDate, val endInclusive: java.time.LocalDate) : BudgetPeriod
}
