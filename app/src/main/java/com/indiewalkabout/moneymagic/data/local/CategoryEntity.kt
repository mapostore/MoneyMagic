package com.indiewalkabout.moneymagic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long,
    val iconKey: String,
    val sortOrder: Int,
    val archived: Boolean,
)
