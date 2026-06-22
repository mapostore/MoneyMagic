# MoneyMagic Offline MVP Design

Date: 2026-06-22

## Goal

Build MoneyMagic as a complete offline Android MVP for budget and spending control. The first release focuses on manual expense entry, budget tracking, local persistence, summaries, sorting/filtering, and local notifications. Voice entry, receipt scanning, bill scanning, and web receipt scanning are planned as later capture modules that feed the same expense draft flow.

## Product Scope

The MVP must help a user answer three questions quickly:

- How much have I spent in the active period?
- Am I close to or over my budget?
- Where did the money go?

Included in the MVP:

- Manual expense creation and editing.
- Weekly, monthly, yearly, and custom budget periods.
- Local-only data storage with Room.
- Spending summaries by day, week, year, and custom range.
- Expense sorting by newest, most expensive, least expensive, and category.
- Category rankings by most and least expensive.
- Local notifications when spending approaches a budget threshold.
- Compose UI, Navigation 3, MVVM, Hilt, coroutines, and Flow.

Deferred until after the MVP:

- Voice expense capture.
- Receipt, bill, and web receipt scanning.
- ML Kit text extraction and parsing.
- Fully custom user-defined expense fields.
- Any backend, sync, account, or cloud feature.

## Navigation

MoneyMagic uses a Control Center structure with four bottom navigation destinations:

- Dashboard: current spending control status, active budget progress, remaining amount, recent expenses, and top categories.
- Expenses: ledger with filtering, sorting, and grouped category view.
- Budgets: budget creation, editing, progress, and alert settings.
- Settings: categories, payment methods, currency display, and notification defaults.

Expense creation is reachable from the Dashboard and Expenses screens through a prominent add action. In the MVP, this opens a manual Add Expense flow. Later capture modes can be added as alternate entry points that produce the same expense draft model.

## Architecture

The project uses a layered MVVM architecture:

- `data`: Room entities, DAOs, database, repository implementations, local mappers, and seed data.
- `domain`: business models, repository interfaces, use cases, period calculations, and summary models.
- `presentation`: Compose screens, ViewModels, UI state models, and Navigation 3 routes.
- `di`: Hilt modules for database, DAOs, repositories, dispatchers, and notification services.
- `core`: shared date, money, result, and formatting utilities.

Dependency direction:

`presentation -> domain -> repository interfaces`

`data -> domain repository interfaces`

ViewModels expose immutable `StateFlow` UI state. Use cases coordinate repositories and domain logic. Repositories expose `Flow` from Room so dashboards, summaries, and filtered lists update automatically after expense or budget changes.

Ktor is not needed in the MVP because there is no backend. ML Kit is deferred until the advanced capture phase.

## Data Model

### Expense

Stored fields:

- `id`
- `amountMinor`
- `currency`
- `dateTime`
- `categoryId`
- `merchant`
- `paymentMethodId`
- `notes`
- `tags`
- `createdAt`
- `updatedAt`

Amounts are stored in minor units to avoid floating-point errors. Tags are stored with a Room type converter in the MVP to avoid a separate tag-management workflow.

### Category

Stored fields:

- `id`
- `name`
- `color`
- `iconKey`
- `sortOrder`
- `archived`

Default categories are seeded on first launch.

### PaymentMethod

Stored fields:

- `id`
- `name`
- `type`
- `archived`

Default payment methods are seeded on first launch.

### Budget

Stored fields:

- `id`
- `name`
- `amountMinor`
- `currency`
- `periodType`: `WEEKLY`, `MONTHLY`, `YEARLY`, or `CUSTOM`
- `customStartDate`
- `customEndDate`
- `categoryScope`: all categories or one category
- `categoryId`
- `notificationThresholdPercent`
- `enabled`

The MVP supports an overall budget plus optional category-specific budgets.

### Computed Domain Models

These are not stored directly:

- `BudgetProgress`: budget amount, spent amount, remaining amount, percent used, threshold state, and over-budget state.
- `SpendingSummary`: totals by period, category rankings, recent expenses, and comparison values.

## Screens and Behavior

### Dashboard

The Dashboard shows:

- Active period spending.
- Remaining budget.
- Budget progress indicator.
- Near-target and over-budget state.
- Recent expenses.
- Top spending categories.
- Empty state that prompts budget creation when no budget exists.

### Add and Edit Expense

The manual expense form includes:

- Amount.
- Date.
- Category.
- Merchant or payee.
- Payment method.
- Notes.
- Tags.

Validation prevents saving an expense with an empty amount, a zero or negative amount, or no category.

### Expenses

The Expenses screen supports:

- Period filter.
- Category filter.
- Sort by newest.
- Sort by most expensive.
- Sort by least expensive.
- Grouped-by-category view.

### Budgets

The Budgets screen supports:

- Create, edit, and delete budgets.
- Weekly, monthly, yearly, and custom date ranges.
- Overall budgets.
- Category-specific budgets.
- Per-budget notification threshold.

### Settings

The Settings screen supports:

- Category management.
- Payment method management.
- Currency display setting.
- Default notification threshold setting.

## Notifications

Notifications are local-only. A budget can trigger a warning when spending reaches the configured threshold, such as 80 percent.

To avoid repeated alerts, each budget period tracks whether the threshold notification has already been sent. Android notification permission is requested when the user enables alerts.

## Testing Strategy

Tests focus on the logic with the highest regression risk:

- Unit tests for week, month, year, and custom period calculations.
- Unit tests for budget progress, remaining amount, threshold crossing, category-specific budgets, and over-budget state.
- Repository tests with an in-memory Room database for expenses and budgets.
- ViewModel tests for dashboard state, expense validation, and filter/sort behavior.
- Compose UI smoke tests for navigation and manual expense creation.

## Implementation Phases

1. Add project dependencies and architecture skeleton.
2. Add domain models and use cases with tests.
3. Add Room database, DAOs, mappers, seed data, and repository implementations.
4. Add Hilt dependency injection.
5. Add Compose navigation and screens.
6. Add local notifications and alert state tracking.
7. Polish UI, run verification, and prepare the next capture-feature plan.

## Open Decisions

None for the MVP. Advanced capture will be designed in a separate spec after the offline spending-control MVP is complete.
