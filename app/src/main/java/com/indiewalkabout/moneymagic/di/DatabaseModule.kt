package com.indiewalkabout.moneymagic.di

import android.content.Context
import androidx.room.Room
import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetAlertDao
import com.indiewalkabout.moneymagic.feature.budgets.data.local.BudgetDao
import com.indiewalkabout.moneymagic.feature.expenses.data.local.CategoryDao
import com.indiewalkabout.moneymagic.feature.expenses.data.local.ExpenseDao
import com.indiewalkabout.moneymagic.core.database.MoneyMagicDatabase
import com.indiewalkabout.moneymagic.core.database.MoneyMagicDatabaseSeedCallback
import com.indiewalkabout.moneymagic.feature.expenses.data.local.PaymentMethodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "money_magic.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMoneyMagicDatabase(
        @ApplicationContext context: Context,
    ): MoneyMagicDatabase =
        Room.databaseBuilder(
            context,
            MoneyMagicDatabase::class.java,
            DATABASE_NAME,
        ).addCallback(MoneyMagicDatabaseSeedCallback)
            .build()

    @Provides
    fun provideExpenseDao(database: MoneyMagicDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideCategoryDao(database: MoneyMagicDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun providePaymentMethodDao(database: MoneyMagicDatabase): PaymentMethodDao =
        database.paymentMethodDao()

    @Provides
    fun provideBudgetDao(database: MoneyMagicDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideBudgetAlertDao(database: MoneyMagicDatabase): BudgetAlertDao =
        database.budgetAlertDao()
}
