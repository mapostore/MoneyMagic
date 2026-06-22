package com.indiewalkabout.moneymagic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMinor: Long,
    val currency: String,
    val dateTime: Instant,
    val categoryId: Long,
    val merchant: String,
    val paymentMethodId: Long?,
    val notes: String,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
