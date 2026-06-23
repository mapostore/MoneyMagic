package com.indiewalkabout.moneymagic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase.Callback
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        PaymentMethodEntity::class,
        BudgetEntity::class,
        BudgetAlertEntity::class,
    ],
    version = 1,
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
            VALUES (1, 'Cash', 'Cash', 0)
            """.trimIndent(),
        )
    }
}
