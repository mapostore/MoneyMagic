package com.indiewalkabout.moneymagic.feature.expenses.data.repository

import com.indiewalkabout.moneymagic.feature.expenses.data.local.ExpenseEntity
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense

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
