package com.indiewalkabout.moneymagic.feature.expenses.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject

data class ExpenseValidationResult(
    val amountMinor: Long?,
    val errors: List<ExpenseValidationError>,
)

enum class ExpenseValidationError {
    EmptyAmount,
    AmountMustBePositive,
    MissingCategory,
    InvalidAmount,
}

class ValidateExpenseUseCase @Inject constructor() {
    operator fun invoke(amountText: String, categoryId: Long?): ExpenseValidationResult {
        val errors = mutableListOf<ExpenseValidationError>()
        val amountMinor = parseAmountMinor(amountText, errors)

        if (categoryId == null) {
            errors += ExpenseValidationError.MissingCategory
        }

        return ExpenseValidationResult(amountMinor = amountMinor, errors = errors)
    }

    private fun parseAmountMinor(
        amountText: String,
        errors: MutableList<ExpenseValidationError>,
    ): Long? {
        val normalizedAmountText = amountText.trim().replace(',', '.')
        if (normalizedAmountText.isBlank()) {
            errors += ExpenseValidationError.EmptyAmount
            return null
        }

        val amount = normalizedAmountText.toBigDecimalOrNull()
        if (amount == null) {
            errors += ExpenseValidationError.InvalidAmount
            return null
        }

        if (amount.scale() > 2) {
            errors += ExpenseValidationError.InvalidAmount
            return null
        }

        if (amount <= BigDecimal.ZERO) {
            errors += ExpenseValidationError.AmountMustBePositive
            return null
        }

        return try {
            amount.movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            errors += ExpenseValidationError.InvalidAmount
            null
        }
    }
}
