# UX filtering redesign – step 12.3: tertiary details on demand

## Goal

Keep technical, operational and corrective information available without letting it dominate the primary analysis flow.

## Changes

### Overview – synchronisation monitoring

`Analysis progress` is now collapsed by default. Its summary still exposes useful status at a glance:

- completed / total repositories;
- running count when non-zero;
- failed count when non-zero.

Opening the disclosure reveals queue/running/completed counts, current job details, recent errors and retry controls.

Loading and error states remain visible rather than being hidden.

### Project detail – assessment rationale

Project significance and user involvement level/score stay visible. Their detailed rationale is now behind an `Assessment rationale` disclosure in each assessment card.

### Project detail – AI profile correction

The correction control is now an `Advanced: AI profile correction` disclosure, collapsed by default. This follows the same hierarchy already introduced for Technology corrections.

### Project detail – synchronisation and refresh

Repository synchronisation details and the manual refresh action are now collapsed behind a summary that still shows the current synchronisation status.

## Intentionally still visible

The following were not hidden:

- Activity changed-line coverage warnings;
- project metadata;
- project timeline;
- technologies and project categories;
- contributor summary;
- significance/involvement level and score;
- synchronisation loading/error states.

The goal is progressive disclosure, not removal of useful information.
