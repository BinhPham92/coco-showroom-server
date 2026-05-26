# §10 · Testing Strategy

> **Pyramid:** lots of fast unit tests at the bottom, a layer of integration / component tests, a thin top of E2E. A11y and visual regression are orthogonal sweeps over the same surface.

## 10.1 Testing philosophy

- **Test behavior, not implementation.** A test that breaks when you rename a state variable is a bad test.
- **One test, one assertion of intent.** Avoid 12-step procedural tests.
- **Tests are documentation.** A reader should learn the component from its test.
- **The CI is the test suite.** If a check isn't in CI, it doesn't exist.

## 10.2 Tools

| Layer                  | Tool                                 | Why                                                 |
| ---------------------- | ------------------------------------ | --------------------------------------------------- |
| Unit                   | **Vitest**                           | Fast, ESM-native, Vite-style, Jest-compatible API   |
| Component              | **Vitest + React Testing Library**   | Test components in isolation; user-centric queries  |
| Integration            | **Vitest + MSW**                     | Mock the network at the service-worker layer        |
| E2E                    | **Playwright**                       | Real browser, real network, parallel, cross-browser |
| A11y                   | **@axe-core/playwright**             | Run axe against every key route                     |
| Visual                 | **Playwright screenshots + reg-cli** | Pixel diff on key states                            |
| Perf                   | **Playwright + web-vitals**          | Capture CWV in test                                 |
| Lighthouse             | **@lhci/cli**                        | Field metrics in CI                                 |
| Mutation (post-launch) | **Stryker**                          | Surface weak tests                                  |

**One toolchain (Playwright) drives E2E, a11y, visual, and perf** — fewer install surfaces, fewer config files.

## 10.3 Unit tests

Co-located with source:

```
features/catalog/
├── lib/ranking.ts
├── lib/ranking.spec.ts
├── components/ProductCard.tsx
└── components/ProductCard.spec.tsx
```

- Pure functions: 100% covered.
- React components: covered for keyboard interaction, conditional rendering, error states.
- Hooks: `@testing-library/react-hooks` patterns.
- Snapshots: **avoided** as the only assertion. Use snapshots for _structure_ (e.g., JSON-LD output), never for component render.

Threshold: **80% line coverage** for `lib/` and `features/*/lib/`. `ui/primitives/` covered by integration tests in Playwright.

## 10.4 Integration tests

For features that compose multiple units (cart store + persistence + component):

```tsx
// features/cart/__tests__/cart-flow.test.tsx
test("add to cart persists across reload", async () => {
  render(<TestApp />);
  await user.click(screen.getByRole("button", { name: /add to cart/i }));
  expect(screen.getByText(/1 item/i)).toBeVisible();

  // Reload simulation
  unmountComponent();
  render(<TestApp />);
  expect(screen.getByText(/1 item/i)).toBeVisible();
});
```

MSW mocks API calls during these tests. Reuse the same handlers as Storybook (when added).

## 10.5 E2E tests

Playwright, against the preview deploy in CI and against `localhost` in dev.

Test surface — minimum coverage:

| Flow                      | Test                                                                         |
| ------------------------- | ---------------------------------------------------------------------------- |
| Discovery                 | Home → Shop → Filter → PDP                                                   |
| Conversion                | PDP → Add to cart → Drawer open → Checkout shipping → payment → confirm      |
| Theme                     | Switch all three themes, verify no console error, no layout shift > 0.05 CLS |
| i18n                      | Toggle VI ↔ EN on home and PDP, verify text + canonical change               |
| Search                    | Open search popover, type, click a result                                    |
| Story                     | Read /story to bottom, verify reduced-motion path                            |
| Support                   | Toggle FAQ topic, expand a question                                          |
| Contact                   | Submit valid form; submit invalid form; honeypot detection                   |
| Account (when auth lands) | Sign-up / sign-in / sign-out                                                 |
| Empty cart                | Direct-navigate to /checkout — redirect to /cart                             |
| 404                       | Visit /nonsense — see localized 404                                          |

Sharded across 4 workers; full suite < 8 min in CI.

## 10.6 Accessibility testing

Two layers:

1. **Automated** (`pnpm test:a11y`) — `@axe-core/playwright` against every public route, in both locales, in all three themes. **Zero serious/critical** fails the build.
2. **Manual** (per release, for changed routes) — NVDA + Firefox, VoiceOver + Safari, TalkBack on a real Android.

Specific assertions in code:

- Every page has a `<main id="main">`.
- Every interactive element has an accessible name.
- Color contrast ratios pass (axe rule `color-contrast`).
- No `tabindex > 0`.
- No `aria-hidden="true"` on a focusable element.
- All headings are sequential.

## 10.7 Performance testing

Two layers:

1. **Lighthouse CI** against preview, on every PR. Thresholds in `lighthouserc.json`. Regressions fail the build.
2. **Playwright + `web-vitals` package** captures LCP / INP / CLS during a scripted user journey. Uploaded to ClickHouse for trend.

Sample lighthouserc config:

```jsonc
{
  "ci": {
    "collect": {
      "url": [
        "https://${PREVIEW}/vi",
        "https://${PREVIEW}/vi/shop",
        "https://${PREVIEW}/vi/product/dong-trung-sapa-grade-aa",
        "https://${PREVIEW}/vi/story",
      ],
      "numberOfRuns": 3,
      "settings": {
        "preset": "mobile",
        "throttling": { "cpuSlowdownMultiplier": 4 },
      },
    },
    "assert": {
      "assertions": {
        "categories:performance": ["error", { "minScore": 0.9 }],
        "categories:accessibility": ["error", { "minScore": 1.0 }],
        "categories:best-practices": ["error", { "minScore": 1.0 }],
        "categories:seo": ["error", { "minScore": 1.0 }],
        "first-contentful-paint": ["warn", { "maxNumericValue": 1800 }],
        "largest-contentful-paint": ["error", { "maxNumericValue": 2500 }],
        "cumulative-layout-shift": ["error", { "maxNumericValue": 0.05 }],
        "total-blocking-time": ["error", { "maxNumericValue": 200 }],
      },
    },
  },
}
```

## 10.8 Visual regression

Playwright captures key states (each route, each theme, two viewports — 375 + 1280) and `reg-cli` diffs against the `main` baseline.

- Tolerance: 0.1% per-pixel difference.
- Differences > tolerance: PR comment with side-by-side; reviewer approves or rejects.
- Baselines update on `main` merge.

Surface covered:

- Home, Shop, PDP, Story, Support, Contact, Cart drawer (open + empty), Modal, Toast.
- Three themes × two viewports × ~10 states = 60 snapshots. Manageable.

## 10.9 Mobile testing

- **Emulated** in CI: Pixel 5 + Galaxy S5 (Lighthouse Mobile preset), iPhone 13 + iPhone SE in Playwright projects.
- **Real device** smoke test per release: a Moto G Power + a 2019 Galaxy A on a slow-4G hotspot. Test the conversion flow end-to-end.

## 10.10 Cross-browser testing

Playwright projects:

- `chromium-mobile`
- `webkit-mobile` (iOS Safari surrogate)
- `chromium-desktop`
- `webkit-desktop`
- `firefox-desktop`

Cốc Cốc is Chromium → covered.

## 10.11 CI testing gates

| Gate                         | Severity | Blocks merge        |
| ---------------------------- | -------- | ------------------- |
| ESLint errors                | error    | yes                 |
| Prettier diff                | error    | yes                 |
| TypeScript errors            | error    | yes                 |
| Unit test failure            | error    | yes                 |
| Integration test failure     | error    | yes                 |
| E2E failure (any project)    | error    | yes                 |
| axe serious/critical         | error    | yes                 |
| axe moderate                 | warn     | no (logged)         |
| Lighthouse regression > 5pt  | error    | yes                 |
| Lighthouse score below floor | error    | yes                 |
| Bundle size > hard limit     | error    | yes                 |
| Bundle delta > +5 kB / route | warn     | no (comment)        |
| Visual diff (after review)   | warn     | no (manual approve) |
| Security audit (high+)       | error    | yes                 |
| Coverage drop > 2%           | warn     | no                  |

## 10.12 Test data

- Product data: `src/content/products/*.json` is the test fixture.
- API mocks (post-backend): MSW handlers in `tests/mocks/`.
- E2E uses **the actual content** — no separate "test" SKUs. Keeps fixtures honest.

## 10.13 What we do NOT test

- Third-party libraries (we trust their tests, we pin versions).
- Browser primitive behavior (we don't test that `<a>` navigates).
- CSS in unit tests (visual regression covers this).
- 100% line coverage as a goal (the cost curve goes vertical past 85%; we stop at "every meaningful branch").

## 10.14 Anti-patterns

- ❌ Testing implementation details (`expect(component.state.foo).toBe(...)`).
- ❌ Mocking everything; tests should exercise real code paths where possible.
- ❌ One giant `it.each` table.
- ❌ Network calls in unit tests.
- ❌ `setTimeout` waits in Playwright — use `expect(...).toBeVisible()`.
- ❌ Snapshots as the only assertion.

## 10.15 Reporting

- Coverage report uploaded to Codecov on every PR.
- Playwright HTML report attached as artifact, accessible from PR comment.
- Lighthouse reports archived for 30 days.
- Trends graphed in a Grafana dashboard (perf metrics from ClickHouse).
