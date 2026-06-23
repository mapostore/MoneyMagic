package com.indiewalkabout.moneymagic.di

import android.content.Context
import androidx.room.Room
import com.indiewalkabout.moneymagic.data.local.BudgetAlertDao
import com.indiewalkabout.moneymagic.data.local.BudgetDao
import com.indiewalkabout.moneymagic.data.local.CategoryDao
import com.indiewalkabout.moneymagic.data.local.ExpenseDao
import com.indiewalkabout.moneymagic.data.local.MoneyMagicDatabase
import com.indiewalkabout.moneymagic.data.local.PaymentMethodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
            "money_magic.db",
        ).build()

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
