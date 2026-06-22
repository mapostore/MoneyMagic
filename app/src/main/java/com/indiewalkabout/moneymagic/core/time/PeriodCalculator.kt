package com.indiewalkabout.moneymagic.core.time

import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DateRange(val start: LocalDate, val endInclusive: LocalDate)

class PeriodCalculator {
    fun rangeFor(period: BudgetPeriod, anchor: LocalDate): DateRange =
        when (period) {
            BudgetPeriod.Weekly -> DateRange(
                start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                endInclusive = anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
            )
            BudgetPeriod.Monthly -> DateRange(
                start = anchor.withDayOfMonth(1),
                endInclusive = anchor.withDayOfMonth(anchor.lengthOfMonth()),
            )
            BudgetPeriod.Yearly -> DateRange(
                start = anchor.withDayOfYear(1),
                endInclusive = anchor.withDayOfYear(anchor.lengthOfYear()),
            )
            is BudgetPeriod.Custom -> DateRange(period.start, period.endInclusive)
        }
}
