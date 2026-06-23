package com.indiewalkabout.moneymagic.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.indiewalkabout.moneymagic.data.local.MoneyMagicDatabase
import com.indiewalkabout.moneymagic.data.repository.BudgetAlertRepositoryImpl
import com.indiewalkabout.moneymagic.data.repository.BudgetRepositoryImpl
import com.indiewalkabout.moneymagic.domain.model.Budget
import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}
