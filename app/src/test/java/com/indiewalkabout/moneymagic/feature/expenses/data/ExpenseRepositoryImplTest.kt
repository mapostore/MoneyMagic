package com.indiewalkabout.moneymagic.feature.expenses.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.database.sqlite.SQLiteConstraintException
import com.indiewalkabout.moneymagic.feature.expenses.data.local.CategoryEntity
import com.indiewalkabout.moneymagic.core.database.MoneyMagicDatabaseSeedCallback
import com.indiewalkabout.moneymagic.core.database.MoneyMagicDatabase
import com.indiewalkabout.moneymagic.feature.expenses.data.local.PaymentMethodEntity
import com.indiewalkabout.moneymagic.feature.budgets.data.repository.BudgetRepositoryImpl
import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetEntity
import com.indiewalkabout.moneymagic.feature.expenses.data.repository.CategoryRepositoryImpl
import com.indiewalkabout.moneymagic.feature.expenses.data.repository.ExpenseRepositoryImpl
import com.indiewalkabout.moneymagic.feature.expenses.data.repository.PaymentMethodRepositoryImpl
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ExpenseRepositoryImplTest {
    private lateinit var database: MoneyMagicDatabase
    private lateinit var repository: ExpenseRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var paymentMethodRepository: PaymentMethodRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoneyMagicDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ExpenseRepositoryImpl(database.expenseDao())
        categoryRepository = CategoryRepositoryImpl(database.categoryDao())
        paymentMethodRepository = PaymentMethodRepositoryImpl(database.paymentMethodDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesAndObservesExpensesNewestFirst() = runTest {
        database.categoryDao().upsert(testCategory())
        val now = Instant.parse("2026-06-22T10:00:00Z")
        repository.save(
            Expense(
                name = "Bakery",
                amountMinor = 1250,
                currency = "EUR",
                dateTime = now,
                categoryId = 1,
                merchant = "Bakery",
                paymentMethodId = null,
                description = "",
                notes = "",
                tags = listOf("food"),
                createdAt = now,
                updatedAt = now,
            )
        )

        val expenses = repository.observeExpenses().first()

        assertEquals(1, expenses.size)
        assertEquals("Bakery", expenses.single().merchant)
        assertEquals(listOf("food"), expenses.single().tags)
    }

    @Test
    fun observesExpenseById() = runTest {
        database.categoryDao().upsert(testCategory())
        val savedId = repository.save(testExpense())

        val expense = repository.observeExpense(savedId).first()

        assertEquals(savedId, expense?.id)
        assertEquals("Bakery", expense?.name)
    }

    @Test
    fun saveWithExistingIdUpdatesExpense() = runTest {
        database.categoryDao().upsert(testCategory())
        val savedId = repository.save(testExpense())

        repository.save(testExpense(id = savedId).copy(name = "Updated", amountMinor = 2500))

        val expense = repository.observeExpense(savedId).first()
        assertEquals("Updated", expense?.name)
        assertEquals(2500L, expense?.amountMinor)
        assertEquals(1, repository.observeExpenses().first().size)
    }

    @Test
    fun deleteRemovesExpenseAndSingleObserverEmitsNull() = runTest {
        database.categoryDao().upsert(testCategory())
        val savedId = repository.save(testExpense())

        repository.delete(savedId)

        assertEquals(null, repository.observeExpense(savedId).first())
    }

    @Test
    fun deleteAllRemovesExpensesWithoutDeletingExpenseMetadata() = runTest {
        database.categoryDao().upsert(testCategory())
        database.budgetDao().upsert(
            BudgetEntity(
                name = "Monthly food",
                amountMinor = 50_000,
                currency = "EUR",
                periodType = "Monthly",
                customStartDate = null,
                customEndDate = null,
                categoryId = 1,
                notificationThresholdPercent = 80,
                enabled = true,
            ),
        )
        database.paymentMethodDao().upsert(
            PaymentMethodEntity(
                id = 7,
                name = "Card",
                type = "Card",
                archived = false,
            )
        )
        repository.save(testExpense(paymentMethodId = 7))
        repository.save(testExpense().copy(id = 0, name = "Second"))

        repository.deleteAll()

        assertTrue(repository.observeExpenses().first().isEmpty())
        assertEquals(1, categoryRepository.observeCategories().first().size)
        assertEquals(1, paymentMethodRepository.observePaymentMethods().first().size)
        assertEquals(1, database.budgetDao().observeBudgets().first().size)
    }

    @Test
    fun seededFreshDatabaseSavesExpenseWithDefaultCategory() = runTest {
        val seededDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoneyMagicDatabase::class.java,
        ).addCallback(MoneyMagicDatabaseSeedCallback)
            .allowMainThreadQueries()
            .build()
        val seededRepository = ExpenseRepositoryImpl(seededDatabase.expenseDao())
        val seededBudgetRepository = BudgetRepositoryImpl(seededDatabase.budgetDao())

        try {
            seededRepository.save(testExpense(categoryId = 1001))

            val expenses = seededRepository.observeExpenses().first()
            val budgets = seededBudgetRepository.observeBudgets().first()
            val categories = seededDatabase.categoryDao().observeCategories(includeArchived = true).first()
            assertTrue(expenses.any { it.merchant == "Bakery" })
            assertTrue(expenses.none { it.tags.contains("demo") })
            assertTrue(budgets.isEmpty())
            assertEquals("ABBONAMENTI", categories.first { it.id == 1001L }.name)
            assertEquals("INTERESSI", categories.first { it.id == 1039L }.name)
        } finally {
            seededDatabase.close()
        }
    }

    @Test
    fun rejectsExpenseWithMissingCategoryReference() = runTest {
        try {
            repository.save(testExpense(categoryId = 999))
            fail("Expected missing category reference to be rejected")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun deletingCategoryWithExpensesIsRejected() = runTest {
        database.categoryDao().upsert(testCategory())
        repository.save(testExpense())

        try {
            database.categoryDao().delete(1)
            fail("Expected deleting a category used by expenses to be rejected")
        } catch (_: SQLiteConstraintException) {
        }

        assertEquals(1, repository.observeExpenses().first().size)
    }

    @Test
    fun rejectsExpenseWithMissingPaymentMethodReference() = runTest {
        database.categoryDao().upsert(testCategory())

        try {
            repository.save(testExpense(paymentMethodId = 999))
            fail("Expected missing payment method reference to be rejected")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test
    fun deletingPaymentMethodClearsExpensePaymentMethodReference() = runTest {
        database.categoryDao().upsert(testCategory())
        database.paymentMethodDao().upsert(
            PaymentMethodEntity(
                id = 7,
                name = "Card",
                type = "Card",
                archived = false,
            )
        )

        repository.save(testExpense(paymentMethodId = 7))
        database.paymentMethodDao().delete(7)

        assertEquals(null, repository.observeExpenses().first().single().paymentMethodId)
    }

    @Test
    fun updatingCategoryReferencedByExpenseKeepsExpenseCategoryReference() = runTest {
        database.categoryDao().upsert(testCategory())
        repository.save(testExpense())

        categoryRepository.save(
            Category(
                id = 1,
                name = "Groceries",
                color = 0xFF336699,
                iconKey = "groceries",
                sortOrder = 1,
                archived = false,
            )
        )

        val expense = repository.observeExpenses().first().single()
        val category = categoryRepository.observeCategories(includeArchived = true).first().single()
        assertEquals(1L, expense.categoryId)
        assertEquals("Groceries", category.name)
    }

    @Test
    fun updatingPaymentMethodReferencedByExpenseKeepsExpensePaymentMethodReference() = runTest {
        database.categoryDao().upsert(testCategory())
        database.paymentMethodDao().upsert(
            PaymentMethodEntity(
                id = 7,
                name = "Card",
                type = "Card",
                archived = false,
            )
        )
        repository.save(testExpense(paymentMethodId = 7))

        paymentMethodRepository.save(
            PaymentMethod(
                id = 7,
                name = "Visa",
                type = PaymentMethodType.Card,
                archived = false,
            )
        )

        val expense = repository.observeExpenses().first().single()
        val paymentMethod = paymentMethodRepository.observePaymentMethods(includeArchived = true).first().single()
        assertEquals(7L, expense.paymentMethodId)
        assertEquals("Visa", paymentMethod.name)
    }

    private fun testCategory(): CategoryEntity = CategoryEntity(
        id = 1,
        name = "Food",
        color = 0xFF00AA00,
        iconKey = "food",
        sortOrder = 0,
        archived = false,
    )

    private fun testExpense(
        id: Long = 0,
        categoryId: Long = 1,
        paymentMethodId: Long? = null,
    ): Expense {
        val now = Instant.parse("2026-06-22T10:00:00Z")
        return Expense(
            id = id,
            name = "Bakery",
            amountMinor = 1250,
            currency = "EUR",
            dateTime = now,
            categoryId = categoryId,
            merchant = "Bakery",
            paymentMethodId = paymentMethodId,
            description = "",
            notes = "",
            tags = listOf("food"),
            createdAt = now,
            updatedAt = now,
        )
    }
}
