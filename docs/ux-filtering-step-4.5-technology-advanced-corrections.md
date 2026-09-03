# UX filtering step 4.5 – Technology corrections under Advanced

## Goal

Keep correction/suppression controls out of the primary Technology analysis flow.

## Change

The Technology inference correction is now placed in a closed `Advanced` disclosure after the evidence/details section. The disclosure is closed by default and contains:

- a short explanation that suppression is intended for misleading assessments,
- the existing suppression semantics,
- the `Suppress technology inference` action.

The main flow therefore remains:

1. filters,
2. compact summary,
3. over time,
4. matching projects,
5. optional evidence/details,
6. optional advanced correction.

No backend correction semantics were changed.

## Regression coverage

The Technologies feature test verifies that `Advanced` is closed by default and that the suppression action becomes available only after opening it.
