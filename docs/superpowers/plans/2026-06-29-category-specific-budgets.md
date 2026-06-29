# Category Specific Budgets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Let users create budgets for all spending or a specific local category.

**Architecture:** Reuse the existing `Budget.categoryId` persistence and `calculateCurrentBudgetProgress` category filtering. Budgets presentation will observe local categories, keep selected category id in form state, save it into the budget, and render category scope in budget rows.

**Tech Stack:** Kotlin, Compose Material 3, Hilt, Coroutine Flow, Room.

---

### Task 1: Budget ViewModel Category Scope

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/budgets/presentation/BudgetsViewModel.kt`
- Modify: `app/src/test/java/com/indiewalkabout/moneymagic/feature/budgets/presentation/BudgetsViewModelTest.kt`

- [x] **Step 1: Add categories and selected category to UI state**
- [x] **Step 2: Inject `CategoryRepository`**
- [x] **Step 3: Combine categories into `uiState`**
- [x] **Step 4: Save `selectedCategoryId` into `Budget.categoryId`**
- [x] **Step 5: Add tests for all-category and category-specific save/progress**

### Task 2: Budget Screen Category Dropdown

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/budgets/presentation/BudgetsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`

- [x] **Step 1: Add localized labels for budget scope**
- [x] **Step 2: Add Material 3 category dropdown**
- [x] **Step 3: Show scope label in budget progress rows**
- [x] **Step 4: Compile and fix UI issues**

### Task 3: Verification And Commit

**Files:**
- All touched files.

- [x] **Step 1: Run `git diff --check`**
- [x] **Step 2: Run `./gradlew :app:testDebugUnitTest`**
- [x] **Step 3: Run `./gradlew :app:assembleDebug`**
- [x] **Step 4: Run `./gradlew :app:connectedDebugAndroidTest`**
- [x] **Step 5: Stage and commit with `feat: add category budgets`**
