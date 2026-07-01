# Receipt and Bill Capture Parser Upgrade Design

Date: 2026-07-01

## Goal

Improve MoneyMagic's offline receipt and bill capture so Italian and English OCR text produces more reliable expense drafts. The first implementation should improve parsing quality before adding heavier image-processing features. The capture flow remains local-only, using the existing ML Kit on-device Latin text recognizer and the existing Add Expense review flow.

## Scope

Included:

- Parse Italian and English receipt/bill OCR text more accurately.
- Extract merchant, total amount, date, and raw recognized text.
- Improve amount selection so totals win over item prices, subtotals, IVA/VAT, change, authorization codes, and card metadata.
- Support common Italian and English labels used on receipts, fiscal receipts, restaurants, utilities, and payment slips.
- Add confidence metadata internally so the UI can later highlight uncertain fields.
- Keep the current gallery and camera-preview entry points.
- Improve no-text and weak-parse behavior without adding online services.
- Add deterministic parser unit tests with realistic Italian and English samples.

Deferred:

- Cloud AI, remote OCR, sync, or backend processing.
- Full document crop, perspective correction, and edge detection.
- PDF and multi-page bill import.
- Line-item extraction.
- Automatic category/payment-method selection.
- A dedicated editable capture-review screen. Drafts continue into the existing Add Expense screen.

## Architecture

The `feature.capture` package stays feature-first:

- `data`: local OCR recognizer implementation backed by ML Kit.
- `domain.model`: receipt draft models and parser confidence models.
- `domain.usecase`: pure text parsing use cases.
- `domain.repository`: OCR recognizer interface.
- `presentation`: capture screen and ViewModel.

The parser remains a pure Kotlin use case so it is easy to test without Android or ML Kit. OCR and parsing stay separate:

`image -> LocalReceiptTextRecognizer -> raw text -> ParseReceiptTextUseCase -> ReceiptDraft -> AddExpenseRoute`

The first implementation should avoid broad refactors. It may split helper classes or small domain models out of the current parser if that makes the parsing rules easier to test.

## Parser Behavior

### Text Normalization

The parser should normalize OCR text before extraction:

- Trim blank lines.
- Collapse repeated spaces.
- Keep original raw text for review.
- Keep a normalized line list for matching.
- Tolerate OCR punctuation noise around currency symbols and labels.
- Treat comma and dot decimal separators as valid.

### Merchant Extraction

The merchant should prefer the first meaningful business-like line near the top of the text.

Ignore likely non-merchant lines:

- Fiscal labels: `scontrino`, `ricevuta`, `fattura`, `documento commerciale`, `receipt`, `invoice`, `tax receipt`.
- Date/time lines.
- Amount, total, IVA/VAT, subtotal, tax, card, and payment lines.
- Address-like lines when a better business name appears above them.
- Numeric-only or mostly-numeric lines.

If no strong merchant is found, leave merchant empty rather than using a misleading line.

### Amount Extraction

The parser should score candidate amounts instead of choosing the largest number blindly.

High-priority positive labels:

- Italian: `totale`, `totale complessivo`, `importo totale`, `importo pagato`, `da pagare`, `pagato`, `saldo`.
- English: `total`, `grand total`, `amount due`, `amount paid`, `paid`, `balance due`.

Negative or low-priority labels:

- Italian: `subtotale`, `imponibile`, `iva`, `resto`, `sconto`, `contanti`, `carta`, `bancomat`, `pos`, `transazione`.
- English: `subtotal`, `tax`, `vat`, `change`, `cash`, `card`, `authorization`, `auth`, `transaction`.

Rules:

- Prefer a high-priority total line over fallback amounts.
- If multiple high-priority total candidates exist, prefer the last strong total in reading order.
- Exclude percentages and tax rates.
- Exclude long numeric identifiers and authorization codes.
- Allow formats such as `12,34`, `12.34`, `1.234,56`, `1,234.56`, `EUR 12,34`, and `€ 12,34`.
- Return amount in the existing Add Expense input format with a dot decimal separator, for example `1234.56`.
- If no reliable labeled total exists, fallback to the largest plausible money amount only when it is not clearly tax/change/card metadata.

### Date Extraction

The parser should support:

- ISO dates: `2026-06-24`.
- Italian/European numeric dates: `24/06/2026`, `24-06-26`, `24.06.2026`.
- English numeric dates: `06/24/2026`, `06-24-26`, when day-first parsing would be invalid or the line has English date hints.
- Month-name dates:
  - Italian: `24 giugno 2026`, `24 giu 2026`.
  - English: `June 24, 2026`, `24 Jun 2026`.

Return dates as `yyyy-MM-dd`, matching the existing Add Expense draft contract. If multiple dates are found, prefer the first receipt/bill issue date over card transaction timestamps when labels make that distinction clear.

### Confidence Metadata

The model should support field confidence internally:

- `High`: strong label or high-quality candidate.
- `Medium`: reasonable fallback.
- `Low`: weak or ambiguous candidate.
- `Missing`: no candidate.

The initial UI does not need to display confidence, but tests should verify that risky fallbacks are not treated as high-confidence totals. This prepares the next review-screen improvement without changing the user flow now.

## Capture Flow

The existing Capture screen remains:

- Select receipt image.
- Scan with camera preview.
- Show extracted draft card.
- Show raw recognized text.
- Continue to Add Expense for review and saving.

Light quality improvements in this phase:

- Preserve raw OCR text even when no draft fields are extracted.
- Show the no-text error only when OCR output is blank.
- Allow review when at least one important field is present.

Heavy image improvements are explicitly deferred to a later capture-quality phase.

## Error Handling

- OCR failure shows the existing scan-failed message.
- Blank OCR text shows the existing no-text message.
- Nonblank OCR text with no reliable fields should show raw text and an empty or partial draft, not a scan failure.
- Parser exceptions should not crash the screen; malformed OCR should produce an empty draft with raw text preserved.

## Testing Strategy

Add parser unit tests for:

- English supermarket receipt with total, tax, and subtotal.
- Italian supermarket receipt with `Totale`, `IVA`, `Imponibile`, and comma decimals.
- Restaurant receipt with service/subtotal and final total.
- Utility bill or invoice-like text with `importo da pagare` / `amount due`.
- Card receipt with authorization and transaction numbers that must not become amounts.
- Thousand separators in Italian and English formats.
- Italian and English month-name dates.
- Ambiguous text where merchant and amount should stay empty or low confidence.

Run:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- `git diff --check`

Connected Android tests are useful for navigation smoke checks when a device allows installs, but parser correctness is verified primarily by unit tests.

## Success Criteria

- Parser tests cover representative Italian and English bills/receipts.
- Existing Add Expense draft prefill still works.
- The capture screen still uses local ML Kit only.
- No backend, paid AI, or online dependency is introduced.
- The parser avoids common false totals from IVA/VAT, subtotal, change, and card authorization lines.
- The app builds and unit tests pass.
