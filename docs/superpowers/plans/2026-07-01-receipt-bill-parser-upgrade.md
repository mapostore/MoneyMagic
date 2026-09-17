# Receipt and Bill Parser Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve offline Italian and English receipt/bill capture by making OCR text parsing more accurate while preserving the existing ML Kit and Add Expense review flow.

**Architecture:** Keep OCR and parsing separate. Add small capture-domain models for confidence and parse results, then upgrade `ParseReceiptTextUseCase` with deterministic line normalization, merchant/date extraction, and scored amount extraction. Presentation changes stay light: nonblank OCR text should preserve raw text and expose a partial draft even when fields are weak or missing.

**Tech Stack:** Kotlin, JUnit, coroutines/Flow, Hilt, Compose, ML Kit on-device text recognition.

---

## File Structure

- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/model/ReceiptDraft.kt`
  - Keep existing draft fields and add optional internal confidence metadata with default values so existing callers keep compiling.
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/model/ReceiptFieldConfidence.kt`
  - Defines `High`, `Medium`, `Low`, and `Missing`.
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/model/ReceiptParseMetadata.kt`
  - Holds confidence for merchant, amount, and date.
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCase.kt`
  - Replace simple regex-only parsing with normalized lines and scored extraction helpers.
- Modify: `app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCaseTest.kt`
  - Add realistic English and Italian parser coverage.
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/presentation/ReceiptCaptureViewModel.kt`
  - Ensure parser failures cannot crash capture and nonblank OCR text yields a draft/raw text instead of a scan failure.
- Create: `app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/presentation/ReceiptCaptureViewModelTest.kt`
  - Verify weak parse and blank OCR behavior.

---

### Task 1: Add Parser Confidence Models

**Files:**
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/model/ReceiptFieldConfidence.kt`
- Create: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/model/ReceiptParseMetadata.kt`
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/model/ReceiptDraft.kt`
- Test: `app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCaseTest.kt`

- [ ] **Step 1: Write the failing confidence metadata test**

Add this test to `ParseReceiptTextUseCaseTest`:

```kotlin
@Test
fun marksRiskyFallbackAmountAsMediumConfidence() {
    val draft = parseReceiptText(
        """
        Corner Cafe
        Espresso 1.20
        Sandwich 6.40
        Tax 0.30
        """.trimIndent(),
    )

    assertEquals("6.40", draft.amount)
    assertEquals(ReceiptFieldConfidence.Medium, draft.metadata.amountConfidence)
}
```

Add this import:

```kotlin
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
```

- [ ] **Step 2: Run the focused parser test and verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCaseTest
```

Expected: compilation fails because `ReceiptFieldConfidence` or `metadata` does not exist.

- [ ] **Step 3: Add the confidence enum**

Create `ReceiptFieldConfidence.kt`:

```kotlin
package com.indiewalkabout.moneymagic.feature.capture.domain.model

enum class ReceiptFieldConfidence {
    High,
    Medium,
    Low,
    Missing,
}
```

- [ ] **Step 4: Add parse metadata**

Create `ReceiptParseMetadata.kt`:

```kotlin
package com.indiewalkabout.moneymagic.feature.capture.domain.model

data class ReceiptParseMetadata(
    val merchantConfidence: ReceiptFieldConfidence = ReceiptFieldConfidence.Missing,
    val amountConfidence: ReceiptFieldConfidence = ReceiptFieldConfidence.Missing,
    val dateConfidence: ReceiptFieldConfidence = ReceiptFieldConfidence.Missing,
)
```

- [ ] **Step 5: Extend ReceiptDraft with metadata**

Update `ReceiptDraft.kt`:

```kotlin
package com.indiewalkabout.moneymagic.feature.capture.domain.model

data class ReceiptDraft(
    val name: String = "",
    val merchant: String = "",
    val amount: String = "",
    val date: String = "",
    val rawText: String = "",
    val metadata: ReceiptParseMetadata = ReceiptParseMetadata(),
)
```

- [ ] **Step 6: Return metadata from the current parser**

In `ParseReceiptTextUseCase.kt`, import:

```kotlin
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptFieldConfidence
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptParseMetadata
```

Change `invoke` so it stores field values and basic confidence:

```kotlin
val merchant = lines.firstOrNull(::looksLikeMerchant).orEmpty()
val amount = findTotalAmount(lines).orEmpty()
val date = findDate(lines).orEmpty()
return ReceiptDraft(
    name = merchant,
    merchant = merchant,
    amount = amount,
    date = date,
    rawText = text,
    metadata = ReceiptParseMetadata(
        merchantConfidence = if (merchant.isBlank()) ReceiptFieldConfidence.Missing else ReceiptFieldConfidence.High,
        amountConfidence = if (amount.isBlank()) ReceiptFieldConfidence.Missing else ReceiptFieldConfidence.Medium,
        dateConfidence = if (date.isBlank()) ReceiptFieldConfidence.Missing else ReceiptFieldConfidence.High,
    ),
)
```

- [ ] **Step 7: Run focused parser tests and verify they pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCaseTest
```

Expected: parser tests pass.

- [ ] **Step 8: Commit confidence model work**

Run:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/model app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCase.kt app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCaseTest.kt
git commit -m "feat: add receipt parse confidence"
```

---

### Task 2: Upgrade Amount Extraction With Scored Candidates

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCase.kt`
- Modify: `app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCaseTest.kt`

- [ ] **Step 1: Add failing tests for Italian IVA and card metadata**

Append these tests:

```kotlin
@Test
fun parsesItalianTotalWithoutChoosingIvaOrImponibile() {
    val draft = parseReceiptText(
        """
        CONAD CITY
        DOCUMENTO COMMERCIALE
        Data 24/06/2026 Ora 18:42
        Imponibile 14,34
        IVA 10% 1,43
        Totale complessivo 15,77
        Pagamento elettronico 15,77
        """.trimIndent(),
    )

    assertEquals("CONAD CITY", draft.merchant)
    assertEquals("15.77", draft.amount)
    assertEquals(ReceiptFieldConfidence.High, draft.metadata.amountConfidence)
}

@Test
fun ignoresCardAuthorizationAndTransactionNumbers() {
    val draft = parseReceiptText(
        """
        Payment Receipt
        CARD VISA **** 1234
        AUTH 987654
        TRANSACTION 000123456789
        AMOUNT PAID EUR 42.80
        """.trimIndent(),
    )

    assertEquals("42.80", draft.amount)
    assertEquals(ReceiptFieldConfidence.High, draft.metadata.amountConfidence)
}
```

- [ ] **Step 2: Add failing test for thousand separators**

Append:

```kotlin
@Test
fun parsesItalianAndEnglishThousandSeparators() {
    val italian = parseReceiptText("Bolletta Energia\nImporto da pagare € 1.234,56")
    val english = parseReceiptText("Utility Bill\nAmount due EUR 1,234.56")

    assertEquals("1234.56", italian.amount)
    assertEquals("1234.56", english.amount)
}
```

- [ ] **Step 3: Run focused parser tests and verify failures**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCaseTest
```

Expected: at least the thousand-separator test fails with the current parser.

- [ ] **Step 4: Replace amount regex and add scored result types**

In `ParseReceiptTextUseCase.kt`, replace `amountPattern`, `totalKeywords`, and `findTotalAmount` with this structure:

```kotlin
private val moneyPattern = Regex(
    pattern = """(?<![\dA-Za-z])(?:€|EUR|EURO|USD|\$)?\s*(\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d{1,6}[.,]\d{2})(?![\dA-Za-z])""",
    option = RegexOption.IGNORE_CASE,
)
private val percentPattern = Regex("""\b\d{1,2}(?:[.,]\d{1,2})?\s*%""")

private val strongTotalKeywords = listOf(
    "totale complessivo",
    "importo totale",
    "importo pagato",
    "importo da pagare",
    "da pagare",
    "totale",
    "pagato",
    "saldo",
    "grand total",
    "amount due",
    "amount paid",
    "balance due",
    "total",
    "paid",
)

private val weakAmountKeywords = listOf(
    "subtotale",
    "imponibile",
    "iva",
    "resto",
    "sconto",
    "contanti",
    "carta",
    "bancomat",
    "pos",
    "transazione",
    "subtotal",
    "tax",
    "vat",
    "change",
    "cash",
    "card",
    "authorization",
    "auth",
    "transaction",
)

private data class ParsedAmount(
    val value: BigDecimal,
    val confidence: ReceiptFieldConfidence,
    val score: Int,
    val lineIndex: Int,
)
```

- [ ] **Step 5: Add scored amount extraction implementation**

Add these helper functions:

```kotlin
private fun findTotalAmount(lines: List<String>): ParsedAmount? {
    val candidates = lines.flatMapIndexed { index, line -> amountCandidates(line, index) }
    return candidates
        .sortedWith(
            compareByDescending<ParsedAmount> { it.score }
                .thenByDescending { it.lineIndex }
                .thenByDescending { it.value },
        )
        .firstOrNull()
}

private fun amountCandidates(line: String, lineIndex: Int): List<ParsedAmount> {
    if (percentPattern.containsMatchIn(line)) {
        val percentRanges = percentPattern.findAll(line).map { it.range }.toList()
        return moneyPattern.findAll(line)
            .filterNot { match -> percentRanges.any { match.range.first >= it.first && match.range.last <= it.last } }
            .mapNotNull { match -> scoredAmount(line, match.groupValues[1], lineIndex) }
            .toList()
    }
    return moneyPattern.findAll(line)
        .mapNotNull { match -> scoredAmount(line, match.groupValues[1], lineIndex) }
        .toList()
}

private fun scoredAmount(line: String, rawAmount: String, lineIndex: Int): ParsedAmount? {
    val value = parseAmount(rawAmount) ?: return null
    if (value <= BigDecimal.ZERO) return null
    val lower = line.lowercase(Locale.ROOT)
    val strong = strongTotalKeywords.any { lower.contains(it) }
    val weak = weakAmountKeywords.any { lower.contains(it) }
    val score = when {
        strong -> 100
        weak -> 10
        else -> 40
    }
    val confidence = when {
        strong -> ReceiptFieldConfidence.High
        weak -> ReceiptFieldConfidence.Low
        else -> ReceiptFieldConfidence.Medium
    }
    return ParsedAmount(value = value, confidence = confidence, score = score, lineIndex = lineIndex)
}
```

- [ ] **Step 6: Normalize amount parsing for Italian and English separators**

Replace `parseAmount`:

```kotlin
private fun parseAmount(amount: String): BigDecimal? {
    val normalized = normalizeAmount(amount) ?: return null
    return normalized.toBigDecimalOrNull()
}

private fun normalizeAmount(amount: String): String? {
    val compact = amount.filter { it.isDigit() || it == ',' || it == '.' }
    if (compact.isBlank()) return null
    val lastComma = compact.lastIndexOf(',')
    val lastDot = compact.lastIndexOf('.')
    val decimalSeparator = when {
        lastComma == -1 -> '.'
        lastDot == -1 -> ','
        lastComma > lastDot -> ','
        else -> '.'
    }
    val integer = compact.substringBeforeLast(decimalSeparator, missingDelimiterValue = compact)
        .filter(Char::isDigit)
    val cents = compact.substringAfterLast(decimalSeparator, missingDelimiterValue = "")
        .filter(Char::isDigit)
    if (integer.isBlank() || cents.length != 2) return null
    return "$integer.$cents"
}
```

- [ ] **Step 7: Update parser invoke to use parsed amount confidence**

In `invoke`, replace amount lookup with:

```kotlin
val parsedAmount = findTotalAmount(lines)
val amount = parsedAmount?.value?.toPlainAmount().orEmpty()
```

Set metadata amount confidence:

```kotlin
amountConfidence = parsedAmount?.confidence ?: ReceiptFieldConfidence.Missing,
```

- [ ] **Step 8: Update merchant filtering to use the new money pattern**

In `looksLikeMerchant`, replace `amountPattern.find(line) == null` with:

```kotlin
moneyPattern.find(line) == null
```

- [ ] **Step 9: Run focused parser tests and verify they pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCaseTest
```

Expected: all parser tests pass.

- [ ] **Step 10: Commit amount extraction upgrade**

Run:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCase.kt app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCaseTest.kt
git commit -m "feat: improve receipt amount parsing"
```

---

### Task 3: Upgrade Date And Merchant Extraction

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCase.kt`
- Modify: `app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCaseTest.kt`

- [ ] **Step 1: Add failing month-name and merchant cleanup tests**

Append:

```kotlin
@Test
fun parsesItalianMonthNameDateAndSkipsFiscalHeader() {
    val draft = parseReceiptText(
        """
        DOCUMENTO COMMERCIALE
        ALIMENTARI ROSSI SRL
        Via Torino 8
        24 giugno 2026
        Totale 18,20
        """.trimIndent(),
    )

    assertEquals("ALIMENTARI ROSSI SRL", draft.merchant)
    assertEquals("2026-06-24", draft.date)
    assertEquals(ReceiptFieldConfidence.High, draft.metadata.dateConfidence)
}

@Test
fun parsesEnglishMonthNameDate() {
    val draft = parseReceiptText(
        """
        North Market
        Tax Receipt
        June 24, 2026
        Grand Total $32.10
        """.trimIndent(),
    )

    assertEquals("North Market", draft.merchant)
    assertEquals("2026-06-24", draft.date)
}
```

- [ ] **Step 2: Add failing English numeric date test**

Append:

```kotlin
@Test
fun parsesEnglishMonthFirstDateWhenDateLabelIsEnglish() {
    val draft = parseReceiptText(
        """
        City Pharmacy
        Date 06/24/2026
        Total 9.99
        """.trimIndent(),
    )

    assertEquals("2026-06-24", draft.date)
}
```

- [ ] **Step 3: Run focused parser tests and verify failures**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCaseTest
```

Expected: month-name and English month-first tests fail.

- [ ] **Step 4: Add merchant exclusion keywords**

In `ParseReceiptTextUseCase.kt`, add:

```kotlin
private val nonMerchantKeywords = listOf(
    "scontrino",
    "ricevuta",
    "fattura",
    "documento commerciale",
    "receipt",
    "invoice",
    "tax receipt",
    "date",
    "data",
    "ora",
    "time",
)
```

Replace `looksLikeMerchant` with:

```kotlin
private fun looksLikeMerchant(line: String): Boolean {
    val lower = line.lowercase(Locale.ROOT)
    val letters = line.count(Char::isLetter)
    val digits = line.count(Char::isDigit)
    return letters >= 3 &&
        letters >= digits &&
        nonMerchantKeywords.none { lower.contains(it) } &&
        strongTotalKeywords.none { lower.contains(it) } &&
        weakAmountKeywords.none { lower.contains(it) } &&
        moneyPattern.find(line) == null &&
        parseDate(line) == null
}
```

- [ ] **Step 5: Add month-name date parsing helpers**

Add:

```kotlin
private val monthNameDatePatterns = listOf(
    Regex("""\b(\d{1,2})\s+([A-Za-zÀ-ÿ]{3,})\s+(\d{2,4})\b""", RegexOption.IGNORE_CASE),
    Regex("""\b([A-Za-zÀ-ÿ]{3,})\s+(\d{1,2}),?\s+(\d{2,4})\b""", RegexOption.IGNORE_CASE),
)

private val monthNames = mapOf(
    "gennaio" to 1, "gen" to 1, "january" to 1, "jan" to 1,
    "febbraio" to 2, "feb" to 2, "february" to 2,
    "marzo" to 3, "mar" to 3, "march" to 3,
    "aprile" to 4, "apr" to 4, "april" to 4,
    "maggio" to 5, "mag" to 5, "may" to 5,
    "giugno" to 6, "giu" to 6, "june" to 6, "jun" to 6,
    "luglio" to 7, "lug" to 7, "july" to 7, "jul" to 7,
    "agosto" to 8, "ago" to 8, "august" to 8, "aug" to 8,
    "settembre" to 9, "set" to 9, "september" to 9, "sep" to 9,
    "ottobre" to 10, "ott" to 10, "october" to 10, "oct" to 10,
    "novembre" to 11, "nov" to 11, "november" to 11,
    "dicembre" to 12, "dic" to 12, "december" to 12, "dec" to 12,
)
```

- [ ] **Step 6: Replace date extraction with a parsed confidence result**

Add:

```kotlin
private data class ParsedDate(
    val value: LocalDate,
    val confidence: ReceiptFieldConfidence,
)
```

Replace `findDate` with:

```kotlin
private fun findDate(lines: List<String>): ParsedDate? {
    lines.forEach { line ->
        parseDate(line)?.let { return it }
    }
    return null
}

private fun parseDate(line: String): ParsedDate? =
    parseIsoDate(line)
        ?: parseMonthNameDate(line)
        ?: parseNumericDate(line)
```

- [ ] **Step 7: Implement numeric and month-name date parsing**

Replace the `java.time.format.DateTimeParseException` import with:

```kotlin
import java.time.DateTimeException
```

Replace `parseIsoDate` and `parseDayFirstDate` with:

```kotlin
private fun parseIsoDate(line: String): ParsedDate? {
    val match = isoDatePattern.find(line) ?: return null
    return runCatching {
        ParsedDate(LocalDate.parse(match.value, DateTimeFormatter.ISO_LOCAL_DATE), ReceiptFieldConfidence.High)
    }.getOrNull()
}

private fun parseMonthNameDate(line: String): ParsedDate? {
    monthNameDatePatterns.forEachIndexed { index, pattern ->
        val match = pattern.find(line) ?: return@forEachIndexed
        val day = (
            if (index == 0) match.groupValues[1].toIntOrNull() else match.groupValues[2].toIntOrNull()
            ) ?: return@forEachIndexed
        val monthText = if (index == 0) match.groupValues[2] else match.groupValues[1]
        val yearText = if (index == 0) match.groupValues[3] else match.groupValues[3]
        val month = monthNames[monthText.lowercase(Locale.ROOT)] ?: return@forEachIndexed
        val year = normalizeYear(yearText)
        return runCatching {
            ParsedDate(LocalDate.of(year, month, day), ReceiptFieldConfidence.High)
        }.getOrNull()
    }
    return null
}

private fun parseNumericDate(line: String): ParsedDate? {
    val match = dayFirstDatePattern.find(line) ?: return null
    val first = match.groupValues[1].toIntOrNull() ?: return null
    val second = match.groupValues[2].toIntOrNull() ?: return null
    val year = normalizeYear(match.groupValues[3])
    val lower = line.lowercase(Locale.ROOT)
    val monthFirst = lower.contains("date") && first in 1..12 && second in 13..31
    val day = if (monthFirst) second else first
    val month = if (monthFirst) first else second
    return try {
        ParsedDate(LocalDate.of(year, month, day), ReceiptFieldConfidence.High)
    } catch (_: DateTimeException) {
        null
    }
}

private fun normalizeYear(rawYear: String): Int =
    if (rawYear.length == 2) "20$rawYear".toInt() else rawYear.toInt()
```

- [ ] **Step 8: Update invoke to use parsed date confidence**

In `invoke`, replace:

```kotlin
val date = findDate(lines).orEmpty()
```

with:

```kotlin
val parsedDate = findDate(lines)
val date = parsedDate?.value?.toString().orEmpty()
```

Set metadata date confidence:

```kotlin
dateConfidence = parsedDate?.confidence ?: ReceiptFieldConfidence.Missing,
```

- [ ] **Step 9: Run focused parser tests and verify they pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCaseTest
```

Expected: all parser tests pass.

- [ ] **Step 10: Commit date and merchant upgrade**

Run:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCase.kt app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/domain/usecase/ParseReceiptTextUseCaseTest.kt
git commit -m "feat: improve receipt merchant and date parsing"
```

---

### Task 4: Harden Capture ViewModel Weak-Parse Behavior

**Files:**
- Modify: `app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/presentation/ReceiptCaptureViewModel.kt`
- Create: `app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/presentation/ReceiptCaptureViewModelTest.kt`

- [ ] **Step 1: Check whether a ViewModel test already exists**

Run:

```bash
ls app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/presentation
```

Expected: if the directory does not exist, create it in Step 2.

- [ ] **Step 2: Add ViewModel tests for blank text and parser failure**

Create `ReceiptCaptureViewModelTest.kt`:

```kotlin
package com.indiewalkabout.moneymagic.feature.capture.presentation

import android.graphics.Bitmap
import android.net.Uri
import com.indiewalkabout.moneymagic.feature.capture.domain.repository.ReceiptTextRecognizer
import com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptCaptureViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankOcrTextShowsNoTextErrorWithoutDraft() = runTest {
        val viewModel = ReceiptCaptureViewModel(FakeRecognizer(Result.success("")), ParseReceiptTextUseCase())

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.rawText)
        assertNull(viewModel.uiState.value.draft)
        assertEquals(ReceiptCaptureError.NoTextFound, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun nonblankWeakOcrTextPreservesRawTextAndDraft() = runTest {
        val viewModel = ReceiptCaptureViewModel(FakeRecognizer(Result.success("unstructured words only")), ParseReceiptTextUseCase())

        viewModel.scanUri(Uri.EMPTY)
        advanceUntilIdle()

        assertEquals("unstructured words only", viewModel.uiState.value.rawText)
        assertNotNull(viewModel.uiState.value.draft)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private class FakeRecognizer(
        private val result: Result<String>,
    ) : ReceiptTextRecognizer {
        override suspend fun recognize(uri: Uri): Result<String> = result
        override suspend fun recognize(bitmap: Bitmap): Result<String> = result
    }
}
```

- [ ] **Step 3: Run ViewModel tests and verify behavior**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.presentation.ReceiptCaptureViewModelTest
```

Expected: if current behavior already matches, tests pass. If parser exceptions can still crash the coroutine, add Step 4.

- [ ] **Step 4: Make parser exceptions safe**

In `ReceiptCaptureViewModel.kt`, replace:

```kotlin
val draft = parseReceiptText(text)
```

with:

```kotlin
val draft = runCatching { parseReceiptText(text) }.getOrElse {
    ReceiptDraft(rawText = text)
}
```

Keep this import if needed:

```kotlin
import com.indiewalkabout.moneymagic.feature.capture.domain.model.ReceiptDraft
```

- [ ] **Step 5: Run ViewModel tests again**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.presentation.ReceiptCaptureViewModelTest
```

Expected: tests pass.

- [ ] **Step 6: Commit capture ViewModel hardening**

Run:

```bash
git add app/src/main/java/com/indiewalkabout/moneymagic/feature/capture/presentation/ReceiptCaptureViewModel.kt app/src/test/java/com/indiewalkabout/moneymagic/feature/capture/presentation/ReceiptCaptureViewModelTest.kt
git commit -m "test: cover receipt capture weak parse states"
```

---

### Task 5: Final Verification

**Files:**
- All touched files.

- [ ] **Step 1: Run parser-focused tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ParseReceiptTextUseCaseTest
```

Expected: build successful.

- [ ] **Step 2: Run all debug unit tests**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: build successful.

- [ ] **Step 3: Run Kotlin compile**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: build successful.

- [ ] **Step 4: Run debug assemble**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: build successful.

- [ ] **Step 5: Check whitespace and staged diff health**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors; status shows either a clean tree or only intentional uncommitted changes.

- [ ] **Step 6: Optional connected test when device allows installs**

Run only if an emulator or physical device accepts APK installs:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: build successful. If the device returns `INSTALL_FAILED_USER_RESTRICTED`, record that connected tests are blocked by device policy and do not treat it as a parser failure.

- [ ] **Step 7: Final commit if any verification-only fixes remain**

If Step 5 shows uncommitted intentional changes, commit them:

```bash
git add app/src/main/java app/src/test/java
git commit -m "chore: finalize receipt parser upgrade"
```

Expected: clean working tree.
