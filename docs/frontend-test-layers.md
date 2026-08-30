# Frontend Test Layers

Step 76 defines five explicit frontend test layers while preserving the existing
`npm test` command as the complete frontend test gate.

| Layer | Directory | Purpose |
| --- | --- | --- |
| Component | `src/test-layers/component` | Isolated reusable component behavior |
| Feature/page | `src/test-layers/feature` | User-visible page and feature flows |
| Responsive | `src/test-layers/responsive` | Compact/mobile interaction behavior where practical in jsdom |
| API error state | `src/test-layers/api-error` | Accessible degraded/error UI for failed APIs |
| Privacy | `src/test-layers/privacy` | Private/public scope indicators and explicit privacy choices |

The existing broader `App.test.tsx` remains part of the complete suite. New
focused tests should normally be placed in the relevant layer directory.

## Run everything

```bash
cd frontend
npm test
```

or:

```bash
bash ./scripts/run-frontend-test-layer.sh all
```

## Run one layer

```bash
bash ./scripts/run-frontend-test-layer.sh component
bash ./scripts/run-frontend-test-layer.sh feature
bash ./scripts/run-frontend-test-layer.sh responsive
bash ./scripts/run-frontend-test-layer.sh api-error
bash ./scripts/run-frontend-test-layer.sh privacy
```

Equivalent npm commands are available as `test:component`, `test:feature`,
`test:responsive`, `test:api-error`, and `test:privacy`.

## Responsive testing

jsdom does not perform CSS layout, so responsive tests should focus on behavior
that can be verified reliably without a real browser: compact navigation state,
mobile menu open/close behavior, accessible controls and preservation of page
selection.

CSS breakpoint correctness remains covered by production build review and can be
expanded later with browser/E2E tests if the project introduces that test layer.

## API error states

Feature hooks and views must expose API failures through visible, accessible
error UI rather than silently failing. Tests should verify `role="alert"` or an
equivalent accessible status where appropriate.

## Privacy indicators

Privacy tests verify what the user can see before an action that may expose
private data. In particular, report export tests assert the *effective
server-side preview scope*, not merely the user's selected radio button.

## Maintenance contract

CI runs:

```bash
python3 scripts/check-frontend-test-layers.py
```

before lint/typecheck/test/build and fails if any required layer is empty or an
unknown test-layer directory appears.
