package com.indiewalkabout.moneymagic.feature.settings.presentation

import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.PaymentMethodRepository
import com.indiewalkabout.moneymagic.feature.settings.domain.usecase.ExportExpensesXlsxUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
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
    fun deleteAllRequiresTrimmedLowercaseYesBeforeDeletingHistory() = runTest(dispatcher) {
        val repository = FakeSettingsExpenseRepository()
        val viewModel = SettingsViewModel(
            categoryRepository = FakeSettingsCategoryRepository,
            paymentMethodRepository = FakeSettingsPaymentMethodRepository,
            expenseRepository = repository,
            exportExpensesXlsx = ExportExpensesXlsxUseCase(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.showDeleteAllConfirmation()
        viewModel.onDeleteAllConfirmationChanged("YES")
        viewModel.deleteAllExpenses()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, repository.deleteAllCalls)
        assertTrue(viewModel.uiState.value.showDeleteAllConfirmation)
        assertFalse(viewModel.uiState.value.canDeleteAllExpenses)

        viewModel.onDeleteAllConfirmationChanged(" yes ")
        viewModel.deleteAllExpenses()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.deleteAllCalls)
        assertFalse(viewModel.uiState.value.showDeleteAllConfirmation)
        assertEquals("", viewModel.uiState.value.deleteAllConfirmationText)
    }

    @Test
    fun deleteAllFailureUnlocksDialogAndShowsErrorWithoutClearingConfirmation() = runTest(dispatcher) {
        val repository = FakeSettingsExpenseRepository(deleteAllFails = true)
        val viewModel = SettingsViewModel(
            categoryRepository = FakeSettingsCategoryRepository,
            paymentMethodRepository = FakeSettingsPaymentMethodRepository,
            expenseRepository = repository,
            exportExpensesXlsx = ExportExpensesXlsxUseCase(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.showDeleteAllConfirmation()
        viewModel.onDeleteAllConfirmationChanged("yes")
        viewModel.deleteAllExpenses()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDeleteAllConfirmation)
        assertFalse(viewModel.uiState.value.isDeletingAllExpenses)
        assertTrue(viewModel.uiState.value.deleteAllExpensesFailed)
        assertEquals("yes", viewModel.uiState.value.deleteAllConfirmationText)
    }
}

private class FakeSettingsExpenseRepository(
    private val deleteAllFails: Boolean = false,
) : ExpenseRepository {
    private val expenses = MutableStateFlow<List<Expense>>(emptyList())
    var deleteAllCalls = 0

    override fun observeExpenses(): Flow<List<Expense>> = expenses
    override fun observeExpense(expenseId: Long): Flow<Expense?> = flowOf(null)
    override suspend fun save(expense: Expense): Long = error("Not used")
    override suspend fun delete(expenseId: Long) = error("Not used")
    override suspend fun deleteAll() {
        if (deleteAllFails) error("Storage unavailable")
        deleteAllCalls += 1
        expenses.value = emptyList()
    }
}

private data object FakeSettingsCategoryRepository : CategoryRepository {
    override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> = flowOf(emptyList())
    override suspend fun save(category: Category): Long = error("Not used")
    override suspend fun archive(categoryId: Long) = error("Not used")
}

private data object FakeSettingsPaymentMethodRepository : PaymentMethodRepository {
    override fun observePaymentMethods(includeArchived: Boolean): Flow<List<PaymentMethod>> = flowOf(emptyList())
    override suspend fun save(paymentMethod: PaymentMethod): Long = error("Not used")
    override suspend fun archive(paymentMethodId: Long) = error("Not used")
}
