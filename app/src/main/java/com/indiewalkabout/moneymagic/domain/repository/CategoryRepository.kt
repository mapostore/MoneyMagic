package com.indiewalkabout.moneymagic.domain.repository

import com.indiewalkabout.moneymagic.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(includeArchived: Boolean = false): Flow<List<Category>>
    suspend fun save(category: Category): Long
}
