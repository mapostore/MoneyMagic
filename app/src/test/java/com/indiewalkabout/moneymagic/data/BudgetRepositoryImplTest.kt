package com.indiewalkabout.moneymagic.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.database.sqlite.SQLiteConstraintException
import com.indiewalkabout.moneymagic.data.local.CategoryEntity
import com.indiewalkabout.moneymagic.data.local.MoneyMagicDatabase
import com.indiewalkabout.moneymagic.data.repository.BudgetAlertRepositoryImpl
import com.indiewalkabout.moneymagic.data.repository.BudgetRepositoryImpl
import com.indiewalkabout.moneymagic.domain.model.Budget
import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BudgetRepositoryImplTest {
    private lateinit var database: MoneyMagicDatabase
    private lateinit var budgetRepository: BudgetRepositoryImpl
    private lateinit var alertRepository: BudgetAlertRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoneyMagicDatabase::class.java,
        ).allowMainThreadQueries().build()
        budgetRepository = BudgetRepositoryImpl(database.budgetDao())
        alertRepository = BudgetAlertRepositoryImpl(database.budgetAlertDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletingBudgetClearsThresholdAlertState() = runTest {
        val budgetId = budgetRepository.save(
            Budget(
                name = "Groceries",
                amountMinor = 50000,
                currency = "EUR",
                period = BudgetPeriod.Monthly,
                categoryId = null,
                notificationThresholdPercent = 80,
                enabled = true,
            )
        )
        val periodKey = "2026-06"

        alertRepository.markThresholdAlertSent(budgetId, periodKey)
        assertTrue(alertRepository.wasThresholdAlertSent(budgetId, periodKey))

        budgetRepository.delete(budgetId)

        assertFalse(alertRepository.wasThresholdAlertSent(budgetId, periodKey))
    }

    @Test
    fun rejectsBudgetWithMissingCategoryReference() = runTest {
        try {
            budgetRepository.save(testBudget(categoryId = 999))
            fail("Expected missing category reference to be rejected")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun deletingCategoryClearsBudgetCategoryReference() = runTest {
        database.categoryDao().upsert(testCategory())
        budgetRepository.save(testBudget(categoryId = 1))

        database.categoryDao().delete(1)

        assertEquals(null, budgetRepository.observeBudgets().first().single().categoryId)
    }

    @Test
    fun updatingBudgetWithAlertKeepsThresholdAlertState() = runTest {
        val budgetId = budgetRepository.save(testBudget(categoryId = null))
        val periodKey = "2026-06"
        alertRepository.markThresholdAlertSent(budgetId, periodKey)

        budgetRepository.save(
            testBudget(categoryId = null).copy(
                id = budgetId,
                name = "Groceries updated",
                amountMinor = 75000,
            )
        )

        assertTrue(alertRepository.wasThresholdAlertSent(budgetId, periodKey))
        assertEquals("Groceries updated", budgetRepository.observeBudgets().first().single().name)
    }

    private fun testCategory(): CategoryEntity = CategoryEntity(
        id = 1,
        name = "Food",
        color = 0xFF00AA00,
        iconKey = "food",
        sortOrder = 0,
        archived = false,
    )

    private fun testBudget(categoryId: Long?): Budget = Budget(
        name = "Groceries",
        amountMinor = 50000,
        currency = "EUR",
        period = BudgetPeriod.Monthly,
        categoryId = categoryId,
        notificationThresholdPercent = 80,
        enabled = true,
    )
}
