package com.indiewalkabout.moneymagic

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
    fun settingsManagementRoutesHideBottomNavigationAndNavigateBack() {
        composeRule.onNodeWithTag("bottom_nav_settings").performClick()

        composeRule.onNodeWithText("Categories").performClick()
        composeRule.onNodeWithText("Category name").assertExists()
        composeRule.onAllNodesWithTag("bottom_nav_settings").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Spending data").assertExists()
        composeRule.onNodeWithTag("bottom_nav_settings").assertExists()
        composeRule.onNodeWithText("Payment methods").performClick()
        composeRule.onNodeWithText("Payment method name").assertExists()
        composeRule.onAllNodesWithTag("bottom_nav_settings").assertCountEquals(0)

        pressBack()

        composeRule.onNodeWithText("Spending data").assertExists()
        composeRule.onNodeWithTag("bottom_nav_settings").assertExists()
    }

    @Test
    fun deletingAllExpensesRequiresTypedLowercaseYes() {
        composeRule.onNodeWithText("Add expense").performClick()
        composeRule.onNodeWithText("Expense name").performTextInput("Delete history test")
        composeRule.onNodeWithText("Amount").performTextInput("12.34")
        composeRule.onNodeWithText("Select category").performClick()
        composeRule.onNodeWithText("ABBONAMENTI").performClick()
        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("bottom_nav_settings").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Delete all expenses"))
        composeRule.onNodeWithText("Delete all expenses").performClick()

        val confirmButtons = composeRule.onAllNodesWithText("Delete all expenses")
        confirmButtons[1].assertIsNotEnabled()
        composeRule.onNodeWithText("Confirmation").performTextInput("YES")
        confirmButtons[1].assertIsNotEnabled()
        composeRule.onNodeWithText("Confirmation").performTextClearance()
        composeRule.onNodeWithText("Confirmation").performTextInput("yes")
        confirmButtons[1].assertIsEnabled().performClick()

        composeRule.onNodeWithText("No expenses to export").assertExists()
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

    @Test
    fun receiptCaptureRouteHidesBottomNavigationAndCanCancelBack() {
        composeRule.onNodeWithText("Scan receipt").performClick()

        composeRule.onNodeWithText("Select image").assertExists()
        composeRule.onNodeWithText("Use camera").assertExists()
        composeRule.onAllNodesWithTag("bottom_nav_dashboard").assertCountEquals(0)

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Budget progress").assertExists()
        composeRule.onNodeWithTag("bottom_nav_dashboard").assertExists()
    }

    @Test
    fun expenseDetailRouteOpensFromHistoryAndCanCancelBack() {
        val expenseName = "Navigation detail expense"
        composeRule.onNodeWithText("Add expense").performClick()
        composeRule.onNodeWithText("Expense name").performTextInput(expenseName)
        composeRule.onNodeWithText("Amount").performTextInput("12.34")
        composeRule.onNodeWithText("Select category").performClick()
        composeRule.onNodeWithText("ABBONAMENTI").performClick()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithTag("bottom_nav_expenses").performClick()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(expenseName))
        composeRule.onNodeWithText(expenseName).performClick()

        composeRule.onNodeWithText("Expense detail").assertExists()
        composeRule.onAllNodesWithText(expenseName).assertCountEquals(2)
        composeRule.onAllNodesWithTag("bottom_nav_expenses").assertCountEquals(0)

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Expense history").assertExists()
        composeRule.onNodeWithTag("bottom_nav_expenses").assertExists()
    }

    @Test
    fun budgetScreenAcceptsMonthlyBudget() {
        composeRule.onNodeWithTag("bottom_nav_budgets").performClick()

        composeRule.onNodeWithText("Budget name").performTextInput("Test monthly")
        composeRule.onNodeWithText("Monthly limit").performTextInput("123.45")
        composeRule.onNodeWithText("Save monthly budget").performClick()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Test monthly - Monthly"))
        composeRule.onNodeWithText("Test monthly - Monthly").assertExists()
    }
}
