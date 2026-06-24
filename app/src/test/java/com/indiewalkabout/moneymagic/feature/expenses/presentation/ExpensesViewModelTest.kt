package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.presentation.ExpensePeriodFilter
import com.indiewalkabout.moneymagic.feature.expenses.presentation.ExpenseSortOption
import com.indiewalkabout.moneymagic.feature.expenses.presentation.ExpensesViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-06-24T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun filtersCurrentWeekAndSortsByHighestAmount() = runTest {
        val repository = FakeExpensesHistoryRepository(
            listOf(
                testExpense(1, 900, "Coffee", "2026-06-24T08:00:00Z"),
                testExpense(2, 5000, "Groceries", "2026-06-23T18:00:00Z"),
                testExpense(3, 15000, "Old bill", "2026-05-10T18:00:00Z"),
            ),
        )
        val viewModel = ExpensesViewModel(repository, clock)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectPeriodFilter(ExpensePeriodFilter.WEEK)
        viewModel.selectSortOption(ExpenseSortOption.HIGHEST_AMOUNT)
        advanceUntilIdle()

        assertEquals(ExpensePeriodFilter.WEEK, viewModel.uiState.value.selectedPeriodFilter)
        assertEquals(ExpenseSortOption.HIGHEST_AMOUNT, viewModel.uiState.value.selectedSortOption)
        assertEquals(listOf("Groceries", "Coffee"), viewModel.uiState.value.expenses.map { it.merchant })
    }

    private fun testExpense(
        id: Long,
        amountMinor: Long,
        merchant: String,
        instant: String,
    ): Expense {
        val dateTime = Instant.parse(instant)
        return Expense(
            id = id,
            amountMinor = amountMinor,
            currency = "EUR",
            dateTime = dateTime,
            categoryId = 1,
            merchant = merchant,
            paymentMethodId = null,
            notes = "",
            tags = emptyList(),
            createdAt = dateTime,
            updatedAt = dateTime,
        )
    }
}

private class FakeExpensesHistoryRepository(
    expenses: List<Expense>,
) : ExpenseRepository {
    private val expensesFlow = MutableStateFlow(expenses)

    override fun observeExpenses(): Flow<List<Expense>> = expensesFlow

    override suspend fun save(expense: Expense): Long = error("Not used")

    override suspend fun delete(expenseId: Long) = Unit
}
