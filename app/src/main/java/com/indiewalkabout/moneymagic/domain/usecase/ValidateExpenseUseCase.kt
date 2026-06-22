package com.indiewalkabout.moneymagic.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode

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

class ValidateExpenseUseCase {
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
        if (amountText.isBlank()) {
            errors += ExpenseValidationError.EmptyAmount
            return null
        }

        val amount = amountText.replace(',', '.').toBigDecimalOrNull()
        if (amount == null) {
            errors += ExpenseValidationError.InvalidAmount
            return null
        }

        if (amount <= BigDecimal.ZERO) {
            errors += ExpenseValidationError.AmountMustBePositive
            return null
        }

        return amount
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}
