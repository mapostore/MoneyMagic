package com.indiewalkabout.moneymagic.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.settings.presentation.components.CategoryCard

@Composable
fun CategoryManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryManagementContent(
        uiState = uiState,
        onBack = onBack,
        onNameChanged = viewModel::onCategoryNameChanged,
        onSave = viewModel::saveCategory,
        onClear = viewModel::clearCategoryForm,
        onEdit = viewModel::editCategory,
        onDelete = { category -> viewModel.deleteCategory(category.id) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryManagementContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.categoryName,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, enabled = uiState.categoryName.isNotBlank()) {
                        Text(stringResource(if (uiState.editingCategoryId == null) R.string.add else R.string.update))
                    }
                    OutlinedButton(onClick = onClear) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }
            items(uiState.categories, key = { it.id }) { category ->
                CategoryCard(category = category, onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryManagementScreenPreview() {
    MaterialTheme {
        CategoryManagementContent(
            uiState = SettingsUiState(
                categories = listOf(Category(1, "BENZINA", 0xFF00AA00, "benzina", 0, false)),
                categoryName = "SPESA",
            ),
            onBack = {},
            onNameChanged = {},
            onSave = {},
            onClear = {},
            onEdit = {},
            onDelete = {},
        )
    }
}
