# UX filtering step 4.3 – compact Technology statistics

## Goal

Reduce the vertical and visual weight of pure statistics in the Technologies view so that **Over time** and **Projects matching this selection** remain the primary analysis content.

## Change

The previous six-card Technology statistics grid has been removed. Its information is now presented as a compact summary in the Technology header:

- matching project count
- first/latest observation range
- evidence item count
- independent evidence type count
- recent project count

The evidence score remains visible in the Technology header for now. Evidence/detail disclosure is handled by the following planned step rather than being mixed into this layout change.

## Resulting content order

1. Analysis filters
2. Technology summary with compact statistics
3. Over time
4. Projects matching this selection
5. Advanced correction

There is no longer a full-width KPI card grid between matching projects and Advanced.

## Responsive behaviour

On wider screens the summary facts form one wrapping line separated by subtle dots. On narrow screens they become a compact vertical list to avoid horizontal overflow.

## Behavioural impact

No query, filtering, timeline, project matching, privacy or correction semantics are changed in this step.
