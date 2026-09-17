package com.indiewalkabout.moneymagic.feature.budgets.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.budgets.domain.usecase.CalculateBudgetProgressUseCase
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {
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
    fun savesMonthlyBudgetFromAmountInput() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val viewModel = BudgetsViewModel(
            budgetRepository = budgetRepository,
            expenseRepository = FakeBudgetExpenseRepository(emptyList()),
            categoryRepository = FakeBudgetCategoryRepository(),
            calculateBudgetProgress = CalculateBudgetProgressUseCase(),
            clock = clock,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onNameChanged("Monthly cap")
        viewModel.onAmountChanged("750.50")
        viewModel.saveBudget()
        advanceUntilIdle()

        assertEquals(1, budgetRepository.savedBudgets.size)
        assertEquals("Monthly cap", budgetRepository.savedBudgets.single().name)
        assertEquals(75050, budgetRepository.savedBudgets.single().amountMinor)
        assertEquals(BudgetPeriod.Monthly, budgetRepository.savedBudgets.single().period)
        assertEquals(null, budgetRepository.savedBudgets.single().categoryId)
    }

    @Test
    fun savesMonthlyBudgetForSelectedCategory() = runTest {
        val budgetRepository = FakeBudgetRepository()
        val viewModel = BudgetsViewModel(
            budgetRepository = budgetRepository,
            expenseRepository = FakeBudgetExpenseRepository(emptyList()),
            categoryRepository = FakeBudgetCategoryRepository(),
            calculateBudgetProgress = CalculateBudgetProgressUseCase(),
            clock = clock,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onNameChanged("Food cap")
        viewModel.onAmountChanged("250.00")
        viewModel.onCategorySelected(1)
        viewModel.saveBudget()
        advanceUntilIdle()

        assertEquals(1, budgetRepository.savedBudgets.size)
        assertEquals("Food cap", budgetRepository.savedBudgets.single().name)
        assertEquals(25000, budgetRepository.savedBudgets.single().amountMinor)
        assertEquals(1L, budgetRepository.savedBudgets.single().categoryId)
        assertEquals(null, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun calculatesProgressForCurrentMonthlyBudget() = runTest {
        val budgetRepository = FakeBudgetRepository(
            budgets = listOf(testBudget(amountMinor = 10000)),
        )
        val viewModel = BudgetsViewModel(
            budgetRepository = budgetRepository,
            expenseRepository = FakeBudgetExpenseRepository(
                listOf(
                    testExpense(4000, "2026-06-20T10:00:00Z"),
                    testExpense(7000, "2026-05-20T10:00:00Z"),
                ),
            ),
            categoryRepository = FakeBudgetCategoryRepository(),
            calculateBudgetProgress = CalculateBudgetProgressUseCase(),
            clock = clock,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val progress = viewModel.uiState.value.budgetProgress.single()
        assertEquals("Monthly cap", progress.budget.name)
        assertEquals(4000, progress.spentMinor)
        assertEquals(40, progress.percentUsed)
    }

    @Test
    fun categoryBudgetProgressOnlyCountsMatchingExpenses() = runTest {
        val budgetRepository = FakeBudgetRepository(
            budgets = listOf(testBudget(amountMinor = 10000, categoryId = 1)),
        )
        val viewModel = BudgetsViewModel(
            budgetRepository = budgetRepository,
            expenseRepository = FakeBudgetExpenseRepository(
                listOf(
                    testExpense(4000, "2026-06-20T10:00:00Z", categoryId = 1),
                    testExpense(7000, "2026-06-20T10:00:00Z", categoryId = 2),
                ),
            ),
            categoryRepository = FakeBudgetCategoryRepository(),
            calculateBudgetProgress = CalculateBudgetProgressUseCase(),
            clock = clock,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val progress = viewModel.uiState.value.budgetProgress.single()
        assertEquals(1L, progress.budget.categoryId)
        assertEquals(4000, progress.spentMinor)
        assertEquals(40, progress.percentUsed)
    }

    private fun testBudget(amountMinor: Long, categoryId: Long? = null): Budget = Budget(
        id = 1,
        name = "Monthly cap",
        amountMinor = amountMinor,
        currency = "EUR",
        period = BudgetPeriod.Monthly,
        categoryId = categoryId,
        notificationThresholdPercent = 80,
        enabled = true,
    )

    private fun testExpense(amountMinor: Long, instant: String, categoryId: Long = 1): Expense {
        val dateTime = Instant.parse(instant)
        return Expense(
            id = 0,
            name = "Test expense",
            amountMinor = amountMinor,
            currency = "EUR",
            dateTime = dateTime,
            categoryId = categoryId,
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

private class FakeBudgetCategoryRepository : CategoryRepository {
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
                Category(
                    id = 2,
                    name = "Transport",
                    color = 0xFF3366AA,
                    iconKey = "transport",
                    sortOrder = 1,
                    archived = false,
                ),
            ),
        )

    override suspend fun save(category: Category): Long = error("Not used")

    override suspend fun archive(categoryId: Long) = error("Not used")
}

private class FakeBudgetRepository(
    budgets: List<Budget> = emptyList(),
) : BudgetRepository {
    private val budgetsFlow = MutableStateFlow(budgets)
    val savedBudgets = mutableListOf<Budget>()

    override fun observeBudgets(): Flow<List<Budget>> = budgetsFlow

    override suspend fun save(budget: Budget): Long {
        savedBudgets += budget
        budgetsFlow.value = budgetsFlow.value + budget.copy(id = savedBudgets.size.toLong())
        return savedBudgets.size.toLong()
    }

    override suspend fun delete(budgetId: Long) = Unit
}

private class FakeBudgetExpenseRepository(
    expenses: List<Expense>,
) : ExpenseRepository {
    private val expensesFlow = MutableStateFlow(expenses)

    override fun observeExpenses(): Flow<List<Expense>> = expensesFlow

    override fun observeExpense(expenseId: Long): Flow<Expense?> =
        expensesFlow.map { expenses -> expenses.firstOrNull { it.id == expenseId } }

    override suspend fun save(expense: Expense): Long = error("Not used")

    override suspend fun delete(expenseId: Long) = Unit

    override suspend fun deleteAll() = Unit
}
