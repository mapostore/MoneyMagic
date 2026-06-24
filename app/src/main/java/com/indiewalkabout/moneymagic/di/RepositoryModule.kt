package com.indiewalkabout.moneymagic.di

import com.indiewalkabout.moneymagic.feature.budgets.data.repository.BudgetAlertRepositoryImpl
import com.indiewalkabout.moneymagic.feature.budgets.data.repository.BudgetRepositoryImpl
import com.indiewalkabout.moneymagic.feature.expenses.data.repository.CategoryRepositoryImpl
import com.indiewalkabout.moneymagic.feature.expenses.data.repository.ExpenseRepositoryImpl
import com.indiewalkabout.moneymagic.feature.expenses.data.repository.PaymentMethodRepositoryImpl
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetAlertRepository
import com.indiewalkabout.moneymagic.feature.budgets.domain.repository.BudgetRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.PaymentMethodRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindExpenseRepository(
        repository: ExpenseRepositoryImpl,
    ): ExpenseRepository

    @Binds
    abstract fun bindCategoryRepository(
        repository: CategoryRepositoryImpl,
    ): CategoryRepository

    @Binds
    abstract fun bindPaymentMethodRepository(
        repository: PaymentMethodRepositoryImpl,
    ): PaymentMethodRepository

    @Binds
    abstract fun bindBudgetRepository(
        repository: BudgetRepositoryImpl,
    ): BudgetRepository

    @Binds
    abstract fun bindBudgetAlertRepository(
        repository: BudgetAlertRepositoryImpl,
    ): BudgetAlertRepository
}
