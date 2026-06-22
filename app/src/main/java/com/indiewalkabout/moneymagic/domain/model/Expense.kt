package com.indiewalkabout.moneymagic.domain.model

import java.time.Instant

data class Expense(
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
