package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.PaymentMethodRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
        val viewModel = ExpensesViewModel(
            expenseRepository = repository,
            categoryRepository = FakeExpenseCategoryRepository(),
            paymentMethodRepository = FakeExpensePaymentMethodRepository(),
            clock = clock,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectPeriodFilter(ExpensePeriodFilter.WEEK)
        viewModel.selectSortOption(ExpenseSortOption.HIGHEST_AMOUNT)
        advanceUntilIdle()

        assertEquals(ExpensePeriodFilter.WEEK, viewModel.uiState.value.selectedPeriodFilter)
        assertEquals(ExpenseSortOption.HIGHEST_AMOUNT, viewModel.uiState.value.selectedSortOption)
        assertEquals(listOf("Groceries", "Coffee"), viewModel.uiState.value.expenses.map { it.expense.merchant })
        assertEquals(listOf("Food", "Food"), viewModel.uiState.value.expenses.map { it.categoryName })
    }

    @Test
    fun deleteExpenseRemovesSelectedExpense() = runTest {
        val repository = FakeExpensesHistoryRepository(
            listOf(
                testExpense(1, 900, "Coffee", "2026-06-24T08:00:00Z"),
                testExpense(2, 5000, "Groceries", "2026-06-23T18:00:00Z"),
            ),
        )
        val viewModel = ExpensesViewModel(
            expenseRepository = repository,
            categoryRepository = FakeExpenseCategoryRepository(),
            paymentMethodRepository = FakeExpensePaymentMethodRepository(),
            clock = clock,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.deleteExpense(1)
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.deletedIds)
        assertEquals(listOf("Groceries"), viewModel.uiState.value.expenses.map { it.expense.name })
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
            name = merchant,
            amountMinor = amountMinor,
            currency = "EUR",
            dateTime = dateTime,
            categoryId = 1,
            merchant = merchant,
            paymentMethodId = null,
            description = "",
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
    val deletedIds = mutableListOf<Long>()

    override fun observeExpenses(): Flow<List<Expense>> = expensesFlow

    override fun observeExpense(expenseId: Long): Flow<Expense?> =
        expensesFlow.map { expenses -> expenses.firstOrNull { it.id == expenseId } }

    override suspend fun save(expense: Expense): Long = error("Not used")

    override suspend fun delete(expenseId: Long) {
        deletedIds += expenseId
        expensesFlow.value = expensesFlow.value.filterNot { it.id == expenseId }
    }

    override suspend fun deleteAll() {
        expensesFlow.value = emptyList()
    }
}

private class FakeExpenseCategoryRepository : CategoryRepository {
    override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> =
        flowOf(
            listOf(
                Category(
                    id = 1,
                    name = "Food",
                    color = 0xFF00AA00,
                    iconKey = "food",
                    sortOrder = 0,
                    archived = false,
                ),
            ),
        )

    override suspend fun save(category: Category): Long = error("Not used")

    override suspend fun archive(categoryId: Long) = error("Not used")
}

private class FakeExpensePaymentMethodRepository : PaymentMethodRepository {
    override fun observePaymentMethods(includeArchived: Boolean): Flow<List<PaymentMethod>> =
        flowOf(
            listOf(
                PaymentMethod(
                    id = 7,
                    name = "Card",
                    type = PaymentMethodType.Card,
                    archived = false,
                ),
            ),
        )

    override suspend fun save(paymentMethod: PaymentMethod): Long = error("Not used")

    override suspend fun archive(paymentMethodId: Long) = error("Not used")
}
