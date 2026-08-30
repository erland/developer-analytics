# Developer Analytics Frontend

React + TypeScript + Vite frontend for Developer Analytics.

## Commands

```bash
npm install
npm run dev
npm run lint
npm run typecheck
npm test
npm run build
```

The repository pins direct dependency versions in `package.json`. A `package-lock.json` should be generated and committed by running `npm install` in an environment with access to the npm registry before CI is enabled.

## Production container

The production web image is built from the repository root so the build can include both the frontend sources and the shared Nginx configuration:

```bash
docker build -f frontend/Dockerfile -t developer-analytics-web .
```

The Dockerfile uses a Node build stage and copies only the compiled Vite output into the final Nginx image. Nginx listens on port `8080` and proxies `/api/` according to `deploy/nginx/nginx.conf`.

A dependency lock file will be introduced once npm dependency resolution can be executed in a network-enabled build environment; until then the Docker build uses the pinned direct dependency versions in `package.json`.


## Authenticated application shell

Step 42 adds the authenticated responsive shell.

The frontend now:

- resolves `/api/auth/session` before rendering private UI,
- keeps anonymous, loading and error states distinct,
- provides desktop sidebar and mobile drawer navigation,
- exposes the planned primary sections,
- shows the signed-in identity,
- reads `/api/me/sync-runs` for a data freshness indicator,
- keeps private dashboard content behind the authenticated state.

Primary sections are Overview, Activity, Projects, Technologies, Project types, Contributions, AI insights, Reports, Privacy/data sources and Account.


## Overview dashboard

Step 43 replaces the authenticated Overview placeholder with a responsive dashboard. It combines existing authenticated APIs to display repository counts, ownership/visibility split, observed commits, activity period, active project count, top technology assessments, project categories and significant external projects.

The layout collapses from three columns to two and then one on phone-sized screens, with project score rows also switching to a stacked layout to avoid horizontal overflow.


## Activity views

Step 44 implements the Activity section with 12-month, 24-month, five-year and all-time filters. It shows yearly and monthly commit bars, active-project counts, average/median commit size, additions/deletions and the selected period's activity range. Charts are CSS-based and collapse cleanly on phone-sized screens.


## Project inventory

Step 45 implements the Projects section as a responsive project inventory with pagination, text search and filters for ownership, visibility, activity, category and technology. Desktop layouts use compact project cards with metadata, while smaller screens stack headings, badges and tags to avoid horizontal scrolling.


## Project detail

Step 46 makes project inventory items selectable. The detail view shows metadata, commit activity timeline, technology evidence, project categories, project significance, user involvement and synchronisation state. The layout switches to single-column presentation on smaller screens.


## Technology views

Step 47 implements the Technologies section with a selectable technology list and detail pane. Each technology shows evidence level, project counts, first/latest observed use, timeline activity and representative projects. The layout collapses to a stacked list/detail presentation on tablets and phones.


## Project-type views

Step 48 implements the Project types section with a selectable category list. Category details show project count, observed activity, evolution over time and representative projects. The same responsive master/detail pattern as the Technologies view is reused for desktop, tablet and phone layouts.
