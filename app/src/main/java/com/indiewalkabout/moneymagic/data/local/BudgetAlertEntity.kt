package com.indiewalkabout.moneymagic.data.local

import androidx.room.Entity

@Entity(
    tableName = "budget_alerts",
    primaryKeys = ["budgetId", "periodKey"],
)
data class BudgetAlertEntity(
    val budgetId: Long,
    val periodKey: String,
)
