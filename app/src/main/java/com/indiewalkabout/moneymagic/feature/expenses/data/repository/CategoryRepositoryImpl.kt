package com.indiewalkabout.moneymagic.feature.expenses.data.repository

import com.indiewalkabout.moneymagic.feature.expenses.data.local.CategoryDao
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> =
        categoryDao.observeCategories(includeArchived).map { categories -> categories.map { it.toDomain() } }

    override suspend fun save(category: Category): Long = categoryDao.upsert(category.toEntity())

    override suspend fun archive(categoryId: Long) {
        categoryDao.archive(categoryId)
    }
}
