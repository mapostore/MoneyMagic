package com.indiewalkabout.moneymagic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BudgetAlertDao {
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM budget_alerts
            WHERE budgetId = :budgetId AND periodKey = :periodKey
        )
        """
    )
    suspend fun wasThresholdAlertSent(budgetId: Long, periodKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: BudgetAlertEntity)
}
