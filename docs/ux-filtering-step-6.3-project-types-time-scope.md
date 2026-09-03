# UX filtering step 6.3 – Project types time scope

Project types now follows the same time-filtering contract as Technologies.

## Behaviour

- Project type timeline rows with no commits, changed lines, line-statistics commits, or active projects are removed.
- Empty years therefore do not appear in `Over time`.
- Selecting a year writes `year` to `AnalysisScope` and the URL.
- Selecting a month writes both `year` and `month`.
- `MatchingProjects` receives the same scope, so the project list always reflects the selected project type and time period.
- Selecting a new year/month clears `from`, `to`, and `week` to avoid contradictory period filters.
- `Back to years` clears only the time selection and preserves project type and other analysis filters.
- Deep links containing `projectType`, `year`, and `month` restore the same drill-down state.

## Backend guard

`MeProjectTypesResource` also removes empty category timeline rows before returning the API response. The frontend keeps a defensive filter so older/cached responses cannot reintroduce empty periods.
