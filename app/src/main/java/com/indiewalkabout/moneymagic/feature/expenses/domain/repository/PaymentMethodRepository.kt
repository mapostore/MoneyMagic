package com.indiewalkabout.moneymagic.feature.expenses.domain.repository

import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun observePaymentMethods(includeArchived: Boolean = false): Flow<List<PaymentMethod>>
    suspend fun save(paymentMethod: PaymentMethod): Long
}
