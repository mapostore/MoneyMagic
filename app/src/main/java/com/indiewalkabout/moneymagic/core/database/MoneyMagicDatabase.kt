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
        deleteDemoData(db)
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
    }

    private fun deleteDemoData(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM expenses WHERE tags LIKE '%demo%'")
        db.execSQL("DELETE FROM budget_alerts WHERE budgetId BETWEEN 101 AND 105")
        db.execSQL("DELETE FROM budgets WHERE id BETWEEN 101 AND 105")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN name TEXT NOT NULL DEFAULT ''")
    }
}
