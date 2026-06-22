package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.ExpenseEntity
import com.indiewalkabout.moneymagic.domain.model.Expense

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amountMinor = amountMinor,
    currency = currency,
    dateTime = dateTime,
    categoryId = categoryId,
    merchant = merchant,
    paymentMethodId = paymentMethodId,
    notes = notes,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amountMinor = amountMinor,
    currency = currency,
    dateTime = dateTime,
    categoryId = categoryId,
    merchant = merchant,
    paymentMethodId = paymentMethodId,
    notes = notes,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
