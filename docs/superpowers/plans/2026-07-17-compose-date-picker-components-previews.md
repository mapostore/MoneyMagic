# Compose Date Picker, Components, And Previews Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a calendar date picker affordance to expense date fields, extract reusable Compose cards/fields into feature `presentation/components` folders, and add Android Studio previews for screens/components.

**Architecture:** Keep feature-first folders. Screens remain responsible for ViewModel collection and navigation callbacks; pure content and reusable UI move into feature-local component files so previews can render without Hilt.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Navigation 3, Hilt, Room, coroutines/Flow, local-only Android app.

## Global Constraints

- Keep the app offline and local-only.
- Preserve current ViewModel contracts and date format `yyyy-MM-dd`.
- Do not introduce backend or paid services.
- Keep changes feature-first, not Gradle multi-module.
- Use Compose Material3 components and previews.
- Do not revert unrelated working tree changes.

---

### Task 1: Expense Date Picker Component

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/components/ExpenseDateField.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/AddExpenseScreen.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/ExpenseDetailScreen.kt`

**Interfaces:**
- Produces: `@Composable fun ExpenseDateField(value: String, onValueChange: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier)`
- Consumes: existing date strings in `yyyy-MM-dd`.

- [ ] Add `ExpenseDateField` with an editable `OutlinedTextField`, trailing `DateRange` icon button, and Material3 `DatePickerDialog`.
- [ ] Convert picker selected millis to `LocalDate` at UTC and write `LocalDate.toString()` back to the ViewModel.
- [ ] Use `ExpenseDateField` in add/scan edit and expense detail edit screens.
- [ ] Add `ExpenseDateFieldPreview`.
- [ ] Run `./gradlew :app:compileDebugKotlin`.

### Task 2: Expense Components Extraction

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/components/ExpenseCard.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/components/ExpenseDropdowns.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/components/ExpandableOcrField.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/ExpensesScreen.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/AddExpenseScreen.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/ExpenseDetailScreen.kt`

**Interfaces:**
- Produces: reusable expense components for cards, category/payment dropdowns, period/sort dropdowns, and expandable OCR field.
- Consumes: `ExpenseListItem`, `ExpensePeriodFilter`, `ExpenseSortOption`, `Category`, `PaymentMethod`.

- [ ] Move `ExpenseCard` and details into `components/ExpenseCard.kt`.
- [ ] Move category/payment/period/sort dropdowns into `components/ExpenseDropdowns.kt`.
- [ ] Move duplicated OCR expandable card into `components/ExpandableOcrField.kt`.
- [ ] Keep public component functions internal to the module by omitting `private`.
- [ ] Add previews for extracted components.
- [ ] Run focused compile.

### Task 3: Dashboard, Settings, Capture, And Budget Components

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/dashboard/presentation/components/DashboardCards.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/settings/presentation/components/SettingsCards.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/presentation/components/ReceiptCards.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/budgets/presentation/components/BudgetComponents.kt`
- Modify existing screen files to import extracted components.

**Interfaces:**
- Produces: reusable card components and previews.
- Consumes: existing domain/UI models.

- [ ] Extract dashboard cards: recent expense, budget progress, category spend.
- [ ] Extract settings cards: category and payment method cards.
- [ ] Extract capture cards: receipt draft, diagnostics, raw text.
- [ ] Extract budget progress row and budget scope dropdown.
- [ ] Add previews for each extracted component.
- [ ] Run compile.

### Task 4: Screen Content Previews

**Files:**
- Modify each feature screen to expose pure content composables when needed and add previews.

**Interfaces:**
- Produces: one `@Preview` per screen: Dashboard, Expenses, Add Expense, Expense Detail, Receipt Capture, Budgets, Settings.

- [ ] Split each Hilt screen into route wrapper plus content composable when needed.
- [ ] Add fake state/sample data functions for previews near each screen.
- [ ] Wrap previews in `MoneyMagicTheme`.
- [ ] Ensure previews do not invoke Hilt, Room, launchers, or navigation.
- [ ] Run `./gradlew :app:compileDebugKotlin`.
- [ ] Run `./gradlew :app:testDebugUnitTest`.

### Task 5: Final Verification

**Files:**
- All changed files.

- [ ] Run `./gradlew :app:compileDebugKotlin`.
- [ ] Run `./gradlew :app:testDebugUnitTest`.
- [ ] Inspect `git diff --stat`.
- [ ] Summarize the date picker, components, previews, and verification.

