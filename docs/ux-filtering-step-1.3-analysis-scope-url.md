# UX filtering step 1.3 – AnalysisScope URL serialisation

This step introduces URL parsing and serialisation for the shared `AnalysisScope` model without changing any existing Explore view behaviour yet.

## Added

- `frontend/src/analysis/AnalysisScopeUrl.ts`
- `analysisScopeToSearchParams(scope)`
- `analysisScopeFromSearchParams(params)`
- feature regression tests for round-tripping, multi-select values, invalid input and unknown parameters

## URL representation

Multi-value filters use repeated query parameters:

```text
?technology=java&technology=typescript&projectType=backend&year=2025
```

Supported parameters are:

- `technology`
- `projectType`
- `ownership`
- `visibility`
- `from`
- `to`
- `year`
- `month`
- `week`
- `search`

Empty values are omitted when serialising. Unknown parameters and invalid enum/year values are ignored when parsing.

## Deliberately not changed yet

- Existing views still keep their current local state.
- Navigation does not yet preserve filters between Explore views.
- Browser history is not yet wired to filter changes.
- `AnalysisViewOptions` remain separate and are not encoded as `AnalysisScope` parameters.

Those behaviours will be introduced in later migration steps after the URL contract is established and tested.
