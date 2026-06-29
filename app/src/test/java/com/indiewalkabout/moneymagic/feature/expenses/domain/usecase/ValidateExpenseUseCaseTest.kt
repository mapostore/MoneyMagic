package com.indiewalkabout.moneymagic.feature.expenses.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateExpenseUseCaseTest {
    private val useCase = ValidateExpenseUseCase()

    @Test
    fun rejectsEmptyAmount() {
        val result = useCase(amountText = "", categoryId = 1)
        assertEquals(ExpenseValidationError.EmptyAmount, result.errors.single())
    }

    @Test
    fun rejectsZeroAmount() {
        val result = useCase(amountText = "0", categoryId = 1)
        assertEquals(ExpenseValidationError.AmountMustBePositive, result.errors.single())
    }

    @Test
    fun rejectsMissingCategory() {
        val result = useCase(amountText = "12.50", categoryId = null)
        assertEquals(ExpenseValidationError.MissingCategory, result.errors.single())
    }

    @Test
    fun acceptsPositiveAmountAndCategory() {
        val result = useCase(amountText = "12.50", categoryId = 1)
        assertTrue(result.errors.isEmpty())
        assertEquals(1250L, result.amountMinor)
    }

    @Test
    fun rejectsMoreThanTwoFractionalDigits() {
        val result = useCase(amountText = "12.345", categoryId = 1)
        assertEquals(ExpenseValidationError.InvalidAmount, result.errors.single())
        assertEquals(null, result.amountMinor)
    }

    @Test
    fun rejectsAmountTooLargeForMinorUnits() {
        val result = useCase(amountText = "999999999999999999999999999.99", categoryId = 1)
        assertEquals(ExpenseValidationError.InvalidAmount, result.errors.single())
        assertEquals(null, result.amountMinor)
    }

    @Test
    fun acceptsPositiveAmountWithSurroundingWhitespace() {
        val result = useCase(amountText = " 12.50 ", categoryId = 1)
        assertTrue(result.errors.isEmpty())
        assertEquals(1250L, result.amountMinor)
    }
}
