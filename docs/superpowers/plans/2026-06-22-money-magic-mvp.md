# MoneyMagic MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the offline MoneyMagic Android MVP for manual expense tracking, budget control, local summaries, and threshold notifications.

**Architecture:** Use MVVM with clear `domain`, `data`, `presentation`, `di`, and `core` packages. Domain models and use cases stay independent from Android where possible; Room and notification APIs live behind repository/service interfaces. Compose screens observe `StateFlow` from Hilt ViewModels and navigate through Navigation 3 routes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Navigation 3, Hilt, Room, coroutines/Flow, JUnit, kotlinx-coroutines-test, AndroidX test, local notifications.

---

## File Structure

Create these package roots under `app/src/main/java/com/indiewalkabout/moneymagic/`:

- `core/money`: `Money.kt`, `CurrencyFormatter.kt`
- `core/time`: `PeriodCalculator.kt`, `DateProvider.kt`
- `domain/model`: `Expense.kt`, `Category.kt`, `PaymentMethod.kt`, `Budget.kt`, `BudgetProgress.kt`, `SpendingSummary.kt`
- `domain/repository`: `ExpenseRepository.kt`, `BudgetRepository.kt`, `CategoryRepository.kt`, `PaymentMethodRepository.kt`, `BudgetAlertRepository.kt`
- `domain/usecase`: use cases for validation, saving expenses, period summaries, budget progress, and threshold alerts
- `data/local`: `MoneyMagicDatabase.kt`, Room entities, DAOs, converters, seed data
- `data/repository`: repository implementations and entity/domain mappers
- `data/notification`: Android notification implementation
- `di`: Hilt modules
- `presentation/navigation`: app routes and nav host
- `presentation/dashboard`, `presentation/expenses`, `presentation/budgets`, `presentation/settings`: screens, state, and ViewModels

Create tests under:

- `app/src/test/java/com/indiewalkabout/moneymagic/core/time`
- `app/src/test/java/com/indiewalkabout/moneymagic/domain/usecase`
- `app/src/test/java/com/indiewalkabout/moneymagic/data`
- `app/src/test/java/com/indiewalkabout/moneymagic/presentation`
- `app/src/androidTest/java/com/indiewalkabout/moneymagic`

Because the current folder is not a git repository, commit steps are written as "run if git has been initialized."

---

### Task 1: Project Dependencies and App Wiring

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/MoneyMagicApplication.kt`

- [ ] **Step 1: Add dependency aliases**

Add these versions and libraries to `gradle/libs.versions.toml`:

```toml
[versions]
hilt = "2.57.1"
hiltNavigationCompose = "1.2.0"
room = "2.8.0"
navigation3 = "1.1.3"
ksp = "2.2.10-2.0.2"
coroutines = "1.10.2"
lifecycle = "2.9.1"
androidxTestCore = "1.6.1"
archCoreTesting = "2.2.0"
serializationJson = "1.8.1"

[libraries]
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
google-hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
google-hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serializationJson" }
androidx-arch-core-testing = { group = "androidx.arch.core", name = "core-testing", version.ref = "archCoreTesting" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTestCore" }

[plugins]
google-hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Add root plugins**

Modify `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.hilt) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 3: Apply app plugins and dependencies**

Modify `app/build.gradle.kts` so plugins and dependencies include:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.hilt)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.google.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.google.hilt.compiler)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 4: Add Hilt application**

Create `app/src/main/java/com/indiewalkabout/moneymagic/MoneyMagicApplication.kt`:

```kotlin
package com.indiewalkabout.moneymagic

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoneyMagicApplication : Application()
```

Modify the `<application>` tag in `AndroidManifest.xml`:

```xml
<application
    android:name=".MoneyMagicApplication"
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.MoneyMagic">
```

- [ ] **Step 5: Verify Gradle configuration**

Run: `./gradlew :app:compileDebugKotlin`

Expected: build reaches Kotlin compilation. If Navigation 3 artifact names differ in the installed repositories, check official AndroidX Navigation 3 docs and update only the aliases.

- [ ] **Step 6: Commit if git exists**

Run if `.git` exists:

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/indiewalkabout/moneymagic/MoneyMagicApplication.kt
git commit -m "chore: add app architecture dependencies"
```

---

### Task 2: Domain Models and Period Calculations

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/core/time/PeriodCalculator.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/domain/model/*.kt`
- Test: `app/src/test/java/com/indiewalkabout/moneymagic/core/time/PeriodCalculatorTest.kt`

- [ ] **Step 1: Write failing period tests**

Create `PeriodCalculatorTest.kt`:

```kotlin
package com.indiewalkabout.moneymagic.core.time

import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodCalculatorTest {
    private val calculator = PeriodCalculator()

    @Test
    fun weeklyRangeStartsOnMondayAndEndsOnSunday() {
        val range = calculator.rangeFor(BudgetPeriod.Weekly, LocalDate.of(2026, 6, 22))
        assertEquals(LocalDate.of(2026, 6, 22), range.start)
        assertEquals(LocalDate.of(2026, 6, 28), range.endInclusive)
    }

    @Test
    fun monthlyRangeUsesFirstAndLastDayOfMonth() {
        val range = calculator.rangeFor(BudgetPeriod.Monthly, LocalDate.of(2026, 2, 14))
        assertEquals(LocalDate.of(2026, 2, 1), range.start)
        assertEquals(LocalDate.of(2026, 2, 28), range.endInclusive)
    }

    @Test
    fun yearlyRangeUsesCalendarYear() {
        val range = calculator.rangeFor(BudgetPeriod.Yearly, LocalDate.of(2026, 9, 3))
        assertEquals(LocalDate.of(2026, 1, 1), range.start)
        assertEquals(LocalDate.of(2026, 12, 31), range.endInclusive)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*PeriodCalculatorTest'`

Expected: FAIL because `PeriodCalculator` and `BudgetPeriod` do not exist.

- [ ] **Step 3: Add minimal domain period model and calculator**

Create `domain/model/BudgetPeriod.kt`:

```kotlin
package com.indiewalkabout.moneymagic.domain.model

sealed interface BudgetPeriod {
    data object Weekly : BudgetPeriod
    data object Monthly : BudgetPeriod
    data object Yearly : BudgetPeriod
    data class Custom(val start: java.time.LocalDate, val endInclusive: java.time.LocalDate) : BudgetPeriod
}
```

Create `core/time/PeriodCalculator.kt`:

```kotlin
package com.indiewalkabout.moneymagic.core.time

import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DateRange(val start: LocalDate, val endInclusive: LocalDate)

class PeriodCalculator {
    fun rangeFor(period: BudgetPeriod, anchor: LocalDate): DateRange =
        when (period) {
            BudgetPeriod.Weekly -> DateRange(
                start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                endInclusive = anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
            )
            BudgetPeriod.Monthly -> DateRange(
                start = anchor.withDayOfMonth(1),
                endInclusive = anchor.withDayOfMonth(anchor.lengthOfMonth()),
            )
            BudgetPeriod.Yearly -> DateRange(
                start = anchor.withDayOfYear(1),
                endInclusive = anchor.withDayOfYear(anchor.lengthOfYear()),
            )
            is BudgetPeriod.Custom -> DateRange(period.start, period.endInclusive)
        }
}
```

Create the remaining domain models with these exact shapes:

```kotlin
package com.indiewalkabout.moneymagic.domain.model

import java.time.Instant

data class Expense(
    val id: Long = 0,
    val amountMinor: Long,
    val currency: String,
    val dateTime: Instant,
    val categoryId: Long,
    val merchant: String,
    val paymentMethodId: Long?,
    val notes: String,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

```kotlin
package com.indiewalkabout.moneymagic.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val color: Long,
    val iconKey: String,
    val sortOrder: Int,
    val archived: Boolean,
)
```

```kotlin
package com.indiewalkabout.moneymagic.domain.model

data class PaymentMethod(
    val id: Long = 0,
    val name: String,
    val type: PaymentMethodType,
    val archived: Boolean,
)

enum class PaymentMethodType { Cash, Card, Bank, Other }
```

```kotlin
package com.indiewalkabout.moneymagic.domain.model

data class Budget(
    val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val period: BudgetPeriod,
    val categoryId: Long?,
    val notificationThresholdPercent: Int,
    val enabled: Boolean,
)
```

```kotlin
package com.indiewalkabout.moneymagic.domain.model

data class BudgetProgress(
    val budget: Budget,
    val spentMinor: Long,
    val remainingMinor: Long,
    val percentUsed: Int,
    val isNearTarget: Boolean,
    val isOverBudget: Boolean,
)
```

```kotlin
package com.indiewalkabout.moneymagic.domain.model

data class SpendingSummary(
    val totalMinor: Long,
    val categoryTotals: List<CategoryTotal>,
    val recentExpenses: List<Expense>,
)

data class CategoryTotal(
    val category: Category,
    val totalMinor: Long,
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*PeriodCalculatorTest'`

Expected: PASS.

- [ ] **Step 5: Commit if git exists**

Run if `.git` exists:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/core/time app/src/main/java/com/indiewalkabout/moneymagic/domain/model app/src/test/java/com/indiewalkabout/moneymagic/core/time
git commit -m "feat: add domain models and period calculations"
```

---

### Task 3: Expense Validation and Budget Progress Use Cases

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/domain/usecase/ValidateExpenseUseCase.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/domain/usecase/CalculateBudgetProgressUseCase.kt`
- Test: `app/src/test/java/com/indiewalkabout/moneymagic/domain/usecase/ValidateExpenseUseCaseTest.kt`
- Test: `app/src/test/java/com/indiewalkabout/moneymagic/domain/usecase/CalculateBudgetProgressUseCaseTest.kt`

- [ ] **Step 1: Write failing validation test**

```kotlin
package com.indiewalkabout.moneymagic.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateExpenseUseCaseTest {
    private val useCase = ValidateExpenseUseCase()

    @Test
    fun rejectsEmptyAmount() {
        val result = useCase(amountText = "", categoryId = 1)
        assertEquals(ExpenseValidationError.EmptyAmount, result.errors.single())
    }

    @Test
    fun rejectsZeroAmount() {
        val result = useCase(amountText = "0", categoryId = 1)
        assertEquals(ExpenseValidationError.AmountMustBePositive, result.errors.single())
    }

    @Test
    fun rejectsMissingCategory() {
        val result = useCase(amountText = "12.50", categoryId = null)
        assertEquals(ExpenseValidationError.MissingCategory, result.errors.single())
    }

    @Test
    fun acceptsPositiveAmountAndCategory() {
        val result = useCase(amountText = "12.50", categoryId = 1)
        assertTrue(result.errors.isEmpty())
        assertEquals(1250L, result.amountMinor)
    }
}
```

- [ ] **Step 2: Run validation test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*ValidateExpenseUseCaseTest'`

Expected: FAIL because use case types do not exist.

- [ ] **Step 3: Implement validation**

```kotlin
package com.indiewalkabout.moneymagic.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode

data class ExpenseValidationResult(
    val amountMinor: Long?,
    val errors: List<ExpenseValidationError>,
)

enum class ExpenseValidationError {
    EmptyAmount,
    AmountMustBePositive,
    MissingCategory,
    InvalidAmount,
}

class ValidateExpenseUseCase {
    operator fun invoke(amountText: String, categoryId: Long?): ExpenseValidationResult {
        val errors = mutableListOf<ExpenseValidationError>()
        val amountMinor = parseAmountMinor(amountText, errors)

        if (categoryId == null) {
            errors += ExpenseValidationError.MissingCategory
        }

        return ExpenseValidationResult(amountMinor = amountMinor, errors = errors)
    }

    private fun parseAmountMinor(
        amountText: String,
        errors: MutableList<ExpenseValidationError>,
    ): Long? {
        if (amountText.isBlank()) {
            errors += ExpenseValidationError.EmptyAmount
            return null
        }

        val amount = amountText.replace(',', '.').toBigDecimalOrNull()
        if (amount == null) {
            errors += ExpenseValidationError.InvalidAmount
            return null
        }

        if (amount <= BigDecimal.ZERO) {
            errors += ExpenseValidationError.AmountMustBePositive
            return null
        }

        return amount
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}
```

- [ ] **Step 4: Write failing budget progress test**

```kotlin
package com.indiewalkabout.moneymagic.domain.usecase

import com.indiewalkabout.moneymagic.domain.model.Budget
import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateBudgetProgressUseCaseTest {
    private val useCase = CalculateBudgetProgressUseCase()

    @Test
    fun calculatesRemainingPercentAndNearTargetState() {
        val budget = Budget(
            id = 1,
            name = "Monthly",
            amountMinor = 10000,
            currency = "EUR",
            period = BudgetPeriod.Monthly,
            categoryId = null,
            notificationThresholdPercent = 80,
            enabled = true,
        )

        val progress = useCase(budget, spentMinor = 8500)

        assertEquals(1500, progress.remainingMinor)
        assertEquals(85, progress.percentUsed)
        assertTrue(progress.isNearTarget)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun marksOverBudgetWhenSpentExceedsBudget() {
        val budget = Budget(1, "Monthly", 10000, "EUR", BudgetPeriod.Monthly, null, 80, true)

        val progress = useCase(budget, spentMinor = 12500)

        assertEquals(-2500, progress.remainingMinor)
        assertEquals(125, progress.percentUsed)
        assertTrue(progress.isOverBudget)
    }
}
```

- [ ] **Step 5: Run budget progress test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*CalculateBudgetProgressUseCaseTest'`

Expected: FAIL because `CalculateBudgetProgressUseCase` does not exist.

- [ ] **Step 6: Implement budget progress**

```kotlin
package com.indiewalkabout.moneymagic.domain.usecase

import com.indiewalkabout.moneymagic.domain.model.Budget
import com.indiewalkabout.moneymagic.domain.model.BudgetProgress
import kotlin.math.roundToInt

class CalculateBudgetProgressUseCase {
    operator fun invoke(budget: Budget, spentMinor: Long): BudgetProgress {
        val percentUsed = if (budget.amountMinor <= 0) {
            0
        } else {
            ((spentMinor.toDouble() / budget.amountMinor.toDouble()) * 100).roundToInt()
        }

        return BudgetProgress(
            budget = budget,
            spentMinor = spentMinor,
            remainingMinor = budget.amountMinor - spentMinor,
            percentUsed = percentUsed,
            isNearTarget = percentUsed >= budget.notificationThresholdPercent,
            isOverBudget = spentMinor > budget.amountMinor,
        )
    }
}
```

- [ ] **Step 7: Run use case tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*ValidateExpenseUseCaseTest' --tests '*CalculateBudgetProgressUseCaseTest'`

Expected: PASS.

- [ ] **Step 8: Commit if git exists**

Run if `.git` exists:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/domain/usecase app/src/test/java/com/indiewalkabout/moneymagic/domain/usecase
git commit -m "feat: add expense validation and budget progress logic"
```

---

### Task 4: Room Database and Repositories

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/data/local/*.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/data/repository/*.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/domain/repository/*.kt`
- Test: `app/src/test/java/com/indiewalkabout/moneymagic/data/ExpenseRepositoryImplTest.kt`

- [ ] **Step 1: Write failing repository test**

```kotlin
package com.indiewalkabout.moneymagic.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.indiewalkabout.moneymagic.data.local.MoneyMagicDatabase
import com.indiewalkabout.moneymagic.data.repository.ExpenseRepositoryImpl
import com.indiewalkabout.moneymagic.domain.model.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ExpenseRepositoryImplTest {
    private lateinit var database: MoneyMagicDatabase
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoneyMagicDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ExpenseRepositoryImpl(database.expenseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesAndObservesExpensesNewestFirst() = runTest {
        val now = Instant.parse("2026-06-22T10:00:00Z")
        repository.save(
            Expense(
                amountMinor = 1250,
                currency = "EUR",
                dateTime = now,
                categoryId = 1,
                merchant = "Bakery",
                paymentMethodId = null,
                notes = "",
                tags = listOf("food"),
                createdAt = now,
                updatedAt = now,
            )
        )

        val expenses = repository.observeExpenses().first()

        assertEquals(1, expenses.size)
        assertEquals("Bakery", expenses.single().merchant)
        assertEquals(listOf("food"), expenses.single().tags)
    }
}
```

- [ ] **Step 2: Run repository test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*ExpenseRepositoryImplTest'`

Expected: FAIL because database, DAO, and repository classes do not exist.

- [ ] **Step 3: Add Room entities, converters, DAO, database, and repository**

Create `domain/repository/ExpenseRepository.kt`:

```kotlin
package com.indiewalkabout.moneymagic.domain.repository

import com.indiewalkabout.moneymagic.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<Expense>>
    suspend fun save(expense: Expense): Long
    suspend fun delete(expenseId: Long)
}
```

Create `data/local/Converters.kt`:

```kotlin
package com.indiewalkabout.moneymagic.data.local

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter fun instantToString(value: Instant?): String? = value?.toString()
    @TypeConverter fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)
    @TypeConverter fun tagsToString(value: List<String>): String = value.joinToString("|")
    @TypeConverter fun stringToTags(value: String): List<String> =
        value.split("|").filter { it.isNotBlank() }
}
```

Create `data/local/ExpenseEntity.kt`:

```kotlin
package com.indiewalkabout.moneymagic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val currency: String,
    val dateTime: Instant,
    val categoryId: Long,
    val merchant: String,
    val paymentMethodId: Long?,
    val notes: String,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

Create `data/local/ExpenseDao.kt`:

```kotlin
package com.indiewalkabout.moneymagic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateTime DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun delete(expenseId: Long)
}
```

Create `data/local/MoneyMagicDatabase.kt`:

```kotlin
package com.indiewalkabout.moneymagic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ExpenseEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MoneyMagicDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}
```

Create `data/repository/ExpenseMappers.kt`:

```kotlin
package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.ExpenseEntity
import com.indiewalkabout.moneymagic.domain.model.Expense

fun Expense.toEntity(): ExpenseEntity =
    ExpenseEntity(id, amountMinor, currency, dateTime, categoryId, merchant, paymentMethodId, notes, tags, createdAt, updatedAt)

fun ExpenseEntity.toDomain(): Expense =
    Expense(id, amountMinor, currency, dateTime, categoryId, merchant, paymentMethodId, notes, tags, createdAt, updatedAt)
```

Create `data/repository/ExpenseRepositoryImpl.kt`:

```kotlin
package com.indiewalkabout.moneymagic.data.repository

import com.indiewalkabout.moneymagic.data.local.ExpenseDao
import com.indiewalkabout.moneymagic.domain.model.Expense
import com.indiewalkabout.moneymagic.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao,
) : ExpenseRepository {
    override fun observeExpenses(): Flow<List<Expense>> =
        dao.observeExpenses().map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(expense: Expense): Long =
        dao.upsert(expense.toEntity())

    override suspend fun delete(expenseId: Long) {
        dao.delete(expenseId)
    }
}
```

- [ ] **Step 4: Run repository test**

Run: `./gradlew :app:testDebugUnitTest --tests '*ExpenseRepositoryImplTest'`

Expected: PASS.

- [ ] **Step 5: Add category, payment method, budget, and alert repository contracts**

Create `domain/repository/BudgetRepository.kt`:

```kotlin
package com.indiewalkabout.moneymagic.domain.repository

import com.indiewalkabout.moneymagic.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeBudgets(): Flow<List<Budget>>
    suspend fun save(budget: Budget): Long
    suspend fun delete(budgetId: Long)
}
```

Create `domain/repository/CategoryRepository.kt`:

```kotlin
package com.indiewalkabout.moneymagic.domain.repository

import com.indiewalkabout.moneymagic.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(includeArchived: Boolean = false): Flow<List<Category>>
    suspend fun save(category: Category): Long
}
```

Create `domain/repository/PaymentMethodRepository.kt`:

```kotlin
package com.indiewalkabout.moneymagic.domain.repository

import com.indiewalkabout.moneymagic.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun observePaymentMethods(includeArchived: Boolean = false): Flow<List<PaymentMethod>>
    suspend fun save(paymentMethod: PaymentMethod): Long
}
```

Create `domain/repository/BudgetAlertRepository.kt`:

```kotlin
package com.indiewalkabout.moneymagic.domain.repository

interface BudgetAlertRepository {
    suspend fun wasThresholdAlertSent(budgetId: Long, periodKey: String): Boolean
    suspend fun markThresholdAlertSent(budgetId: Long, periodKey: String)
}
```

- [ ] **Step 6: Add remaining local table shapes**

Create entities and DAOs with these exact table names and primary keys:

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val iconKey: String,
    val sortOrder: Int,
    val archived: Boolean,
)

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val archived: Boolean,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val periodType: String,
    val customStartDate: String?,
    val customEndDate: String?,
    val categoryId: Long?,
    val notificationThresholdPercent: Int,
    val enabled: Boolean,
)

@Entity(tableName = "budget_alerts", primaryKeys = ["budgetId", "periodKey"])
data class BudgetAlertEntity(
    val budgetId: Long,
    val periodKey: String,
)
```

Each DAO must include an `observe...()` query ordered by user-visible name or sort order, an `@Insert(onConflict = OnConflictStrategy.REPLACE)` save method, and delete/archive queries needed by the UI. Update `MoneyMagicDatabase` to include all five entities and DAO accessors.

- [ ] **Step 7: Run database tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*RepositoryImplTest'`

Expected: PASS for implemented repository tests.

- [ ] **Step 8: Commit if git exists**

Run if `.git` exists:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/data app/src/main/java/com/indiewalkabout/moneymagic/domain/repository app/src/test/java/com/indiewalkabout/moneymagic/data
git commit -m "feat: add local database repositories"
```

---

### Task 5: Hilt Modules and Main Activity Shell

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/MainActivity.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/di/RepositoryModule.kt`

- [ ] **Step 1: Add Hilt activity annotation**

Modify `MainActivity.kt`:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
```

Add import:

```kotlin
import dagger.hilt.android.AndroidEntryPoint
```

- [ ] **Step 2: Add database module**

```kotlin
package com.indiewalkabout.moneymagic.di

import android.content.Context
import androidx.room.Room
import com.indiewalkabout.moneymagic.data.local.ExpenseDao
import com.indiewalkabout.moneymagic.data.local.MoneyMagicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyMagicDatabase =
        Room.databaseBuilder(context, MoneyMagicDatabase::class.java, "money_magic.db")
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides
    fun provideExpenseDao(database: MoneyMagicDatabase): ExpenseDao = database.expenseDao()
}
```

- [ ] **Step 3: Add repository module**

```kotlin
package com.indiewalkabout.moneymagic.di

import com.indiewalkabout.moneymagic.data.repository.ExpenseRepositoryImpl
import com.indiewalkabout.moneymagic.domain.repository.ExpenseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository
}
```

Extend the module with budget/category/payment/alert bindings after Task 4 adds those implementations.

- [ ] **Step 4: Verify Hilt compilation**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 5: Commit if git exists**

Run if `.git` exists:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/MainActivity.kt app/src/main/java/com/indiewalkabout/moneymagic/di
git commit -m "feat: wire database and repositories with hilt"
```

---

### Task 6: Dashboard, Expenses, Budgets, and Settings UI Shell

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/MainActivity.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/navigation/MoneyMagicNavHost.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/dashboard/DashboardScreen.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/expenses/ExpensesScreen.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/budgets/BudgetsScreen.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/settings/SettingsScreen.kt`
- Android Test: `app/src/androidTest/java/com/indiewalkabout/moneymagic/MoneyMagicNavigationTest.kt`

- [ ] **Step 1: Write failing Compose navigation smoke test**

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.indiewalkabout.moneymagic.MoneyMagicNavigationTest`

Expected: FAIL because the UI still shows the starter greeting.

- [ ] **Step 3: Add screen shells**

Create `DashboardScreen.kt`:

```kotlin
package com.indiewalkabout.moneymagic.presentation.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Text("Budget progress")
        Text("Recent expenses")
        Text("Top categories")
    }
}
```

Create `ExpensesScreen.kt`:

```kotlin
package com.indiewalkabout.moneymagic.presentation.expenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExpensesScreen(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text("Expense history", style = MaterialTheme.typography.headlineMedium)
        Text("Filter by period")
        Text("Sort by newest")
    }
}
```

Create `BudgetsScreen.kt`:

```kotlin
package com.indiewalkabout.moneymagic.presentation.budgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BudgetsScreen(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text("Budget control", style = MaterialTheme.typography.headlineMedium)
        Text("Weekly budgets")
        Text("Monthly budgets")
    }
}
```

Create `SettingsScreen.kt`:

```kotlin
package com.indiewalkabout.moneymagic.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Categories")
        Text("Payment methods")
    }
}
```

- [ ] **Step 4: Add nav host and bottom navigation**

Create `MoneyMagicNavHost.kt` with four destinations. Use Navigation 3's user-owned back stack with `NavDisplay`.

```kotlin
package com.indiewalkabout.moneymagic.presentation.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.indiewalkabout.moneymagic.presentation.budgets.BudgetsScreen
import com.indiewalkabout.moneymagic.presentation.dashboard.DashboardScreen
import com.indiewalkabout.moneymagic.presentation.expenses.ExpensesScreen
import com.indiewalkabout.moneymagic.presentation.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute : NavKey
@Serializable data object ExpensesRoute : NavKey
@Serializable data object BudgetsRoute : NavKey
@Serializable data object SettingsRoute : NavKey

private data class TopLevelDestination(
    val key: NavKey,
    val label: String,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(DashboardRoute, "Dashboard"),
    TopLevelDestination(ExpensesRoute, "Expenses"),
    TopLevelDestination(BudgetsRoute, "Budgets"),
    TopLevelDestination(SettingsRoute, "Settings"),
)

@Composable
fun MoneyMagicNavHost() {
    val backStack = rememberNavBackStack(DashboardRoute)
    val current = backStack.lastOrNull() ?: DashboardRoute

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination.key,
                        onClick = {
                            if (current != destination.key) {
                                backStack.clear()
                                backStack.add(destination.key)
                            }
                        },
                        label = { Text(destination.label) },
                        icon = {},
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<DashboardRoute> { DashboardScreen() }
                entry<ExpensesRoute> { ExpensesScreen() }
                entry<BudgetsRoute> { BudgetsScreen() }
                entry<SettingsRoute> { SettingsScreen() }
            },
        )
    }
}
```

Modify `MainActivity.kt` content to render `MoneyMagicNavHost()` inside `MoneyMagicTheme`.

- [ ] **Step 5: Run smoke test**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.indiewalkabout.moneymagic.MoneyMagicNavigationTest`

Expected: PASS on a connected emulator/device.

- [ ] **Step 6: Commit if git exists**

Run if `.git` exists:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/MainActivity.kt app/src/main/java/com/indiewalkabout/moneymagic/presentation app/src/androidTest/java/com/indiewalkabout/moneymagic/MoneyMagicNavigationTest.kt
git commit -m "feat: add main compose navigation shell"
```

---

### Task 7: Manual Expense Flow and Dashboard State

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/expenses/AddExpenseViewModel.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/expenses/AddExpenseScreen.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/presentation/dashboard/DashboardViewModel.kt`
- Test: `app/src/test/java/com/indiewalkabout/moneymagic/presentation/AddExpenseViewModelTest.kt`

- [ ] **Step 1: Write failing AddExpenseViewModel test**

```kotlin
package com.indiewalkabout.moneymagic.presentation

import com.indiewalkabout.moneymagic.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.domain.usecase.ValidateExpenseUseCase
import com.indiewalkabout.moneymagic.presentation.expenses.AddExpenseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AddExpenseViewModelTest {
    @Test
    fun invalidAmountShowsErrorAndDoesNotSave() = runTest {
        val repository = FakeExpenseRepository()
        val viewModel = AddExpenseViewModel(repository, ValidateExpenseUseCase())

        viewModel.onAmountChanged("0")
        viewModel.onCategorySelected(1)
        viewModel.save()

        assertFalse(viewModel.uiState.value.canSave)
        assertEquals(0, repository.savedCount)
    }
}

private class FakeExpenseRepository : ExpenseRepository {
    var savedCount = 0
    override fun observeExpenses(): Flow<List<com.indiewalkabout.moneymagic.domain.model.Expense>> = emptyFlow()
    override suspend fun save(expense: com.indiewalkabout.moneymagic.domain.model.Expense): Long {
        savedCount += 1
        return savedCount.toLong()
    }
    override suspend fun delete(expenseId: Long) = Unit
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*AddExpenseViewModelTest'`

Expected: FAIL because ViewModel does not exist.

- [ ] **Step 3: Implement AddExpenseViewModel**

```kotlin
package com.indiewalkabout.moneymagic.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiewalkabout.moneymagic.domain.model.Expense
import com.indiewalkabout.moneymagic.domain.repository.ExpenseRepository
import com.indiewalkabout.moneymagic.domain.usecase.ValidateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class AddExpenseUiState(
    val amountText: String = "",
    val categoryId: Long? = null,
    val merchant: String = "",
    val notes: String = "",
    val canSave: Boolean = true,
    val errorText: String? = null,
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val validateExpense: ValidateExpenseUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    fun onAmountChanged(value: String) {
        _uiState.value = _uiState.value.copy(amountText = value, canSave = true, errorText = null)
    }

    fun onCategorySelected(value: Long) {
        _uiState.value = _uiState.value.copy(categoryId = value, canSave = true, errorText = null)
    }

    fun save() {
        val state = _uiState.value
        val validation = validateExpense(state.amountText, state.categoryId)
        if (validation.errors.isNotEmpty() || validation.amountMinor == null) {
            _uiState.value = state.copy(canSave = false, errorText = "Check amount and category")
            return
        }

        viewModelScope.launch {
            val now = Instant.now()
            expenseRepository.save(
                Expense(
                    amountMinor = validation.amountMinor,
                    currency = "EUR",
                    dateTime = now,
                    categoryId = state.categoryId,
                    merchant = state.merchant,
                    paymentMethodId = null,
                    notes = state.notes,
                    tags = emptyList(),
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }
}
```

- [ ] **Step 4: Run ViewModel test**

Run: `./gradlew :app:testDebugUnitTest --tests '*AddExpenseViewModelTest'`

Expected: PASS.

- [ ] **Step 5: Add Compose Add Expense form and wire Dashboard quick add**

Create `AddExpenseScreen.kt` with amount, category, merchant, notes fields, and a Save button. Use existing Material 3 components: `OutlinedTextField`, `Button`, `Scaffold`, and `SnackbarHost` for validation errors.

- [ ] **Step 6: Verify app compiles**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 7: Commit if git exists**

Run if `.git` exists:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/presentation app/src/test/java/com/indiewalkabout/moneymagic/presentation
git commit -m "feat: add manual expense flow"
```

---

### Task 8: Budget Alerts and Local Notifications

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/data/notification/BudgetNotificationService.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/domain/usecase/CheckBudgetThresholdUseCase.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/indiewalkabout/moneymagic/domain/usecase/CheckBudgetThresholdUseCaseTest.kt`

- [ ] **Step 1: Write failing threshold alert test**

```kotlin
package com.indiewalkabout.moneymagic.domain.usecase

import com.indiewalkabout.moneymagic.domain.model.Budget
import com.indiewalkabout.moneymagic.domain.model.BudgetPeriod
import com.indiewalkabout.moneymagic.domain.model.BudgetProgress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckBudgetThresholdUseCaseTest {
    @Test
    fun emitsAlertOnlyOncePerBudgetPeriod() = runTest {
        val repository = FakeBudgetAlertRepository()
        val useCase = CheckBudgetThresholdUseCase(repository)
        val budget = Budget(1, "Monthly", 10000, "EUR", BudgetPeriod.Monthly, null, 80, true)
        val progress = BudgetProgress(budget, 8500, 1500, 85, true, false)

        assertTrue(useCase(progress, periodKey = "2026-06"))
        assertFalse(useCase(progress, periodKey = "2026-06"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*CheckBudgetThresholdUseCaseTest'`

Expected: FAIL because `CheckBudgetThresholdUseCase` has not been created.

- [ ] **Step 3: Implement threshold use case**

```kotlin
package com.indiewalkabout.moneymagic.domain.usecase

import com.indiewalkabout.moneymagic.domain.model.BudgetProgress
import com.indiewalkabout.moneymagic.domain.repository.BudgetAlertRepository
import javax.inject.Inject

class CheckBudgetThresholdUseCase @Inject constructor(
    private val alertRepository: BudgetAlertRepository,
) {
    suspend operator fun invoke(progress: BudgetProgress, periodKey: String): Boolean {
        if (!progress.budget.enabled || !progress.isNearTarget) return false
        if (alertRepository.wasThresholdAlertSent(progress.budget.id, periodKey)) return false
        alertRepository.markThresholdAlertSent(progress.budget.id, periodKey)
        return true
    }
}
```

Add this fake inside the test file:

```kotlin
private class FakeBudgetAlertRepository : com.indiewalkabout.moneymagic.domain.repository.BudgetAlertRepository {
    private val sent = mutableSetOf<Pair<Long, String>>()
    override suspend fun wasThresholdAlertSent(budgetId: Long, periodKey: String): Boolean =
        sent.contains(budgetId to periodKey)
    override suspend fun markThresholdAlertSent(budgetId: Long, periodKey: String) {
        sent += budgetId to periodKey
    }
}
```

- [ ] **Step 4: Add notification permission**

Add before `<application>` in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- [ ] **Step 5: Implement notification service**

```kotlin
package com.indiewalkabout.moneymagic.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.indiewalkabout.moneymagic.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BudgetNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun showBudgetThresholdNotification(budgetName: String, percentUsed: Int) {
        val channelId = "budget_alerts"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Budget alert")
            .setContentText("$budgetName is $percentUsed% used")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(budgetName.hashCode(), notification)
    }
}
```

- [ ] **Step 6: Run threshold tests and compile**

Run: `./gradlew :app:testDebugUnitTest --tests '*CheckBudgetThresholdUseCaseTest' && ./gradlew :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 7: Commit if git exists**

Run if `.git` exists:

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/indiewalkabout/moneymagic/data/notification app/src/main/java/com/indiewalkabout/moneymagic/domain/usecase app/src/test/java/com/indiewalkabout/moneymagic/domain/usecase
git commit -m "feat: add budget threshold alerts"
```

---

### Task 9: Final Verification

**Files:**
- Review: `app/src/main/java/com/indiewalkabout/moneymagic`
- Review: `app/src/test/java/com/indiewalkabout/moneymagic`
- Review: `app/src/androidTest/java/com/indiewalkabout/moneymagic`

- [ ] **Step 1: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: all unit tests pass.

- [ ] **Step 2: Run Kotlin compilation**

Run: `./gradlew :app:compileDebugKotlin`

Expected: compile succeeds.

- [ ] **Step 3: Run instrumented smoke test**

Run with emulator/device connected:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: navigation smoke test passes.

- [ ] **Step 4: Build debug APK**

Run: `./gradlew :app:assembleDebug`

Expected: debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 5: Manual smoke test**

Install and open the app, then verify:

- Dashboard opens first.
- Add Expense form rejects zero amount.
- Add Expense form saves a positive amount with category.
- Expenses screen shows the saved expense.
- Budget screen accepts a monthly budget.
- Dashboard reflects spending against budget.

- [ ] **Step 6: Commit if git exists**

Run if `.git` exists:

```bash
git status --short
git add app docs/superpowers
git commit -m "feat: complete offline spending control mvp"
```
