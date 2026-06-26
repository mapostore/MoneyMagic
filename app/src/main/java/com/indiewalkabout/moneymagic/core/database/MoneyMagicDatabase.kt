package com.indiewalkabout.moneymagic.core.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase.Callback
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetAlertDao
import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetAlertEntity
import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetDao
import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetEntity
import com.indiewalkabout.moneymagic.feature.expenses.data.local.CategoryDao
import com.indiewalkabout.moneymagic.feature.expenses.data.local.CategoryEntity
import com.indiewalkabout.moneymagic.feature.expenses.data.local.ExpenseDao
import com.indiewalkabout.moneymagic.feature.expenses.data.local.ExpenseEntity
import com.indiewalkabout.moneymagic.feature.expenses.data.local.PaymentMethodDao
import com.indiewalkabout.moneymagic.feature.expenses.data.local.PaymentMethodEntity
import java.time.Instant

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        PaymentMethodEntity::class,
        BudgetEntity::class,
        BudgetAlertEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MoneyMagicDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetAlertDao(): BudgetAlertDao
}

object MoneyMagicDatabaseSeedCallback : Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        seedDemoData(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        seedDemoData(db)
    }

    private fun seedDemoData(db: SupportSQLiteDatabase) {
        val createdAt = Instant.parse("2026-06-23T09:00:00Z").toEpochMilli()

        db.execSQL(
            """
            INSERT OR IGNORE INTO categories (id, name, color, iconKey, sortOrder, archived)
            VALUES
                (1, 'Food', 4278255360, 'food', 0, 0),
                (2, 'Transport', 4278223103, 'transport', 1, 0),
                (3, 'Home', 4294944000, 'home', 2, 0),
                (4, 'Other', 4286611584, 'other', 3, 0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO payment_methods (id, name, type, archived)
            VALUES
                (1, 'Cash', 'Cash', 0),
                (2, 'Debit Card', 'Card', 0),
                (3, 'Credit Card', 'Card', 0),
                (4, 'Bank Transfer', 'Bank', 0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO budgets (
                id,
                name,
                amountMinor,
                currency,
                periodType,
                customStartDate,
                customEndDate,
                categoryId,
                notificationThresholdPercent,
                enabled
            )
            VALUES
                (101, 'Monthly spending cap', 160000, 'EUR', 'MONTHLY', NULL, NULL, NULL, 80, 1),
                (102, 'Weekly essentials', 42000, 'EUR', 'WEEKLY', NULL, NULL, NULL, 75, 1),
                (103, 'Food monthly', 36000, 'EUR', 'MONTHLY', NULL, NULL, 1, 80, 1),
                (104, 'Transport monthly', 18000, 'EUR', 'MONTHLY', NULL, NULL, 2, 80, 1),
                (105, 'Summer check-in', 90000, 'EUR', 'CUSTOM', '2026-06-01', '2026-06-30', NULL, 85, 1)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO expenses (
                id,
                name,
                amountMinor,
                currency,
                dateTime,
                categoryId,
                merchant,
                paymentMethodId,
                notes,
                tags,
                createdAt,
                updatedAt
            )
            VALUES
                (101, 'Breakfast', 845, 'EUR', ${millis("2026-06-23T07:45:00Z")}, 1, 'Corner Bakery', 2, 'Breakfast before work', '["demo","food"]', $createdAt, $createdAt),
                (102, 'Weekly groceries', 3290, 'EUR', ${millis("2026-06-22T17:20:00Z")}, 1, 'Fresh Market', 2, 'Weekly groceries', '["demo","groceries"]', $createdAt, $createdAt),
                (103, 'Metro ticket', 250, 'EUR', ${millis("2026-06-22T06:35:00Z")}, 2, 'Metro Ticket', 1, 'Morning commute', '["demo","transport"]', $createdAt, $createdAt),
                (104, 'Book order', 5690, 'EUR', ${millis("2026-06-21T19:10:00Z")}, 4, 'Bookshop Online', 3, 'Personal development book', '["demo","shopping"]', $createdAt, $createdAt),
                (105, 'Electricity bill', 11990, 'EUR', ${millis("2026-06-20T10:00:00Z")}, 3, 'Energy Utility', 4, 'Monthly electricity bill', '["demo","home","bill"]', $createdAt, $createdAt),
                (106, 'Team lunch', 1860, 'EUR', ${millis("2026-06-18T12:40:00Z")}, 1, 'Lunch Spot', 2, 'Team lunch', '["demo","food"]', $createdAt, $createdAt),
                (107, 'Fuel refill', 4200, 'EUR', ${millis("2026-06-14T15:30:00Z")}, 2, 'Fuel Station', 3, 'Weekend trip fuel', '["demo","transport"]', $createdAt, $createdAt),
                (108, 'Movie night', 1599, 'EUR', ${millis("2026-06-10T20:05:00Z")}, 4, 'Cinema', 2, 'Movie night', '["demo","leisure"]', $createdAt, $createdAt)
            """.trimIndent(),
        )
    }

    private fun millis(value: String): Long = Instant.parse(value).toEpochMilli()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN name TEXT NOT NULL DEFAULT ''")
    }
}
