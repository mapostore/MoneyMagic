package com.indiewalkabout.moneymagic.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.database.sqlite.SQLiteConstraintException
import com.indiewalkabout.moneymagic.data.local.CategoryEntity
import com.indiewalkabout.moneymagic.data.local.MoneyMagicDatabase
import com.indiewalkabout.moneymagic.data.local.PaymentMethodEntity
import com.indiewalkabout.moneymagic.data.repository.ExpenseRepositoryImpl
import com.indiewalkabout.moneymagic.domain.model.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
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
        database.categoryDao().upsert(testCategory())
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

    @Test
    fun rejectsExpenseWithMissingCategoryReference() = runTest {
        try {
            repository.save(testExpense(categoryId = 999))
            fail("Expected missing category reference to be rejected")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun deletingCategoryWithExpensesIsRejected() = runTest {
        database.categoryDao().upsert(testCategory())
        repository.save(testExpense())

        try {
            database.categoryDao().delete(1)
            fail("Expected deleting a category used by expenses to be rejected")
        } catch (_: SQLiteConstraintException) {
        }

        assertEquals(1, repository.observeExpenses().first().size)
    }

    @Test
    fun rejectsExpenseWithMissingPaymentMethodReference() = runTest {
        database.categoryDao().upsert(testCategory())

        try {
            repository.save(testExpense(paymentMethodId = 999))
            fail("Expected missing payment method reference to be rejected")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun deletingPaymentMethodClearsExpensePaymentMethodReference() = runTest {
        database.categoryDao().upsert(testCategory())
        database.paymentMethodDao().upsert(
            PaymentMethodEntity(
                id = 7,
                name = "Card",
                type = "Card",
                archived = false,
            )
        )

        repository.save(testExpense(paymentMethodId = 7))
        database.paymentMethodDao().delete(7)

        assertEquals(null, repository.observeExpenses().first().single().paymentMethodId)
    }

    private fun testCategory(): CategoryEntity = CategoryEntity(
        id = 1,
        name = "Food",
        color = 0xFF00AA00,
        iconKey = "food",
        sortOrder = 0,
        archived = false,
    )

    private fun testExpense(
        categoryId: Long = 1,
        paymentMethodId: Long? = null,
    ): Expense {
        val now = Instant.parse("2026-06-22T10:00:00Z")
        return Expense(
            amountMinor = 1250,
            currency = "EUR",
            dateTime = now,
            categoryId = categoryId,
            merchant = "Bakery",
            paymentMethodId = paymentMethodId,
            notes = "",
            tags = listOf("food"),
            createdAt = now,
            updatedAt = now,
        )
    }
}
