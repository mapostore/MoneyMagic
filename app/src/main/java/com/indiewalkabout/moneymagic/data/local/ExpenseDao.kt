package com.indiewalkabout.moneymagic.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateTime DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Upsert
    suspend fun upsert(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun delete(expenseId: Long)
}
