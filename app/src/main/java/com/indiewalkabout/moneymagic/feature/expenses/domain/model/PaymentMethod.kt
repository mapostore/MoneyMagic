package com.indiewalkabout.moneymagic.feature.expenses.domain.model

data class PaymentMethod(
    val id: Long = 0,
    val name: String,
    val type: PaymentMethodType,
    val archived: Boolean,
)

enum class PaymentMethodType { Cash, Card, Bank, Other }
