# Receipt Scanning Groundwork Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Add an offline receipt capture entry point that uses local ML Kit text recognition to create a reviewable expense draft.

**Architecture:** Add a `feature.capture` package with a local text recognizer, a pure receipt parser, and a Compose capture screen. The capture screen sends parsed draft fields into the existing Add Expense route so users review and save with the normal expense form.

**Tech Stack:** Kotlin, Compose Material 3, Navigation 3, Hilt, Coroutine Flow, ML Kit on-device text recognition.

---

### Task 1: Receipt Text Parsing

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/model/ReceiptDraft.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCase.kt`
- Create: `app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCaseTest.kt`

- [x] **Step 1: Write parser tests for merchant, date, and total amount**
- [x] **Step 2: Implement parser with local text heuristics**

### Task 2: Add Expense Draft Prefill

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/AddExpenseViewModel.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/AddExpenseScreen.kt`
- Modify: `app/src/test/java/com/indiewalkabout/moneymagic/feature/expenses/presentation/AddExpenseViewModelTest.kt`

- [x] **Step 1: Test applying a draft to Add Expense form**
- [x] **Step 2: Add `ExpenseDraftInput` and `applyDraft`**
- [x] **Step 3: Pass route draft values into screen once**

### Task 3: Local ML Kit Capture Screen

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/repository/ReceiptTextRecognizer.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/data/LocalReceiptTextRecognizer.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/presentation/ReceiptCaptureViewModel.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/presentation/ReceiptCaptureScreen.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/navigation/MoneyMagicNavHost.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`

- [x] **Step 1: Add bundled ML Kit dependency**
- [x] **Step 2: Implement local recognizer for file URI and camera preview bitmap**
- [x] **Step 3: Add capture ViewModel and Compose screen**
- [x] **Step 4: Add capture route from dashboard**

### Task 4: Verification And Commit

**Files:**
- All touched files.

- [x] **Step 1: Run `git diff --check`**
- [x] **Step 2: Run `./gradlew :app:testDebugUnitTest`**
- [x] **Step 3: Run `./gradlew :app:assembleDebug`**
- [x] **Step 4: Run `./gradlew :app:connectedDebugAndroidTest`**
- [x] **Step 5: Commit with `feat: add receipt scanning draft flow`**
