# UX filtering step 11.1 – grouped primary navigation

## Goal

Make the primary navigation reflect the mental model introduced by the shared analysis scope instead of presenting every destination as one flat list.

## Navigation structure

The authenticated sidebar is now grouped as:

- **Overview**
- **Explore**
  - Activity
  - Projects
  - Technologies
  - Project types
  - Contributions
- **Insights**
  - AI insights
  - Reports
- **Settings**
  - Privacy/data sources
  - Account

Overview remains a standalone starting point. Explore contains the views that share analysis context, Insights contains derived/output-oriented views, and Settings contains account/data configuration.

## Behaviour

This step changes information architecture only:

- destination names are unchanged;
- active-section behaviour is unchanged;
- project-detail handling is unchanged;
- `AnalysisScope` query parameters continue to survive Explore navigation;
- Timeline remains absent because its functionality has already been consolidated into Activity.

## Responsive behaviour

The navigation area can scroll independently when viewport height is limited. The footer remains visible as a separate non-shrinking area, avoiding loss of Settings destinations or sign-out controls on compact screens.

## Regression coverage

Authenticated shell tests now verify that the Explore, Insights and Settings group labels are rendered while the existing destination buttons remain available.
