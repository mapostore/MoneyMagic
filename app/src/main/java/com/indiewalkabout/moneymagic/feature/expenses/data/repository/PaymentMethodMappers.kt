package com.indiewalkabout.moneymagic.feature.expenses.data.repository

import com.indiewalkabout.moneymagic.feature.expenses.data.local.PaymentMethodEntity
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType

fun PaymentMethod.toEntity(): PaymentMethodEntity = PaymentMethodEntity(
    id = id,
    name = name,
    type = type.name,
    archived = archived,
)

fun PaymentMethodEntity.toDomain(): PaymentMethod = PaymentMethod(
    id = id,
    name = name,
    type = PaymentMethodType.entries.firstOrNull { it.name == type } ?: PaymentMethodType.Other,
    archived = archived,
)
