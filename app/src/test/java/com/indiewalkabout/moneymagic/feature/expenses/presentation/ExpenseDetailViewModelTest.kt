package com.indiewalkabout.moneymagic.feature.expenses.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.PaymentMethodRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.usecase.ValidateExpenseUseCase
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
class ExpenseDetailViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-06-24T12:30:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPopulatesExistingExpense() = runTest {
        val viewModel = testViewModel(FakeDetailExpenseRepository(listOf(testExpense())))

        viewModel.load(42)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Coffee run", state.name)
        assertEquals("12.50", state.amount)
        assertEquals(1L, state.categoryId)
        assertEquals(7L, state.paymentMethodId)
        assertEquals("Food", state.categories.single().name)
        assertEquals("Card", state.paymentMethods.single().name)
        assertEquals("Quick coffee", state.description)
    }

    @Test
    fun loadInitializesBlankDescriptionWithCategoryDateAndTime() = runTest {
        val viewModel = testViewModel(
            FakeDetailExpenseRepository(listOf(testExpense().copy(description = ""))),
        )

        viewModel.load(42)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("  Food ${state.date} ${state.time}", state.description)
    }

    @Test
    fun blankDescriptionAutoDefaultDoesNotOverwriteUserEditOnRefresh() = runTest {
        val repository = FakeDetailExpenseRepository(listOf(testExpense().copy(description = "")))
        val viewModel = testViewModel(repository)

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.onDescriptionChanged("Manual description")
        repository.refresh(testExpense().copy(description = ""))
        advanceUntilIdle()

        assertEquals("Manual description", viewModel.uiState.value.description)
    }

    @Test
    fun suggestsCategoryFromDescriptionWhenCurrentCategoryIsMissing() = runTest {
        val viewModel = testViewModel(
            repository = FakeDetailExpenseRepository(listOf(testExpense().copy(categoryId = 999))),
            categoryRepository = FakeDetailCategoryRepository(
                listOf(
                    testCategory(id = 1, name = "Food"),
                    testCategory(id = 2, name = "BENZINA"),
                ),
            ),
        )

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.onDescriptionChanged("rifornimento benzina")
        advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.categoryId)
    }

    @Test
    fun keepsExistingCategoryWhenTextMatchesAnotherCategory() = runTest {
        val viewModel = testViewModel(
            repository = FakeDetailExpenseRepository(listOf(testExpense().copy(categoryId = 1))),
            categoryRepository = FakeDetailCategoryRepository(
                listOf(
                    testCategory(id = 1, name = "Food"),
                    testCategory(id = 2, name = "BENZINA"),
                ),
            ),
        )

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.onCategorySelected(1)
        viewModel.onDescriptionChanged("benzina")
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.categoryId)
    }

    @Test
    fun doesNotSuggestCategoryWhenLoadedCategoryIsValid() = runTest {
        val viewModel = testViewModel(
            repository = FakeDetailExpenseRepository(listOf(testExpense().copy(categoryId = 1))),
            categoryRepository = FakeDetailCategoryRepository(
                listOf(
                    testCategory(id = 1, name = "Food"),
                    testCategory(id = 2, name = "BENZINA"),
                ),
            ),
        )

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.onDescriptionChanged("benzina")
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.categoryId)
    }

    @Test
    fun saveUpdatesExistingExpenseAndPreservesStableFields() = runTest {
        val repository = FakeDetailExpenseRepository(listOf(testExpense()))
        val viewModel = testViewModel(repository)

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.onNameChanged("Updated coffee")
        viewModel.onAmountChanged("15.75")
        viewModel.onMerchantChanged("New Cafe")
        viewModel.onDescriptionChanged("Better beans")
        viewModel.onNotesChanged("OCR payload")
        viewModel.save()
        advanceUntilIdle()

        val saved = repository.savedExpenses.single()
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(42L, saved.id)
        assertEquals("Updated coffee", saved.name)
        assertEquals(1575, saved.amountMinor)
        assertEquals("EUR", saved.currency)
        assertEquals(listOf("demo"), saved.tags)
        assertEquals(Instant.parse("2026-06-20T09:00:00Z"), saved.createdAt)
        assertEquals(Instant.parse("2026-06-24T12:30:00Z"), saved.updatedAt)
        assertEquals("New Cafe", saved.merchant)
        assertEquals("Better beans", saved.description)
        assertEquals("OCR payload", saved.notes)
    }

    @Test
    fun invalidAmountDoesNotSave() = runTest {
        val repository = FakeDetailExpenseRepository(listOf(testExpense()))
        val viewModel = testViewModel(repository)

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.onAmountChanged("0")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(0, repository.savedExpenses.size)
        assertEquals(ExpenseDetailError.AmountMustBePositive, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun deleteRemovesExpenseAndMarksDeleted() = runTest {
        val repository = FakeDetailExpenseRepository(listOf(testExpense()))
        val viewModel = testViewModel(repository)

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals(listOf(42L), repository.deletedIds)
        assertTrue(viewModel.uiState.value.isDeleted)
    }

    @Test
    fun requestDeleteShowsConfirmationWithoutDeleting() = runTest {
        val repository = FakeDetailExpenseRepository(listOf(testExpense()))
        val viewModel = testViewModel(repository)

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.requestDeleteConfirmation()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(emptyList<Long>(), repository.deletedIds)
    }

    @Test
    fun dismissDeleteConfirmationHidesConfirmationWithoutDeleting() = runTest {
        val repository = FakeDetailExpenseRepository(listOf(testExpense()))
        val viewModel = testViewModel(repository)

        viewModel.load(42)
        advanceUntilIdle()
        viewModel.requestDeleteConfirmation()
        viewModel.dismissDeleteConfirmation()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(emptyList<Long>(), repository.deletedIds)
    }

    private fun testViewModel(
        repository: ExpenseRepository,
        categoryRepository: CategoryRepository = FakeDetailCategoryRepository(),
    ): ExpenseDetailViewModel =
        ExpenseDetailViewModel(
            expenseRepository = repository,
            categoryRepository = categoryRepository,
            paymentMethodRepository = FakeDetailPaymentMethodRepository(),
            validateExpense = ValidateExpenseUseCase(),
            clock = clock,
        )

    private fun testExpense(): Expense =
        Expense(
            id = 42,
            name = "Coffee run",
            amountMinor = 1250,
            currency = "EUR",
            dateTime = Instant.parse("2026-06-24T08:00:00Z"),
            categoryId = 1,
            merchant = "Corner Cafe",
            paymentMethodId = 7,
            description = "Quick coffee",
            notes = "Morning stop",
            tags = listOf("demo"),
            createdAt = Instant.parse("2026-06-20T09:00:00Z"),
            updatedAt = Instant.parse("2026-06-20T09:00:00Z"),
        )
}

private class FakeDetailExpenseRepository(
    expenses: List<Expense>,
) : ExpenseRepository {
    private val expensesFlow = MutableStateFlow(expenses)
    val savedExpenses = mutableListOf<Expense>()
    val deletedIds = mutableListOf<Long>()

    fun refresh(expense: Expense) {
        expensesFlow.value = expensesFlow.value.filterNot { it.id == expense.id } + expense
    }

    override fun observeExpenses(): Flow<List<Expense>> = expensesFlow

    override fun observeExpense(expenseId: Long): Flow<Expense?> =
        expensesFlow.map { expenses -> expenses.firstOrNull { it.id == expenseId } }

    override suspend fun save(expense: Expense): Long {
        savedExpenses += expense
        expensesFlow.value = expensesFlow.value.filterNot { it.id == expense.id } + expense
        return expense.id
    }

    override suspend fun delete(expenseId: Long) {
        deletedIds += expenseId
        expensesFlow.value = expensesFlow.value.filterNot { it.id == expenseId }
    }
}

private fun testCategory(id: Long, name: String): Category =
    Category(
        id = id,
        name = name,
        color = 0xFF00AA00,
        iconKey = name.lowercase(),
        sortOrder = id.toInt(),
        archived = false,
    )

private class FakeDetailCategoryRepository(
    private val categories: List<Category> = listOf(testCategory(id = 1, name = "Food")),
) : CategoryRepository {
    override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> =
        flowOf(categories)

    override suspend fun save(category: Category): Long = error("Not used")

    override suspend fun archive(categoryId: Long) = error("Not used")
}

private class FakeDetailPaymentMethodRepository : PaymentMethodRepository {
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
