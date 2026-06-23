package com.indiewalkabout.moneymagic.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
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
        Text(
            text = "Budget progress",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Recent expenses",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Top categories",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
