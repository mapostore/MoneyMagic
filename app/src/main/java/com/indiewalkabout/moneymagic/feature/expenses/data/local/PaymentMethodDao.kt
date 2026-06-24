package com.indiewalkabout.moneymagic.feature.expenses.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query(
        """
        SELECT * FROM payment_methods
        WHERE (:includeArchived = 1 OR archived = 0)
        ORDER BY name ASC
        """
    )
    fun observePaymentMethods(includeArchived: Boolean = false): Flow<List<PaymentMethodEntity>>

    @Upsert
    suspend fun upsert(paymentMethod: PaymentMethodEntity): Long

    @Query("UPDATE payment_methods SET archived = 1 WHERE id = :paymentMethodId")
    suspend fun archive(paymentMethodId: Long)

    @Query("DELETE FROM payment_methods WHERE id = :paymentMethodId")
    suspend fun delete(paymentMethodId: Long)
}
