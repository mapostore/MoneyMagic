package com.indiewalkabout.moneymagic

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MoneyMagicNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationOpensMainDestinations() {
        composeRule.onNodeWithText("Dashboard").assertExists()
        composeRule.onNodeWithText("Expenses").performClick()
        composeRule.onNodeWithText("Expense history").assertExists()
        composeRule.onNodeWithText("Budgets").performClick()
        composeRule.onNodeWithText("Budget control").assertExists()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Settings").assertExists()
    }
}
