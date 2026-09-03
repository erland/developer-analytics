# UX filtering redesign – current-state inventory

Status: baseline inventory for step 0.1 of the UX filtering redesign plan.

This document records the current interaction model before functional changes are introduced. It is intended to be the reference point for later refactoring of navigation, filtering, time drill-down and matching-project behaviour.

## Target principle

The redesign is guided by one central rule:

> A user selection should constrain the complete current analysis – summary, over-time view and project list – and the project list should always show which projects the analysis above is based on.

No functional behaviour is changed by this inventory step.

## Current top-level navigation

`frontend/src/components/AuthenticatedShell.tsx` owns the current section in local React state and renders one analysis component at a time.

Current sections:

1. Overview
2. Activity
3. Timeline
4. Projects
5. Technologies
6. Project types
7. Contributions
8. AI insights
9. Reports
10. Privacy/data sources
11. Account

The selected section is not represented in a shared analysis scope. Project detail is also primarily handled by local component/shell state rather than route-based state.

## Analysis-view inventory

| View | Main component | Main data hook/API | Filter/selection state today | Time model | Project result | Main inconsistency |
| --- | --- | --- | --- | --- | --- | --- |
| Activity | `ActivityView.tsx` | `useActivityView(period)` / activity API | `period`, `metric`, `drillYear`, selected month detail | year/month, depending on selected period | Project names are embedded in a selected period detail, not a reusable matching-project list | Time selection is local and is not shared with Projects/Technologies/etc. |
| Timeline | `TimelineView.tsx` | `useActivityView('all')` / activity API | `metric`, `colourBy`, `selectedYear`, `selectedMonth`, `selectedWeek` | year → month → week | Period-specific project rows | Duplicates much of Activity but uses a separate drill-down and selection model |
| Projects | `ProjectInventoryView.tsx` | `useProjectInventory(filters)` / `/api/me/project-inventory` | one `InventoryFilters` object plus `selectedProjectId` | activity filter only; no shared year/month/week scope | Primary paginated project inventory | Technology/category filter choices are derived from the current page of results, not complete facets |
| Technologies | `TechnologyViews.tsx` | `useTechnologyViews()` / `/api/me/technologies` | `selectedKey`, `selectedProjectId` | technology-specific monthly timeline | `representativeProjects`, presented as all matching projects | Selecting a technology is local detail selection rather than a reusable analysis filter |
| Project types | `ProjectTypeViews.tsx` | `useProjectTypes()` / `/api/me/project-types` | `selectedKey`, `selectedProjectId` | project-type monthly timeline | `representativeProjects`, presented as all matching projects | Same local-selection pattern as Technologies, but independent of every other view |
| Contributions | `ContributionsView.tsx` | `useContributions()` / `/api/me/contributions?limit=100` | no user filter state | no interactive time scope | recently active projects | Primarily statistics + recent projects; cannot be combined with Technology/Project type/time selections |

## Detailed findings

### 1. Activity

Relevant files:

- `frontend/src/components/ActivityView.tsx`
- `frontend/src/hooks/useActivityView.ts`

Local state:

```ts
const [period, setPeriod] = useState<ActivityPeriod>('12m')
const [metric, setMetric] = useState<ActivityMetric>('lines')
```

The content component adds:

```ts
const [drillYear, setDrillYear] = useState<number | null>(null)
const [selected, setSelected] = useState<... | null>(null)
```

Current behaviour:

- user selects a broad period: 12 months, 24 months, 5 years or all time;
- user selects Changed lines or Commits;
- longer ranges start at yearly level;
- selecting a year changes the chart to months;
- selecting a month shows a textual detail containing project names;
- five large metric cards are displayed before the trend.

Observed UX consequences:

- the time drill-down does not become a reusable filter;
- the matching projects are not rendered using the same project result model as Projects/Technologies/Project types;
- the metric cards consume substantial vertical space before the trend;
- metric is a view option but is visually presented alongside Period, which is an analysis filter.

### 2. Timeline

Relevant files:

- `frontend/src/components/TimelineView.tsx`
- `frontend/src/hooks/useActivityView.ts`

Local state:

```ts
const [metric, setMetric] = useState<ActivityMetric>('lines')
const [colourBy, setColourBy] = useState<ColourBy>('projectType')
const [selectedYear, setSelectedYear] = useState<number | null>(null)
const [selectedMonth, setSelectedMonth] = useState<string | null>(null)
const [selectedWeek, setSelectedWeek] = useState<string | null>(null)
```

Current behaviour:

- always loads all activity;
- supports year → month → week drill-down;
- supports Changed lines / Commits;
- supports Colour by Project type / Technology / None;
- derives period-specific projects from `projectsOverTime`;
- can open project detail.

Observed UX consequences:

- Activity and Timeline overlap heavily but maintain separate interaction state;
- Colour by Technology looks similar to a Technology filter but changes presentation rather than dataset membership;
- year/month/week selections disappear when navigating to another analysis view;
- the current `ProjectLifecycle` representation has a singular `projectType` and singular `technology`, while project inventory supports multiple categories and technologies per project. This needs explicit review before shared filtering is implemented.

### 3. Projects

Relevant files:

- `frontend/src/components/ProjectInventoryView.tsx`
- `frontend/src/hooks/useProjectInventory.ts`
- backend project-inventory resource/service queried by `/api/me/project-inventory`

Current filter model:

```ts
export type InventoryFilters = {
  page: number
  pageSize: number
  search: string
  ownership: string
  visibility: string
  activity: string
  category: string
  technology: string
}
```

This is currently the closest implementation to the desired filtering model because a filter set directly constrains a project result list.

Important issue identified:

`knownCategories` and `knownTechnologies` are generated from:

```ts
inventory.data.items
```

The response is paginated (`pageSize` defaults to 25). Therefore the available Category and Technology options are based on the projects returned on the current page, not necessarily the complete project inventory.

Consequences:

- filter options can be incomplete;
- available options may change with pagination or other filters;
- the Projects filter UI cannot currently serve as a reliable global facet source.

This should later be replaced with complete backend facets/aggregrations.

### 4. Technologies

Relevant files:

- `frontend/src/components/TechnologyViews.tsx`
- `frontend/src/hooks/useTechnologyViews.ts`
- `/api/me/technologies`

Local state:

```ts
const [selectedKey, setSelectedKey] = useState<string | null>(null)
const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
```

Current hierarchy:

1. technology list;
2. technology hero;
3. correction/suppression action;
4. six metric cards;
5. timeline;
6. all matching projects.

The technology API already supplies useful technology-specific data:

- evidence level/score;
- project count;
- first/latest observation;
- monthly timeline;
- matching/representative projects;
- privacy provenance.

Observed UX consequences:

- choosing Java is treated as local detail selection instead of `Technology = Java` in a shared analysis scope;
- users must pass correction controls and a large statistics area before reaching time/project information;
- selection cannot constrain Activity, Timeline, Projects or Contributions;
- project detail replaces the technology view locally, so the previous scope is component state rather than durable navigation state.

Technologies is the strongest candidate for the first reference implementation of the future interaction model.

### 5. Project types

Relevant files:

- `frontend/src/components/ProjectTypeViews.tsx`
- `frontend/src/hooks/useProjectTypes.ts`
- `/api/me/project-types`

Local state mirrors Technologies:

```ts
const [selectedKey, setSelectedKey] = useState<string | null>(null)
const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
```

Current hierarchy:

1. project-type list;
2. category hero;
3. three metric cards;
4. category activity over time;
5. all matching projects.

The backend response already includes a category-specific timeline and matching projects, making this a good second adopter after Technologies.

Observed UX consequence:

The interaction pattern is similar to Technologies but implemented independently. A future shared scope should make selecting a category equivalent to `Project type = <category>` across Explore views.

### 6. Contributions

Relevant files:

- `frontend/src/components/ContributionsView.tsx`
- `frontend/src/hooks/useContributions.ts`
- `/api/me/contributions?limit=100`

Current hierarchy:

1. four metric cards: commits, pull requests, reviews, issues;
2. recently active projects.

There is no local user-adjustable analysis filter and no shared time/technology/project-type scope.

Observed UX consequence:

The view starts with pure statistics and cannot answer questions such as:

- contributions during 2025;
- contributions in Java projects;
- contributions in Game projects;
- matching projects for a combined selection.

## Current state categories

The analysis UI currently uses at least four separate state concepts without a common abstraction.

### A. Dataset filters

Examples:

- Projects: ownership, visibility, activity, category, technology, search.

These affect which projects are returned.

### B. Detail selections

Examples:

- Technologies: `selectedKey`;
- Project types: `selectedKey`.

These currently choose a detail panel but conceptually should become reusable dataset filters.

### C. Time drill-down

Examples:

- Activity: `period`, `drillYear`, selected month;
- Timeline: `selectedYear`, `selectedMonth`, `selectedWeek`.

These currently exist in two independent implementations.

### D. View options

Examples:

- `metric`;
- `colourBy`.

These alter presentation/measurement and should remain separate from dataset filters.

## Existing API/data strengths to preserve

The redesign should reuse rather than replace several existing capabilities:

- project inventory already supports server-side filtering and pagination;
- technology responses already contain timeline and project associations;
- project-type responses already contain timeline and project associations;
- activity data already supports year/month/week information and project lifecycle activity;
- project detail can already be opened from several views;
- privacy provenance is already represented in technology analysis;
- changed-line coverage is already explicitly surfaced when incomplete.

## Structural gaps to address in later steps

### Gap 1 – No shared `AnalysisScope`

Technology, Project type, Ownership and time selections cannot be composed consistently across views.

### Gap 2 – No URL-backed analysis state

Most analysis state is held in component `useState`, which limits:

- browser back/forward semantics;
- bookmarking;
- refresh persistence;
- deep linking;
- carrying filters between views.

### Gap 3 – No complete project facets

Projects derives Technology/Category choices from the current paginated response.

### Gap 4 – Activity and Timeline overlap

Both implement time analysis but with different state and result presentation.

### Gap 5 – Matching-project semantics are inconsistent

- Technologies: explicit list;
- Project types: explicit list;
- Timeline: period project details;
- Activity: project names in selected period text;
- Contributions: recent projects;
- Projects: full paginated inventory.

A shared `MatchingProjects` result model/component is needed later.

### Gap 6 – Statistics dominate some primary flows

Metric grids precede the main exploratory content in Activity, Technologies, Project types and Contributions.

## Baseline interaction map

```text
AuthenticatedShell
│
├─ Activity
│  ├─ period (local)
│  ├─ metric (local view option)
│  ├─ year/month drill-down (local)
│  └─ project names in selected period
│
├─ Timeline
│  ├─ metric (local view option)
│  ├─ colourBy (local view option)
│  ├─ year/month/week drill-down (local)
│  └─ period projects
│
├─ Projects
│  ├─ search (local)
│  ├─ ownership (local)
│  ├─ visibility (local)
│  ├─ activity (local)
│  ├─ category (local)
│  ├─ technology (local)
│  └─ paginated projects
│
├─ Technologies
│  ├─ selected technology (local)
│  ├─ technology timeline
│  └─ technology projects
│
├─ Project types
│  ├─ selected category (local)
│  ├─ category timeline
│  └─ category projects
│
└─ Contributions
   ├─ aggregate metrics
   └─ recent projects
```

No selection edge currently connects these views.

## Expected migration direction

The later implementation should converge toward:

```text
Shared AnalysisScope
│
├─ technology(s)
├─ project type(s)
├─ ownership
├─ visibility where relevant
├─ time scope
└─ search where relevant

       ↓

View-specific presentation
├─ compact summary
├─ over time
└─ matching projects

Separate AnalysisViewOptions
├─ metric
├─ colour by
├─ sort
└─ grouping
```

## Step 0.1 completion criteria

This baseline step is complete when:

- the current navigation sections are documented;
- filter/selection state is identified for all primary Explore views;
- current time drill-down models are identified;
- current project-result behaviour is identified;
- known inconsistencies and API/data gaps are recorded;
- no application behaviour has changed.

All criteria above are satisfied by this document.

## Next step

**Step 0.2 – add/strengthen regression tests for today's critical behaviours before functional refactoring begins.**

The first test pass should cover at minimum:

- opening Technologies and selecting a technology;
- opening a project from Technologies;
- Project types selection and project opening;
- Timeline year → month → week drill-down;
- Activity period/year/month behaviour;
- Projects filters and pagination;
- project-detail back behaviour.
