package com.indiewalkabout.moneymagic.feature.budgets.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.Budget
import com.indiewalkabout.moneymagic.feature.budgets.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.budgets.domain.usecase.CalculateBudgetProgressUseCase
import com.indiewalkabout.moneymagic.feature.budgets.presentation.BudgetsViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private fun testBudget(amountMinor: Long): Budget = Budget(
        id = 1,
        name = "Monthly cap",
        amountMinor = amountMinor,
        currency = "EUR",
        period = BudgetPeriod.Monthly,
        categoryId = null,
        notificationThresholdPercent = 80,
        enabled = true,
    )

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
            notes = "",
            tags = emptyList(),
            createdAt = dateTime,
            updatedAt = dateTime,
        )
    }
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
}
