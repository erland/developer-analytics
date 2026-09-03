# UX filtering step 5.2 – Technology timeline only shows active periods

## Goal

Technology **Over time** should only list years and months where there is actual recorded activity in projects where the selected technology has been observed.

A calendar period must not be shown merely because a zero-valued timeline row exists.

## Activity rule

A technology timeline month is considered active when at least one of these values is greater than zero:

- commits
- changed lines
- commits with line statistics
- active project count

Rows where all four values are zero are removed before they reach the API response. The Technology view also applies the same rule defensively before rendering the shared drill-down chart.

## Result

For a technology with activity in, for example, 2018, 2020 and 2025, the yearly view only exposes those years. After selecting a year, only months with activity are displayed.

This step does not change the semantic contract from step 5.1: activity is still activity in projects where the technology has been observed, not proof that every individual commit used the technology.

## Verification

Regression coverage verifies that a zero-only 2025 point is absent while an active 2026 point remains visible. Backend unit coverage verifies the activity predicate for commits, changed lines and line-statistics activity.
