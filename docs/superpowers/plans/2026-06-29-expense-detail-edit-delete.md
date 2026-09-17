# Expense Detail Edit Delete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Add an offline expense detail screen that lets the user review, edit, save, and delete existing expenses.

**Architecture:** Reuse the existing feature-first `feature.expenses` MVVM structure. The repository remains the persistence boundary; detail state combines one selected expense with categories/payment methods, then saves by upserting the original expense id.

**Tech Stack:** Kotlin, Compose Material 3, Navigation 3, Hilt, Coroutine Flow, Room.

---

### Task 1: Repository Single Expense Observation

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/data/local/ExpenseDao.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/data/repository/ExpenseRepositoryImpl.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/domain/repository/ExpenseRepository.kt`
- Modify tests/fakes that implement `ExpenseRepository`.

- [x] **Step 1: Add repository contract**

Add `fun observeExpense(expenseId: Long): Flow<Expense?>` to `ExpenseRepository`.

- [x] **Step 2: Add DAO query**

Add `@Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1") fun observeExpense(expenseId: Long): Flow<ExpenseEntity?>`.

- [x] **Step 3: Implement repository mapping**

Map nullable entity to nullable domain in `ExpenseRepositoryImpl`.

- [x] **Step 4: Update test fakes**

Every fake `ExpenseRepository` must implement `observeExpense`.

- [x] **Step 5: Verify**

Run `./gradlew :app:testDebugUnitTest`.

### Task 2: Expense Detail ViewModel

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/ExpenseDetailViewModel.kt`
- Create: `app/src/test/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/ExpenseDetailViewModelTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`

- [x] **Step 1: Add detail UI state**

State includes loading flags, id, editable fields, categories, payment methods, canSave, deleted/saved markers, and typed localized error.

- [x] **Step 2: Load existing expense into form**

Observe `expenseRepository.observeExpense(expenseId)`, categories, and payment methods. Populate form once from the existing expense and keep list data live.

- [x] **Step 3: Save edits**

Validate amount/category/date/time, preserve original `id`, `currency`, `tags`, and `createdAt`, update `updatedAt`, then call `expenseRepository.save`.

- [x] **Step 4: Delete expense**

Call `expenseRepository.delete(expenseId)` and mark deleted for navigation.

- [x] **Step 5: Test save/delete behavior**

Unit tests verify existing values load, save preserves id and updates edited fields, invalid values do not save, and delete calls repository once.

### Task 3: Expense Detail Compose UI

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/ExpenseDetailScreen.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/ExpensesScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`

- [x] **Step 1: Add detail screen**

Build a Compose form matching Add Expense controls: name, amount, category dropdown, payment method dropdown, date/time, merchant, notes, save, delete, and back.

- [x] **Step 2: Make expense cards clickable**

Expose `onExpenseClick: (Long) -> Unit` from `ExpensesScreen` and call it from each card.

- [x] **Step 3: Localize visible text**

Add strings for detail title, delete, deleted state, missing expense, and save errors in English and Italian.

- [x] **Step 4: Verify compile**

Run `./gradlew :app:compileDebugKotlin`.

### Task 4: Navigation Integration

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/navigation/MoneyMagicNavHost.kt`
- Modify: `app/src/androidTest/java/com/indiewalkabout/moneymagic/MoneyMagicNavigationTest.kt`

- [x] **Step 1: Add route**

Add `@Serializable internal data class ExpenseDetailRoute(val expenseId: Long) : MoneyMagicRoute`.

- [x] **Step 2: Navigate from list**

Pass `onExpenseClick = { backStack.add(ExpenseDetailRoute(it)) }` to `ExpensesScreen`.

- [x] **Step 3: Register detail entry**

Render `ExpenseDetailScreen(expenseId = route.expenseId, onBack = ..., onDeleted = ...)` and hide bottom navigation for detail routes.

- [x] **Step 4: Update instrumentation coverage**

Add a navigation test that opens Expenses, taps a seeded card such as `Weekly groceries`, verifies detail title, then backs to Expense history.

- [x] **Step 5: Verify**

Run `./gradlew :app:connectedDebugAndroidTest`.

### Task 5: Final Verification And Commit

**Files:**
- All touched files.

- [x] **Step 1: Run verification**

Run `git diff --check`, `./gradlew :app:testDebugUnitTest`, `./gradlew :app:assembleDebug`, and `./gradlew :app:connectedDebugAndroidTest`.

- [x] **Step 2: Stage intentionally**

Stage source, tests, resources, and plan. Leave `.idea/` untracked.

- [x] **Step 3: Commit**

Commit with `feat: add expense detail editing`.
