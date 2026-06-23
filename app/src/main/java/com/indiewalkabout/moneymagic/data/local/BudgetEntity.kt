package com.indiewalkabout.moneymagic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "budgets",
    indices = [Index("categoryId")],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val periodType: String,
    val customStartDate: LocalDate?,
    val customEndDate: LocalDate?,
    val categoryId: Long?,
    val notificationThresholdPercent: Int,
    val enabled: Boolean,
)
