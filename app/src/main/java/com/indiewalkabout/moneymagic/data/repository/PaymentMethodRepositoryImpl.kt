package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.PaymentMethodDao
import com.indiewalkabout.moneymagic.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaymentMethodRepositoryImpl(
    private val paymentMethodDao: PaymentMethodDao,
) : PaymentMethodRepository {
    override fun observePaymentMethods(includeArchived: Boolean): Flow<List<PaymentMethod>> =
        paymentMethodDao.observePaymentMethods(includeArchived).map { methods -> methods.map { it.toDomain() } }

    override suspend fun save(paymentMethod: PaymentMethod): Long =
        paymentMethodDao.upsert(paymentMethod.toEntity())
}
