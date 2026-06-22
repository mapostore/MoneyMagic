package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.CategoryEntity
import com.indiewalkabout.moneymagic.domain.model.Category

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
