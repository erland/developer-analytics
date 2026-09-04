# UX filtering step 2.2 – active filter chips

This step extends the shared `AnalysisFilters` component with a compact visual representation of the complete active `AnalysisScope`.

## Behaviour

- Every active technology and project type is shown as its own removable chip.
- Ownership, visibility and search are also shown when active, even if the current view does not expose a selector for that dimension.
- Time selection is represented as one `Period` chip. Removing it clears `from`, `to`, `year`, `month` and `week` together while preserving all non-time filters.
- Removing one chip preserves every other active filter.
- `Clear all` remains available and clears the complete `AnalysisScope`.
- Labels use the configured facet label when one is available and fall back to the stored key otherwise.

The important design intent is that inherited scope must never be invisible. When cross-view scope is introduced later, a view can therefore show filters that originated elsewhere without needing to expose every selector itself.

No existing Explore view is wired to `AnalysisFilters` yet, so this step does not intentionally change production navigation/filter behaviour.
