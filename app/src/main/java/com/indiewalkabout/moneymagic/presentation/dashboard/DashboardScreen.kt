package com.indiewalkabout.moneymagic.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
        )
        Button(
            onClick = onAddExpenseClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add expense")
        }
        Text(
            text = "Budget progress",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Recent expenses",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = if (uiState.recentExpenses.isEmpty()) {
                "No recent expenses"
            } else {
                "${uiState.recentExpenses.size} recent expenses"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Top categories",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
