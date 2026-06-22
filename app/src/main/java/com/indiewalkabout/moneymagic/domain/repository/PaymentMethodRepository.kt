package com.indiewalkabout.moneymagic.domain.repository

import com.indiewalkabout.moneymagic.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun observePaymentMethods(includeArchived: Boolean = false): Flow<List<PaymentMethod>>
    suspend fun save(paymentMethod: PaymentMethod): Long
}
