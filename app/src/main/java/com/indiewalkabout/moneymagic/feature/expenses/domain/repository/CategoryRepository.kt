package com.indiewalkabout.moneymagic.feature.expenses.domain.repository

import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(includeArchived: Boolean = false): Flow<List<Category>>
    suspend fun save(category: Category): Long
}
