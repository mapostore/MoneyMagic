package com.indiewalkabout.moneymagic.feature.expenses.data.repository

import com.indiewalkabout.moneymagic.feature.expenses.data.local.PaymentMethodDao
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PaymentMethodRepositoryImpl @Inject constructor(
    private val paymentMethodDao: PaymentMethodDao,
) : PaymentMethodRepository {
    override fun observePaymentMethods(includeArchived: Boolean): Flow<List<PaymentMethod>> =
        paymentMethodDao.observePaymentMethods(includeArchived).map { methods -> methods.map { it.toDomain() } }

    override suspend fun save(paymentMethod: PaymentMethod): Long =
        paymentMethodDao.upsert(paymentMethod.toEntity())

    override suspend fun archive(paymentMethodId: Long) {
        paymentMethodDao.archive(paymentMethodId)
    }
}
