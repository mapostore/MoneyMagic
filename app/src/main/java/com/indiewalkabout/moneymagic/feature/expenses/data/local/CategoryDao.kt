package com.indiewalkabout.moneymagic.feature.expenses.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT * FROM categories
        WHERE (:includeArchived = 1 OR archived = 0)
        ORDER BY sortOrder ASC, name ASC
        """
    )
    fun observeCategories(includeArchived: Boolean = false): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(category: CategoryEntity): Long

    @Query("UPDATE categories SET archived = 1 WHERE id = :categoryId")
    suspend fun archive(categoryId: Long)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun delete(categoryId: Long)
}
