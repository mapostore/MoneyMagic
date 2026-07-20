package com.indiewalkabout.moneymagic.feature.settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Category
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethod
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.PaymentMethodType

@Composable
fun CategoryCard(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(onClick = { onEdit(category) }) {
                Text(stringResource(R.string.edit))
            }
            OutlinedButton(onClick = { onDelete(category) }) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    paymentMethod: PaymentMethod,
    onEdit: (PaymentMethod) -> Unit,
    onDelete: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = paymentMethod.name, style = MaterialTheme.typography.titleSmall)
                Text(text = paymentMethod.type.label(), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { onEdit(paymentMethod) }) {
                Text(stringResource(R.string.edit))
            }
            OutlinedButton(onClick = { onDelete(paymentMethod) }) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun PaymentMethodType.label(): String =
    stringResource(
        when (this) {
            PaymentMethodType.Cash -> R.string.cash
            PaymentMethodType.Card -> R.string.card
            PaymentMethodType.Bank -> R.string.bank
            PaymentMethodType.Other -> R.string.other
        },
    )

@Preview(showBackground = true)
@Composable
private fun SettingsCardsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryCard(
                category = Category(1, "BENZINA", 0xFF00AA00, "benzina", 0, false),
                onEdit = {},
                onDelete = {},
            )
            PaymentMethodCard(
                paymentMethod = PaymentMethod(1, "Card", PaymentMethodType.Card, false),
                onEdit = {},
                onDelete = {},
            )
        }
    }
}
