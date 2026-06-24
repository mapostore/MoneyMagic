package com.indiewalkabout.moneymagic.feature.dashboard.domain.model

import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense

data class SpendingSummary(
    val totalMinor: Long,
    val categoryTotals: List<CategoryTotal>,
    val recentExpenses: List<Expense>,
)

data class CategoryTotal(
    val category: Category,
    val totalMinor: Long,
)
