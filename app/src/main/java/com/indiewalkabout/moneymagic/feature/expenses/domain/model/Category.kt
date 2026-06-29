package com.indiewalkabout.moneymagic.feature.expenses.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val color: Long,
    val iconKey: String,
    val sortOrder: Int,
    val archived: Boolean,
)
