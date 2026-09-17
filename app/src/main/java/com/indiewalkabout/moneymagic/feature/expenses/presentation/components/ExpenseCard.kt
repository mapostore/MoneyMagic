package com.indiewalkabout.moneymagic.feature.expenses.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.indiewalkabout.moneymagic.R
import com.indiewalkabout.moneymagic.feature.expenses.domain.model.Expense
import com.indiewalkabout.moneymagic.feature.expenses.presentation.ExpenseListItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExpenseCard(
    item: ExpenseListItem,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expense = item.expense
    var expanded by remember { mutableStateOf(false) }
    val title = expense.name.ifBlank { expense.merchant.ifBlank { stringResource(R.string.add_expense) } }
    val expandContentDescription = stringResource(
        if (expanded) R.string.collapse_expense_details else R.string.expand_expense_details,
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}")
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedButton(onClick = onRemoveClick) {
                    Text(stringResource(R.string.remove_expense))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = formatAmount(expense.amountMinor, expense.currency),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = formatDate(expense),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Filled.KeyboardArrowDown
                            } else {
                                Icons.AutoMirrored.Filled.KeyboardArrowRight
                            },
                            contentDescription = expandContentDescription,
                        )
                    }
                }
            }
            if (expanded) {
                ExpenseDetails(item = item)
            }
        }
    }
}

@Composable
private fun ExpenseDetails(item: ExpenseListItem) {
    val expense = item.expense
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (item.categoryName.isNotBlank()) {
            Text(
                text = "${stringResource(R.string.category)}: ${item.categoryName}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item.paymentMethodName?.let { paymentMethodName ->
            Text(
                text = "${stringResource(R.string.payment_method)}: $paymentMethodName",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (expense.merchant.isNotBlank()) {
            Text(
                text = "${stringResource(R.string.merchant)}: ${expense.merchant}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (expense.description.isNotBlank()) {
            Text(
                text = "${stringResource(R.string.description)}: ${expense.description}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (expense.notes.isNotBlank()) {
            Text(
                text = "${stringResource(R.string.ocr)}: ${expense.notes}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatAmount(amountMinor: Long, currency: String): String {
    val amount = amountMinor / 100.0
    return "%s %.2f".format(Locale.getDefault(), currency, amount)
}

private fun formatDate(expense: Expense): String =
    DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(expense.dateTime)

@Preview(showBackground = true)
@Composable
private fun ExpenseCardPreview() {
    MaterialTheme {
        ExpenseCard(
            item = ExpenseListItem(
                expense = Expense(
                    id = 7,
                    name = "Fuel stop",
                    amountMinor = 4567,
                    currency = "EUR",
                    dateTime = Instant.parse("2026-06-24T12:30:00Z"),
                    categoryId = 2,
                    merchant = "Q8",
                    paymentMethodId = 1,
                    description = "BENZINA 2026-06-24 14:30",
                    notes = "OCR recognized text",
                    tags = emptyList(),
                    createdAt = Instant.parse("2026-06-24T12:30:00Z"),
                    updatedAt = Instant.parse("2026-06-24T12:30:00Z"),
                ),
                categoryName = "BENZINA",
                paymentMethodName = "Card",
            ),
            onClick = {},
            onRemoveClick = {},
        )
    }
}
