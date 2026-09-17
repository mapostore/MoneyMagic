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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
    fun invalidAmountShowsErrorAndDoesNotSave() = runTest {
        val repository = FakeExpenseRepository()
        val viewModel = testViewModel(repository)

        viewModel.onAmountChanged("0")
        viewModel.onCategorySelected(1)
        viewModel.save()

        assertFalse(viewModel.uiState.value.canSave)
        assertEquals(0, repository.savedCount)
    }

    @Test
    fun validExpenseSavesAndMarksSaved() = runTest {
        val repository = FakeExpenseRepository()
        val viewModel = testViewModel(repository)

        viewModel.onNameChanged("Lunch stop")
        viewModel.onAmountChanged("12.34")
        viewModel.onCategorySelected(1)
        viewModel.onPaymentMethodSelected(7)
        viewModel.onMerchantChanged("Corner Market")
        viewModel.onDescriptionChanged("Lunch")
        viewModel.onNotesChanged("OCR text")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(1, repository.savedCount)
        assertEquals("Lunch stop", repository.savedExpenses.single().name)
        assertEquals(1234, repository.savedExpenses.single().amountMinor)
        assertEquals(7L, repository.savedExpenses.single().paymentMethodId)
        assertEquals("Corner Market", repository.savedExpenses.single().merchant)
        assertEquals("Lunch", repository.savedExpenses.single().description)
        assertEquals("OCR text", repository.savedExpenses.single().notes)
    }

    @Test
    fun saveWhileSavingDoesNotSaveDuplicateExpense() = runTest {
        val repository = FakeExpenseRepository(saveDelayMillis = 1_000)
        val viewModel = testViewModel(repository)

        viewModel.onAmountChanged("12.34")
        viewModel.onCategorySelected(1)
        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, repository.savedCount)
    }

    @Test
    fun applyDraftPrefillsExpenseForm() = runTest {
        val viewModel = testViewModel(FakeExpenseRepository())

        viewModel.applyDraft(
            ExpenseDraftInput(
                name = "Fresh Market",
                amount = "12.34",
                date = "2026-06-24",
                merchant = "Fresh Market",
                description = "Groceries for dinner",
                notes = "Recognized from receipt",
            ),
        )

        assertEquals("Fresh Market", viewModel.uiState.value.name)
        assertEquals("12.34", viewModel.uiState.value.amount)
        assertEquals("2026-06-24", viewModel.uiState.value.date)
        assertEquals("Fresh Market", viewModel.uiState.value.merchant)
        assertEquals("Groceries for dinner", viewModel.uiState.value.description)
        assertEquals("Recognized from receipt", viewModel.uiState.value.notes)
    }

    @Test
    fun applyDraftWithoutDateDescriptionUsesCurrentDefaultsInsteadOfPreviousScanValues() = runTest {
        val viewModel = testViewModel(FakeExpenseRepository())

        viewModel.applyDraft(
            ExpenseDraftInput(
                name = "First scan",
                amount = "12.34",
                date = "2026-06-01",
                merchant = "First market",
                description = "Old scan description",
                notes = "Old OCR",
            ),
        )
        viewModel.onTimeChanged("09:15")

        viewModel.applyDraft(
            ExpenseDraftInput(
                name = "Second scan",
                amount = "45.67",
                merchant = "Second market",
                notes = "New OCR",
            ),
        )

        assertEquals("Second scan", viewModel.uiState.value.name)
        assertEquals("45.67", viewModel.uiState.value.amount)
        assertEquals("2026-06-24", viewModel.uiState.value.date)
        assertEquals("12:30", viewModel.uiState.value.time)
        assertEquals("Second market", viewModel.uiState.value.merchant)
        assertEquals("", viewModel.uiState.value.description)
        assertEquals("New OCR", viewModel.uiState.value.notes)
    }

    @Test
    fun applyDraftSuggestsCategoryFromScannedTextWhenNoValidCategoryExists() = runTest {
        val viewModel = testViewModel(
            repository = FakeExpenseRepository(),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    testCategory(id = 1, name = "Food"),
                    testCategory(id = 2, name = "BENZINA"),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.applyDraft(
            ExpenseDraftInput(
                name = "Q8",
                merchant = "Q8",
                description = "Receipt for refuel",
                notes = "benzina verde self service",
            ),
        )

        assertEquals(2L, viewModel.uiState.value.categoryId)
    }

    @Test
    fun suggestedCategoryDoesNotOverrideCategorySelectedByUser() = runTest {
        val viewModel = testViewModel(
            repository = FakeExpenseRepository(),
            categoryRepository = FakeCategoryRepository(
                categories = listOf(
                    testCategory(id = 1, name = "Food"),
                    testCategory(id = 2, name = "BENZINA"),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onCategorySelected(1)
        viewModel.onDescriptionChanged("benzina verde self service")

        assertEquals(1L, viewModel.uiState.value.categoryId)
    }

    private fun testViewModel(
        repository: ExpenseRepository,
        categoryRepository: CategoryRepository = FakeCategoryRepository(),
    ): AddExpenseViewModel =
        AddExpenseViewModel(
            expenseRepository = repository,
            categoryRepository = categoryRepository,
            paymentMethodRepository = FakePaymentMethodRepository(),
            validateExpense = ValidateExpenseUseCase(),
            clock = clock,
        )
}

private class FakeExpenseRepository(
    private val saveDelayMillis: Long = 0,
) : ExpenseRepository {
    var savedCount = 0
    val savedExpenses = mutableListOf<Expense>()

    override fun observeExpenses(): Flow<List<Expense>> = emptyFlow()

    override fun observeExpense(expenseId: Long): Flow<Expense?> = emptyFlow()

    override suspend fun save(expense: Expense): Long {
        if (saveDelayMillis > 0) {
            delay(saveDelayMillis)
        }
        savedCount += 1
        savedExpenses += expense
        return savedCount.toLong()
    }

    override suspend fun delete(expenseId: Long) = Unit

    override suspend fun deleteAll() = Unit
}

private class FakeCategoryRepository(
    private val categories: List<Category> = listOf(testCategory(id = 1, name = "Food")),
) : CategoryRepository {
    override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> =
        flowOf(categories)

    override suspend fun save(category: Category): Long = error("Not used")

    override suspend fun archive(categoryId: Long) = error("Not used")
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

private class FakePaymentMethodRepository : PaymentMethodRepository {
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
