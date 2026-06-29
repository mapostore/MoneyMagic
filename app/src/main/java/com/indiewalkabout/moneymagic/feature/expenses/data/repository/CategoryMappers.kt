package com.indiewalkabout.moneymagic.feature.expenses.data.repository

import com.indiewalkabout.moneymagic.feature.expenses.data.local.CategoryEntity
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    color = color,
    iconKey = iconKey,
    sortOrder = sortOrder,
    archived = archived,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    color = color,
    iconKey = iconKey,
    sortOrder = sortOrder,
    archived = archived,
)
