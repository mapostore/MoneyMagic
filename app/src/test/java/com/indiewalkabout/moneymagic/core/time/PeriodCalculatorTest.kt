package com.indiewalkabout.moneymagic.core.time

import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodCalculatorTest {
    private val calculator = PeriodCalculator()

    @Test
    fun weeklyRangeStartsOnMondayAndEndsOnSunday() {
        val range = calculator.rangeFor(BudgetPeriod.Weekly, LocalDate.of(2026, 6, 22))
        assertEquals(LocalDate.of(2026, 6, 22), range.start)
        assertEquals(LocalDate.of(2026, 6, 28), range.endInclusive)
    }

    @Test
    fun monthlyRangeUsesFirstAndLastDayOfMonth() {
        val range = calculator.rangeFor(BudgetPeriod.Monthly, LocalDate.of(2026, 2, 14))
        assertEquals(LocalDate.of(2026, 2, 1), range.start)
        assertEquals(LocalDate.of(2026, 2, 28), range.endInclusive)
    }

    @Test
    fun yearlyRangeUsesCalendarYear() {
        val range = calculator.rangeFor(BudgetPeriod.Yearly, LocalDate.of(2026, 9, 3))
        assertEquals(LocalDate.of(2026, 1, 1), range.start)
        assertEquals(LocalDate.of(2026, 12, 31), range.endInclusive)
    }
}
