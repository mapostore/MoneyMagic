package com.indiewalkabout.moneymagic.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.indiewalkabout.moneymagic.data.local.MoneyMagicDatabase
import com.indiewalkabout.moneymagic.data.repository.ExpenseRepositoryImpl
import com.indiewalkabout.moneymagic.domain.model.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ExpenseRepositoryImplTest {
    private lateinit var database: MoneyMagicDatabase
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoneyMagicDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ExpenseRepositoryImpl(database.expenseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesAndObservesExpensesNewestFirst() = runTest {
        val now = Instant.parse("2026-06-22T10:00:00Z")
        repository.save(
            Expense(
                amountMinor = 1250,
                currency = "EUR",
                dateTime = now,
                categoryId = 1,
                merchant = "Bakery",
                paymentMethodId = null,
                notes = "",
                tags = listOf("food"),
                createdAt = now,
                updatedAt = now,
            )
        )

        val expenses = repository.observeExpenses().first()

        assertEquals(1, expenses.size)
        assertEquals("Bakery", expenses.single().merchant)
        assertEquals(listOf("food"), expenses.single().tags)
    }
}
