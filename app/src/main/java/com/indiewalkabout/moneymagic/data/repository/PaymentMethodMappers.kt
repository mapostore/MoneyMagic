package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.PaymentMethodEntity
import com.indiewalkabout.moneymagic.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.domain.model.PaymentMethodType

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
