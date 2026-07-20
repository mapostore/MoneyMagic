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
    version = 3,
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
        seedDefaultCategories(db)
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

    private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
        val names = defaultCategoryNames.joinToString(",") { "'${it.sqlEscaped()}'" }
        db.execSQL("UPDATE categories SET archived = 1 WHERE name NOT IN ($names)")
        defaultCategoryNames.forEachIndexed { index, name ->
            val id = DEFAULT_CATEGORY_ID_START + index
            val color = defaultCategoryColors[index % defaultCategoryColors.size]
            val iconKey = name.lowercase().replace(" ", "_")
            db.execSQL(
                """
                UPDATE categories
                SET color = $color,
                    iconKey = '${iconKey.sqlEscaped()}',
                    sortOrder = $index,
                    archived = 0
                WHERE name = '${name.sqlEscaped()}'
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO categories (id, name, color, iconKey, sortOrder, archived)
                SELECT $id, '${name.sqlEscaped()}', $color, '${iconKey.sqlEscaped()}', $index, 0
                WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '${name.sqlEscaped()}')
                """.trimIndent(),
            )
        }
    }
}

private const val DEFAULT_CATEGORY_ID_START = 1001L

private val defaultCategoryNames = listOf(
    "ABBONAMENTI",
    "ADSL",
    "AMORE",
    "ARTE",
    "ASSICURAZIONI",
    "AUTO",
    "AUTOSTRADA",
    "BENEFICIENZA",
    "BENZINA",
    "BERE",
    "BORSA",
    "CASA",
    "CURA_PERSONA",
    "DIESEL",
    "FILM",
    "GAS",
    "GATTI",
    "INVESTIMENTO",
    "LIBRI",
    "LUCE",
    "MANGIARE FUORI",
    "MOBILE DEV",
    "MOBILE",
    "MUTUO",
    "PARCHEGGIO",
    "PENSIONE",
    "REGALI",
    "SALUTE",
    "SPESA",
    "SPESE CONDOMINIALI",
    "SPORT",
    "STIPENDIO",
    "SVAGO",
    "TASSE",
    "TECH",
    "VACANZE",
    "VESTITI",
    "CONGUAGLIO",
    "INTERESSI",
)

private val defaultCategoryColors = listOf(
    0xFF2563EB,
    0xFF16A34A,
    0xFFDB2777,
    0xFF7C3AED,
    0xFF0891B2,
    0xFFEA580C,
    0xFF4F46E5,
    0xFF059669,
    0xFFDC2626,
    0xFF9333EA,
    0xFF0F766E,
    0xFFCA8A04,
)

private fun String.sqlEscaped(): String = replace("'", "''")

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN name TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN description TEXT NOT NULL DEFAULT ''")
    }
}
