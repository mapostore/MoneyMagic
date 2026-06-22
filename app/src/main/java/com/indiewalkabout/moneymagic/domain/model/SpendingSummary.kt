package com.indiewalkabout.moneymagic.domain.model

data class SpendingSummary(
    val totalMinor: Long,
    val categoryTotals: List<CategoryTotal>,
    val recentExpenses: List<Expense>,
)

data class CategoryTotal(
    val category: Category,
    val totalMinor: Long,
)
