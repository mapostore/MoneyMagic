# OCR Fixture Regressions

Use this workflow when a real receipt, bill, or invoice scan parses incorrectly.

1. Scan the document in the app.
2. Use **Share OCR text** from the scan diagnostics card.
3. Save the shared text as:
   `app/src/test/resources/ocr-fixtures/<case_name>.txt`
4. Add expected values beside it as:
   `app/src/test/resources/ocr-fixtures/<case_name>.expected.txt`

Expected file format:

```text
documentType=BillInvoice
merchant=Example Provider
amount=42.80
date=2026-07-31
```

Supported `documentType` values:

- `Receipt`
- `BillInvoice`
- `Unknown`

Then add `<case_name>` to `fixtureNames` in
`ReceiptOcrFixtureRegressionTest` and run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.indiewalkabout.moneymagic.feature.capture.domain.usecase.ReceiptOcrFixtureRegressionTest'
```

Keep fixture text as raw OCR output. Do not manually clean spacing, line order, OCR mistakes, or casing unless the app itself produced that cleaned text.
