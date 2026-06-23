package com.indiewalkabout.moneymagic

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test

class MoneyMagicNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationOpensMainDestinations() {
        composeRule.onNodeWithText("Budget progress").assertExists()
        composeRule.onNodeWithTag("bottom_nav_expenses").performClick()
        composeRule.onNodeWithText("Expense history").assertExists()
        composeRule.onNodeWithTag("bottom_nav_budgets").performClick()
        composeRule.onNodeWithText("Budget control").assertExists()
        composeRule.onNodeWithTag("bottom_nav_settings").performClick()
        composeRule.onNodeWithText("Payment methods").assertExists()
    }

    @Test
    fun backFromTopLevelDestinationKeepsCurrentScreen() {
        composeRule.onNodeWithTag("bottom_nav_expenses").performClick()
        composeRule.onNodeWithText("Expense history").assertExists()

        pressBack()

        composeRule.onNodeWithText("Expense history").assertExists()
    }

    @Test
    fun addExpenseRouteHidesBottomNavigationAndCanCancelBack() {
        composeRule.onNodeWithText("Add expense").performClick()

        composeRule.onNodeWithText("Amount").assertExists()
        composeRule.onAllNodesWithTag("bottom_nav_dashboard").assertCountEquals(0)

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Budget progress").assertExists()
        composeRule.onNodeWithTag("bottom_nav_dashboard").assertExists()
    }
}
