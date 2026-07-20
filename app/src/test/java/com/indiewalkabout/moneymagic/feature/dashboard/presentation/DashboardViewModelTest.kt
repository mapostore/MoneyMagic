package com.indiewalkabout.moneymagic.feature.dashboard.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetRepository
import com.indiewalkabout.moneymagic.feature.budgets.domain.usecase.CalculateBudgetProgressUseCase
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
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
class DashboardViewModelTest {
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
    fun exposesCurrentBudgetProgressFromExpenses() = runTest {
        val viewModel = DashboardViewModel(
            expenseRepository = FakeDashboardExpenseRepository(
                listOf(
                    testExpense(2500, "2026-06-20T10:00:00Z"),
                    testExpense(9000, "2026-05-20T10:00:00Z"),
                ),
            ),
            budgetRepository = FakeDashboardBudgetRepository(
                listOf(
                    Budget(
                        id = 1,
                        name = "Monthly cap",
                        amountMinor = 10000,
                        currency = "EUR",
                        period = BudgetPeriod.Monthly,
                        categoryId = null,
                        notificationThresholdPercent = 80,
                        enabled = true,
                    ),
                ),
            ),
            categoryRepository = FakeDashboardCategoryRepository(),
            calculateBudgetProgress = CalculateBudgetProgressUseCase(),
            clock = clock,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val progress = viewModel.uiState.value.budgetProgress.single()
        assertEquals("Monthly cap", progress.budget.name)
        assertEquals(2500, progress.spentMinor)
        assertEquals(25, progress.percentUsed)
        assertEquals("Food", viewModel.uiState.value.topCategories.single().category.name)
        assertEquals(11500, viewModel.uiState.value.topCategories.single().amountMinor)
    }

    private fun testExpense(amountMinor: Long, instant: String): Expense {
        val dateTime = Instant.parse(instant)
        return Expense(
            id = 0,
            name = "Test expense",
            amountMinor = amountMinor,
            currency = "EUR",
            dateTime = dateTime,
            categoryId = 1,
            merchant = "Merchant",
            paymentMethodId = null,
            description = "",
            notes = "",
            tags = emptyList(),
            createdAt = dateTime,
            updatedAt = dateTime,
        )
    }
}

private class FakeDashboardCategoryRepository : CategoryRepository {
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

private class FakeDashboardExpenseRepository(
    expenses: List<Expense>,
) : ExpenseRepository {
    private val expensesFlow = MutableStateFlow(expenses)

    override fun observeExpenses(): Flow<List<Expense>> = expensesFlow

    override fun observeExpense(expenseId: Long): Flow<Expense?> =
        expensesFlow.map { expenses -> expenses.firstOrNull { it.id == expenseId } }

    override suspend fun save(expense: Expense): Long = error("Not used")

    override suspend fun delete(expenseId: Long) = Unit
}

private class FakeDashboardBudgetRepository(
    budgets: List<Budget>,
) : BudgetRepository {
    private val budgetsFlow = MutableStateFlow(budgets)

    override fun observeBudgets(): Flow<List<Budget>> = budgetsFlow

    override suspend fun save(budget: Budget): Long = error("Not used")

    override suspend fun delete(budgetId: Long) = Unit
}
