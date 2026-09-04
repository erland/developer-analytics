# UX filtering step 7.1 – Activity/Timeline overlap inventory

## Purpose

This step inventories the current `Activity` and `Timeline` views before they are consolidated.
No runtime behaviour is changed in step 7.1.

The goal is to identify:

- functionality that is duplicated,
- functionality unique to either view,
- the data contract both views already share,
- what must be preserved when Timeline is folded into Activity,
- what should be removed rather than carried forward.

## Current shared data source

Both views use:

```ts
useActivityView(...)
```

and therefore ultimately read:

```text
GET /api/me/activity
```

`ActivityView` requests a selected preset period:

```ts
useActivityView('12m' | '24m' | '5y' | 'all')
```

`TimelineView` always requests:

```ts
useActivityView('all')
```

The underlying `ActivityData` already contains all major time resolutions needed for a consolidated view:

- `commitsPerYear`
- `commitsPerMonth`
- `commitsPerWeek`
- `projectsOverTime`

`projectsOverTime` additionally contains per-project:

- repository id/name,
- primary project type,
- all project types,
- primary technology,
- all technologies,
- monthly activity,
- weekly activity.

This means consolidation does **not** require a new activity data source first.

## Current Activity responsibilities

`ActivityView` currently owns:

### Scope-like controls

- period preset:
  - 12 months,
  - 24 months,
  - 5 years,
  - all time.

This period control changes which activity data is fetched.

### View option

- trend measure:
  - changed lines,
  - commits.

This is presentation state rather than analysis scope.

### Summary statistics

Activity currently renders five large KPI cards:

- commits,
- active projects,
- average commit size,
- additions,
- deletions.

It also renders:

- line-statistics coverage note,
- first/last activity period.

### Time exploration

For long intervals (`5y` / `all`):

```text
year → month
```

For shorter intervals:

```text
month
```

Selecting a month creates a small local drill-down summary containing:

- selected period,
- metric value,
- commits,
- changed lines,
- active project count,
- project names as text.

### Important limitation

The Activity drill-down does **not** provide:

- week-level drill-down,
- a real project result list,
- project navigation,
- colour-by,
- AnalysisScope URL state.

## Current Timeline responsibilities

`TimelineView` currently owns:

### View options

- measure:
  - changed lines,
  - commits.
- colour by:
  - project type,
  - technology,
  - none.

Both belong naturally in `AnalysisViewOptions`.

### Time exploration

Timeline provides the richer hierarchy:

```text
year → month → week
```

It builds all three levels from `projectsOverTime`.

### Project breakdown

For a selected period Timeline shows a real project list with:

- repository name,
- navigation to project detail,
- all known project types,
- all known technologies,
- commits,
- changed lines,
- metric-specific contribution.

### Segmentation

Timeline can visually segment a time bar by:

- primary project type,
- primary technology,
- single colour.

The primary value is intentionally used for visual segmentation to avoid counting the same project activity multiple times when a project has multiple technologies/project types.

### Important limitations

Timeline currently:

- always fetches all-time activity,
- uses local `selectedYear`, `selectedMonth`, `selectedWeek`,
- is not connected to `AnalysisScope`,
- has no Technology/Project type/Ownership filters,
- duplicates measure selection already present in Activity.

## Functional overlap

| Capability | Activity | Timeline | Consolidated target |
|---|---|---|---|
| `/api/me/activity` | Yes | Yes | One shared query |
| Changed lines / commits | Yes | Yes | One `AnalysisViewOptions.metric` |
| Year trend | Yes for long ranges | Yes | Yes |
| Month trend | Yes | Yes | Yes |
| Week trend | No | Yes | Preserve Timeline behaviour |
| Project breakdown | Text names only | Full project list | Full project list / `MatchingProjects` |
| Open project | No | Yes | Preserve |
| Colour by | No | Yes | Preserve as view option |
| Period preset | Yes | No | Preserve, but map to scope |
| Summary statistics | Yes | No | Preserve compactly |
| Line coverage note | Yes | Yes | Render once |
| URL/scope time state | No | No | Add via `AnalysisScope` |

## Duplication that should disappear

The merged Activity view should have only one implementation of:

- metric selector,
- changed-line coverage warning,
- year/month rendering,
- time drill-down state,
- selected-period project result.

The existing separate `ActivityContent` bar implementation and `TimelineBars` implementation should not both survive long term.

## Recommended source implementation

Use `TimelineView`'s period-building/drill-down behaviour as the **functional base** because it already supports:

```text
year → month → week → projects
```

Then bring Activity-specific capabilities into that view:

- period presets / AnalysisScope period,
- compact activity summary,
- first/last activity information.

This is less risky than extending `ActivityView`'s current bar model from month detail to week/project detail.

## Recommended consolidated Activity structure

```text
Activity

Analysis filters
[Technology] [Project type] [Ownership] [Period]

View
[Measure: Changed lines] [Colour by: Project type]

Compact activity summary
commits · active projects · observed period

Over time
year → month → week

Projects matching this selection
...

▸ Activity statistics
```

The key UX rule remains:

> Everything selected above the project list must describe the same project/data population shown below it.

## Scope vs view options

### AnalysisScope

The consolidated Activity view should eventually use:

- technologies,
- project types,
- ownership,
- visibility where relevant,
- from/to,
- year/month/week.

### AnalysisViewOptions

Keep these separate:

- metric,
- colourBy,
- sorting/grouping where later needed.

The current `ActivityPeriod` presets (`12m`, `24m`, `5y`, `all`) should be treated as a convenient UI for producing an AnalysisScope date range, not as an independent long-term state model.

## Project list target

The current Timeline project list is useful as an interaction baseline, but the consolidated view should converge on the shared `MatchingProjects` component so that:

- project semantics are identical across Explore,
- current AnalysisScope is reused,
- pagination behaves consistently,
- project detail/back behaves consistently,
- filtering is performed by the Projects API rather than reconstructed from chart data.

Timeline's `PeriodDetail` can therefore be considered transitional code once `MatchingProjects` is connected to Activity scope.

## Statistics target

Activity's current five large KPI cards should **not** be copied unchanged into the consolidated view.

Recommended primary summary:

```text
1,842 commits · 37 active projects · observed 2011–2026
```

More detailed statistics such as:

- average commit size,
- additions,
- deletions,
- line-statistics coverage,

should be compact or placed in an expandable secondary section.

This is consistent with the simplification already performed for Technologies.

## Navigation impact

After functional parity has been verified, the primary navigation should no longer need a separate `Timeline` entry.

Current:

```text
Activity
Timeline
```

Target:

```text
Activity
```

The Timeline navigation item must not be removed before all of these have moved into Activity:

- year/month/week drill-down,
- measure selector,
- colour-by,
- selected-period project result,
- project detail navigation.

## Migration risks

### 1. Different source aggregates

Activity uses `commitsPerYear` / `commitsPerMonth`, while Timeline rebuilds periods from `projectsOverTime`.

During consolidation, counts from both paths must be compared to ensure they agree for the same period/metric before one path is removed.

### 2. Changed-line coverage

Changed lines may be incomplete when additions/deletions are unavailable for some commits.
The existing coverage warning must remain visible when relevant.

### 3. Multi-classification projects

A project can have several technologies/project types.
Filtering must use the full arrays added in step 5.4, while visual colour segmentation should avoid double-counting.

### 4. Period presets vs URL state

The current Activity period preset is local state.
When migrated, preset changes must produce deterministic AnalysisScope date filters and browser-history behaviour.

### 5. Project list semantics

Timeline's project list is calculated from chart data, while `MatchingProjects` queries the Projects API.
Their period semantics must agree before Timeline's local list is removed.

## Verification required before removing Timeline

The merged Activity view must prove all of these scenarios:

1. `All time → year → month → week` works.
2. Changed lines and commits both work.
3. Colour by project type works.
4. Colour by technology works.
5. A selected period updates the project result.
6. A project opens and Back restores the same analysis state.
7. Technology/Project type filters can coexist with time drill-down.
8. Browser back/forward restores time/filter state.
9. Mobile layout remains usable.
10. Counts for equivalent Activity/Timeline periods are consistent.

## Decision from step 7.1

Proceed with consolidation.

The recommended direction for step 7.2 is:

> Make Activity the single time-analysis view, using Timeline's year/month/week and project-breakdown behaviour as the functional base, then layer the shared AnalysisScope/AnalysisFilters and compact Activity summary onto it.

Do **not** keep Activity and Timeline as two long-term parallel analysis experiences.
