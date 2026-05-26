# §15 · Implementation Plan (Claude Code roadmap)

> **How to use this section:** each phase is a self-contained delivery with explicit goals, tasks, deliverables, and acceptance criteria. Claude Code (or a human implementer) walks the phases in order, opening one PR per task block, with the phase's acceptance criteria as the merge gate.

Phases are sized to be **completable in 1–3 days each by a focused implementer**. Phase 1 sets the foundation; everything after layers on top.

## Phase 1 · Foundation

**Goal:** repo exists, the dev loop works, CI catches the basics.

### Tasks

1. **Repo bootstrap**
   - Initialize Git, push to GitHub.
   - `.gitignore`, `.gitattributes`, `.editorconfig`, `.nvmrc` (Node 20.18), `LICENSE`.
   - `pnpm init` at root; `pnpm-workspace.yaml`.
   - Folder skeleton from [§2.2](./02-architecture.md).
2. **Next.js scaffold**
   - `pnpm create next-app apps/web --typescript --tailwind --app --no-src-dir-prompt` then move into `src/`.
   - Pin Next ≥ 15.x, React 19, TypeScript 5.5+.
   - Strict `tsconfig.json` per [§9.8](./09-development-workflow.md).
3. **Tooling**
   - ESLint flat config + plugins (typescript-eslint strict, jsx-a11y, tailwindcss, import, react, next).
   - Prettier + `prettier-plugin-tailwindcss`.
   - Husky + `lint-staged` + `commitlint` + `gitleaks` pre-commit.
   - Vitest configured; one trivial passing test.
   - Playwright configured with `chromium-mobile` + `webkit-mobile`; one passing smoke test.
4. **CI**
   - `.github/workflows/ci.yml` (lint + typecheck + test).
   - `.github/workflows/e2e.yml` (Playwright on preview URL).
   - `.github/workflows/deploy-preview.yml` (Vercel preview on PR).
   - `dependabot.yml` or `renovate.json` (pick one; default Renovate).
5. **Env**
   - `lib/env.ts` with Zod-validated env, `.env.example` checked in.
6. **Docs**
   - `README.md` per [§8.15](./08-github-repo.md).
   - `CONTRIBUTING.md`, `SECURITY.md`, `CODEOWNERS`.
   - `docs/PRD/` copied in (this document).
   - `docs/ADR/0001-app-router.md`, `0002-zustand.md`, `0003-themes-as-css.md`.

### Deliverables

- A repo that runs `pnpm dev` cleanly.
- A preview URL on PR.
- All CI jobs green on an empty `index` page.

### Acceptance criteria

- [x] `pnpm install && pnpm dev` works from a fresh clone in < 2 min. _(install: 0.7 s, dev ready: ~1.5 s)_
- [ ] CI runs in < 5 min and all jobs green. ⚠️ _blocked — needs GitHub remote + first push_
- [ ] Preview URL deployed for a no-op PR. ⚠️ _blocked — needs Vercel project + GitHub secrets (VERCEL_TOKEN, VERCEL_ORG_ID, VERCEL_PROJECT_ID)_
- [x] Husky pre-commit blocks a bad commit message. _("bad message" rejected: subject/type may not be empty)_
- [x] `.env.example` matches `lib/env.ts` schema (a CI check enforces this). _(scripts/check-env.mjs: all 6 keys present)_

### Validation steps

1. Fresh clone in a temp directory. Run `pnpm install && pnpm dev`. Open `localhost:3000`.
2. Open a no-op PR. Confirm CI green + Vercel preview URL.
3. Try committing with a bad message (`asdf`). Husky rejects.

---

## Phase 2 · Core architecture

**Goal:** the skeleton has shape — routing, layouts, i18n, theme, state — and nothing more. The page renders blank-but-correct.

### Tasks

1. **App Router skeleton**
   - Route groups: `(marketing)`, `(shop)`, `(checkout)`, `(account)`.
   - `[locale]` segment with `vi` / `en`.
   - `layout.tsx`, `error.tsx`, `not-found.tsx`, `loading.tsx` per group.
   - Middleware: locale redirect + CSP nonce + theme cookie read.
2. **i18n**
   - `next-intl` installed.
   - `lib/i18n/` with message catalogs (start with: `common`, `nav`, `footer`).
   - `t()` typed by message keys.
   - Language switch in `NavBar`.
3. **Theme system**
   - `styles/tokens.css` (raw + semantic).
   - `styles/theme-royal.css`, `theme-midnight.css`, `theme-ocean.css` ported from prototype.
   - `<link disabled>` mechanism preserved.
   - Theme bootstrap inline script + cookie + localStorage sync.
4. **State**
   - `features/cart/store.ts` (Zustand + persist).
   - `lib/utils/cn.ts`.
   - `lib/fetcher.ts` skeleton.
5. **API boundary**
   - `lib/api/products/get-products.ts` reading from `content/products/*.json`.
   - Zod schemas (`schema.ts`).
   - `content/products/*.json` ported from prototype's product data.
6. **Primitives**
   - `ui/primitives/Button`, `Input`, `Tag`, `Badge`, `Container`, `Stack`, `Grid` (per [§7.6](./07-ui-ux-standards.md)).
7. **Patterns**
   - `NavBar`, `Footer`, `SectionHead`, `EmptyState`.

### Deliverables

- All public routes render a placeholder shell.
- VI and EN locales swap correctly.
- Three themes swap correctly with no flash.
- Sample primitives + patterns visible at `/dev/playground` (gated `process.env.NODE_ENV !== 'production'`).

### Acceptance criteria

- [x] `/vi`, `/en` both render with locale-correct strings.
- [ ] Theme switch is < 250 ms with no CLS.
- [ ] No theme flash on hard reload (cookie-driven SSR).
- [ ] Cart store survives reload (`localStorage` persistence).
- [ ] No client component without a justifying comment.
- [ ] Lighthouse on `/vi`: Perf ≥ 95 (it's an empty page; this is the floor).

### Validation steps

1. Hard-reload each route in each theme; observe zero theme flash.
2. Toggle locale; observe URL + content change.
3. Add cart item from the playground; reload; cart persists.
4. Lighthouse mobile run on `/vi`.

---

## Phase 3 · Design system

**Goal:** every primitive and pattern from [§7](./07-ui-ux-standards.md) exists, theme-aware, tested.

### Tasks

1. **Primitives**
   - Complete the rest of the list from [§2.5](./02-architecture.md): `Modal`, `Drawer`, `Toast`, `Popover`, `Tooltip`, `Tabs`, `Accordion`, `Price`, `Stars`, `Eyebrow`, `Stamp`, `Quote`, `Skeleton`, `Spinner`, `FormField` group, `Select`, `Checkbox`, `Radio`, `Toggle`.
   - Each built on Radix where applicable; theme-token CSS.
   - Each with a Vitest component test.
2. **Patterns**
   - `Breadcrumb`, `ProductCard`, `PriceBlock`, `BackLink`, `Modal` composition.
3. **Icons**
   - Install Lucide; create `ui/icon/` re-export with size + a11y defaults.
4. **Theme switcher**
   - Port from prototype, refactor as a feature (`features/theme-switcher/`).
   - Cookie + localStorage + DOM mutation; no React re-render cascade.
5. **Fonts**
   - Self-host Lora / Be Vietnam Pro / JetBrains Mono via `next/font/local`.
   - Subset to required ranges.
   - Preload above-fold faces.
6. **Forms**
   - `react-hook-form` + Zod resolver wired into `FormField`.
   - Example contact-form skeleton (no submit yet).

### Deliverables

- `/dev/playground` showcases every primitive and pattern in all three themes + both locales.
- Component tests cover keyboard + a11y for every interactive primitive.

### Acceptance criteria

- [ ] axe sweep on `/dev/playground` returns 0 serious/critical in every theme.
- [ ] Visual regression baseline captured.
- [ ] All primitives are `forwardRef`-compatible.
- [ ] Theme switch on `/dev/playground` produces 0 layout shift.
- [ ] Font weight (gzip) ≤ 60 kB.

### Validation steps

1. Open `/dev/playground`. Tab through every component. Focus visible everywhere.
2. Switch themes. Spot-check for color regressions.
3. Run `pnpm test:a11y` and `pnpm test:visual`.

---

## Phase 4 · Real screens

**Goal:** every public route from [§2.7](./02-architecture.md) is rendered and content-complete. Still no backend.

### Tasks

1. **Home**
   - Hero + curated grid + values + story teaser + newsletter.
   - LCP element identified, `priority` set.
2. **Shop**
   - Grid + filters (URL state via `nuqs`).
   - Empty state for "no results".
3. **PDP**
   - Gallery, info column, qty + add-to-cart, tabs (story / spec / reviews), related products.
   - Add-to-cart wires to Zustand store.
   - JSON-LD `Product` schema.
4. **Cart drawer**
   - Slide-in `Drawer`, line items, total, "checkout" CTA.
5. **Checkout**
   - Three-step (ship / pay / review).
   - Form validation per step.
   - Dev-only "place order" → `/checkout/confirm/[localId]`.
6. **Confirm**
   - Order summary from `localStorage`; "back to shop" CTA.
7. **Story**
   - Long-form MDX page; reduced-motion respected.
8. **Support**
   - Topic landing + FAQ accordion.
9. **Contact**
   - Form → `/api/contact` (logs to KV).
10. **Account (stub)**
    - Reserved route renders "coming soon" + redirect-on-auth shape.

### Deliverables

- Every route from [§2.7](./02-architecture.md) live, content-complete, fully bilingual.

### Acceptance criteria

- [ ] Every page has unique `<title>` and meta description.
- [ ] JSON-LD validates on PDP, Home, Story, FAQ.
- [ ] Contact form submits to `/api/contact`; rate limit + honeypot + Zod pass.
- [ ] Cart flow works end-to-end (add → view → checkout → confirm) in dev.
- [ ] All routes Lighthouse mobile ≥ 90.
- [ ] axe 0 serious/critical on every route.

### Validation steps

1. Walk the conversion flow on a real phone in both locales, both extreme themes.
2. Submit contact form 6× rapidly — 429 returned after 5th.
3. View source on each route; confirm SSR HTML is complete.

---

## Phase 5 · Performance hardening

**Goal:** every page comfortably under budget on a Moto G Power / slow 4G.

### Tasks

1. **Image pipeline**
   - All images via `next/image`.
   - LQIP / blur placeholder for hero + PDP gallery.
   - `sizes` accurate; `priority` only on LCP.
2. **Bundle gates**
   - `size-limit` configured per route.
   - PR comment with bundle delta.
3. **Streaming**
   - Suspense boundaries around "related products" and "reviews" on PDP.
   - Skeletons match real layout.
4. **Lazy load**
   - Framer Motion dynamic-imported behind reduced-motion check.
   - Theme switcher dynamic-imported.
   - Gallery carousel dynamic-imported.
5. **Caching**
   - `revalidate` set per route per [§3.3](./03-performance.md).
   - `revalidateTag` infrastructure ready (webhook handler stub).
6. **Lighthouse CI**
   - Workflow runs against preview; thresholds enforced.

### Deliverables

- Lighthouse CI passing budgets on every public route.
- Bundle size dashboard.

### Acceptance criteria

- [ ] LCP P75 (Lighthouse mobile) ≤ 2.0 s on home / PDP / shop.
- [ ] INP from web-vitals capture ≤ 150 ms.
- [ ] CLS ≤ 0.05.
- [ ] Route JS (gzip) ≤ 90 kB.
- [ ] Total page weight (mobile, home) ≤ 350 kB.

### Validation steps

1. Lighthouse CI report shows green budgets.
2. Real device test: Moto G class on a throttled hotspot.
3. Bundle analyzer audit — top 10 chunks reviewed.

---

## Phase 6 · Security hardening

**Goal:** site is safe to ship; no easy wins for an attacker.

### Tasks

1. **CSP**
   - Nonce-based, strict, per [§4.2](./04-security.md).
   - Reports to Sentry.
2. **Headers**
   - HSTS, COOP, COEP, Permissions-Policy, Referrer-Policy.
   - Snapshot test in Playwright.
3. **Forms**
   - Turnstile + honeypot on contact + newsletter.
   - Double-submit CSRF token.
4. **Dependency**
   - `pnpm audit` gate.
   - Postinstall script allow-list.
   - SBOM generated per release.
5. **Sourcemaps**
   - Uploaded to Sentry, stripped from public.
6. **Sentry**
   - DSN configured (server + client).
   - `beforeSend` scrubs PII.
   - Releases tagged with Git SHA.

### Deliverables

- Mozilla Observatory: A or A+.
- securityheaders.com: A.
- All forms guarded.

### Acceptance criteria

- [ ] No `unsafe-inline` / `unsafe-eval` in production CSP.
- [ ] HSTS submitted to preload list (or queued).
- [ ] `pnpm audit --prod` clean.
- [ ] No secrets in any committed file (gitleaks pass).
- [ ] Production sourcemaps return 404.

### Validation steps

1. `curl -I https://staging.cocoshowroom.vn` — verify every header.
2. Mozilla Observatory + securityheaders.com scans pass.
3. Probe contact form with curl bypassing JS — server rejects without CSRF token.

---

## Phase 7 · Testing & quality

**Goal:** the test suite is the safety net; every release passes.

### Tasks

1. **Unit + integration**
   - Cover `lib/`, `features/*/lib/`, primitive components, Zustand stores.
   - 80% line coverage on covered areas.
2. **E2E**
   - All flows per [§10.5](./10-testing.md).
   - Sharded across 4 workers.
3. **Accessibility**
   - axe sweep on every public route in every theme + locale.
   - Keyboard-only walkthrough script.
4. **Visual**
   - Baselines captured for every route × theme × viewport.
   - PR comment on diff.
5. **Performance**
   - Lighthouse CI thresholds locked.
   - Web-vitals capture in Playwright; uploaded.
6. **Cross-browser**
   - Playwright projects: chromium-mobile, webkit-mobile, chromium-desktop, webkit-desktop, firefox-desktop.

### Deliverables

- Green CI on every PR.
- Coverage report on Codecov.
- Playwright HTML report archived.

### Acceptance criteria

- [ ] Full CI < 12 min wall time.
- [ ] Coverage on `lib/` ≥ 80%.
- [ ] axe 0 serious/critical on every public route.
- [ ] Lighthouse CI thresholds enforced.

### Validation steps

1. Force a failing test; confirm CI blocks the merge.
2. Break a contrast rule; confirm axe catches it.
3. Add 20 kB of JS; confirm bundle gate blocks.

---

## Phase 8 · Production readiness

**Goal:** ready to flip DNS to production. All operations in place.

### Tasks

1. **Monitoring**
   - Sentry production environment.
   - Vitals → ClickHouse pipeline.
   - Plausible (or PostHog) configured + consent-gated.
   - Checkly synthetic from Singapore PoP every 5 min.
   - Grafana dashboards live.
2. **Alerts**
   - Pagerduty (or Better Stack) configured per [§11.7](./11-monitoring.md).
   - Slack #frontend-alerts.
   - On-call rotation (or solo schedule).
3. **Runbooks**
   - `docs/runbooks/` covers the 5 most likely alerts.
4. **Deploy**
   - Production Vercel project.
   - Domain (apex + www).
   - SSL verified.
   - HSTS preload submitted.
5. **SEO**
   - Sitemap submitted to Google Search Console.
   - Robots verified.
   - Schema validates.
6. **Legal**
   - Privacy policy (VI + EN).
   - Terms of service (VI + EN).
   - Cookie consent banner.
   - Business registration footer.
7. **Launch checklist** (see [appendix](./appendix-checklists.md))

### Deliverables

- A green production site at `cocoshowroom.vn`.

### Acceptance criteria

- [ ] Lighthouse on prod ≥ 90 perf, 100 a11y/bp/seo.
- [ ] Synthetic green for 24 hrs continuous.
- [ ] No Sentry errors in 24-hr soak.
- [ ] DNS verified, SSL A+, HSTS active.
- [ ] Sitemap indexed (≥ 90% within 14 days post-launch).
- [ ] All launch-checklist items signed off.

### Validation steps

1. Walk the launch checklist.
2. 24-hr soak with synthetic traffic.
3. Manual smoke test on real Android + real iPhone + Cốc Cốc.
4. Submit sitemap; verify in GSC.

---

## Phase ordering and dependencies

```
Phase 1 ──▶ Phase 2 ──▶ Phase 3 ──▶ Phase 4
                              │
                              ▼
                      Phase 5 ──▶ Phase 6 ──▶ Phase 7 ──▶ Phase 8
                       (parallel with 4 once primitives exist)
```

Phases 5 (perf) and 6 (security) can start once Phase 3 (design system) is green; they don't need every screen to be finished.

## Estimated calendar

For a single senior frontend engineer working full-time:

| Phase                     | Working days                               |
| ------------------------- | ------------------------------------------ |
| 1 — Foundation            | 2                                          |
| 2 — Core architecture     | 4                                          |
| 3 — Design system         | 6                                          |
| 4 — Real screens          | 10                                         |
| 5 — Performance hardening | 4                                          |
| 6 — Security hardening    | 3                                          |
| 7 — Testing               | 4                                          |
| 8 — Production readiness  | 3                                          |
| **Total**                 | **~36 working days (~7–8 calendar weeks)** |

With a pair (frontend + design support), shave ~20%.

## Hand-off to Claude Code

The instruction set for Claude Code:

> Read `PRD/README.md`, then walk Phase 1 in `PRD/15-implementation-plan.md`. Open one PR per task block in the phase. Use `PRD/` as the source of truth for all decisions; refer to [§2 Architecture](./02-architecture.md) for module boundaries, [§7 UI/UX](./07-ui-ux-standards.md) for component contracts, [§9](./09-development-workflow.md) for commit conventions. Block on a clarifying question only when the PRD genuinely contradicts itself or omits a needed detail; otherwise proceed.

Claude Code should also keep `docs/ADR/` current — any decision that deviates from the PRD gets an ADR documenting the deviation + rationale.
