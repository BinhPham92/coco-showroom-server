# Appendix · Checklists

> Pull these out for the moments they're for. Each is a one-page artifact you can paste into a PR description, a release issue, or a post-mortem doc.

## A. Performance budget checklist (per PR)

- [ ] Route JS (gzip) ≤ 90 kB (CI gate)
- [ ] CSS total (gzip) ≤ 25 kB
- [ ] Image above the fold ≤ 200 kB
- [ ] Total page weight ≤ 500 kB (home, mobile)
- [ ] Lighthouse mobile perf ≥ 90 on every touched route
- [ ] LCP ≤ 2.5 s (lab, mobile)
- [ ] INP ≤ 200 ms
- [ ] CLS ≤ 0.05
- [ ] TBT ≤ 200 ms
- [ ] No new font face
- [ ] No new top-level dependency without size review
- [ ] Bundle delta comment: ≤ +5 kB / route
- [ ] No client component added without a comment justifying it
- [ ] Hero image preloaded with `priority`
- [ ] Skeletons match real layout (zero CLS)

## B. Security checklist (per release)

- [ ] CSP unchanged or tightened (no new `unsafe-*`)
- [ ] Headers snapshot test passes (HSTS, COOP, COEP, Permissions-Policy, X-Content-Type-Options)
- [ ] `pnpm audit --prod` returns 0 high/critical
- [ ] No new `dangerouslySetInnerHTML` without DOMPurify
- [ ] No new package without security review + license check
- [ ] All new forms have: Zod validation + Turnstile + honeypot + CSRF token
- [ ] All new env vars are in `lib/env.ts` schema
- [ ] No new third-party script without CSP `connect-src` review
- [ ] Sourcemaps uploaded to Sentry, stripped from public
- [ ] Gitleaks clean
- [ ] No secrets in CI logs
- [ ] Rate limits set on all new `/api/*` endpoints

## C. Accessibility checklist (per PR)

- [ ] All interactive elements reachable by Tab
- [ ] Focus visible (2px gold outline, 2px offset) on every focusable element
- [ ] `<label>` on every input (not placeholder-as-label)
- [ ] Image alts present (or `alt=""` if decorative)
- [ ] Headings sequential, single `<h1>`
- [ ] Contrast checked for any new color usage (token matrix updated)
- [ ] No `outline: none` without replacement
- [ ] No keyboard trap
- [ ] Motion respects `prefers-reduced-motion`
- [ ] axe sweep returns 0 serious/critical
- [ ] Touch targets ≥ 44×44 px
- [ ] Form errors use `role="alert"`
- [ ] `<html lang>` set correctly for the locale
- [ ] Foreign-language inline spans marked with `lang=`

## D. SEO checklist (per PR touching public routes)

- [ ] Unique `<title>` set via `generateMetadata`
- [ ] Description ≤ 160 chars
- [ ] OG image present (or default fallback)
- [ ] `canonical` correct + locale-prefixed
- [ ] `hreflang` pair (vi-VN, en-US, x-default)
- [ ] One `<h1>`, sequential headings
- [ ] Image `alt`s descriptive
- [ ] JSON-LD validates
- [ ] No accidental `noindex` on indexable routes
- [ ] Sitemap regenerates without errors
- [ ] No new URL pattern without breadcrumb + canonical

## E. PR checklist

- [ ] Title follows Conventional Commit shape
- [ ] One feature per PR
- [ ] Description follows PR template
- [ ] Linked issue
- [ ] Screenshots / recordings for visual changes
- [ ] Changesets entry included (if user-visible)
- [ ] Tests added or updated
- [ ] Manual click-through on a real phone done
- [ ] CI green
- [ ] Bundle delta acceptable
- [ ] Lighthouse delta acceptable
- [ ] Visual diffs reviewed
- [ ] Reviewer assigned per CODEOWNERS

## F. Launch readiness checklist

### Product

- [ ] All routes from [§2.7](./02-architecture.md) live
- [ ] Bilingual parity verified (VI + EN)
- [ ] All three themes work without regressions
- [ ] Conversion flow tested on real Android + real iPhone + Cốc Cốc
- [ ] All product data finalized in `content/products/`
- [ ] Story and FAQ content reviewed by founder + legal
- [ ] OG images render correctly when shared on Facebook, Zalo, Twitter, LinkedIn

### Engineering

- [ ] Lighthouse mobile ≥ 90 perf, 100 a11y/bp/seo on every public route
- [ ] LCP P75 ≤ 2.0 s (lab) on every public route
- [ ] axe 0 serious/critical
- [ ] Bundle within budget on every route
- [ ] All `TODO`s have linked issues
- [ ] No `console.log` in production
- [ ] Source maps uploaded to Sentry, not public
- [ ] Production CSP active (not report-only)
- [ ] HSTS submitted to preload list

### Operations

- [ ] Production Vercel project configured
- [ ] DNS verified (apex + www); SSL A+
- [ ] Sentry production env configured
- [ ] Vitals pipeline live
- [ ] Plausible / PostHog live + consent-gated
- [ ] Checkly synthetic running from SG PoP
- [ ] Grafana dashboards published
- [ ] Pagerduty / Better Stack alerts configured
- [ ] Slack #frontend-alerts and #deploys live
- [ ] On-call rotation documented (or solo schedule)
- [ ] Runbooks for top 5 alerts written
- [ ] Rollback rehearsed end-to-end

### Legal & compliance

- [ ] Privacy policy published (VI + EN)
- [ ] Terms of service published (VI + EN)
- [ ] Cookie consent banner active
- [ ] Business registration number in footer
- [ ] Contact email + phone published
- [ ] `security@cocoshowroom.vn` mailbox monitored
- [ ] `dataRequests@cocoshowroom.vn` mailbox monitored
- [ ] SPF + DKIM + DMARC published

### SEO

- [ ] Sitemap submitted to GSC (VI + EN)
- [ ] Robots verified
- [ ] All JSON-LD validates
- [ ] hreflang matrix audited
- [ ] Search Console & Bing Webmaster verified
- [ ] First-week ranking targets agreed (post-launch hot list of 5 queries)

### Post-launch (T+24h, T+7d)

- [ ] T+24h: synthetic green for 24 hrs, no Sentry P0
- [ ] T+24h: real-user CWV from Vercel matches lab
- [ ] T+7d: GSC indexed count ≥ 50% of pages
- [ ] T+7d: First-week post-mortem (anything surprised us?)
- [ ] T+30d: GSC indexed ≥ 90%

## G. Incident response (mid-incident)

- [ ] On-call paged, response acknowledged within 5 min
- [ ] Severity assessed (P0–P3)
- [ ] If P0/P1: status page updated (or tweet)
- [ ] Slack #incident-active opened with link in #frontend-alerts
- [ ] Roll back considered within 15 min (if recent deploy)
- [ ] Sentry, Vercel logs, Checkly correlated
- [ ] Mitigation deployed or rollback executed
- [ ] All-clear in #incident-active
- [ ] Post-mortem doc opened in `docs/postmortems/` within 24 hrs
- [ ] Action items added to backlog with labels

## H. Recommended npm packages (locked list)

The "yes" list — packages we've evaluated and approved. Anything outside requires an ADR.

### Runtime

- `next` (≥ 15.0)
- `react`, `react-dom` (≥ 19.0)
- `zod` (^3.x)
- `zustand` (^4.x)
- `@tanstack/react-query` (^5.x, when backend lands)
- `react-hook-form` (^7.x)
- `@hookform/resolvers` (^3.x)
- `next-intl` (^3.x)
- `clsx`, `tailwind-merge`
- `class-variance-authority`
- `nuqs` (^2.x)
- `@radix-ui/react-*` (latest per primitive)
- `@radix-ui/react-slot`
- `lucide-react`
- `framer-motion` (^11.x, lazy)
- `pino` (server logs)
- `@sentry/nextjs`
- `web-vitals` (^4.x)
- `dompurify` (when reviews land)
- `schema-dts` (typed JSON-LD)
- `date-fns` + locale `vi`
- `cookie` (lightweight cookie parser)

### Dev / build

- `typescript` (^5.5)
- `eslint` (^9.x), flat config
- `@typescript-eslint/*` (^8.x)
- `eslint-plugin-react`, `eslint-plugin-react-hooks`
- `eslint-plugin-jsx-a11y`
- `eslint-plugin-import`
- `eslint-plugin-tailwindcss`
- `eslint-config-next`
- `prettier` + `prettier-plugin-tailwindcss`
- `husky` + `lint-staged` + `@commitlint/cli`
- `gitleaks`
- `vitest` + `@testing-library/react` + `@testing-library/user-event`
- `@playwright/test`
- `@axe-core/playwright`
- `@lhci/cli`
- `@next/bundle-analyzer`
- `size-limit`
- `msw` (when mocking the backend)
- `@changesets/cli`
- `pnpm` (package manager)
- `tsx` (for scripts)

### Avoided

- ❌ `lodash` (use built-ins)
- ❌ `moment` / `dayjs` past simple cases (`date-fns` only)
- ❌ `axios` (use `fetch`)
- ❌ `redux` / `redux-toolkit` (Zustand suffices)
- ❌ `styled-components` / `emotion` (Tailwind + tokens)
- ❌ `material-ui` / `chakra` (Radix + own skin)
- ❌ `react-router` (App Router is the router)
- ❌ `swr` (TanStack Query if needed)

## I. Anti-patterns reference card

Refuse on review:

- ❌ `<div onClick>` for action — use `<button>`
- ❌ `useEffect` for fetching on mount in a Server-eligible component
- ❌ `process.env.X` outside `lib/env.ts`
- ❌ `fetch()` outside `lib/fetcher.ts` / `lib/api/*`
- ❌ `<img>` tag (use `next/image`)
- ❌ Inline styles for layout
- ❌ Magic numbers / one-off colors
- ❌ `any` type
- ❌ `// @ts-ignore`
- ❌ Snapshot-only tests
- ❌ Spinners on data-driven routes (use skeletons)
- ❌ `tabindex` > 0
- ❌ `outline: none` without replacement
- ❌ Color as only state indicator
- ❌ Placeholder-as-label
- ❌ `setTimeout` waits in Playwright
- ❌ Polling for inventory
- ❌ React Context for hot-changing values
