# Export Test Coverage

Step 68 hardens the report/export privacy boundary with explicit automated tests.

## Privacy boundary

`ReportExportPrivacyBoundaryTest` verifies:

1. a `PUBLIC_ONLY` report excludes private repositories from both aggregates and
   project detail and never exposes a real private repository name through the
   masking path,
2. `PUBLIC_PLUS_PRIVATE_AGGREGATES` can include private aggregate data but cannot
   expose private project detail and uses generic repository labels when names
   are hidden,
3. `FULL_PRIVATE_DETAIL` only accepts private repositories that are explicitly
   `includedInAnalysis`; private repositories excluded from analysis remain
   outside both aggregate and detail sets.

The same `ReportPrivacyPolicy` is used by `CanonicalReportService`, so these
tests cover the policy used by real report construction rather than a duplicate
test-only rule.

## Markdown/PDF parity

`MarkdownPdfCoreContentParityTest` renders one `CanonicalReport` through both
renderers and extracts text from the generated PDF. It asserts that core report
content is present in both formats, including:

- summary,
- data coverage,
- project categories,
- technology analysis,
- activity,
- significant projects,
- methodology.

The PDF test also verifies that privacy markings remain visible in the rendered
document.

This complements the existing report-type, preview and canonical-model tests.
