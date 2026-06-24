package com.indiewalkabout.moneymagic.feature.budgets.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "budget_alerts",
    primaryKeys = ["budgetId", "periodKey"],
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("budgetId")],
)
data class BudgetAlertEntity(
    val budgetId: Long,
    val periodKey: String,
)
