# UX filtering redesign – step 12.1: KPI/statistics inventory

## Goal

Identify large statistics/KPI blocks that consume vertical space and classify them as **primary**, **secondary**, or **tertiary** information before changing presentation.

This step is intentionally documentation-only. Runtime behaviour is unchanged.

## Classification rule

- **Primary** – needed to understand the current analysis/result immediately.
- **Secondary** – useful context, but should usually fit in a compact summary row/bar.
- **Tertiary** – evidence, methodology, operational detail or correction controls that can be disclosed on demand.

The Explore redesign principle remains:

> Filter / current scope → over time → matching projects → secondary/tertiary detail.

## Current inventory

| View | Current statistics presentation | Priority | Recommendation |
| --- | --- | --- | --- |
| Activity | 5 large metric cards: commits, active projects, average commit size, additions, deletions | Secondary, with active projects/commits closest to primary | Replace cards with one compact summary bar above the timeline. Keep line-coverage warning when relevant. Move activity-period note into the same compact summary. |
| Contributions | 4 large metric cards: commits, pull requests, reviews, issues | Secondary | Replace with compact contribution summary. Keep `MatchingProjects` immediately after summary. |
| Overview | 6 large metric cards: repositories analysed, own/external, public/private, commits, active projects, activity period | Secondary overview information | Keep overview facts visible but compress to a dense summary strip/list. Overview is allowed slightly more summary content than Explore views, but six large cards are unnecessarily tall. |
| Technologies | Previously large KPI cards; now compact summary + collapsed evidence/details | Primary/secondary split already implemented | No further KPI-card work required in step 12.2. Preserve current structure. |
| Project types | Compact summary + collapsed classification statistics | Primary/secondary split already implemented | No further KPI-card work required in step 12.2. Preserve current structure. |
| Projects | Primarily filter controls + project result list | Primary result data | No KPI-card compression required. Continue treating project list as the result/facit. |
| Project detail – activity | 6 large cards: commits, pull requests, reviews, issues, additions, deletions | Secondary | Replace with compact project activity summary. Timeline should become visually more prominent than six cards. |
| Project detail – contributors | 3 large cards: total contributors, people, bots | Tertiary/secondary | Replace with compact inline facts or a small definition list inside the Contributors section. |
| Project detail – significance/involvement | Two assessment cards with scores and rationale | Secondary/tertiary | Keep the assessments, but they are not part of the generic KPI-card compression. Consider disclosure/rationale compaction in a later visual-cleanup step. |
| AI insights | No generic `metric-grid` KPI block found | N/A | No action in 12.2. |
| Reports | No generic KPI block found | N/A | No action in 12.2. |
| Privacy/data sources | No generic KPI block found | N/A | No action in 12.2. |
| Account | No generic KPI block found | N/A | No action in 12.2. |

## Highest-priority scroll reductions

### 1. Activity

Current order after controls:

```text
5 metric cards
coverage warning (sometimes)
timeline / drill-down
activity period note
```

The timeline is the principal analytical content, yet five cards appear before it.

Recommended target:

```text
Activity summary
12,430 commits · 84 active projects · 42 avg lines/commit · +1.2M / −840k
Aug 2010 – Sep 2026

coverage warning (only when needed)

Over time
...
```

The activity-period block should be merged into the compact summary rather than remain a separate section after the timeline.

### 2. Contributions

Current order:

```text
filters
4 metric cards
matching projects
```

Recommended target:

```text
filters
Commits 12,430 · PRs 816 · Reviews 502 · Issues 214
matching projects
```

The four values are useful, but do not justify four large cards.

### 3. Overview

The Overview currently starts with six large repository/activity cards. Unlike Explore views, the Overview exists specifically to summarise, so these facts should remain visible. However, the presentation can be much denser.

Recommended target:

```text
Repositories 214 · Active projects 72 · Commits 18,430
Own/external 163/51 · Public/private 188/26 · Aug 2010 – Sep 2026
```

or a responsive two-row summary strip.

### 4. Project detail

A project detail currently opens with six large activity cards before repository metadata and the activity timeline. The activity values are contextual, while the project timeline and classifications are more useful for exploration.

Recommended target:

```text
Commits 342 · PRs 28 · Reviews 41 · Issues 13 · +48k / −31k lines

Repository
...

Commit activity over time
...
```

Contributor counts should also use inline facts instead of three cards.

## Generic component direction

Instead of repeatedly creating `metric-grid` + `metric-card`, introduce a compact shared presentation in step 12.2, for example:

```tsx
<SummaryFacts
  ariaLabel="Activity summary"
  items={[
    { label: 'Commits', value: '12,430' },
    { label: 'Active projects', value: '84' },
    ...
  ]}
/>
```

Target characteristics:

- inline/wrapping on desktop;
- compact stacked or two-column layout on mobile;
- label remains visible and accessible;
- no oversized number typography;
- reusable across Activity, Contributions, Overview and Project detail;
- supports a compact text value such as an activity period;
- does not hide important data behind interaction merely to save space.

## What should *not* be collapsed

The following should remain immediately visible when relevant:

- active filters / AnalysisScope;
- Over time / timeline visualisation;
- Matching projects;
- an incomplete changed-line coverage warning;
- key page/result heading;
- a concise summary of the selected technology/project type.

The goal is not to hide information. It is to stop secondary statistics from dominating the viewport.

## Recommended step 12.2 implementation order

1. Add a shared compact `SummaryFacts` component and responsive styling.
2. Replace Activity's five KPI cards and separate activity-period block.
3. Replace Contributions' four KPI cards.
4. Replace Overview's six KPI cards.
5. Replace Project detail activity KPI cards.
6. Replace Project detail contributor KPI cards.
7. Remove now-unused local `Metric`/`MetricCard` helpers where safe.
8. Keep `.metric-grid/.metric-card` temporarily if other non-migrated consumers remain; remove only when confirmed unused.
9. Add/update regression tests that verify the compact summary exists and legacy metric grids are absent in migrated views.

## Definition of done for step 12.1

- All generic KPI-card usages in the frontend have been located.
- Each usage has a primary/secondary/tertiary classification.
- The highest-value compaction targets are documented.
- No runtime code or behaviour is changed.
- Step 12.2 has a concrete implementation order.
