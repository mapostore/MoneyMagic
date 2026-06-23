package com.indiewalkabout.moneymagic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(paymentMethod: PaymentMethodEntity): Long

    @Query("UPDATE payment_methods SET archived = 1 WHERE id = :paymentMethodId")
    suspend fun archive(paymentMethodId: Long)
}
