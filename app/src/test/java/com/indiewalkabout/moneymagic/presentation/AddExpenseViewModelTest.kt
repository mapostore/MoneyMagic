package com.indiewalkabout.moneymagic.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.indiewalkabout.moneymagic.domain.model.Expense
import com.indiewalkabout.moneymagic.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.domain.usecase.ValidateExpenseUseCase
import com.indiewalkabout.moneymagic.presentation.expenses.AddExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invalidAmountShowsErrorAndDoesNotSave() = runTest {
        val repository = FakeExpenseRepository()
        val viewModel = AddExpenseViewModel(repository, ValidateExpenseUseCase())

        viewModel.onAmountChanged("0")
        viewModel.onCategorySelected(1)
        viewModel.save()

        assertFalse(viewModel.uiState.value.canSave)
        assertEquals(0, repository.savedCount)
    }

    @Test
    fun validExpenseSavesAndMarksSaved() = runTest {
        val repository = FakeExpenseRepository()
        val viewModel = AddExpenseViewModel(repository, ValidateExpenseUseCase())

        viewModel.onAmountChanged("12.34")
        viewModel.onCategorySelected(1)
        viewModel.onMerchantChanged("Corner Market")
        viewModel.onNotesChanged("Lunch")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(1, repository.savedCount)
        assertEquals(1234, repository.savedExpenses.single().amountMinor)
        assertEquals("Corner Market", repository.savedExpenses.single().merchant)
        assertEquals("Lunch", repository.savedExpenses.single().notes)
    }
}

private class FakeExpenseRepository : ExpenseRepository {
    var savedCount = 0
    val savedExpenses = mutableListOf<Expense>()

    override fun observeExpenses(): Flow<List<Expense>> = emptyFlow()

    override suspend fun save(expense: Expense): Long {
        savedCount += 1
        savedExpenses += expense
        return savedCount.toLong()
    }

    override suspend fun delete(expenseId: Long) = Unit
}
